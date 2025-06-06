(ns parry.components.counter)

(defn render-ui [state]
  [:div.m-8
    [:h1.text-lg "Counter"]
    [:div "Number is " (:number state)]
    [:button
     {:on {:click [[::inc-number]]}}
     "Count"]])
