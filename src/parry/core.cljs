(ns parry.core
  (:require [replicant.dom :as r]
            [parry.ui.timer :as timer]
            [parry.ui.layout :as layout]
            [parry.ui.counter :as counter]
            [parry.ui.trainer :as trainer]
            [parry.ui.header :as header]
            [parry.ui.footer :as footer]
            [clojure.walk :as walk]))

(def views
  [{:id :trainer
    :text "Parry Trainer"}])

(defn get-current-view [state]
  (or (:current-view state)
      (-> views first :id)))

(defn render-ui [state]
  [:div
   (header/render)
   (trainer/render-ui state)
   (footer/render)])

(defn process-effect [store [effect & args]]
  (case effect
    :effect/assoc-in
    (apply swap! store assoc-in args)
    
    :effect/start-animation
    (swap! store assoc-in [:trainer :animation-start] (.getTime (js/Date.)))))

(defn perform-actions [state event-data]
  (mapcat
    (fn [action]
      (or (counter/perform-action state action)
          (trainer/perform-action state action)
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

(defn update-trainer-animation [store]
  (let [state @store
        trainer-state (:trainer state)]
    (when (and (:active trainer-state) (:animation-start trainer-state))
      (let [now (.getTime (js/Date.))
            start-time (:animation-start trainer-state)
            elapsed (- now start-time)
            duration 4000 ; 4 seconds for full animation
            progress (min 1.0 (/ elapsed duration))]
        (swap! store assoc-in [:trainer :cursor-position] progress)
        (when (>= progress 1.0)
          (swap! store update :trainer assoc
                 :active false
                 :cursor-position 0.0
                 :animation-start nil))))))

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
  
  ;; Keyboard handler for SPACE key
  (.addEventListener js/document "keydown"
    (fn [e]
      (when (and (= (.-code e) "Space")
                 (get-in @store [:trainer :active]))
        (.preventDefault e)
        (->> [[:trainer/attempt]]
             (perform-actions @store)
             (run! #(process-effect store %))))))
  
  ;; Animation loop for trainer using requestAnimationFrame
  (letfn [(animate []
            (update-trainer-animation store)
            (js/requestAnimationFrame animate))]
    (animate))
  
  (swap! store assoc ::loaded-at (.getTime (js/Date.))))
