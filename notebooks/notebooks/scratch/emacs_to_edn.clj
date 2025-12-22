(ns scratch.emacs-to-edn
  (:require
    [clojure.string :as str]
    [schema :refer [flute]]
    [emacs-to-edn :refer [valid-prefix?
                          is-table-header?
                          initially-cleaning
                          convert-title-to-keyword
                          process-table-header
                          process-table-row
                          org-processed-list-to-edn
                          post-proccess-edn
                          process-content

                          count-*
                          is-org-header?
                          is-table-content?]]
    [malli.core :as m]))

(def lot-dcm615-file-path "../../flute-data/models/lot-dcm615.org")

(def lot-dcm615-content (slurp lot-dcm615-file-path))

(def initially-cleaned-content
  (filter
    valid-prefix?
    (str/split-lines lot-dcm615-content)))

initially-cleaned-content

(defn process-headings
  [])

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

(def example-flute-data (org-processed-list-to-edn processed-content))

example-flute-data

(def post-proccessed_example (post-proccess-edn example-flute-data))

post-proccessed_example

(m/validate flute post-proccessed_example)

(:errors (m/explain flute post-proccessed_example))

(-> "../../flute-data/models/lot-dcm615.org"
    slurp
    initially-cleaning
    process-content
    org-processed-list-to-edn
    post-proccess-edn)
