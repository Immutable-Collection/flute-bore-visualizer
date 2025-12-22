(ns scratch.demo
  (:require
    [clojure.edn :as edn]
    [flute :refer [export-png-file export-stl-file polygon-rotator
                   render-code-model rudall-and-carte lot-model cut-view]]
    [scad-clj.scad :refer [write-scad] :as scad]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.tableplot.v1.plotly :as plotly]
    [tablecloth.api :as tc]))

(comment
  (scicloj.clay.v2.api/make! 
  {:source-path "notebooks/scratch/demo.clj"}))
