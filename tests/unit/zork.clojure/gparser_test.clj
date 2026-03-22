(ns gparser-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [zork1.actions :as z]
            [gparser :refer [parse]]))

;;; ---------------------------------------------------------------------------
;;; Fixture — world must be loaded for find-object to work
;;; ---------------------------------------------------------------------------

(defn load-world-fixture [f]
  (z/load-world!)
  (f))

(use-fixtures :once load-world-fixture)

;;; ---------------------------------------------------------------------------
;;; Direction synonyms
;;; ---------------------------------------------------------------------------

(deftest bare-directions
  (testing "cardinal directions"
    (is (= {:verb :go :dir :north} (parse "north")))
    (is (= {:verb :go :dir :north} (parse "n")))
    (is (= {:verb :go :dir :south} (parse "south")))
    (is (= {:verb :go :dir :south} (parse "s")))
    (is (= {:verb :go :dir :east}  (parse "east")))
    (is (= {:verb :go :dir :east}  (parse "e")))
    (is (= {:verb :go :dir :west}  (parse "west")))
    (is (= {:verb :go :dir :west}  (parse "w"))))
  (testing "diagonal directions"
    (is (= {:verb :go :dir :ne} (parse "ne")))
    (is (= {:verb :go :dir :ne} (parse "northeast")))
    (is (= {:verb :go :dir :sw} (parse "sw")))
    (is (= {:verb :go :dir :sw} (parse "southwest"))))
  (testing "go + direction"
    (is (= {:verb :go :dir :north} (parse "go north")))
    (is (= {:verb :go :dir :west}  (parse "walk west")))
    (is (= {:verb :go :dir :east}  (parse "run east")))))

;;; ---------------------------------------------------------------------------
;;; Verb synonyms
;;; ---------------------------------------------------------------------------

(deftest verb-synonyms
  (testing "look"
    (is (= {:verb :look} (parse "look")))
    (is (= {:verb :look} (parse "l")))
    (is (= {:verb :look} (parse "stare")))
    (is (= {:verb :look} (parse "gaze"))))
  (testing "examine"
    (is (= {:verb :examine :obj :mailbox} (parse "examine mailbox")))
    (is (= {:verb :examine :obj :mailbox} (parse "x mailbox")))
    (is (= {:verb :examine :obj :mailbox} (parse "describe mailbox"))))
  (testing "inventory"
    (is (= {:verb :inventory} (parse "inventory")))
    (is (= {:verb :inventory} (parse "i"))))
  (testing "take synonyms"
    (is (= {:verb :take :obj :advertisement} (parse "take leaflet")))
    (is (= {:verb :take :obj :advertisement} (parse "get leaflet"))))
  (testing "quit"
    (is (= {:verb :quit} (parse "quit")))
    (is (= {:verb :quit} (parse "q")))))

;;; ---------------------------------------------------------------------------
;;; Two-word verb patterns
;;; ---------------------------------------------------------------------------

(deftest two-word-patterns
  (testing "look at OBJECT → examine"
    (is (= {:verb :examine :obj :mailbox} (parse "look at mailbox"))))
  (testing "look in OBJECT → look-in"
    (is (= {:verb :look-in :obj :mailbox} (parse "look in mailbox")))
    (is (= {:verb :look-in :obj :mailbox} (parse "look inside mailbox"))))
  (testing "look OBJECT alone → unrecognised"
    (is (= :unrecognised (:obj (parse "look mailbox"))))))

;;; ---------------------------------------------------------------------------
;;; Two-object commands
;;; ---------------------------------------------------------------------------

(deftest two-object-commands
  (testing "put X in Y"
    (let [result (parse "put leaflet in mailbox")]
      (is (= :put           (:verb result)))
      (is (= :advertisement (:obj result)))
      (is (= :mailbox       (:container result))))))

;;; ---------------------------------------------------------------------------
;;; Unknown input
;;; ---------------------------------------------------------------------------

(deftest unknown-input
  (is (= :unknown (:verb (parse "xyzzy"))))
  (is (= :unknown (:verb (parse "frotz"))))
  (is (= "xyzzy"  (:input (parse "xyzzy")))))
