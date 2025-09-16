(ns demo
     (:require [tablecloth.api :as tc]
               [scicloj.tableplot.v1.plotly :as plotly])
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
 (-> "Rudall and Carte.edn"
     slurp
     edn/read-string))
