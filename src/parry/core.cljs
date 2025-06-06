(ns parry.core
  (:require [replicant.dom :as r]
            [parry.ui.layout :as layout]
            [parry.ui.counter :as counter]))

(def views
  [{:id :counter
    :text "Counter"}
   {:id :temperatures
    :text "Temperatures"}])

(defn get-current-view [state]
  (or (:current-view state)
      (-> views first :id)))

(defn render-ui [state]
  (let [current-view (get-current-view state)]
    [:div.m-8
      (layout/tab-bar current-view views)
      (case current-view
        :counter
        (counter/render-ui state)
        [:h1 "Select something else."])]))

(defn process-effect [store [effect & args]]
  (case effect
    :effect/assoc-in
    (apply swap! store assoc-in args)))

(defn perform-actions [state event-data]
  (mapcat
    (fn [action]
      (or (counter/perform-action state action)
          (case (first action)
            :action/assoc-in
            [(into [:effect/assoc-in] (rest action))]
            (prn "Unknown action"))))
    event-data))

(defn init [store]
  (add-watch store ::render (fn [_ _ _ new-state]
                              (r/render
                                js/document.body
                                (render-ui new-state))))
  (r/set-dispatch!
   (fn [_ event-data]
     (->> (perform-actions @store event-data)
          (run! #(process-effect store %)))))
  (swap! store assoc ::loaded-at (.getTime (js/Date.))))
