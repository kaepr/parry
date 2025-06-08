(ns parry.dev
  (:require [parry.core :as parry]
            [parry.timing :as timing]))

(def store (atom {:number 0
                   :trainer {:active false
                             :cursor-position 0.0
                             :score 0
                             :misses 0
                             :last-result nil
                             :timeline-width 600
                             :animation-start nil
                             :animation-duration (timing/get-pattern-duration-ms :street-fighter-basic)
                             :selected-pattern :street-fighter-basic
                             :zones (timing/get-pattern-zones :street-fighter-basic)}}))

(defn main []
  (parry/init store))

(defn ^:dev/after-load reload []
  (parry/init store))
