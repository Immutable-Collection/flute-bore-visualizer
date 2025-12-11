(ns flute
  (:require [scad-clj.model :as m]
            [clojure.math :refer [PI]]
            [malli.core :as mal]
            [emacs-to-edn :as e2e]
            [scad-clj.scad :refer [write-scad] :as scad]
            [clojure.java.shell :refer [sh]]))



(defn extract-positions-diameters
   [data]
         [(map :diameter data)
   (map :position data)])

(defn polygon-rotator
  ([data] (m/extrude-rotate {:fn 64}
                            (m/polygon (map (fn [point][(/ (:diameter point) 2) (:position point)]) data))))
  ([points-diameter points-position]
   (let [number-of-points (dec (count points-position))
         profile-points (concat
                          (for [i (range (inc number-of-points))]
                            [(/ (nth points-diameter i) 2) (nth points-position i)])
                          (for [i (reverse (range (inc number-of-points)))]
                            [0 (nth points-position i)]))]
     (m/extrude-rotate {:fn 64}
                       (m/polygon profile-points)))))

(defn finger-holes
  ([data] (let [[diameters
                 positions] (extract-positions-diameters data)]
            (finger-holes diameters positions)))
  ([finger-holes-diameter finger-holes-position]
   (apply m/union
          (for [i (range (count finger-holes-diameter))]
            (->> (m/cylinder (/ (nth finger-holes-diameter i) 2) 40 {:center false})
                 (m/rotate [0 (/ PI 2) 0])
                 (m/translate [0 0 (nth finger-holes-position i)]))))))

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
  (sh "openscad" "--imgsize=1000,800" "--render" "-o" picture filename))

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
         ;; filter out inactive holes
         remove-indexed (fn [s v] (vec (keep-indexed (fn [i x] (when (not (s i)) x)) v)))
         filtered-finger-holes-diameter (vec (remove-indexed (set inactive-holes) finger-holes-diameter))
         filtered-finger-holes-position (vec (remove-indexed (set inactive-holes) finger-holes-position))]
     (m/with-fn 60
                (m/union
                  (m/difference
                    (polygon-rotator outside-diameters lengths)
                    (polygon-rotator diameters inside-lengths)
                    (finger-holes filtered-finger-holes-diameter filtered-finger-holes-position))
                  (polygon-rotator cork-diameters cork-positions))))))

(defn cut-view
  [model]
  (m/rotate (/ PI 2) [0 1 0] (m/rotate PI [0 0 1]
                                          (m/difference
                                            model
                                            (m/translate [-50 0 -70] (m/cube 100 100 700 {:center false}))))))
; TODO finger holes seems are not straight
(defn bore-section [data]
  (let [ordered-data (sort-by :position data)
        start (first ordered-data)
        end (last ordered-data)
        new-data (conj (into [] (concat [{:diameter 0 :position (- (:position start) 0.1)}] data) )
        {:diameter 0 :position (+ (:position end) 0.1)})]
        (println "data")
        (println data)
        (println "new data")
        (println new-data)
    (polygon-rotator ordered-data))
  )

