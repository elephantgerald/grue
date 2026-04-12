(ns zork1.actions
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;;; ---------------------------------------------------------------------------
;;; World state — mirrors ZIL globals
;;; ---------------------------------------------------------------------------

(def world       (atom nil))            ; loaded from 1dungeon.edn
(def here        (atom :west-of-house)) ; current room  — ZIL: HERE
(def winner      (atom :adventurer))    ; current actor — ZIL: WINNER
(def turns       (atom 0))              ; move counter  — ZIL: MOVES
(def score       (atom 0))              ; current score — ZIL: SCORE
(def base-score  (atom 0))              ; pickup total  — ZIL: BASE-SCORE
(def won-flag    (atom false))          ; endgame flag  — ZIL: WON-FLAG
(def rug-moved   (atom false))          ; rug moved in living room — ZIL: RUG-MOVED

;;; ---------------------------------------------------------------------------
;;; World loading
;;; ---------------------------------------------------------------------------

(defn load-world! []
  (reset! world (edn/read-string (slurp (io/resource "1dungeon.edn"))))
  (reset! turns 0)
  (reset! score 0)
  (reset! base-score 0)
  (reset! won-flag false)
  (reset! rug-moved false))

;;; ---------------------------------------------------------------------------
;;; World queries
;;; ---------------------------------------------------------------------------

(defn get-room [room-key]
  (get-in @world [:rooms room-key]))

(defn get-object [obj-key]
  (get-in @world [:objects obj-key]))

(defn objects-in [location-key]
  (filter (fn [[_ obj]] (= (:location obj) location-key))
          (:objects @world)))

;;; flag? mirrors ZIL FSET?
(defn flag? [obj flag]
  (contains? (:flags obj) flag))

;;; ---------------------------------------------------------------------------
;;; SCORE-UPD / SCORE-OBJ — ZIL: ROUTINE SCORE-UPD / ROUTINE SCORE-OBJ
;;; ---------------------------------------------------------------------------

;;; ZIL: SCORE-UPD — add n to both base-score and score.
;;; At 350 points (first time): set WON-FLAG, clear MAP :invisible, clear
;;; WEST-OF-HOUSE :touchbit, and print the endgame whisper (gverbs.zil:1855–1866).
(defn score-upd [n]
  (swap! base-score + n)
  (swap! score + n)
  (when (and (= @score 350) (not @won-flag))
    (reset! won-flag true)
    (swap! world assoc-in [:flags :won-flag] true)
    (swap! world update-in [:objects :map :flags] (fnil disj #{}) :invisible)
    (swap! world update-in [:rooms :west-of-house :flags] disj :touchbit)
    (println "An almost inaudible voice whispers in your ear, \"Look to your treasures for the final secret.\"")))

;;; ZIL: SCORE-OBJ — award entity's :value points (once only: zeroed after).
;;; Works for both objects (:objects) and rooms (:rooms), matching ZIL where
;;; rooms and objects share the same property table (V-WALK calls SCORE-OBJ on
;;; the destination room, gverbs.zil:2122).
(defn score-obj [entity-key]
  (let [path (if (get-in @world [:objects entity-key])
               [:objects entity-key :value]
               [:rooms entity-key :value])
        v    (get-in @world path 0)]
    (when (pos? v)
      (score-upd v)
      (swap! world assoc-in path 0))))

;;; ---------------------------------------------------------------------------
;;; Room action handlers — ZIL: ROUTINE WEST-HOUSE (RARG)
;;; Dispatch on room key, called with a message keyword (:m-look etc.)
;;; ---------------------------------------------------------------------------

(defmulti room-action (fn [room-key _msg] room-key))

(defmethod room-action :west-house [_ msg]
  (when (= msg :m-look)
    (println "You are standing in an open field west of a white house, with a boarded front door.")))

;;; EAST-HOUSE — description depends on whether kitchen window is open
(defmethod room-action :east-house [_ msg]
  (when (= msg :m-look)
    (let [window (get-object :kitchen-window)]
      (print "You are behind the white house. A path leads into the forest to the east. In one corner of the house there is a small window which is ")
      (if (flag? window :openbit)
        (println "open.")
        (println "slightly ajar.")))))

;;; FOREST-ROOM — M-LOOK handled by :ldesc on the room; handler is for climbing only
(defmethod room-action :forest-room [_ _] nil)

;;; TREE-ROOM — ZIL: ROUTINE TREE-ROOM (RARG)
(defmethod room-action :tree-room [_ msg]
  (when (= msg :m-look)
    (println "You are about 10 feet above the ground nestled among some large branches. The nearest branch above you is above your reach.")))

;;; CLEARING-FCN — ZIL: ROUTINE CLEARING-FCN (RARG)
;;; M-LOOK: base clearing description. Grate state handled when grate is implemented.
(defmethod room-action :clearing-fcn [_ msg]
  (when (= msg :m-look)
    (println "You are in a clearing, with a forest surrounding you on all sides. A path leads south.")))

;;; STONE-BARROW-FCN — M-LOOK handled by :ldesc; handler is for entering only
(defmethod room-action :stone-barrow-fcn [_ _] nil)

;;; KITCHEN-FCN — ZIL: ROUTINE KITCHEN-FCN (RARG) in 1actions.zil:385
;;; M-LOOK description interpolates kitchen-window state.
(defmethod room-action :kitchen-fcn [_ msg]
  (when (= msg :m-look)
    (let [window (get-object :kitchen-window)]
      (print "You are in the kitchen of the white house. A table seems to\nhave been used recently for the preparation of food. A passage\nleads to the west and a dark staircase can be seen leading\nupward. A dark chimney leads down and to the east is a small\nwindow which is ")
      (if (flag? window :openbit)
        (println "open.")
        (println "slightly ajar.")))))

;;; LIVING-ROOM-FCN — ZIL: ROUTINE LIVING-ROOM-FCN (RARG) in 1actions.zil:449
;;; M-LOOK description varies based on rug-moved flag and trap-door openbit.
(defmethod room-action :living-room-fcn [_ msg]
  (when (= msg :m-look)
    (let [rug-moved  @rug-moved
          door-open  (flag? (get-object :trap-door) :openbit)]
      (print "You are in the living room. There is a doorway to the east, a wooden\ndoor with strange gothic lettering to the west, which appears to be\nnailed shut, a trophy case, ")
      (println (cond
                 (and rug-moved door-open) "and a rug lying beside an open trap door."
                 rug-moved                 "and a closed trap door at your feet."
                 door-open                 "and an open trap door at your feet."
                 :else                     "and a large oriental rug in the center of the room.")))))

;;; CELLAR-FCN — ZIL: ROUTINE CELLAR-FCN (RARG) in 1actions.zil:531
;;; M-LOOK: fixed description.
;;; M-ENTER: if trap-door was open and untouched, crash it shut (1actions.zil:537-543).
(defmethod room-action :cellar-fcn [_ msg]
  (case msg
    :m-look
    (println "You are in a dark and damp cellar with a narrow passageway leading\nnorth, and a crawlway to the south. On the west is the bottom of a\nsteep metal ramp which is unclimbable.")

    :m-enter
    (let [trap (get-object :trap-door)]
      (when (and (flag? trap :openbit) (not (flag? trap :touchbit)))
        (swap! world update-in [:objects :trap-door :flags] #(-> % (disj :openbit) (conj :touchbit)))
        (println "The trap door crashes shut, and you hear someone barring it.")
        (println)))

    nil))

;;; Default — room has no special handler
(defmethod room-action :default [_ _] nil)

;;; ---------------------------------------------------------------------------
;;; Object action handlers — ZIL: ROUTINE MAILBOX-F ()
;;; Dispatch on object key, called with the current verb keyword.
;;; Return :m-handled to stop further dispatch, nil to pass through.
;;; ---------------------------------------------------------------------------

(defmulti object-action (fn [obj-key _verb] obj-key))

(defmethod object-action :mailbox [_ verb]
  (when (= verb :take)
    (println "It is securely anchored.")
    :m-handled))

;;; Default — object has no special handler
(defmethod object-action :default [_ _] nil)

;;; ---------------------------------------------------------------------------
;;; V-LOOK-INSIDE — ZIL: ROUTINE V-LOOK-INSIDE
;;; ---------------------------------------------------------------------------

(defn v-look-inside [obj-key]
  (let [obj      (get-object obj-key)
        contents (objects-in obj-key)]
    (if-not (flag? obj :openbit)
      (println (str "The " (:desc obj) " is closed."))
      (if (empty? contents)
        (println (str "The " (:desc obj) " is empty."))
        (do
          (println (str "The " (:desc obj) " contains:"))
          (doseq [[_ o] contents]
            (println (str "  A " (:desc o)))))))))

;;; ---------------------------------------------------------------------------
;;; V-EXAMINE — ZIL: ROUTINE V-EXAMINE
;;; ---------------------------------------------------------------------------

(defn v-examine [obj-key]
  (let [obj (get-object obj-key)]
    (cond
      (:text obj)          (println (:text obj))
      (flag? obj :contbit) (v-look-inside obj-key)
      :else                (println (str "There's nothing special about the "
                                         (:desc obj) ".")))))

;;; ---------------------------------------------------------------------------
;;; V-OPEN — ZIL: ROUTINE V-OPEN
;;; ---------------------------------------------------------------------------

(defn v-open [obj-key]
  (let [obj (get-object obj-key)]
    (cond
      (not (or (flag? obj :contbit)
               (flag? obj :doorbit)))
      (println (str "You must tell me how to do that to a " (:desc obj) "."))

      (flag? obj :openbit)
      (println "It is already open.")

      :else
      (do
        (swap! world update-in [:objects obj-key :flags] conj :openbit :touchbit)
        (let [contents (seq (objects-in obj-key))]
          (cond
            ;; one untouched item with an fdesc — show fdesc
            (and (= 1 (count contents))
                 (not (flag? (val (first contents)) :touchbit))
                 (:fdesc (val (first contents))))
            (do (println (str "The " (:desc obj) " opens."))
                (println (:fdesc (val (first contents)))))

            ;; empty or transparent
            (nil? contents)
            (println "Opened.")

            ;; anything else — list contents
            :else
            (do (print (str "Opening the " (:desc obj) " reveals "))
                (println (str (str/join ", "
                               (map (fn [[_ o]] (str "a " (:desc o))) contents))
                              "."))))
          :turn)))))

;;; ---------------------------------------------------------------------------
;;; V-READ — ZIL: ROUTINE V-READ
;;; ---------------------------------------------------------------------------

(defn v-read [obj-key]
  (let [obj (get-object obj-key)]
    (if (flag? obj :readbit)
      (do (println (:text obj)) :turn)
      (println (str "How does one read a " (:desc obj) "?")))))

;;; ---------------------------------------------------------------------------
;;; V-LOOK — ZIL: ROUTINE V-LOOK + DESCRIBE-OBJECTS
;;; Always shows full description. Sets :touchbit to mark room as visited.
;;; Rooms with :ldesc print it directly. Rooms with :action call the handler.
;;; ---------------------------------------------------------------------------

(defn describe-objects []
  (doseq [[_ obj] (objects-in @here)]
    (cond
      (:ldesc obj)                (println (:ldesc obj))
      (not (flag? obj :ndescbit)) (println (str "There is a " (:desc obj) " here.")))))

(defn v-look []
  (swap! world update-in [:rooms @here :flags] conj :touchbit)
  (let [room (get-room @here)]
    (println (:desc room))
    (if (:ldesc room)
      (println (:ldesc room))
      (room-action (:action room) :m-look))
    (describe-objects)))

;;; ---------------------------------------------------------------------------
;;; V-LOOK-BRIEF — room name + objects only, no long description
;;; ZIL: DESCRIBE-ROOM when TOUCHBIT already set (room previously visited)
;;; ---------------------------------------------------------------------------

(defn v-look-brief []
  (println (:desc (get-room @here)))
  (describe-objects))

;;; ---------------------------------------------------------------------------
;;; ARRIVE! — called on entering a room via movement
;;; Fires M-ENTER on the room action handler (side effects: trap-door close, etc.)
;;; then shows full or brief description depending on :touchbit.
;;; ---------------------------------------------------------------------------

(defn arrive! []
  (if (nil? (get-room @here))
    (println "That part of the world isn't implemented yet.")
    (do
      (room-action (:action (get-room @here)) :m-enter)
      (if (flag? (get-room @here) :touchbit)
        (v-look-brief)
        (v-look)))))

;;; ---------------------------------------------------------------------------
;;; V-CLOSE — ZIL: ROUTINE V-CLOSE
;;; ---------------------------------------------------------------------------

(defn v-close [obj-key]
  (let [obj (get-object obj-key)]
    (cond
      (not (or (flag? obj :contbit)
               (flag? obj :doorbit)))
      (println (str "You must tell me how to do that to a " (:desc obj) "."))

      (not (flag? obj :openbit))
      (println "It is already closed.")

      :else
      (do
        (swap! world update-in [:objects obj-key :flags] disj :openbit)
        (println "Closed.")
        :turn))))

;;; ---------------------------------------------------------------------------
;;; V-PUT — ZIL: ROUTINE V-PUT
;;; Move prso into prsi (a container), checking open and capacity.
;;; ---------------------------------------------------------------------------

(defn v-put [obj-key container-key]
  (let [obj       (get-object obj-key)
        container (get-object container-key)]
    (cond
      (nil? container)
      (println "I don't know what you mean.")

      (not (= (:location obj) :winner))
      (println "You're not holding that.")

      (not (flag? container :contbit))
      (println (str "You can't put anything in the " (:desc container) "."))

      (not (flag? container :openbit))
      (println (str "The " (:desc container) " is closed."))

      :else
      (do
        (swap! world assoc-in [:objects obj-key :location] container-key)
        (score-obj obj-key)
        (println "Done.")
        :turn))))

;;; ---------------------------------------------------------------------------
;;; V-TAKE — ZIL: ROUTINE V-TAKE + ITAKE
;;; TRYTAKEBIT: call action handler first — if :m-handled, stop.
;;; TAKEBIT:    move to player inventory (:winner).
;;; ---------------------------------------------------------------------------

(defn v-take [obj-key]
  (let [obj (get-object obj-key)]
    (cond
      ;; already carrying it
      (= (:location obj) :winner)
      (println "You already have that.")

      ;; TRYTAKEBIT — give the object a chance to block it
      (and (flag? obj :trytakebit)
           (= :m-handled (object-action obj-key :take)))
      nil

      ;; TAKEBIT — take it; award any pickup score (ZIL: SCORE-OBJ in ITAKE)
      (flag? obj :takebit)
      (do (swap! world assoc-in [:objects obj-key :location] :winner)
          (score-obj obj-key)
          (println "Taken.")
          :turn)

      :else
      (println "You can't take that."))))

;;; ---------------------------------------------------------------------------
;;; V-INVENTORY — ZIL: ROUTINE V-INVENTORY
;;; ---------------------------------------------------------------------------

(defn v-inventory []
  (let [carrying (seq (objects-in :winner))]
    (if-not carrying
      (println "You are empty-handed.")
      (do
        (println "You are carrying:")
        (doseq [[_ obj] carrying]
          (println (str "  A " (:desc obj))))))))

;;; ---------------------------------------------------------------------------
;;; V-DROP — ZIL: ROUTINE V-DROP
;;; ---------------------------------------------------------------------------

(defn v-drop [obj-key]
  (let [obj (get-object obj-key)]
    (if (not= (:location obj) :winner)
      (println "You're not holding that.")
      (do
        (swap! world assoc-in [:objects obj-key :location] @here)
        (println "Dropped.")
        :turn))))

;;; ---------------------------------------------------------------------------
;;; V-WAIT — ZIL: ROUTINE V-WAIT
;;; ---------------------------------------------------------------------------

(defn v-wait []
  (println "Time passes.")
  :turn)

;;; ---------------------------------------------------------------------------
;;; V-DIAGNOSE — ZIL: ROUTINE V-DIAGNOSE
;;; ---------------------------------------------------------------------------

(defn v-diagnose []
  (println "You are in perfect health."))

;;; ---------------------------------------------------------------------------
;;; V-MOVE — ZIL: ROUTINE V-MOVE (push/pull/move)
;;; ---------------------------------------------------------------------------

(defn v-move [obj-key]
  (let [obj (get-object obj-key)]
    (cond
      (nil? obj)
      (println "I don't see that here.")

      (not (#{@here :winner} (:location obj)))
      (println "I don't see that here.")

      :else
      (println "You can't move that."))))

;;; ---------------------------------------------------------------------------
;;; V-SCORE — ZIL: ROUTINE V-SCORE in 1actions.zil
;;; ---------------------------------------------------------------------------

(defn v-score []
  (println (str "Your score is " @score " (total of 350 points), in " @turns
                (if (= @turns 1) " move." " moves.")))
  (println (str "This gives you the rank of "
                (cond
                  (= @score 350) "Master Adventurer"
                  (> @score 330) "Wizard"
                  (> @score 300) "Master"
                  (> @score 200) "Adventurer"
                  (> @score 100) "Junior Adventurer"
                  (> @score 50)  "Novice Adventurer"
                  (> @score 25)  "Amateur Adventurer"
                  :else          "Beginner")
                "."))
  @score)

;;; ---------------------------------------------------------------------------
;;; V-WALK — ZIL: ROUTINE V-WALK
;;; Exit types: keyword (go), string (blocked), map {:to :room :if :flag}
;;; ---------------------------------------------------------------------------

(defn v-walk [direction]
  (let [exit (get-in (get-room @here) [:exits direction])]
    (cond
      (nil? exit)
      (println "You can't go that way.")

      (string? exit)
      (println exit)

      (keyword? exit)
      (do (reset! here exit)
          (score-obj exit)
          (arrive!)
          :turn)

      ;; conditional on a global flag: {:to :room :if :flag}
      (and (map? exit) (:if exit))
      (if (get-in @world [:flags (:if exit)])
        (do (reset! here (:to exit))
            (score-obj (:to exit))
            (arrive!)
            :turn)
        (println "You can't go that way."))

      ;; conditional on an object's :openbit: {:to :room :if-open :obj-key}
      ;; on failure: "The [desc] is closed." — ZIL generates this from the object
      (and (map? exit) (:if-open exit))
      (let [obj (get-object (:if-open exit))]
        (if (flag? obj :openbit)
          (do (reset! here (:to exit))
              (score-obj (:to exit))
              (arrive!)
              :turn)
          ;; Invisible objects (e.g. trap door before rug is moved) don't
          ;; reveal themselves in the blocked message — ZIL falls through to
          ;; "You can't go that way."
          (if (flag? obj :invisible)
            (println "You can't go that way.")
            (println (str "The " (:desc obj) " is closed."))))))))
