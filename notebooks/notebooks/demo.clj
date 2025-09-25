(ns demo
  (:require
    [clojure.edn :as edn]
    [flute :refer [export-png-file export-stl-file render-code-model
                   rudall-and-carte]]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.tableplot.v1.plotly :as plotly]
    [tablecloth.api :as tc]))

;; TODO read from edn file
(def wood
  (-> "notebooks/data/wood.edn"
      slurp
      edn/read-string
      :data))

(def df (tc/dataset wood))

df

(-> df
    #_(tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/base {:=x :from-cap
                  :=y :bore-diameter
                  :=width 1200
                  :=height 400})
    (plotly/layer-line {:=mark-color "purple"}))

(def flute
  (-> "notebooks/Rudall and Carte.edn"
      slurp
      edn/read-string))

(def left-hand-section (-> flute :flute :bore :left-hand-section :measurements))

(tc/dataset left-hand-section)

(def right-hand-section (-> flute :flute :bore :right-hand-section :measurements))

(tc/dataset right-hand-section)

(def foot-section (-> flute :flute :bore :foot-section :measurements))

(tc/dataset foot-section)

;; (def flute-data (concat right-hand-section left-hand-section foot-section))


;; TODO these should read from the data files
(def inside-lengths
  [-0.01, 3, 4, 6, 8, 11, 13, 17, 29, 35, 42, 47, 51, 60, 65, 72, 79, 84, 96, 102, 107, 112, 117, 123, 130, 139, 145, 151, 155, 161, 165, 173, 177, 183, 187, 188, 193, 200, 205, 207, 209.01])

(def lengths
  [0, 3, 4, 6, 8, 11, 13, 17, 29, 35, 42, 47, 51, 60, 65, 72, 79, 84, 96, 102, 107, 112, 117, 123, 130, 139, 145, 151, 155, 161, 165, 173, 177, 183, 187, 188, 193, 200, 205, 207, 209])

(def diameters
  [18.9, 18.8, 18.7, 18.6, 18.5, 18.4, 18.3, 18.2, 18.1, 18.0, 17.9, 17.8, 17.7, 17.6, 17.5, 17.4, 17.3, 17.2, 17.1, 17.0, 16.9, 16.8, 16.7, 16.6, 16.5, 16.4, 16.3, 16.2, 16.1, 16.0, 15.9, 15.8, 15.7, 15.6, 15.5, 15.4, 15.3, 15.2, 15.3, 15.4, 15.5])

(def outside-diameters
  [30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,
   30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,
   30,30,30,30,30,30,30,30,30,30,30])

(def finger-holes-diameter [10,10,10,10])
(def finger-holes-position [30,60,90,120])

;; Render to SCAD file
(render-code-model "flute_example.scad"
                   (rudall-and-carte
                     outside-diameters lengths
                     diameters inside-lengths
                     finger-holes-diameter finger-holes-position))

(export-stl-file "notebooks/flute_example.stl" "flute_example.scad")

(export-png-file  "notebooks/preview.png" "flute_example.scad")

;; [Flute Preview](preview.png)
(kind/hiccup
  [:img {:src "notebooks/preview.png"}])
