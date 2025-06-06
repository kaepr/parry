(ns parry.dev
  (:require [parry.core :as parry]))

(def store (atom {:number 0
                   :trainer {:active false
                             :cursor-position 0.0
                             :score 0
                             :last-result nil
                             :timeline-width 600
                             :animation-start nil
                             :selected-pattern :street-fighter-basic
                             :zones [{:start 0.2 :end 0.6 :type :danger}
                                     {:start 0.6 :end 0.7 :type :parry}
                                     {:start 0.7 :end 0.9 :type :danger}]}}))

(defn main []
  (parry/init store))

(defn ^:dev/after-load reload []
  (parry/init store))
