(ns world-objects-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [zork1.actions :as z]))

(defn reset-world! []
  (z/load-world!))

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; Takeable items — must exist with :takebit
;;; ---------------------------------------------------------------------------

(deftest sword-exists-and-takeable
  (let [obj (z/get-object :sword)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest lamp-exists-and-takeable
  (let [obj (z/get-object :lamp)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest rope-exists-and-takeable
  (let [obj (z/get-object :rope)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest knife-exists-and-takeable
  (let [obj (z/get-object :knife)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest bottle-exists-and-takeable
  (let [obj (z/get-object :bottle)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest garlic-exists-and-takeable
  (let [obj (z/get-object :garlic)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest painting-exists-and-takeable
  (let [obj (z/get-object :painting)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest coffin-exists-and-takeable
  (let [obj (z/get-object :coffin)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest bag-of-coins-exists-and-takeable
  (let [obj (z/get-object :bag-of-coins)]
    (is (some? obj))
    (is (contains? (:flags obj) :takebit))))

(deftest rug-exists
  (is (some? (z/get-object :rug))))

;;; ---------------------------------------------------------------------------
;;; Initial locations
;;; ---------------------------------------------------------------------------

(deftest sword-starts-in-living-room
  (is (= :living-room (:location (z/get-object :sword)))))

(deftest lamp-starts-in-living-room
  (is (= :living-room (:location (z/get-object :lamp)))))

(deftest rope-starts-in-attic
  (is (= :attic (:location (z/get-object :rope)))))

(deftest knife-starts-on-attic-table
  (is (= :attic-table (:location (z/get-object :knife)))))

(deftest bag-of-coins-starts-in-maze-5
  (is (= :maze-5 (:location (z/get-object :bag-of-coins)))))

(deftest rug-starts-in-living-room
  (is (= :living-room (:location (z/get-object :rug)))))

(deftest painting-starts-in-gallery
  (is (= :gallery (:location (z/get-object :painting)))))

(deftest coffin-starts-in-egypt-room
  (is (= :egypt-room (:location (z/get-object :coffin)))))

;;; ---------------------------------------------------------------------------
;;; Containers — must have :contbit and correct capacity
;;; ---------------------------------------------------------------------------

(deftest bottle-is-container
  (let [obj (z/get-object :bottle)]
    (is (contains? (:flags obj) :contbit))
    (is (= 4 (:capacity obj)))))

(deftest coffin-is-container
  (let [obj (z/get-object :coffin)]
    (is (contains? (:flags obj) :contbit))
    (is (= 35 (:capacity obj)))))

(deftest sandwich-bag-is-container
  (let [obj (z/get-object :sandwich-bag)]
    (is (contains? (:flags obj) :contbit))
    (is (= 9 (:capacity obj)))))

;;; ---------------------------------------------------------------------------
;;; Treasure values
;;; ---------------------------------------------------------------------------

(deftest chalice-treasure-values
  (let [obj (z/get-object :chalice)]
    (is (= 10 (:value obj)))
    (is (= 5 (:tvalue obj)))))

(deftest painting-treasure-values
  (let [obj (z/get-object :painting)]
    (is (= 4 (:value obj)))
    (is (= 6 (:tvalue obj)))))

(deftest trunk-treasure-values
  (let [obj (z/get-object :trunk)]
    (is (= 15 (:value obj)))
    (is (= 5 (:tvalue obj)))))

;;; ---------------------------------------------------------------------------
;;; NPCs — must have :actorbit
;;; ---------------------------------------------------------------------------

(deftest troll-is-actor
  (let [obj (z/get-object :troll)]
    (is (some? obj))
    (is (contains? (:flags obj) :actorbit))))

(deftest thief-is-actor
  (let [obj (z/get-object :thief)]
    (is (some? obj))
    (is (contains? (:flags obj) :actorbit))))

(deftest cyclops-is-actor
  (let [obj (z/get-object :cyclops)]
    (is (some? obj))
    (is (contains? (:flags obj) :actorbit))))

;;; ---------------------------------------------------------------------------
;;; Scenery globals — must exist at :local-globals
;;; ---------------------------------------------------------------------------

(deftest white-house-is-local-global
  (is (= :local-globals (:location (z/get-object :white-house)))))

(deftest board-is-local-global
  (is (= :local-globals (:location (z/get-object :board)))))

(deftest forest-is-local-global
  (is (= :local-globals (:location (z/get-object :forest)))))

(deftest grate-is-local-global
  (is (= :local-globals (:location (z/get-object :grate)))))

;;; ---------------------------------------------------------------------------
;;; :invisible flag — objects starting invisible must not auto-describe
;;; ---------------------------------------------------------------------------

(deftest trunk-starts-invisible
  ;; trunk starts invisible and must not appear when looking in its room
  (is (contains? (:flags (z/get-object :trunk)) :invisible)))

(deftest thief-starts-invisible
  ;; thief starts invisible and must not appear when looking in round-room
  (is (contains? (:flags (z/get-object :thief)) :invisible)))

(deftest scarab-starts-invisible
  (is (contains? (:flags (z/get-object :scarab)) :invisible)))

(deftest pot-of-gold-starts-invisible
  (is (contains? (:flags (z/get-object :pot-of-gold)) :invisible)))
