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

(def initially-cleaned-content
  (filter
    ;; TODO for finding heading use a regex with pattern *** followed by a space

    #(or
       (str/starts-with? % "|")
       (str/starts-with? % "*"))

    (str/split-lines lot-dcm615-content)))

initially-cleaned-content

;; # processing contents
#_(map
 #()
 initially-cleaned-content)