(def test-data {:head-joint {:bore-diameters [{:diameter 20.0, :position 0.0} {:diameter 20.0, :position 1.0} {:diameter 19.9, :position 15.0} {:diameter 19.8, :position 165.0} {:diameter 19.7, :position 175.0} {:diameter 19.6, :position 185.0} {:diameter 19.5, :position 191.0} {:diameter 19.5, :position 222.8}], :outside-diameters [{:position 222.8, :min 29.3, :max 29.1, :diameter 29.2} {:position 159.0, :min 30.25, :max 30.15, :diameter 30.2} {:position 47.8, :min 29.8, :max 29.5, :diameter 29.65} {:position 38.16, :min 29.85, :max 29.85, :diameter 29.85} {:position 38.15, :min 30.2, :max 30.2, :diameter 30.2} {:position 37.8, :min 30.2, :max 30.2, :diameter 30.2} {:position 36.8, :min 31.4, :max 31.4, :diameter 31.4} {:position 36.79, :min 31.0, :max 31.0, :diameter 31.0} {:position 36.25, :min 31.0, :max 31.0, :diameter 31.0} {:position 36.24, :min 30.6, :max 30.6, :diameter 30.6} {:position 20.2, :min 35.45, :max 35.05, :diameter 35.25} {:position 2.51, :min 30.8, :max 30.8, :diameter 30.8} {:position 2.5, :min 31.05, :max 31.05, :diameter 31.05} {:position 2.0, :min 31.05, :max 31.05, :diameter 31.05} {:position 1.9, :min 31.85, :max 31.85, :diameter 31.85} {:position 1.0, :min 30.0, :max 30.0, :diameter 30.0} {:position 0.0, :min 30.0, :max 30.0, :diameter 30.0}], :holes [{:longitudinal 12.0, :lateral 12.0, :position 159.0, :diameter 12.0}]}, :middle-joint {:bore-diameters [{:diameter 19.1, :position 0.0} {:diameter 19.1, :position 2.0} {:diameter 19.0, :position 34.0} {:diameter 18.9, :position 45.0} {:diameter 18.8, :position 52.0} {:diameter 18.7, :position 58.0} {:diameter 18.6, :position 63.0} {:diameter 18.5, :position 68.0} {:diameter 18.4, :position 72.0} {:diameter 18.3, :position 72.0} {:diameter 18.2, :position 80.0} {:diameter 18.1, :position 83.0} {:diameter 18.0, :position 86.0} {:diameter 17.9, :position 89.0} {:diameter 17.7, :position 101.0} {:diameter 17.6, :position 106.0} {:diameter 17.5, :position 110.0} {:diameter 17.4, :position 119.0} {:diameter 17.3, :position 125.0} {:diameter 17.1, :position 138.0} {:diameter 17.0, :position 142.5} {:diameter 16.9, :position 149.0} {:diameter 16.8, :position 156.0} {:diameter 16.7, :position 165.0} {:diameter 16.6, :position 170.0} {:diameter 16.5, :position 174.0} {:diameter 16.4, :position 180.0} {:diameter 16.3, :position 182.0} {:diameter 16.2, :position 186.0} {:diameter 16.1, :position 189.0} {:diameter 16.0, :position 191.0} {:diameter 15.9, :position 194.0} {:diameter 15.8, :position 208.0} {:diameter 15.9, :position 215.0}], :outside-diameters [{:position 159.05, :diameter 29.2} {:position 317.49, :diameter 26.0}], :holes [{:position 64.6, :longitudinal 6.9, :lateral 6.45, :diameter 6.675} {:position 102.0, :longitudinal 6.85, :lateral 6.5, :diameter 6.675} {:position 141.7, :longitudinal 5.75, :lateral 5.55, :diameter 5.65}]}, :right-hand-joint {:bore-diameters [{:diameter 15.4, :position 0.0} {:diameter 15.4, :position 29.0} {:diameter 15.3, :position 34.0} {:diameter 15.2, :position 51.0} {:diameter 15.1, :position 53.0} {:diameter 15.0, :position 55.0} {:diameter 14.9, :position 61.0} {:diameter 14.8, :position 68.0} {:diameter 14.7, :position 75.0} {:diameter 14.6, :position 80.0} {:diameter 14.5, :position 88.0} {:diameter 14.4, :position 93.0} {:diameter 14.3, :position 96.0} {:diameter 14.2, :position 98.0} {:diameter 14.1, :position 106.0} {:diameter 14.0, :position 111.0} {:diameter 13.9, :position 119.0} {:diameter 13.8, :position 128.0} {:diameter 13.7, :position 137.0} {:diameter 13.6, :position 142.5} {:diameter 13.5, :position 145.5} {:diameter 13.4, :position 152.0} {:diameter 13.3, :position 161.0}], :outside-diameters [{:position 0.0, :min 27.15, :max 27.15, :diameter 27.15} {:position 0.5, :min 27.15, :max 27.15, :diameter 27.15} {:position 1.2, :min 28.7, :max 28.7, :diameter 28.7} {:position 1.7, :min 27.9, :max 27.9, :diameter 27.9} {:position 2.0, :min 27.9, :max 27.9, :diameter 27.9} {:position 2.1, :min 27.2, :max 27.2, :diameter 27.2} {:position 17.2, :min 31.6, :max 31.5, :diameter 31.55} {:position 29.79, :min 27.0, :max 27.0, :diameter 27.0} {:position 29.8, :min 27.45, :max 27.45, :diameter 27.45} {:position 30.0, :min 28.3, :max 28.3, :diameter 28.3} {:position 31.7, :min 27.0, :max 27.0, :diameter 27.0} {:position 31.71, :min 26.68, :max 26.68, :diameter 26.68} {:position 144.4, :min 25.95, :max 26.95, :diameter 26.45}], :holes [{:position 40.15, :longitudinal 6.8, :lateral 6.5, :diameter 6.65} {:position 78.65, :longitudinal 6.35, :lateral 6.15, :diameter 6.25} {:position 116.9, :longitudinal 4.95, :lateral 4.9, :diameter 4.925}]}, :foot-joint {:bore-diameters [{:diameter 15.0, :position 0.0} {:diameter 15.0, :position 40.0} {:diameter 14.9, :position 45.0} {:diameter 14.8, :position 47.0} {:diameter 14.7, :position 50.0} {:diameter 14.6, :position 51.0} {:diameter 14.5, :position 55.0} {:diameter 14.4, :position 57.0} {:diameter 14.3, :position 60.0} {:diameter 14.1, :position 63.0} {:diameter 14.0, :position 66.0} {:diameter 13.9, :position 68.0} {:diameter 13.8, :position 70.0} {:diameter 13.7, :position 71.0} {:diameter 13.6, :position 73.0} {:diameter 13.5, :position 76.0} {:diameter 13.43, :position 78.0} {:diameter 13.43, :position 97.6}], :outside-diameters [{:position 0.0, :diameter 26.5} {:position 1.0, :diameter 26.8} {:position 1.01, :diameter 28.4} {:position 2.2, :diameter 28.4} {:position 2.21, :diameter 28.0} {:position 3.0, :diameter 27.7} {:position 3.01, :diameter 27.3} {:position 3.5, :diameter 27.3} {:position 3.51, :diameter 26.8} {:position 15.8, :diameter 26.9} {:position 15.81, :diameter 27.5} {:position 17.0, :diameter 27.5} {:position 17.01, :diameter 28.6} {:position 17.75, :diameter 28.6} {:position 21.0, :diameter 34.25} {:position 25.0, :diameter 34.25} {:position 27.0, :diameter 29.45} {:position 27.5, :diameter 29.45} {:position 27.51, :diameter 27.5} {:position 28.6, :diameter 27.5} {:position 28.61, :diameter 25.6} {:position 97.6, :diameter 24.75}], :holes [{:position 30.8, :longitudinal 9.1, :lateral 8.7, :diameter 8.9}]}, :model "lot-dcm615"}

)

