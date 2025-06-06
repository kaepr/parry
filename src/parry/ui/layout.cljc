(ns parry.ui.layout)

(defn tab-bar [current-view views]
  [:div.mb-4 {:role "tablist"}
   (for [{:keys [id text]} views]
     (let [current? (= id current-view)]
       [:a.p-4 (cond-> {:role "tab"}
                 current? (assoc :class "bg-gray-200")
                 (not current?)
                 (assoc-in [:on :click]
                           [[:action/assoc-in [:current-view] id]]))
        text]))])
