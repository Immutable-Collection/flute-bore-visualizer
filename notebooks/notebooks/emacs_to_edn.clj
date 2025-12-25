(ns emacs-to-edn
  (:require
   [clojure.string :as str]))

(defn valid-prefix?
  [s]
  (boolean (re-find #"^(\|+ |\*+ )" s)))

(defn initially-cleaning
  "Remove all except the org headings and table data from the data representing the imported org file."
  [data]
  (filter
    valid-prefix?
    (str/split-lines data)))

(defn count-*
  [s]
  (get (frequencies s) \* 0))

(defn is-table-header?
  [s]
  (boolean (re-find #"^\|\s*([A-Za-z]+)" s)))

(defn is-table-content?
  [s]
  (boolean (re-find #"^\|\s*(-?\d+(?:\.\d+)?)" s)))

(defn is-org-header?
  [s]
  (str/starts-with? s "*"))

(defn get-row-items
  [s]
  (map str/trim (rest (str/split s #"\|"))))

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

(defn process-content
  [data]
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

(defn org-to-edn
  [org-path]
  (-> org-path
      slurp
      initially-cleaning
      process-content
      org-processed-list-to-edn
      post-proccess-edn))


