(ns parry.dev
  (:require [parry.core :as parry]))

(def store (atom {:number 0}))

(defn main []
  (parry/init store))

(defn ^:dev/after-load reload []
  (parry/init store))
