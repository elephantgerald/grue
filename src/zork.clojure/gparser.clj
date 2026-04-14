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

(def prepositions #{"in" "into" "inside" "on" "onto"})

(def verbs
  {"look"      :look    "l"         :look
   "stare"     :look    "gaze"      :look
   "examine"   :examine "x"         :examine
   "describe"  :examine "what"      :examine
   "open"      :open
   "read"      :read    "skim"      :read
   "take"      :take    "get"       :take
   "pick"      :take    "carry"     :take
   "put"       :put     "place"     :put    "insert" :put
   "drop"      :drop    "throw"     :drop
   "close"     :close   "shut"      :close
   "inventory" :inventory "i"       :inventory
   "go"        :go      "walk"      :go
   "run"       :go      "proceed"   :go    "step" :go
   "climb"     :climb
   "wait"      :wait    "z"         :wait
   "again"     :again   "g"         :again
   "diagnose"  :diagnose
   "move"      :move    "push"      :move  "pull" :move
   "score"     :score
   "quit"      :quit    "q"         :quit
   "light"     :lamp-on
   "extinguish" :lamp-off "douse"   :lamp-off})

;;; ---------------------------------------------------------------------------
;;; Object lookup — mirrors ZIL SEARCH-LIST / DO-SL (gparser.zil:1202).
;;; Only accessible objects are searched: those directly at @here or @winner,
;;; or inside open/transparent containers reachable from those locations.
;;; Returns :not-found when no accessible object matches the word.
;;; ---------------------------------------------------------------------------

(defn- accessible? [obj-key]
  ;; An object is in scope if it is:
  ;;   - directly in the current room (@here)
  ;;   - in player inventory: location = :winner (v-take sentinel) or @winner (:adventurer)
  ;;   - inside an open/transparent container that is itself accessible
  (let [obj (get-in @world/world [:objects obj-key])
        loc (:location obj)]
    (cond
      (nil? loc) false
      (or (= loc @world/here)
          (= loc :winner)
          (= loc @world/winner)) true
      ;; inside a container — container must itself be accessible and open/transparent
      :else (let [container (get-in @world/world [:objects loc])]
              (and container
                   (or (contains? (:flags container) :openbit)
                       (contains? (:flags container) :transbit))
                   (accessible? loc))))))

(defn find-object [word]
  (let [kw (keyword word)
        result (some (fn [[obj-key obj]]
                       (when (and (contains? (:synonyms obj) kw)
                                  (accessible? obj-key))
                         obj-key))
                     (:objects @world/world))]
    (or result :not-found)))

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
      (let [verb      (verbs first-word)
            next-word (first rest-words)
            obj-word  (second rest-words)]
        (cond
          ;; go + direction: "go north"
          (and (= verb :go) (directions next-word))
          {:verb :go :dir (directions next-word)}

          ;; look at OBJECT → examine
          (and (= verb :look) (= next-word "at") obj-word)
          {:verb :examine :obj (find-object obj-word)}

          ;; look in/inside OBJECT → look-in
          (and (= verb :look) (#{"in" "inside"} next-word) obj-word)
          {:verb :look-in :obj (find-object obj-word)}

          ;; look with unrecognised extra words
          (and (= verb :look) next-word)
          {:verb :look :obj :unrecognised}

          ;; bare look
          (= verb :look)
          {:verb :look}

          ;; put X in/into/on Y  — two-object command
          (and (= verb :put)
               (>= (count rest-words) 3)
               (prepositions (second rest-words)))
          {:verb      verb
           :obj       (find-object (first rest-words))
           :container (find-object (nth rest-words 2))}

          ;; verb + object word
          next-word
          {:verb verb :obj (find-object next-word)}

          ;; bare verb: "quit", "inventory"
          :else
          {:verb verb}))

      ;; unrecognised
      :else
      {:verb :unknown :input input})))
