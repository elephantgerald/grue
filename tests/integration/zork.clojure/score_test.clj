(ns score-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [zork1.actions :as z]
            [gparser :as p]
            [gmain :as g]))

;;; ---------------------------------------------------------------------------
;;; Helpers
;;; ---------------------------------------------------------------------------

(defn reset-world! []
  (z/load-world!)
  (reset! z/here :west-of-house)
  (reset! z/winner :adventurer)
  (reset! g/last-cmd nil))

(defn do! [input]
  (g/perform (p/parse input)))

(defn output-of [thunk]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (thunk))
    (clojure.string/trim (str sw))))

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; Initial state
;;; ---------------------------------------------------------------------------

(deftest score-starts-at-zero
  (is (= 0 @z/score)))

(deftest base-score-starts-at-zero
  (is (= 0 @z/base-score)))

;;; ---------------------------------------------------------------------------
;;; V-SCORE display — ZIL: ROUTINE V-SCORE (1actions.zil)
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest score-command-format-at-zero
  (let [out (output-of #(do! "score"))]
    (is (clojure.string/includes? out "Your score is 0 (total of 350 points), in 0 moves."))
    (is (clojure.string/includes? out "This gives you the rank of Beginner."))))

(deftest score-command-singular-move
  (do! "north")
  (let [out (output-of #(do! "score"))]
    (is (clojure.string/includes? out "in 1 move."))))

;;; ---------------------------------------------------------------------------
;;; Rank table — ZIL: COND block in V-SCORE
;;; Thresholds: =350 Master Adventurer, >330 Wizard, >300 Master,
;;;             >200 Adventurer, >100 Junior Adventurer, >50 Novice Adventurer,
;;;             >25 Amateur Adventurer, else Beginner
;;; ---------------------------------------------------------------------------

(deftest rank-beginner-at-zero
  (reset! z/score 0)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Beginner.")))

(deftest rank-beginner-at-25
  (reset! z/score 25)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Beginner.")))

(deftest rank-amateur-above-25
  (reset! z/score 26)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Amateur Adventurer.")))

(deftest rank-novice-above-50
  (reset! z/score 51)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Novice Adventurer.")))

(deftest rank-junior-above-100
  (reset! z/score 101)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Junior Adventurer.")))

(deftest rank-adventurer-above-200
  (reset! z/score 201)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Adventurer.")))

(deftest rank-master-above-300
  (reset! z/score 301)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Master.")))

(deftest rank-wizard-above-330
  (reset! z/score 331)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Wizard.")))

(deftest rank-master-adventurer-at-350
  (reset! z/score 350)
  (is (clojure.string/includes? (output-of #(do! "score")) "rank of Master Adventurer.")))

;;; ---------------------------------------------------------------------------
;;; score-upd — ZIL: ROUTINE SCORE-UPD
;;; ---------------------------------------------------------------------------

(deftest score-upd-increments-score
  (z/score-upd 10)
  (is (= 10 @z/score)))

(deftest score-upd-increments-base-score
  (z/score-upd 10)
  (is (= 10 @z/base-score)))

(deftest score-upd-is-additive
  (z/score-upd 10)
  (z/score-upd 5)
  (is (= 15 @z/score))
  (is (= 15 @z/base-score)))

;;; ---------------------------------------------------------------------------
;;; score-obj — ZIL: ROUTINE SCORE-OBJ
;;; Awards object's :value once, then zeroes it so repeat pickups don't score.
;;; ---------------------------------------------------------------------------

(deftest score-obj-awards-value-on-first-call
  ;; Give the leaflet a value directly to test score-obj in isolation
  (swap! z/world assoc-in [:objects :leaflet :value] 5)
  (z/score-obj :leaflet)
  (is (= 5 @z/score)))

(deftest score-obj-zeroes-value-after-awarding
  (swap! z/world assoc-in [:objects :leaflet :value] 5)
  (z/score-obj :leaflet)
  (z/score-obj :leaflet)  ; second call — value already 0
  (is (= 5 @z/score)))    ; still 5, not 10

(deftest score-obj-no-effect-when-value-is-zero
  ;; mailbox has no :value key → defaults to 0, no score change
  (z/score-obj :mailbox)
  (is (= 0 @z/score)))

;;; ---------------------------------------------------------------------------
;;; score does not cost a turn — meta-command
;;; ---------------------------------------------------------------------------

(deftest score-command-costs-no-turn
  (do! "score")
  (is (= 0 @z/turns)))
