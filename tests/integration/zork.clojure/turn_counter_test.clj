(ns turn-counter-test
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

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; Initial state
;;; ---------------------------------------------------------------------------

(deftest turns-start-at-zero
  (is (= 0 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Successful movement costs a turn
;;; ---------------------------------------------------------------------------

(deftest successful-move-costs-one-turn
  (do! "north")
  (is (= 1 @z/turns)))

(deftest each-successful-move-costs-one-turn
  ;; west-of-house → north-of-house → path, both valid exits
  (do! "north")
  (do! "north")
  (is (= 2 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Failed movement does NOT cost a turn
;;; ---------------------------------------------------------------------------

(deftest blocked-string-exit-costs-no-turn
  ;; "The door is boarded and you can't remove the boards."
  (do! "east")
  (is (= 0 @z/turns)))

(deftest impossible-direction-costs-no-turn
  ;; "You can't go that way."
  (do! "up")
  (is (= 0 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Successful object interaction costs a turn
;;; ---------------------------------------------------------------------------

(deftest open-costs-one-turn
  (do! "open mailbox")
  (is (= 1 @z/turns)))

(deftest close-costs-one-turn
  (do! "open mailbox")
  (do! "close mailbox")
  (is (= 2 @z/turns)))

(deftest take-costs-one-turn
  (do! "open mailbox")
  (do! "take leaflet")
  (is (= 2 @z/turns)))

(deftest drop-costs-one-turn
  (do! "open mailbox")
  (do! "take leaflet")
  (do! "drop leaflet")
  (is (= 3 @z/turns)))

(deftest put-costs-one-turn
  (do! "open mailbox")
  (do! "take leaflet")
  (do! "put leaflet in mailbox")
  (is (= 3 @z/turns)))

(deftest read-costs-one-turn
  (do! "open mailbox")
  (do! "take leaflet")
  (do! "read leaflet")
  (is (= 3 @z/turns)))

(deftest wait-costs-one-turn
  (do! "wait")
  (is (= 1 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Failed object interaction does NOT cost a turn
;;; ---------------------------------------------------------------------------

(deftest open-already-open-costs-no-extra-turn
  (do! "open mailbox")
  (do! "open mailbox")
  (is (= 1 @z/turns)))

(deftest close-already-closed-costs-no-turn
  (do! "close mailbox")
  (is (= 0 @z/turns)))

(deftest take-unrecognised-costs-no-turn
  (do! "take foobar")
  (is (= 0 @z/turns)))

(deftest take-anchored-costs-no-turn
  ;; mailbox has :trytakebit — action handler blocks it
  (do! "take mailbox")
  (is (= 0 @z/turns)))

(deftest take-already-held-costs-no-turn
  (do! "open mailbox")
  (do! "take leaflet")
  (do! "take leaflet")
  (is (= 2 @z/turns)))

(deftest drop-not-holding-costs-no-turn
  (do! "drop leaflet")
  (is (= 0 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Meta-commands do NOT cost a turn
;;; ---------------------------------------------------------------------------

(deftest look-costs-no-turn
  (do! "look")
  (is (= 0 @z/turns)))

(deftest examine-costs-no-turn
  (do! "examine mailbox")
  (is (= 0 @z/turns)))

(deftest inventory-costs-no-turn
  (do! "inventory")
  (is (= 0 @z/turns)))

(deftest score-costs-no-turn
  (do! "score")
  (is (= 0 @z/turns)))

(deftest diagnose-costs-no-turn
  (do! "diagnose")
  (is (= 0 @z/turns)))

(deftest unknown-command-costs-no-turn
  (do! "xyzzy")
  (is (= 0 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; again — ZIL semantics (verified against DOSBox r88/840726)
;;; ---------------------------------------------------------------------------

(deftest again-with-no-prior-command-says-beg-pardon
  ;; ZIL: empty AGAIN-LEXV → "Beg pardon?"
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (do! "again"))
    (is (= "Beg pardon?" (clojure.string/trim (str sw))))))

(deftest again-after-unknown-word-says-repeat-mistake
  ;; ZIL: P-WON false (parse failed) → "That would just repeat a mistake."
  (do! "xyzzy")
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (do! "again"))
    (is (= "That would just repeat a mistake." (clojure.string/trim (str sw))))))

(deftest again-replays-last-parsed-command-even-if-action-failed
  ;; ZIL: AGAIN replays last PARSED command regardless of whether the
  ;; action succeeded. take mailbox parses OK, action fails → again replays it.
  (do! "take mailbox")
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (do! "again"))
    (is (= "It is securely anchored." (clojure.string/trim (str sw))))))

(deftest again-replaying-turn-costing-command-costs-a-turn
  (do! "wait")
  (do! "again")
  (is (= 2 @z/turns)))

(deftest again-replaying-north-costs-a-turn
  (do! "north")
  (reset! z/here :west-of-house)   ; teleport back without counting
  (do! "again")
  (is (= 2 @z/turns)))
