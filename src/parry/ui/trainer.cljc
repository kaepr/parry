(ns parry.ui.trainer)

(def parry-patterns
  {:street-fighter-basic
   {:name "Street Fighter - Basic"
    :description "Simple parry timing from SF3"
    :zones [{:start 0.2 :end 0.6 :type :danger}
            {:start 0.6 :end 0.7 :type :parry}
            {:start 0.7 :end 0.9 :type :danger}]}
   
   :street-fighter-multi
   {:name "Street Fighter - Multi Hit"
    :description "Multiple parry windows like multi-hit combos"
    :zones [{:start 0.1 :end 0.3 :type :danger}
            {:start 0.3 :end 0.4 :type :parry}
            {:start 0.4 :end 0.6 :type :danger}
            {:start 0.6 :end 0.7 :type :parry}
            {:start 0.7 :end 0.9 :type :danger}]}
   
   :dark-souls-easy
   {:name "Dark Souls - Easy"
    :description "Generous parry window for beginners"
    :zones [{:start 0.3 :end 0.5 :type :danger}
            {:start 0.5 :end 0.7 :type :parry}
            {:start 0.7 :end 0.9 :type :danger}]}
   
   :dark-souls-hard
   {:name "Dark Souls - Hard"
    :description "Tight parry window for experts"
    :zones [{:start 0.4 :end 0.55 :type :danger}
            {:start 0.55 :end 0.6 :type :parry}
            {:start 0.6 :end 0.8 :type :danger}]}
   
   :sekiro-deflect
   {:name "Sekiro - Deflect"
    :description "Perfect deflect timing"
    :zones [{:start 0.35 :end 0.55 :type :danger}
            {:start 0.55 :end 0.65 :type :parry}
            {:start 0.65 :end 0.85 :type :danger}]}
   
   :nightmare-mode
   {:name "Nightmare Mode"
    :description "Multiple frame-perfect windows - for true masters only"
    :zones [{:start 0.1 :end 0.17 :type :danger}
            {:start 0.17 :end 0.2 :type :parry}
            {:start 0.2 :end 0.27 :type :danger}
            {:start 0.27 :end 0.3 :type :parry}
            {:start 0.3 :end 0.42 :type :danger}
            {:start 0.42 :end 0.45 :type :parry}
            {:start 0.45 :end 0.57 :type :danger}
            {:start 0.57 :end 0.6 :type :parry}
            {:start 0.6 :end 0.72 :type :danger}
            {:start 0.72 :end 0.75 :type :parry}
            {:start 0.75 :end 0.9 :type :danger}]}})

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
                     :danger ["bg-red-500"]
                     :parry ["bg-green-500"]
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
  (let [trainer-state (or (:trainer state) {:active false :score 0 :cursor-position 0.0 :timeline-width 600 :zones [] :selected-pattern :street-fighter-basic})
        {:keys [active score last-result selected-pattern]} trainer-state
        current-pattern (get parry-patterns selected-pattern)]
    [:div.bg-white.text-black.min-h-screen
     [:div.max-w-4xl.mx-auto.px-4.py-6
      
      ;; Pattern selection
      [:div.mb-6
       [:label.block.mb-2.text-sm.font-normal "Select pattern:"]
       [:select.border.border-gray-300.px-3.py-2.text-sm.w-full.max-w-md
        {:value (name selected-pattern)
         :on {:change [[:trainer/select-pattern :event.target/value-as-keyword]]}}
        (for [[pattern-key pattern-data] parry-patterns]
          [:option {:key (name pattern-key) :value (name pattern-key)}
           (:name pattern-data)])]
       (when current-pattern
         [:p.text-gray-600.text-sm.mt-1 (:description current-pattern)])]
      
      ;; Stats
      [:div.mb-6
       [:p.text-sm.mb-2 "Score: " [:span.font-medium (str score)]]
       (when last-result
         [:p.text-sm
          "Result: "
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
          pattern-zones (:zones (get parry-patterns selected-pattern))]
      [[:effect/assoc-in [:trainer] {:active false
                                     :cursor-position 0.0
                                     :score 0
                                     :last-result nil
                                     :timeline-width 600
                                     :animation-start nil
                                     :selected-pattern selected-pattern
                                     :zones pattern-zones}]])
    
    :trainer/select-pattern
    (let [new-pattern (keyword (second action))
          pattern-zones (:zones (get parry-patterns new-pattern))]
      [[:effect/assoc-in [:trainer :selected-pattern] new-pattern]
       [:effect/assoc-in [:trainer :zones] pattern-zones]
       [:effect/assoc-in [:trainer :active] false]
       [:effect/assoc-in [:trainer :cursor-position] 0.0]])
    
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
