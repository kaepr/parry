(ns parry.ui.counter)


(defn perform-action [state [action & _args]]
  (when (= ::inc-number action)
    [[:effect/assoc-in [:number] (inc (:number state))]]))

(defn render-ui [state]
  [:div
    [:h1.text-lg "Counter"]
    [:div "Number is " (:number state)]
    [:button
     {:on {:click [[::inc-number]]}}
     "Count"]])
