(ns parry.timing)

;; Frame timing constants
(def FRAME_DURATION_MS 16) ; 60fps = 16.67ms per frame, rounded to 16ms

(defn frames-to-ms
  "Convert frame count to milliseconds"
  [frames]
  (* frames FRAME_DURATION_MS))

(defn ms-to-frames
  "Convert milliseconds to frame count"
  [ms]
  (Math/round (/ ms FRAME_DURATION_MS)))

(defn calculate-total-frames
  "Calculate total frames from a frame sequence"
  [frame-sequence]
  (reduce + (map second frame-sequence)))

(defn frame-sequence-to-zones
  "Convert frame sequence like [[:danger 10] [:parry 2]] to normalized zones"
  [frame-sequence]
  (let [total-frames (calculate-total-frames frame-sequence)]
    (loop [remaining-sequence frame-sequence
           current-frame 0
           zones []]
      (if (empty? remaining-sequence)
        zones
        (let [[zone-type frame-count] (first remaining-sequence)
              start-position (/ current-frame total-frames)
              end-position (/ (+ current-frame frame-count) total-frames)
              new-zone {:type zone-type
                        :start start-position
                        :end end-position
                        :frame-start current-frame
                        :frame-end (+ current-frame frame-count)}]
          (recur (rest remaining-sequence)
                 (+ current-frame frame-count)
                 (conj zones new-zone)))))))

(defn get-zone-at-frame
  "Get the zone type at a specific frame number"
  [zones frame-number]
  (some (fn [zone]
          (when (and (>= frame-number (:frame-start zone))
                     (< frame-number (:frame-end zone)))
            (:type zone)))
        zones))

(defn get-zone-at-position
  "Get the zone type at a normalized position (0.0 to 1.0)"
  [zones position total-frames]
  (let [frame-number (Math/floor (* position total-frames))]
    (get-zone-at-frame zones frame-number)))

(defn position-to-frame
  "Convert normalized position to frame number"
  [position total-frames]
  (Math/floor (* position total-frames)))

(defn frame-to-position
  "Convert frame number to normalized position"
  [frame total-frames]
  (/ frame total-frames))

;; Frame-based pattern definitions
(def frame-patterns
  {:street-fighter-basic
   {:name "Street Fighter - Basic"
    :description "Simple parry timing from SF3"
    :frames [[:danger 30] [:parry 10] [:danger 20]]}
   
   :street-fighter-multi
   {:name "Street Fighter - Multi Hit"
    :description "Multiple parry windows like multi-hit combos"
    :frames [[:danger 15] [:parry 8] [:danger 20] [:parry 8] [:danger 15]]}
   
   :dark-souls-easy
   {:name "Dark Souls - Easy"
    :description "Generous parry window for beginners"
    :frames [[:danger 25] [:parry 20] [:danger 15]]}
   
   :dark-souls-hard
   {:name "Dark Souls - Hard"
    :description "Tight parry window for experts"
    :frames [[:danger 35] [:parry 6] [:danger 15]]}
   
   :sekiro-deflect
   {:name "Sekiro - Deflect"
    :description "Perfect deflect timing"
    :frames [[:danger 30] [:parry 12] [:danger 18]]}
   
   :nightmare-mode
   {:name "Nightmare Mode"
    :description "Multiple frame-perfect windows - for true masters only"
    :frames [[:danger 8] [:parry 2] [:danger 6] [:parry 2] [:danger 10] 
             [:parry 2] [:danger 8] [:parry 2] [:danger 10] [:parry 2] [:danger 8]]}
   
   :combo-sequence
   {:name "Combo Sequence"
    :description "Long multi-hit combo with varying parry windows"
    :frames [[:danger 45] [:parry 8] [:danger 30] [:parry 12] [:danger 40] 
             [:parry 6] [:danger 25] [:parry 10] [:danger 35] [:parry 4] [:danger 50]]}
   
   :boss-fight
   {:name "Boss Fight"
    :description "Extended boss attack pattern with recovery windows"
    :frames [[:danger 60] [:parry 15] [:danger 80] [:parry 20] [:danger 100] 
             [:parry 8] [:danger 70] [:parry 25] [:danger 90] [:parry 12] [:danger 110]]}
   
   :rhythm-game
   {:name "Rhythm Game"
    :description "Musical timing pattern - 4 seconds of complex rhythm"
    :frames [[:danger 20] [:parry 4] [:danger 16] [:parry 4] [:danger 12] [:parry 4] 
             [:danger 20] [:parry 6] [:danger 18] [:parry 4] [:danger 14] [:parry 4]
             [:danger 22] [:parry 8] [:danger 16] [:parry 4] [:danger 18] [:parry 6]
             [:danger 20] [:parry 4] [:danger 16] [:parry 2] [:danger 12] [:parry 2] [:danger 20]]}
   
   :endurance-test
   {:name "Endurance Test"
    :description "Long 8-second pattern testing sustained concentration"
    :frames [[:danger 75] [:parry 12] [:danger 65] [:parry 15] [:danger 85] [:parry 10]
             [:danger 55] [:parry 18] [:danger 95] [:parry 8] [:danger 45] [:parry 20]
             [:danger 105] [:parry 6] [:danger 35] [:parry 22] [:danger 115] [:parry 14] [:danger 25]]}})

(defn get-pattern-zones
  "Get normalized zones for a pattern by key"
  [pattern-key]
  (when-let [pattern (get frame-patterns pattern-key)]
    (frame-sequence-to-zones (:frames pattern))))

(defn get-pattern-total-frames
  "Get total frame count for a pattern"
  [pattern-key]
  (when-let [pattern (get frame-patterns pattern-key)]
    (calculate-total-frames (:frames pattern))))

(defn get-pattern-duration-ms
  "Get total duration in milliseconds for a pattern"
  [pattern-key]
  (when-let [total-frames (get-pattern-total-frames pattern-key)]
    (frames-to-ms total-frames)))
