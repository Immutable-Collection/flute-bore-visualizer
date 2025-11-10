(ns flute
  (:require [scad-clj.model :as m]
            [malli.core :as mal]
            [emacs-to-edn :as e2e]
            [scad-clj.scad :refer [write-scad] :as scad]
            [clojure.java.shell :refer [sh]]))

(defn extract-positions-diameters
  [data]
  [(map :diameter data)
   (map :position data)])

(defn polygon-rotator
  ([data] (let [[points-diameter
                 points-position] (extract-positions-diameters data)]
            (polygon-rotator points-diameter points-position)))
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
                 (m/rotate [0 90 0])
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
  (m/rotate (/ 3.142 2) [0 1 0] (m/rotate 3.14 [0 0 1]
                                          (m/difference
                                            model
                                            (m/translate [-50 0 -70] (m/cube 100 100 700 {:center false}))))))

;; TODO adding cork spec to the flute schema
(defn flute-section
  [data]
  (let [validation-result (mal/validate e2e/joint data)]
    (if validation-result
      (m/with-fn 60
                 (m/union
                   (m/difference
                     (polygon-rotator (:outside-diameters data))
                     (polygon-rotator (:bore-diameters data))
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
     (map-indexed #(m/translate [0 (* %1 20) 0] %2) [head middle right-hand foot])
)))

(defn org-to-flute-3d-model [org-path]
  (let [data (-> org-path
                 slurp
                 e2e/org-processed-list-to-edn
                 e2e/post-proccess-edn)
        flute-data-validated? (mal/validate e2e/flute data)]
    (if flute-data-validated?
      (flute-model data)
      (throw (Exception. (mal/explain e2e/flute data))))))
;; single lot model is build through assembly of these parts
