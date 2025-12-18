(ns demo
  (:require
    [clojure.edn :as edn]
    [flute :refer [export-png-file export-stl-file polygon-rotator
                   render-code-model rudall-and-carte lot-model cut-view]]
    [scad-clj.scad :refer [write-scad] :as scad]
    [scicloj.kindly.v4.kind :as kind]
    [scicloj.tableplot.v1.plotly :as plotly]
    [tablecloth.api :as tc]))

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
  (-> "notebooks/data/Rudall and Carte.edn"
      slurp
      edn/read-string))

(def left-hand-section (-> flute :flute :bore :left-hand-section :measurements))

(tc/dataset left-hand-section)

(def right-hand-section (-> flute :flute :bore :right-hand-section :measurements))

(tc/dataset right-hand-section)

(def foot-section (-> flute :flute :bore :foot-section :measurements))

(tc/dataset foot-section)

;; (def flute-data (concat right-hand-section left-hand-section foot-section))

#_(spit "flute_example.scad"
      (rudall-and-carte
        outside-diameters lengths
        diameters inside-lengths
        finger-holes-diameter finger-holes-position))

;; (export-stl-file "notebooks/flute_example.stl" "flute_example.scad")

;; (export-png-file  "notebooks/preview.png" "flute_example.scad")

;; [Flute Preview](preview.png)
(kind/hiccup
  [:img {:src "notebooks/preview.png"}])

;; ## LOT flute
;; ### Data
(def lot
  (-> "notebooks/data/lot.edn"
      slurp
      edn/read-string))

(defn extract-items-from-edn
  [path-in-model model-data diameter-key]
  (map (fn [x] {:diameter (get x diameter-key) :distance (:distance-from-soundhole x)}) (get-in model-data path-in-model)))

(def bore-data
  (sort-by :distance (apply concat (map (fn [path] (extract-items-from-edn path lot :bore-diameter)) [[:bore :head-joint]
                                                                                                      [:bore :foot-joint]
                                                                                                      [:bore :right-hand-joint]
                                                                                                      [:bore :middle-joint]]))))

(def outside-diameter-data
  (sort-by :distance (apply concat (map (fn [path] (extract-items-from-edn path lot :diameter)) [[:outside-diameter :head-joint]
                                                                                                 [:outside-diameter :foot-joint]
                                                                                                 [:outside-diameter :right-hand-joint]
                                                                                                 [:outside-diameter :middle-joint]]))))

(def outside-diameter-ds (tc/dataset outside-diameter-data))

outside-diameter-ds

(-> outside-diameter-ds
    (plotly/base {:=x :distance
                  :=y :diameter
                  :=width 1200
                  :=height 400})
    (plotly/layer-line {:=mark-color "blue"}))

(def bore-data-ds (tc/dataset bore-data))

(-> bore-data-ds
    (plotly/base {:=x :distance
                  :=y :diameter
                  :=width 1200
                  :=height 400})
    (plotly/layer-line {:=mark-color "green"}))

(def finger-hole-data
  (sort-by :distance
           (apply concat (map (fn [path] (extract-items-from-edn path lot :diameter)) [[:finger-holes :head-joint]

                                                                                       [:finger-holes :foot-joint]
                                                                                       [:finger-holes :right-hand-joint]
                                                                                       [:finger-holes :middle-joint]]))))

finger-hole-data

(def generated-lot-model
  (lot-model
    outside-diameter-data
    bore-data
    finger-hole-data
    {:start-from-soundhole -20 :end-from-soundhole -40 :diameter 20}
    []))

(spit "lot.scad"
      (write-scad generated-lot-model))

(spit "lot-cut.scad"
      (write-scad (cut-view generated-lot-model)))

;; (export-stl-file "notebooks/lot.stl" "lot.scad")

(export-png-file  "notebooks/lot-cut.png" "lot-cut.scad")

(kind/hiccup
  [:img {:src "notebooks/lot.png"}])

(kind/hiccup
  [:img {:src "notebooks/lot-cut.png"}])

;; TODO automatic convert Text base STL to binary STL
;; closing hour through
;; ## Music note
;; ### edn format list of
;; #### putting finger on holes [{:hole-number}]
;; #### blowing speed [{:speed :time }] 
;; TODO we have issues on the post process edn
;; (flute/org-to-flute-3d-model "../../flute-data/models/lot-dcm615.org")

(flute/org->cad!  "../../flute-data/models/lot-dcm615.org" "notebooks/org-lot.scad")

(export-png-file  "notebooks/org-lot-cut.png" "notebooks/org-lot.scad")

(kind/hiccup
  [:img {:src "notebooks/org-lot-cut.png"}])
