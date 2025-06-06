(ns parry.core
  (:require [replicant.dom :as r]
            [parry.ui.timer :as timer]
            [parry.ui.layout :as layout]
            [parry.ui.counter :as counter]
            [clojure.walk :as walk]))

(def views
  [{:id :counter
    :text "Counter"}
   {:id :temperatures
    :text "Temperatures"}
   {:id :timer
    :text "Timer"}])

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
        :timer
        (timer/render-ui state)
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

(defn interpolate [event data]
  (walk/postwalk
   (fn [x]
     (case x
       :event.target/value-as-number
       (some-> event .-target .-valueAsNumber)
       :event.target/value-as-keyword
       (some-> event .-target .-value keyword)
       :event.target/value
       (some-> event .-target .-value)
       :clock/now
       (js/Date.)
       x))
   data))

(defn init [store]
  (add-watch store ::render (fn [_ _ _ new-state]
                              (r/render
                                js/document.body
                                (render-ui new-state))))
  (r/set-dispatch!
   (fn [{:replicant/keys [dom-event]} event-data]
     (->> (interpolate dom-event event-data)
          (perform-actions @store)
          (run! #(process-effect store %)))))
  (swap! store assoc ::loaded-at (.getTime (js/Date.))))
