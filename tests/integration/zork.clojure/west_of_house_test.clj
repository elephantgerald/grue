(ns west-of-house-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [zork1.actions :as z]
            [gparser :as p]
            [gmain :as g]))

;;; ---------------------------------------------------------------------------
;;; Helpers
;;; ---------------------------------------------------------------------------

(defn reset-world! []
  (z/load-world!)
  (reset! z/here :west-of-house)
  (reset! z/winner :adventurer))

(defn game-output
  "Run a command string through the full pipeline, return trimmed stdout."
  [input]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (g/perform (p/parse input)))
    (str/trim (str sw))))

;;; Reset world before every test — each test starts from scratch
(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; LOOK — verified against DOSBox revision 88 / serial 840726
;;; ---------------------------------------------------------------------------

(deftest look-describes-room
  (is (= (str "West of House\n"
              "You are standing in an open field west of a white house, "
              "with a boarded front door.\n"
              "There is a small mailbox here.")
         (game-output "look"))))

(deftest look-synonyms
  (testing "l is an alias for look"
    (is (= (game-output "look") (game-output "l")))))

(deftest look-object-unrecognised
  (is (= "That sentence isn't one I recognize."
         (game-output "look mailbox"))))

;;; ---------------------------------------------------------------------------
;;; OPEN
;;; ---------------------------------------------------------------------------

(deftest open-mailbox-reveals-leaflet
  (is (= "Opening the small mailbox reveals a leaflet."
         (game-output "open mailbox"))))

(deftest open-mailbox-already-open
  (game-output "open mailbox")
  (is (= "It is already open."
         (game-output "open mailbox"))))

;;; ---------------------------------------------------------------------------
;;; LOOK AT / LOOK IN — verified against DOSBox
;;; ---------------------------------------------------------------------------

(deftest look-at-closed-mailbox
  (is (= "The small mailbox is closed."
         (game-output "look at mailbox"))))

(deftest look-at-open-mailbox-with-leaflet
  (game-output "open mailbox")
  (is (= "The small mailbox contains:\n  A leaflet"
         (game-output "look at mailbox"))))

(deftest look-at-open-empty-mailbox
  (game-output "open mailbox")
  (game-output "take leaflet")
  (is (= "The small mailbox is empty."
         (game-output "look at mailbox"))))

(deftest look-in-closed-mailbox
  (is (= "The small mailbox is closed."
         (game-output "look in mailbox"))))

;;; ---------------------------------------------------------------------------
;;; TAKE
;;; ---------------------------------------------------------------------------

(deftest take-mailbox-is-anchored
  (is (= "It is securely anchored."
         (game-output "take mailbox"))))

(deftest take-leaflet-requires-open-mailbox
  (game-output "open mailbox")
  (is (= "Taken." (game-output "take leaflet"))))

(deftest take-leaflet-already-held
  (game-output "open mailbox")
  (game-output "take leaflet")
  (is (= "You already have that."
         (game-output "take leaflet"))))

;;; ---------------------------------------------------------------------------
;;; READ
;;; ---------------------------------------------------------------------------

(deftest read-leaflet
  (game-output "open mailbox")
  (game-output "take leaflet")
  (is (= (str "\"WELCOME TO ZORK!\n\n"
              "ZORK is a game of adventure, danger, and low cunning. "
              "In it you will explore some of the most amazing territory "
              "ever seen by mortals. No computer should be without one!\"")
         (game-output "read leaflet"))))

;;; ---------------------------------------------------------------------------
;;; EXAMINE
;;; ---------------------------------------------------------------------------

(deftest examine-no-object
  (is (= "What do you want to examine?"
         (game-output "examine"))))

(deftest examine-closed-mailbox
  (is (= "The small mailbox is closed."
         (game-output "examine mailbox"))))

(deftest examine-leaflet
  (game-output "open mailbox")
  (game-output "take leaflet")
  (is (= (str "\"WELCOME TO ZORK!\n\n"
              "ZORK is a game of adventure, danger, and low cunning. "
              "In it you will explore some of the most amazing territory "
              "ever seen by mortals. No computer should be without one!\"")
         (game-output "examine leaflet"))))

;;; ---------------------------------------------------------------------------
;;; CLOSE
;;; ---------------------------------------------------------------------------

(deftest close-mailbox
  (game-output "open mailbox")
  (is (= "Closed." (game-output "close mailbox"))))

(deftest close-mailbox-already-closed
  (is (= "It is already closed."
         (game-output "close mailbox"))))

;;; ---------------------------------------------------------------------------
;;; PUT
;;; ---------------------------------------------------------------------------

(deftest put-leaflet-back-in-mailbox
  (game-output "open mailbox")
  (game-output "take leaflet")
  (is (= "Done." (game-output "put leaflet in mailbox"))))

;;; ---------------------------------------------------------------------------
;;; Movement
;;; ---------------------------------------------------------------------------

(deftest east-is-blocked
  (is (= "The door is boarded and you can't remove the boards."
         (game-output "east"))))

(deftest unrecognised-direction
  (is (= "You can't go that way."
         (game-output "go up"))))
