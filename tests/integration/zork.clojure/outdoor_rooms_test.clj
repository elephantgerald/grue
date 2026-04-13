(ns outdoor-rooms-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
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

(defn game-output [input]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (g/perform (p/parse input)))
    (str/trim (str sw))))

(defn move-to! [room-key]
  (reset! z/here room-key))

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; Forest Path — verified against frotz (revision 88 / serial 840726)
;;; ---------------------------------------------------------------------------

(deftest path-look
  (move-to! :path)
  (is (= (str "Forest Path\n"
              "This is a path winding through a dimly lit forest. The path heads "
              "north-south here. One particularly large tree with some low branches "
              "stands at the edge of the path.")
         (game-output "look"))))

(deftest path-up-blocked
  (move-to! :path)
  (game-output "up")
  (is (= :up-a-tree @z/here)))

;;; ---------------------------------------------------------------------------
;;; Up a Tree — verified against frotz (revision 88 / serial 840726)
;;; ---------------------------------------------------------------------------

(deftest up-a-tree-look
  ;; Nest shows as "There is a bird's nest here." — no :ldesc, no :ndescbit.
  ;; Verified against frotz r88/840726 (nest was absent before objects were added).
  (move-to! :up-a-tree)
  (is (= (str "Up a Tree\n"
              "You are about 10 feet above the ground nestled among some large "
              "branches. The nearest branch above you is above your reach.\n"
              "There is a bird's nest here.")
         (game-output "look"))))

(deftest up-a-tree-up-blocked
  (move-to! :up-a-tree)
  (is (= "You cannot climb any higher."
         (game-output "up"))))

(deftest up-a-tree-down-returns-to-path
  (move-to! :up-a-tree)
  (game-output "down")
  (is (= :path @z/here)))

;;; ---------------------------------------------------------------------------
;;; Grating Clearing — verified against frotz (revision 88 / serial 840726)
;;; ---------------------------------------------------------------------------

(deftest grating-clearing-look
  ;; Leaves show their :ldesc when looking. Grate is :ndescbit so it doesn't appear.
  ;; Verified against frotz r88/840726 (leaves were absent before objects were added).
  (move-to! :grating-clearing)
  (is (= (str "Clearing\n"
              "You are in a clearing, with a forest surrounding you on all sides. "
              "A path leads south.\n"
              "On the ground is a pile of leaves.")
         (game-output "look"))))

(deftest grating-clearing-north-blocked
  (move-to! :grating-clearing)
  (is (= "The forest becomes impenetrable to the north."
         (game-output "north"))))

(deftest grating-clearing-down-blocked
  (move-to! :grating-clearing)
  (is (= "You can't go that way."
         (game-output "down"))))

;;; ---------------------------------------------------------------------------
;;; Forest 2 (dimly lit, east of path) — verified against frotz
;;; ---------------------------------------------------------------------------

(deftest forest-2-look
  (move-to! :forest-2)
  (is (= (str "Forest\n"
              "This is a dimly lit forest, with large trees all around.")
         (game-output "look"))))

(deftest forest-2-north-blocked
  (move-to! :forest-2)
  (is (= "The forest becomes impenetrable to the north."
         (game-output "north"))))

;;; ---------------------------------------------------------------------------
;;; Forest 3 (south of house side) — verified against frotz
;;; ---------------------------------------------------------------------------

(deftest forest-3-look
  (move-to! :forest-3)
  (is (= (str "Forest\n"
              "This is a dimly lit forest, with large trees all around.")
         (game-output "look"))))

(deftest forest-3-east-blocked
  (move-to! :forest-3)
  (is (= "The rank undergrowth prevents eastward movement."
         (game-output "east"))))

(deftest forest-3-south-blocked
  (move-to! :forest-3)
  (is (= "Storm-tossed trees block your way."
         (game-output "south"))))

;;; ---------------------------------------------------------------------------
;;; Mountains — verified against frotz
;;; ---------------------------------------------------------------------------

(deftest mountains-look
  (move-to! :mountains)
  (is (= (str "Forest\n"
              "The forest thins out, revealing impassable mountains.")
         (game-output "look"))))

(deftest mountains-east-blocked
  (move-to! :mountains)
  (is (= "The mountains are impassable."
         (game-output "east"))))

(deftest mountains-up-blocked
  (move-to! :mountains)
  (is (= "The mountains are impassable."
         (game-output "up"))))

;;; ---------------------------------------------------------------------------
;;; Clearing (east of house side) — verified against frotz
;;; ---------------------------------------------------------------------------

(deftest clearing-look
  (move-to! :clearing)
  (is (= (str "Clearing\n"
              "You are in a small clearing in a well marked forest path that "
              "extends to the east and west.")
         (game-output "look"))))

;;; ---------------------------------------------------------------------------
;;; Stone Barrow — verified against frotz
;;; ---------------------------------------------------------------------------

(deftest stone-barrow-look
  (move-to! :stone-barrow)
  (is (= (str "Stone Barrow\n"
              "You are standing in front of a massive barrow of stone. In the "
              "east face is a huge stone door which is open. You cannot see into "
              "the dark of the tomb.")
         (game-output "look"))))

(deftest stone-barrow-ne-to-west-of-house
  (move-to! :stone-barrow)
  (game-output "ne")
  (is (= :west-of-house @z/here)))
