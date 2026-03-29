(ns zork1.actions
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

;;; ---------------------------------------------------------------------------
;;; World state — mirrors ZIL globals
;;; ---------------------------------------------------------------------------

(def world  (atom nil))            ; loaded from 1dungeon.edn
(def here   (atom :west-of-house)) ; current room  — ZIL: HERE
(def winner (atom :adventurer))    ; current actor — ZIL: WINNER

;;; ---------------------------------------------------------------------------
;;; World loading
;;; ---------------------------------------------------------------------------

(defn load-world! []
  (reset! world (edn/read-string (slurp (io/resource "1dungeon.edn")))))

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
                (println (str (clojure.string/join ", "
                               (map (fn [[_ o]] (str "a " (:desc o))) contents))
                              ".")))))))))

;;; ---------------------------------------------------------------------------
;;; V-READ — ZIL: ROUTINE V-READ
;;; ---------------------------------------------------------------------------

(defn v-read [obj-key]
  (let [obj (get-object obj-key)]
    (if (flag? obj :readbit)
      (println (:text obj))
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
;;; First visit (no :touchbit): full description. Revisit: brief.
;;; ---------------------------------------------------------------------------

(defn arrive! []
  (if (nil? (get-room @here))
    (println "That part of the world isn't implemented yet.")
    (if (flag? (get-room @here) :touchbit)
      (v-look-brief)
      (v-look))))

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
        (println "Closed.")))))

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
        (println "Done.")))))

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

      ;; TAKEBIT — take it
      (flag? obj :takebit)
      (do (swap! world assoc-in [:objects obj-key :location] :winner)
          (println "Taken."))

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
          (arrive!))

      ;; conditional on a global flag: {:to :room :if :flag}
      (and (map? exit) (:if exit))
      (if (get-in @world [:flags (:if exit)])
        (do (reset! here (:to exit))
            (arrive!))
        (println "You can't go that way."))

      ;; conditional on an object's :openbit: {:to :room :if-open :obj-key}
      ;; on failure: "The [desc] is closed." — ZIL generates this from the object
      (and (map? exit) (:if-open exit))
      (let [obj (get-object (:if-open exit))]
        (if (flag? obj :openbit)
          (do (reset! here (:to exit))
              (arrive!))
          (println (str "The " (:desc obj) " is closed.")))))))
