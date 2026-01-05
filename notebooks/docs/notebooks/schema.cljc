(ns schema)

(def hole
  [:map
   [:position :float]
   [:diameter :float]
   ;; other data, lateral diameter, longtitudonal diameter, undercut etc
   ])

(def joint
  [:map
   [:bore-diameters    [:vector :map]]
   [:outside-diameters [:vector :map]]
   [:holes             [:vector #'hole]]])

(def assembly-item
  [:map
   [:distance :float]])

(def cork
  [:map
   [:diameter :float]
   [:length   :float]])

(def assembly
  [:map
   [:head-joint       [:vector #'assembly-item]]
   [:middle-joint     [:vector #'assembly-item]]
   [:right-hand-joint [:vector #'assembly-item]]
   [:foot-joint       [:vector #'assembly-item]]
   [:cork             [:vector #'assembly-item]]])

(def flute
  [:map
   [:model :string]
   [:cork             [:vector #'cork]]
   [:head-joint       #'joint]
   [:middle-joint     #'joint]
   [:right-hand-joint #'joint]
   [:foot-joint       #'joint]
   [:assembly         #'assembly]])
