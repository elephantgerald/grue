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
;;; Turn rule — ZIL MAIN-LOOP-1 (gmain.zil:169-172): if the parse succeeded
;;; (P-WON), CLOCKER runs and MOVES increments REGARDLESS of whether the action
;;; succeeded — except for meta-verbs (SCORE, VERBOSE, BRIEF, VERSION, ...).
;;; Parser failures (unknown word, missing object, HAVE check) cost nothing.
;;; All counts below verified against the original r88/840726 via dfrotz
;;; (2026-09-25), which superseded the earlier "failed actions are free" model.
;;; ---------------------------------------------------------------------------

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
;;; Failed movement still costs a turn (the parse succeeded)
;;; ---------------------------------------------------------------------------

(deftest blocked-string-exit-costs-one-turn
  ;; "The door is boarded and you can't remove the boards." — original: 1 move
  (do! "east")
  (is (= 1 @z/turns)))

(deftest impossible-direction-costs-one-turn
  ;; "You can't go that way." — original: 1 move
  (do! "up")
  (is (= 1 @z/turns)))

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

(deftest wait-costs-three-turns
  ;; ZIL V-WAIT (gverbs.zil:1514) runs CLOCKER 3 times itself, then sets
  ;; CLOCK-WAIT so the main loop's CLOCKER is skipped — original: 3 moves
  (do! "wait")
  (is (= 3 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Failed object interaction still costs a turn (the parse succeeded)
;;; ---------------------------------------------------------------------------

(deftest open-already-open-costs-a-turn
  ;; original: 2 moves
  (do! "open mailbox")
  (do! "open mailbox")
  (is (= 2 @z/turns)))

(deftest close-already-closed-costs-a-turn
  ;; "It is already closed." — original: 1 move
  (do! "close mailbox")
  (is (= 1 @z/turns)))

(deftest take-anchored-costs-a-turn
  ;; "It is securely anchored." — the object ACTION pre-empts, still 1 move
  (do! "take mailbox")
  (is (= 1 @z/turns)))

(deftest take-already-held-costs-a-turn
  ;; "You already have that." — original: 3 moves
  (do! "open mailbox")
  (do! "take leaflet")
  (do! "take leaflet")
  (is (= 3 @z/turns)))

(deftest failed-lamp-actions-each-cost-a-turn
  ;; All six failed V-LAMP-ON/OFF outcomes cost a move in the original,
  ;; including the two LANTERN pre-empts ("burned-out").
  (reset! z/here :living-room)
  (z/update-lit!)
  (testing "It is already off."
    (do! "take lamp")                       ; 1
    (do! "extinguish lamp")                 ; 2
    (is (= 2 @z/turns)))
  (testing "It is already on."
    (do! "light lamp")                      ; 3
    (do! "light lamp")                      ; 4
    (is (= 4 @z/turns)))
  (testing "You can't turn that on/off."
    (do! "take sword")                      ; 5
    (do! "light sword")                     ; 6
    (do! "extinguish sword")                ; 7
    (is (= 7 @z/turns)))
  (testing "burned-out lamp pre-empted by LANTERN"
    (swap! z/world update-in [:objects :lamp :flags] #(-> % (disj :onbit) (conj :rmungbit)))
    (do! "light lamp")                      ; 8
    (do! "extinguish lamp")                 ; 9
    (is (= 9 @z/turns))))

;;; ---------------------------------------------------------------------------
;;; Parser failures do NOT cost a turn (P-WON false → no CLOCKER)
;;; ---------------------------------------------------------------------------

(deftest take-unknown-word-costs-no-turn
  ;; "I don't know the word "qwxzt"." — original: 0 moves. (Was "take foobar",
  ;; but foobar is in Zork's vocabulary: the original says "You can't see any
  ;; foobar here!" and charges a move. Our parser has no vocabulary, so it
  ;; can't distinguish known-but-absent from unknown yet.)
  (do! "take qwxzt")
  (is (= 0 @z/turns)))

(deftest take-with-no-object-costs-no-turn
  ;; "What do you want to take?" — parser orphan, original: 0 moves
  (do! "take")
  (is (= 0 @z/turns)))

(deftest drop-not-holding-costs-no-turn
  ;; ZIL: the parser's HAVE check ("You don't have that!") — original: 0 moves
  (do! "drop leaflet")
  (is (= 0 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Observation verbs cost a turn — they are not on ZIL's meta-verb list
;;; ---------------------------------------------------------------------------

(deftest look-costs-a-turn
  (do! "look")
  (is (= 1 @z/turns)))

(deftest examine-costs-a-turn
  (do! "examine mailbox")
  (is (= 1 @z/turns)))

(deftest inventory-costs-a-turn
  (do! "inventory")
  (is (= 1 @z/turns)))

(deftest diagnose-costs-a-turn
  (do! "diagnose")
  (is (= 1 @z/turns)))

;;; ---------------------------------------------------------------------------
;;; Meta-verbs and unknown words do NOT cost a turn
;;; ---------------------------------------------------------------------------

(deftest score-costs-no-turn
  (do! "score")
  (is (= 0 @z/turns)))

(deftest unknown-command-costs-no-turn
  ;; "I don't know the word "qwxzt"." — original: 0 moves. (Was "xyzzy", but
  ;; xyzzy is a real Zork word — the original answers it and charges a move.)
  (do! "qwxzt")
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
  ;; again replays wait at wait's cost — original: 3 + 3 = 6 moves
  (do! "wait")
  (do! "again")
  (is (= 6 @z/turns)))

(deftest again-replaying-north-costs-a-turn
  (do! "north")
  (reset! z/here :west-of-house)   ; teleport back without counting
  (do! "again")
  (is (= 2 @z/turns)))
