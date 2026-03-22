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
        (println "It is empty.")
        (do
          (println (str "The " (:desc obj) " contains:"))
          (doseq [[_ o] contents]
            (println (str "  A " (:desc o) "."))))))))

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
;;; V-LOOK — ZIL: ROUTINE V-LOOK
;;; ---------------------------------------------------------------------------

(defn v-look []
  (let [room (get-room @here)]
    (println (:desc room))
    (room-action (:action room) :m-look)
    (doseq [[_ obj] (objects-in @here)]
      (when-let [ldesc (:ldesc obj)]
        (println ldesc)))))

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
          (v-look))

      (map? exit)
      (if (get-in @world [:flags (:if exit)])
        (do (reset! here (:to exit))
            (v-look))
        (println "You can't go that way.")))))
