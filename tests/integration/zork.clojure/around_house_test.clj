(ns around-house-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [zork1.actions :as z]
            [gparser :as p]
            [gmain :as g]))

;;; ---------------------------------------------------------------------------
;;; Helpers — same pattern as west-of-house-test
;;; ---------------------------------------------------------------------------

(defn reset-world! []
  (z/load-world!)
  (reset! z/here :west-of-house)
  (reset! z/winner :adventurer))

(defn game-output [input]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (g/perform (p/parse input)))
    (str/trim (str sw))))

(defn move-to! [room-key]
  (reset! z/here room-key))

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; North of House — verified against DOSBox revision 88 / serial 840726
;;; ---------------------------------------------------------------------------

(deftest north-of-house-look
  (move-to! :north-of-house)
  (is (= (str "North of House\n"
              "You are facing the north side of a white house. "
              "There is no door here, and all the windows are boarded up. "
              "To the north a narrow path winds through the trees.")
         (game-output "look"))))

(deftest north-of-house-south-blocked
  (move-to! :north-of-house)
  (is (= "The windows are all boarded."
         (game-output "south"))))

;;; ---------------------------------------------------------------------------
;;; South of House — verified against DOSBox revision 88 / serial 840726
;;; ---------------------------------------------------------------------------

(deftest south-of-house-look
  (move-to! :south-of-house)
  (is (= (str "South of House\n"
              "You are facing the south side of a white house. "
              "There is no door here, and all the windows are boarded.")
         (game-output "look"))))

(deftest south-of-house-north-blocked
  (move-to! :south-of-house)
  (is (= "The windows are all boarded."
         (game-output "north"))))

;;; ---------------------------------------------------------------------------
;;; Behind House (East of House) — verified against DOSBox revision 88 / serial 840726
;;; ---------------------------------------------------------------------------

(deftest behind-house-look-window-ajar
  (move-to! :east-of-house)
  (is (= (str "Behind House\n"
              "You are behind the white house. A path leads into the forest "
              "to the east. In one corner of the house there is a small window "
              "which is slightly ajar.")
         (game-output "look"))))

(deftest behind-house-west-blocked-when-window-closed
  (move-to! :east-of-house)
  (is (= "You can't go that way."
         (game-output "west"))))

;;; ---------------------------------------------------------------------------
;;; Forest (west of house) — verified against DOSBox revision 88 / serial 840726
;;; ---------------------------------------------------------------------------

(deftest forest-1-look
  (move-to! :forest-1)
  (is (= (str "Forest\n"
              "This is a forest, with trees in all directions. "
              "To the east, there appears to be sunlight.")
         (game-output "look"))))

(deftest forest-1-west-blocked
  (move-to! :forest-1)
  (is (= "You would need a machete to go further west."
         (game-output "west"))))

;;; ---------------------------------------------------------------------------
;;; Walking the perimeter
;;; ---------------------------------------------------------------------------

(deftest walk-north-from-west
  (game-output "north")
  (is (= :north-of-house @z/here)))

(deftest walk-east-from-north
  (move-to! :north-of-house)
  (game-output "east")
  (is (= :east-of-house @z/here)))

(deftest walk-south-from-east
  (move-to! :east-of-house)
  (game-output "south")
  (is (= :south-of-house @z/here)))

(deftest walk-west-from-south
  (move-to! :south-of-house)
  (game-output "west")
  (is (= :west-of-house @z/here)))

(deftest walk-west-to-forest
  (game-output "west")
  (is (= :forest-1 @z/here)))
