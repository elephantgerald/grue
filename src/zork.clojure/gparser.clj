(ns gparser
  (:require [zork1.actions :as world]
            [clojure.string :as str]))

;;; ---------------------------------------------------------------------------
;;; Synonym tables — mirrors GSYNTAX.ZIL
;;; ---------------------------------------------------------------------------

(def directions
  {"north" :north "n"     :north
   "south" :south "s"     :south
   "east"  :east  "e"     :east
   "west"  :west  "w"     :west
   "ne"    :ne    "northeast" :ne  "northe" :ne
   "nw"    :nw    "northwest" :nw
   "se"    :se    "southeast" :se  "southe" :se
   "sw"    :sw    "southwest" :sw
   "up"    :up    "u"     :up
   "down"  :down  "d"     :down
   "in"    :in
   "out"   :out})

(def verbs
  {"look"    :look    "l"       :look
   "stare"   :look    "gaze"    :look
   "examine" :examine "x"       :examine
   "describe":examine "what"    :examine
   "open"    :open
   "read"    :read    "skim"    :read
   "go"      :go      "walk"    :go
   "run"     :go      "proceed" :go    "step" :go
   "quit"    :quit    "q"       :quit})

;;; ---------------------------------------------------------------------------
;;; Object lookup — find an object key by word, searching :synonyms
;;; ---------------------------------------------------------------------------

(defn find-object [word]
  (let [kw (keyword word)]
    (some (fn [[obj-key obj]]
            (when (contains? (:synonyms obj) kw)
              obj-key))
          (:objects @world/world))))

;;; ---------------------------------------------------------------------------
;;; parse — string -> {:verb :v :obj :o :dir :d}
;;; ---------------------------------------------------------------------------

(defn parse [input]
  (let [tokens (str/split (str/lower-case (str/trim input)) #"\s+")
        [first-word & rest-words] tokens]
    (cond
      ;; bare direction: "north", "n", "sw" etc.
      (directions first-word)
      {:verb :go :dir (directions first-word)}

      ;; known verb
      (verbs first-word)
      (let [verb (verbs first-word)
            next-word (first rest-words)]
        (cond
          ;; go + direction: "go north"
          (and (= verb :go) (directions next-word))
          {:verb :go :dir (directions next-word)}

          ;; verb + object word
          next-word
          {:verb verb :obj (find-object next-word)}

          ;; bare verb: "look", "quit"
          :else
          {:verb verb}))

      ;; unrecognised
      :else
      {:verb :unknown :input input})))
