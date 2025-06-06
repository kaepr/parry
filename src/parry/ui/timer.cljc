(ns parry.ui.timer)

(defn get-view-state [state]
  {:pct 0
   :elapsed "0s"
   :duration 20})

(defn render-ui [state]
  [:div
   [:h1 "Timer"]
   [:div
    "Elapsed time: "
    [:progress {:value 37 :max 100}]]
   [:div "11s"]
   [:div "Duration "
    [:input.range {:type "range"
                   :min 0
                   :value 20}]]
   [:button "Reset"]])
