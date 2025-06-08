(ns parry.ui.trainer
  (:require [parry.timing :as timing]))


(defn render-timeline [trainer-state]
  (let [{:keys [cursor-position zones timeline-width]} trainer-state
        cursor-left (* (or cursor-position 0) timeline-width)]
    [:div.w-full.max-w-2xl.mx-auto.my-6
     [:div.relative.bg-gray-50.h-16.border.border-gray-300.overflow-hidden
      {:style {:width "100%"}}
      
      ;; Render zones
      (for [zone zones]
        (let [{:keys [start end type]} zone
              zone-left (* start 100)
              zone-width (* (- end start) 100)]
          [:div.absolute.h-full
           {:key (str start "-" end)
            :class (case type
                     :danger ["bg-red-300" "border-red-400"]
                     :parry ["bg-green-300" "border-green-400"]
                     "bg-gray-100")
            :style {:left (str zone-left "%")
                    :width (str zone-width "%")}}]))
      
      ;; Moving cursor - visible black line
      [:div.absolute.w-1.h-full.bg-black.z-10.shadow-sm
       {:style {:left (str (* (or cursor-position 0) 100) "%")}}]]
     
     ;; Simple labels
     [:div.flex.justify-between.text-xs.text-gray-500.mt-1
      [:span "Start"]
      [:span "End"]]]))

(defn render-ui [state]
  (let [trainer-state (or (:trainer state) {:active false :score 0 :misses 0 :cursor-position 0.0 :timeline-width 600 :zones [] :selected-pattern :street-fighter-basic})
        {:keys [active score misses last-result selected-pattern]} trainer-state
        current-pattern (get timing/frame-patterns selected-pattern)]
    [:div.bg-white.text-black.min-h-screen
     [:div.max-w-4xl.mx-auto.px-4.py-6
      
      ;; Pattern selection
      [:div.mb-6
       [:label.block.mb-2.text-sm.font-normal "Select pattern:"]
       [:select.border.border-gray-300.px-3.py-2.text-sm.w-full.max-w-md
        {:value (name selected-pattern)
         :on {:change [[:trainer/select-pattern :event.target/value-as-keyword]]}}
        (for [[pattern-key pattern-data] timing/frame-patterns]
          [:option {:key (name pattern-key) :value (name pattern-key)}
           (:name pattern-data)])]
       (when current-pattern
         [:p.text-gray-600.text-sm.mt-1 (:description current-pattern)])]
      
      ;; Stats and timing info
      [:div.mb-6
       [:div.flex.space-x-6.mb-2
        [:p.text-sm "Hits: " [:span.font-medium.text-green-700 (str score)]]
        [:p.text-sm "Misses: " [:span.font-medium.text-red-700 (str (or misses 0))]]
        (when current-pattern
          (let [total-frames (timing/get-pattern-total-frames selected-pattern)
                current-frame (if active 
                                (timing/position-to-frame (or (:cursor-position trainer-state) 0) total-frames)
                                0)
                duration-ms (timing/get-pattern-duration-ms selected-pattern)]
            [:p.text-sm.text-gray-600 
             "Frame: " current-frame "/" total-frames 
             " (" duration-ms "ms)"]))]
       
       ;; Show completion summary when sequence finishes
       (when (and (not active) (or (> score 0) (> (or misses 0) 0)))
         (let [total-attempts (+ score (or misses 0))
               accuracy (if (> total-attempts 0) 
                         (Math/round (* 100 (/ score total-attempts))) 
                         0)]
           [:div.p-3.bg-gray-50.border.border-gray-200.rounded.mb-2
            [:p.text-sm.font-medium.mb-1 "Session Complete"]
            [:div.flex.space-x-4.text-sm
             [:span "Hits: " [:span.text-green-700 (str score)]]
             [:span "Misses: " [:span.text-red-700 (str (or misses 0))]]
             [:span "Accuracy: " [:span.font-medium (str accuracy "%")]]]]))
       
       (when last-result
         [:p.text-sm
          "Last: "
          [:span {:class (case last-result
                           :perfect "text-green-700"
                           :good "text-orange-600" 
                           :miss "text-red-700")}
           (case last-result
             :perfect "Perfect"
             :good "Good"
             :miss "Miss")]])]
      
      ;; Instructions
      [:p.text-sm.text-gray-600.mb-4 "Press SPACE when the cursor is in the green zone."]
      
      ;; Timeline
      (render-timeline trainer-state)
      
      ;; Controls
      [:div.mt-6
       (if active
         [:button.border.border-gray-300.px-4.py-2.text-sm.mr-2.hover:bg-gray-50
          {:on {:click [[:trainer/stop]]}}
          "Stop"]
         [:button.border.border-gray-300.px-4.py-2.text-sm.mr-2.hover:bg-gray-50
          {:on {:click [[:trainer/start]]}}
          "Start"])
       
       [:button.border.border-gray-300.px-4.py-2.text-sm.hover:bg-gray-50
        {:on {:click [[:trainer/reset]]}}
        "Reset"]]]]))

(defn perform-action [state action]
  (case (first action)
    :trainer/start
    [[:effect/assoc-in [:trainer :active] true]
     [:effect/assoc-in [:trainer :cursor-position] 0.0]
     [:effect/start-animation]]
    
    :trainer/stop  
    [[:effect/assoc-in [:trainer :active] false]]
    
    :trainer/reset
    (let [trainer-state (:trainer state)
          selected-pattern (or (:selected-pattern trainer-state) :street-fighter-basic)
          pattern-zones (timing/get-pattern-zones selected-pattern)
          pattern-duration (timing/get-pattern-duration-ms selected-pattern)]
      [[:effect/assoc-in [:trainer] {:active false
                                     :cursor-position 0.0
                                     :score 0
                                     :misses 0
                                     :last-result nil
                                     :timeline-width 600
                                     :animation-start nil
                                     :animation-duration pattern-duration
                                     :selected-pattern selected-pattern
                                     :zones pattern-zones}]])
    
    :trainer/select-pattern
    (let [new-pattern (keyword (second action))
          pattern-zones (timing/get-pattern-zones new-pattern)
          pattern-duration (timing/get-pattern-duration-ms new-pattern)]
      [[:effect/assoc-in [:trainer :selected-pattern] new-pattern]
       [:effect/assoc-in [:trainer :zones] pattern-zones]
       [:effect/assoc-in [:trainer :animation-duration] pattern-duration]
       [:effect/assoc-in [:trainer :active] false]
       [:effect/assoc-in [:trainer :cursor-position] 0.0]
       [:effect/assoc-in [:trainer :score] 0]
       [:effect/assoc-in [:trainer :misses] 0]
       [:effect/assoc-in [:trainer :last-result] nil]])
    
    :trainer/attempt
    (let [trainer-state (:trainer state)
          cursor-pos (:cursor-position trainer-state)
          zones (:zones trainer-state)
          in-green-zone? (some (fn [zone]
                                 (and (= (:type zone) :parry)
                                      (<= (:start zone) cursor-pos (:end zone))))
                               zones)]
      (if in-green-zone?
        [[:effect/assoc-in [:trainer :score] (inc (:score trainer-state))]
         [:effect/assoc-in [:trainer :last-result] :perfect]]
        [[:effect/assoc-in [:trainer :misses] (inc (or (:misses trainer-state) 0))]
         [:effect/assoc-in [:trainer :last-result] :miss]]))
    
    :trainer/update-cursor
    (let [new-position (second action)]
      [[:effect/assoc-in [:trainer :cursor-position] new-position]])
    
    nil))
