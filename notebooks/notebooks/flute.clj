(ns flute
  (:require [scad-clj.model :as m]
            [clojure.math :refer [PI]]
            [malli.core :as mal]
            [emacs-to-edn :as e2e]
            [schema :refer [joint flute assembly]]
            [scad-clj.scad :refer [write-scad] :as scad]
            [clojure.java.shell :refer [sh]]))

(defn extract-positions-diameters
  [data]
  [(map :diameter data)
   (map :position data)])

(defn polygon-rotator
  ([data] (m/extrude-rotate {:fn 64}
                            (m/polygon (map (fn [point] [(/ (:diameter point) 2) (:position point)]) data))))
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

;; TODO finger holes seems are not straight
(defn bore-section
  [data]
  (let [ordered-data (sort-by :position data)
        start (first ordered-data)
        end (last ordered-data)
        new-data (conj (into [] (concat [{:diameter 0 :position (- (:position start) 0.1)}] data))
                       {:diameter 0 :position (+ (:position end) 0.1)})]

    (polygon-rotator ordered-data)))

(defn flute-section
  [data]

  (let [validation-result (mal/validate joint data)
        section-data (concat (sort-by :position (:outside-diameters data))
                             (reverse (sort-by :position (:bore-diameters data))))]

    (if validation-result
      (m/with-fn 60
                 (m/union
                   (m/difference
                     (polygon-rotator section-data)
                     ;; (bore-section (:bore-diameters data))
                     (finger-holes (:holes data)))))
      ;; TODO else should raise exception
      (throw (Exception. (:errors (mal/explain joint data)))))))

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
        [head middle right-hand foot]))))

(defn flute-model-parts
  [data]
  (let [head (m/rotatec [m/pi 0 0]
                        (flute-section (:head-joint data)))
        foot (flute-section (:foot-joint data))
        middle (flute-section (:middle-joint data))
        right-hand (flute-section (:right-hand-joint data))]
    [head middle right-hand foot]))

(defn org-to-flute-3d-model
  [org-path]
  (let [data (e2e/org-to-edn org-path)
        flute-data-validated? (mal/validate flute data)]
    (if flute-data-validated?
      (flute-model data)
      (println (mal/explain flute data))
      #_(throw (Exception. "my message" #_(mal/explain flute data))))))

;; single lot model is build through assembly of these parts

(defn org-to-flute-3d-model-parts
  [org-path]
  (let [data (e2e/org-to-edn org-path)
        flute-data-validated? (mal/validate flute data)]
    (if flute-data-validated?
      (flute-model-parts data)
      (println (:errors (mal/explain flute data)))
      #_(throw (Exception. "my message" #_(mal/explain flute data))))))

(defn org->cad!
  [org-path cad-path]
  (render-code-model (org-to-flute-3d-model org-path) cad-path))

(defn org->cad-parts!
  [org-path cad-path]
  (let [parts (org-to-flute-3d-model-parts org-path)
        data (e2e/org-to-edn org-path)
        assembly (:assembly data)
        assembly-keys [:head-joint :middle-joint :right-hand-joint :foot-joint]
        part-names ["head" "middle" "right_hand" "foot"]]
    ;; cad-path
    ;; (apply write-scad parts)
    (spit
      cad-path
      (write-scad
        (m/include "../constructive/constructive-compiled.scad")
        (map-indexed
          (fn [idx part]
            (m/define-module (nth part-names idx) part)) parts)
        (m/rotatec 
         [0 (/ (* 3 m/pi) 2) m/pi]
                   (m/call-module-with-block
                     "assemble"
                     (map-indexed
                       (fn [idx part-name]
                         (m/call-module-with-block
                          "add"
                          (m/translate
                           [0 0 (get-in
                                 assembly
                                 [(nth assembly-keys idx) 0 :distance])]
                           (m/call-module part-name))))
                       part-names)))))))

flute

(m/call "add")

;; (:call {:function "add"} nil)


assembly

(org->cad-parts! "../../flute-data/models/lot-dcm615.org" "notebooks/org-lot-assembled3.scad")
