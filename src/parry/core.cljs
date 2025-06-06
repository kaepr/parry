(ns parry.core
  (:require [replicant.dom :as r]
            [parry.components.counter :as counter]))

(defonce state (atom {:number 0}))

(defn render-ui [state]
  (r/render
   js/document.body
   (counter/render-ui state)))

(defn init [store]
  (add-watch store ::render (fn [_ _ _ new-state]
                              (render-ui new-state)))
  (r/set-dispatch!
   (fn [_ event-data]
     (doseq [[action & args] event-data]
       (case action
        ::counter/inc-number
        (swap! store update :number inc)))))
  (swap! store assoc ::loaded-at (.getTime (js/Date.))))
