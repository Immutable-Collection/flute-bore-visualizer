(ns flute
  (:require [scad-clj.model :as m]
            [scad-clj.scad :refer [write-scad] :as scad]
            [clojure.java.shell :refer [sh]]))

(defn polygon-rotator
  [points-diameter points-position]
  (let [number-of-points (dec (count points-position))
        profile-points (concat
                         (for [i (range (inc number-of-points))]
                           [(/ (nth points-diameter i) 2) (nth points-position i)])
                         (for [i (reverse (range (inc number-of-points)))]
                           [0 (nth points-position i)]))]
    (m/extrude-rotate {:fn 64}
                      (m/polygon profile-points))))

(defn finger-holes
  [finger-holes-diameter finger-holes-position]
  (apply m/union
         (for [i (range (count finger-holes-diameter))]
           (->> (m/cylinder (/ (nth finger-holes-diameter i) 2) 40 {:center false})
                (m/rotate [0 90 0])
                (m/translate [0 0 (nth finger-holes-position i)])))))

(defn rudall-and-carte
  [outside-diameters lengths diameters inside-lengths finger-holes-diameter finger-holes-position]

  (m/with-fn 60 (m/difference
    (polygon-rotator outside-diameters lengths)
    (polygon-rotator diameters inside-lengths)
    (finger-holes finger-holes-diameter finger-holes-position))))

;; Render to SCAD file
(defn render-code-model
  [model filename]
  (spit filename
        (write-scad
          model)))

(defn export-stl-file
  [stl-file filename]
  (sh "openscad" "-o" stl-file filename))

(defn export-png-file
  [picture filename]
  (sh "openscad" "--imgsize=800,600" "--render" "-o" picture filename))

(defn lot-model
  ([outside-specs bore-specs finger-holes-specs]
   (lot-model outside-specs bore-specs finger-holes-specs {:start-from-soundhole -20 :end-from-soundhole -40 :diameter 20}))
  ([outside-specs bore-specs finger-holes-specs cork]
   (lot-model outside-specs bore-specs finger-holes-specs cork []))
  ([outside-specs bore-specs finger-holes-specs cork inactive-holes]
   (let [outside-diameters (map :diameter outside-specs)
         lengths (map  :distance outside-specs)
         diameters (map  :diameter bore-specs)
         inside-lengths (map :distance bore-specs)
         finger-holes-diameter (map  :diameter finger-holes-specs)
         finger-holes-position (map :distance finger-holes-specs)
         ;; TODO cork should be generated from the diameter of the bore
         cork-diameters [(:diameter cork) (:diameter cork)]
         cork-positions [(:start-from-soundhole cork) (:end-from-soundhole cork)]
         ; filter out inactive holes
         remove-indexed (fn [s v] (vec (keep-indexed (fn [i x] (when (not (s i)) x)) v)))
         filtered-finger-holes-diameter (vec (remove-indexed (set inactive-holes) finger-holes-diameter))
         filtered-finger-holes-position (vec (remove-indexed (set inactive-holes) finger-holes-position))
         ]  
     (m/with-fn 60
     (m/union
      (m/difference
       (polygon-rotator outside-diameters lengths)
       (polygon-rotator diameters inside-lengths)
       (finger-holes filtered-finger-holes-diameter filtered-finger-holes-position))
      (polygon-rotator cork-diameters cork-positions))
     )    
     )))

(defn cut-view
  [model]
  (m/rotate (/ 3.142 2) [0 1 0] (m/rotate 3.14 [0 0 1]
                                          (m/difference
                                            model
                                            (m/translate [-50 0 -70] (m/cube 100 100 700 {:center false}))))))
