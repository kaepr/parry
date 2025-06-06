(ns parry.ui.trainer)

(defn render-timeline [trainer-state]
  (let [{:keys [cursor-position zones timeline-width]} trainer-state
        cursor-left (* (or cursor-position 0) timeline-width)]
    [:div.relative.bg-gray-800.h-16.rounded.overflow-hidden
     {:style {:width (str timeline-width "px")}}
     
     ;; Render zones
     (for [zone zones]
       (let [{:keys [start end type]} zone
             zone-left (* start timeline-width)
             zone-width (* (- end start) timeline-width)]
         [:div.absolute.h-full
          {:key (str start "-" end)
           :class (case type
                    :danger "bg-red-500"
                    :parry "bg-green-500"
                    "bg-gray-600")
           :style {:left (str zone-left "px")
                   :width (str zone-width "px")}}]))
     
     ;; Moving cursor - make it more visible
     [:div.absolute.w-1.h-full.bg-white.shadow-lg.z-10
      {:style {:left (str cursor-left "px")}}]
     
     ;; Debug info - show cursor position
     [:div.absolute.top-0.left-0.text-white.text-xs.bg-black.px-1
      (str "pos: " (or cursor-position 0) " left: " cursor-left "px")]]))

(defn render-ui [state]
  (let [trainer-state (or (:trainer state) {:active false :score 0 :cursor-position 0.0 :timeline-width 600 :zones []})
        {:keys [active score last-result]} trainer-state]
    [:div.space-y-6
     [:div.text-center
      [:h1.text-2xl.font-bold.mb-2 "Street Fighter Parry Trainer"]
      [:p.text-gray-400 "Press SPACE when the cursor is in the green zone"]]
     
     [:div.flex.justify-center.space-x-8.text-lg
      [:div "Score: " [:span.font-bold score]]
      (when last-result
        [:div {:class (case last-result
                        :perfect "text-green-400"
                        :good "text-yellow-400" 
                        :miss "text-red-400")}
         (case last-result
           :perfect "PERFECT!"
           :good "Good"
           :miss "Miss")])]
     
     [:div.flex.justify-center
      (render-timeline trainer-state)]
     
     [:div.text-center.space-x-4
      (if active
        [:button.px-4.py-2.bg-red-600.text-white.rounded
         {:on {:click [[:trainer/stop]]}}
         "Stop"]
        [:button.px-4.py-2.bg-green-600.text-white.rounded
         {:on {:click [[:trainer/start]]}}
         "Start Training"])
      
      [:button.px-4.py-2.bg-gray-600.text-white.rounded
       {:on {:click [[:trainer/reset]]}}
       "Reset"]]
     
     (when active
       [:div.text-center.text-sm.text-gray-400.mt-4
        "Press SPACE or click anywhere to parry"])]))

(defn perform-action [state action]
  (case (first action)
    :trainer/start
    [[:effect/assoc-in [:trainer :active] true]
     [:effect/assoc-in [:trainer :cursor-position] 0.0]
     [:effect/start-animation]]
    
    :trainer/stop  
    [[:effect/assoc-in [:trainer :active] false]]
    
    :trainer/reset
    [[:effect/assoc-in [:trainer] {:active false
                                   :cursor-position 0.0
                                   :score 0
                                   :last-result nil
                                   :timeline-width 600
                                   :animation-start nil
                                   :zones [{:start 0.1 :end 0.3 :type :danger}
                                           {:start 0.3 :end 0.4 :type :parry}
                                           {:start 0.4 :end 0.6 :type :danger}
                                           {:start 0.6 :end 0.7 :type :parry}
                                           {:start 0.7 :end 0.9 :type :danger}]}]]
    
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
        [[:effect/assoc-in [:trainer :last-result] :miss]]))
    
    :trainer/update-cursor
    (let [new-position (second action)]
      [[:effect/assoc-in [:trainer :cursor-position] new-position]])
    
    nil))