(def bore-data (:bore-diameters (:middle-joint test-data)))

bore-data

(map (fn [item] [(:diameter item) (:position item)] ) bore-data)

(def bore-section-rr (bore-section bore-data))


(render-code-model bore-section-rr "test2.scad")

;; TODO adding cork spec to the flute schema
(defn flute-section
  [data]
  (println "data")
  (println data)
  (let [validation-result (mal/validate e2e/joint data)
        section-data (concat (sort-by :position (:outside-diameters data))
                             (reverse (sort-by :position (:bore-diameters data))))]
    (println "section data")
    (println section-data)     
    (println "end section data")                    
    (if validation-result
      (m/with-fn 60
        (m/union
         (m/difference
          (polygon-rotator section-data)
                     ;(bore-section (:bore-diameters data))
          (finger-holes (:holes data)))))
      ;; TODO else should raise exception
      (throw (Exception. (:errors (mal/explain e2e/joint data)))))))


;; define sections for each part of the model

(defn flute-model
  [data]
  (let [head (flute-section (:head-joint data))
        foot (flute-section (:foot-joint data))
        middle (flute-section (:middle-joint data))
        right-hand (flute-section (:right-hand-joint data))]
    (m/union
     (map-indexed 
     #(m/translate [(* %1 40) (* %1 40) 0] %2) 
     [head middle right-hand foot])
)))


(defn org-to-flute-3d-model [org-path]
  (let [data (e2e/org-to-edn org-path)
        flute-data-validated? (mal/validate e2e/flute data)]
    (if flute-data-validated?
      (flute-model data)
      (println (mal/explain e2e/flute data))
       #_(throw (Exception. "my message" #_(mal/explain e2e/flute data))))))
;; single lot model is build through assembly of these parts


(defn org->cad! [org-path cad-path]
 (render-code-model (org-to-flute-3d-model org-path) cad-path))
  