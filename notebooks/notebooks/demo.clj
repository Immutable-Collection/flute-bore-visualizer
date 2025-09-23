(ns demo
     (:require [tablecloth.api :as tc]
               [clojure.edn :as edn]
               [scicloj.tableplot.v1.plotly :as plotly]
               [scad-clj.model :as m]
               [clojure.java.shell :refer [sh]]
               [scad-clj.scad :refer [write-scad] :as scad])
)

(def wood [{:from-cap 0 :bore-diameter 19 :bore-radius 9.5 :outer-diameter 26 :outer-radius 13}
           {:from-cap 210 :bore-diameter 19 :bore-radius 9.5 :outer-diameter 27 :outer-radius 13.5}
           {:from-cap 410 :bore-diameter 14 :bore-radius 7 :outer-diameter 24 :outer-radius 12}
           {:from-cap 530 :bore-diameter 12 :bore-radius 6 :outer-diameter 23 :outer-radius 11.5}
           {:from-cap 600 :bore-diameter 12.5 :bore-radius 6.25 :outer-diameter 20 :outer-radius 10}])

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

(def left-hand-section (-> flute :flute :bore :left-hand-section :measurements ))

(tc/dataset left-hand-section)

(def right-hand-section (-> flute :flute :bore :right-hand-section :measurements ))

(tc/dataset right-hand-section)

(def foot-section (-> flute :flute :bore :foot-section :measurements ))

(tc/dataset foot-section)

;(def flute-data (concat right-hand-section left-hand-section foot-section))

(defn polygon-rotator
  "Creates a 3D solid by rotate-extruding a 2D polygon profile.
   points-diameter and points-position are vectors of equal length."
  [points-diameter points-position]
  (let [number-of-points (dec (count points-position))
        profile-points (concat
                        (for [i (range (inc number-of-points))]
                          [(double (/ (nth points-diameter i) 2))
                           (double (nth points-position i))])
                        (for [i (reverse (range (inc number-of-points)))]
                          [0 (double (nth points-position i))]))]
    (m/extrude-rotate
     {:fn 64}
     (m/polygon profile-points))))

(defn finger-holes
  "Creates finger holes as cylinders cut through the body."
  [finger-holes-diameter finger-holes-position]
  (apply m/union
         (for [i (range (count finger-holes-diameter))]
           (->> (m/cylinder (/ (nth finger-holes-diameter i) 2) 40 )
                (m/rotate [0 90 0])
                (m/translate [0 0 (nth finger-holes-position i)])))))

(defn rudall-and-carte
  "Creates the flute body with bore and finger holes."
  [outside-diameters outside-positions
   bore-diameter bore-positions
   finger-holes-diameter finger-holes-position]
  (m/difference
   (polygon-rotator outside-diameters outside-positions)
   (polygon-rotator bore-diameter bore-positions)
   (finger-holes finger-holes-diameter finger-holes-position)))


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

(defn polygon-rotator [points-diameter points-position]
  (let [number-of-points (dec (count points-position))
        profile-points (concat
                        (for [i (range (inc number-of-points))]
                          [(/ (nth points-diameter i) 2) (nth points-position i)])
                        (for [i (reverse (range (inc number-of-points)))]
                          [0 (nth points-position i)]))]
    (m/extrude-rotate {:fn 64}
                      (m/polygon profile-points))))

(defn finger-holes [finger-holes-diameter finger-holes-position]
  (apply m/union
         (for [i (range (count finger-holes-diameter))]
           (->> (m/cylinder (/ (nth finger-holes-diameter i) 2) 40 {:center false})
                (m/rotate [0 90 0])
                (m/translate [0 0 (nth finger-holes-position i)])))))

(defn rudall-and-carte
  [outside-diameters lengths diameters inside-lengths finger-holes-diameter finger-holes-position]
  (m/difference
   (polygon-rotator outside-diameters lengths)
   (polygon-rotator diameters inside-lengths)
   (finger-holes finger-holes-diameter finger-holes-position)))

;; Render to SCAD file
(spit "flute_example.scad"
      (write-scad
       (rudall-and-carte
        outside-diameters lengths
        diameters inside-lengths
        finger-holes-diameter finger-holes-position)))

(sh "openscad" "-o" "flute_example.stl" "flute_example.scad")
;openscad -o flute_example.stl flute_example.scad
;openscad --imgsize=800,600 --render -o preview.png flute_example.scad
(sh "openscad" "--imgsize=800,600" "--render" "-o" "preview.png" "flute_example.scad")