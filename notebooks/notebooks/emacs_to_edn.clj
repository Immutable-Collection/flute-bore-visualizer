(ns emacs-to-edn
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.tableplot.v1.plotly :as plotly]
    [tablecloth.api :as tc]
    [malli.core :as m]))

(def lot-dcm615-file-path "../../flute-data/models/lot-dcm615.org")

(def lot-dcm615-content (slurp lot-dcm615-file-path))

(defn valid-prefix?
  [s]
  (boolean (re-find #"^(\|+ |\*+ )" s)))

(def initially-cleaned-content
  (filter
    valid-prefix?
    (str/split-lines lot-dcm615-content)))

(defn initially-cleaning 
  "Remove all except the org headings and table data from the data representing the imported org file."
  [data]
  (filter
   valid-prefix?
   (str/split-lines data)))

initially-cleaned-content

(defn count-*
  [s]
  (get (frequencies s) \* 0))

(defn is-table-header?
  [s]
  (boolean (re-find #"^\|\s*([A-Za-z]+)" s)))

(defn is-table-content?
  [s]
  (boolean (re-find #"^\|\s*(\d+(?:\.\d+)?)" s)))

(defn is-org-header?
  [s]
  (str/starts-with? s "*"))

(defn get-row-items
  [s]
  (map str/trim (rest (str/split s #"\|"))))

(defn process-headings
  [])

(defn convert-title-to-keyword
  [header]
  (-> header
      str/lower-case
      (str/replace " " "-")
      keyword))

(defn process-table-header
  [s]
  (map convert-title-to-keyword (get-row-items s)))

(defn process-table-row
  [s]
  (map Float/parseFloat (get-row-items s)))

(defn process-content [data]
  (map
    #(cond
       (is-table-content? %) {:type :table-content
                              :content %
                              :data (process-table-row %)}
       (is-table-header?  %) {:type :table-heading
                              :content %
                              :data (process-table-header %)}
       (is-org-header?    %) {:type :heading
                              :level (count-* %)
                              :content (convert-title-to-keyword
                                         (-> %
                                             (str/replace "* " "")
                                             (str/replace "*" "")))}
       :else nil)
    data))

;; # processing contents
(def processed-content
  (map
    #(cond
       (is-table-content? %) {:type :table-content
                              :content %
                              :data (process-table-row %)}
       (is-table-header?  %) {:type :table-heading
                              :content %
                              :data (process-table-header %)}
       (is-org-header?    %) {:type :heading
                              :level (count-* %)
                              :content (convert-title-to-keyword
                                         (-> %
                                             (str/replace "* " "")
                                             (str/replace "*" "")))}
       :else nil)
    initially-cleaned-content))

processed-content

(filter #(= :heading (:type %)) processed-content)

;; TODO table structure & table content

;; ### creating map structure based on the heading levels

(defn add-org-row-to-map
  [row result-map path header]
  (cond
    (= :heading (:type row))
    (assoc-in result-map path {})
    (= :table-content (:type row))
    ;; TODO re-write with specter!
    ;; :data should be removed
    (assoc-in result-map path (conj (into [] (let [current-data (get-in result-map path)] (if (map? current-data) [] current-data))) (zipmap header (:data row))))
    :else result-map))

(defn org-processed-list-to-edn
  [input-list]
  (loop [processing input-list
         acc {}
         path []
         level 0
         data-heading nil]
    (if (=  0 (count processing))
      acc
      (let [processing-item (first processing)
            item-level (:level processing-item)

            item-content (:content processing-item)
            new-path (cond
                       (nil? item-level) path
                       (> level item-level) (conj (into [] (-> path butlast butlast)) item-content)
                       (< level item-level) (conj path item-content)
                       (= level item-level) (conj (into [] (butlast path)) item-content)
                       :else path)
            new-level (cond
                        (nil? item-level) level
                        (> level item-level) item-level
                        (< level item-level) item-level
                        (= level item-level) item-level
                        :else level)

            heading (cond
                      (= :table-heading (:type processing-item)) (:data processing-item)
                      (= :table-content (:type processing-item)) data-heading
                      :else nil)]
        (recur (rest processing) (add-org-row-to-map processing-item  acc new-path heading) new-path new-level heading)))))

(defn post-proccess-edn
  [input]
  (let [main-key (first (keys input))
        data (first (vals input))]
    (dissoc (assoc data :model (name main-key)) main-key)))

(def example-flute-data (org-processed-list-to-edn processed-content))

example-flute-data

(def hole
  [:map
   [:position :float]
   [:diameter :float]
   ;; other data, lateral diameter, longtitudonal diameter, undercut etc
   ])

(def joint
  [:map
   [:bore-diameters    [:vector :map]]
   [:outside-diameters [:vector :map]]
   [:holes             [:vector #'hole]]])

(def flute
  [:map
   [:model :string]
   [:head-joint       #'joint]
   [:middle-joint     #'joint]
   [:right-hand-joint #'joint]
   [:foot-joint       #'joint]])

(def post-proccessed_example (post-proccess-edn example-flute-data))

post-proccessed_example

(m/validate flute post-proccessed_example)

(:errors (m/explain flute post-proccessed_example))

(defn org-to-edn [org-path]
(-> org-path
    slurp
    initially-cleaning
    process-content
    org-processed-list-to-edn
    post-proccess-edn))
    

(org-to-edn "../../flute-data/models/lot-dcm615.org")    