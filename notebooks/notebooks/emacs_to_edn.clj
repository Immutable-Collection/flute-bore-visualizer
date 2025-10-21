(ns emacs_to_edn
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.tableplot.v1.plotly :as plotly]
    [tablecloth.api :as tc]))

;; #reading a lot file

(def lot-dcm615-file-path "../../flute-data/models/lot-dcm615.org")

(def lot-dcm615-content (slurp lot-dcm615-file-path))

(defn valid-prefix? [s]
  (boolean (re-find #"^(\|+ |\*+ )" s)))

(def initially-cleaned-content
  (filter
    valid-prefix?
    (str/split-lines lot-dcm615-content)))

initially-cleaned-content

(defn count-* [s](get (frequencies s) \* 0))

(defn is-table-header? [s]
  (boolean (re-find #"^\|\s*([A-Za-z]+)" s)))

(defn is-table-content? [s]
  (boolean (re-find #"^\|\s*(\d+(?:\.\d+)?)" s)))

(defn is-org-header? [s]
  (str/starts-with? s "*"))

(defn get-row-items [s] 
(map str/trim (rest (str/split s #"\|"))))

(defn process-headings [])
(defn process-table-header [s]
(map str/lower-case (get-row-items s)))
(defn process-table-row [s]
(map Float/parseFloat (get-row-items s)))

;; # processing contents
(def processed-content (map-indexed
 #(cond 
    (is-table-content? %2) {:type :table-content :content %2 :row %1 :data (process-table-row %2)}
    (is-table-header?  %2) {:type :table-heading :content %2 :row %1 :data (process-table-header %2)} 
    (is-org-header?    %2) {:type :heading :level (count-* %2) :content %2 :row %1}
    :else nil)
 initially-cleaned-content))

processed-content


(filter #(= :table-heading (:type %)) processed-content)