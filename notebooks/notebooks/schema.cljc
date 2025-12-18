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

(def assembly
  [:map
   [:head-joint       [:vector :float]]
   [:middle-joint     [:vector :float]]
   [:right-hand-joint [:vector :float]]
   [:foot-joint       [:vector :float]]])

(def flute
  [:map
   [:model :string]
   [:head-joint       #'joint]
   [:middle-joint     #'joint]
   [:right-hand-joint #'joint]
   [:foot-joint       #'joint]
   [:assembly         #'assembly]])
