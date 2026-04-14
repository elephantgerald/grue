(ns darkness-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [zork1.actions :as z]))

(defn reset-world! []
  (z/load-world!))

(defn output-of [thunk]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (thunk))
    (str/trim (str sw))))

(use-fixtures :each (fn [f] (reset-world!) (f)))

;;; ---------------------------------------------------------------------------
;;; lit? — ZIL LIT? routine (gparser.zil:1333)
;;; ---------------------------------------------------------------------------

(deftest outdoor-room-is-lit
  ;; Outdoor rooms have :onbit → always lit regardless of lamp
  (reset! z/here :west-of-house)
  (is (z/lit?)))

(deftest underground-room-is-dark
  ;; Underground rooms have no :onbit and no lit lamp in inventory
  (reset! z/here :cellar)
  (is (not (z/lit?))))

(deftest lamp-on-in-inventory-lights-dark-room
  ;; Carrying a :lightbit + :onbit object lights the room
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] conj :onbit)
  (is (z/lit?)))

(deftest lamp-without-onbit-does-not-light-room
  ;; :lightbit alone is not enough — lamp must also be :onbit (turned on)
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (is (not (z/lit?))))

;;; ---------------------------------------------------------------------------
;;; update-lit! — @lit atom reflects current room lighting
;;; ---------------------------------------------------------------------------

(deftest lit-atom-true-in-outdoor-room
  (reset! z/here :west-of-house)
  (z/update-lit!)
  (is @z/lit))

(deftest lit-atom-false-in-dark-room
  (reset! z/here :cellar)
  (z/update-lit!)
  (is (not @z/lit)))

(deftest lit-atom-true-when-lamp-on-in-inventory
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] conj :onbit)
  (z/update-lit!)
  (is @z/lit))

;;; ---------------------------------------------------------------------------
;;; v-look in darkness — ZIL DESCRIBE-ROOM (gverbs.zil:1637-1649)
;;; ---------------------------------------------------------------------------

(deftest look-in-dark-room-shows-pitch-black
  (reset! z/here :cellar)
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (str/includes? out "It is pitch black."))))

(deftest look-in-dark-room-shows-grue-warning
  (reset! z/here :cellar)
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (str/includes? out "You are likely to be eaten by a grue."))))

(deftest look-in-dark-room-hides-room-description
  ;; Room desc ("dark and damp cellar") must not appear when dark
  (reset! z/here :cellar)
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (not (str/includes? out "dark and damp cellar")))))

(deftest look-in-dark-room-hides-objects
  ;; Objects in room must not be listed when dark
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :sword :location] (constantly :cellar))
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (not (str/includes? out "sword")))))

(deftest look-in-dark-room-skips-touchbit
  ;; ZIL DESCRIBE-ROOM does not set :touchbit when dark
  (reset! z/here :cellar)
  (z/update-lit!)
  (z/v-look)
  (is (not (contains? (:flags (z/get-room :cellar)) :touchbit))))

(deftest look-in-lit-room-shows-description
  (reset! z/here :west-of-house)
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (str/includes? out "You are standing in an open field"))))

;;; ---------------------------------------------------------------------------
;;; jigs-up — ZIL JIGS-UP macro
;;; ---------------------------------------------------------------------------

(deftest jigs-up-returns-quit
  (is (= :quit (z/jigs-up "You are eaten."))))

(deftest jigs-up-prints-death-message
  (let [out (output-of #(z/jigs-up "You are eaten."))]
    (is (str/includes? out "You are eaten."))))

(deftest jigs-up-prints-death-block
  (let [out (output-of #(z/jigs-up "test"))]
    (is (str/includes? out "You have died"))))

(deftest jigs-up-deducts-score
  (reset! z/score 50)
  (z/jigs-up "test")
  (is (= 40 @z/score)))

;;; ---------------------------------------------------------------------------
;;; Lamp fuel — ZIL I-LANTERN interrupt (1actions.zil:2315)
;;; Total fuel: 185 turns. lamp-power counts down from 185.
;;; ---------------------------------------------------------------------------

(defn- setup-lamp-on-in-inventory! []
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] conj :onbit)
  (reset! z/lamp-power 185))

(deftest lamp-at-85-prints-dimmer
  ;; After 100 turns (power drops from 185 to 85): "The lamp appears a bit dimmer."
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (reset! z/lamp-power 86)
  (let [out (output-of z/tick-lamp!)]
    (is (str/includes? out "The lamp appears a bit dimmer."))))

(deftest lamp-at-15-prints-definitely-dimmer
  ;; After 70 more turns (power drops to 15): "The lamp is definitely dimmer now."
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (reset! z/lamp-power 16)
  (let [out (output-of z/tick-lamp!)]
    (is (str/includes? out "The lamp is definitely dimmer now."))))

(deftest lamp-at-0-prints-nearly-out
  ;; After 15 more turns (power drops to 0): "The lamp is nearly out."
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (reset! z/lamp-power 1)
  (let [out (output-of z/tick-lamp!)]
    (is (str/includes? out "The lamp is nearly out."))))

(deftest lamp-below-0-burns-out
  ;; Next tick after 0: lamp gets :rmungbit, loses :onbit, prints burnout message
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (reset! z/lamp-power 0)
  (let [out (output-of z/tick-lamp!)]
    (is (str/includes? out "You'd better have more light than from the brass lantern."))
    (is (contains? (:flags (z/get-object :lamp)) :rmungbit))
    (is (not (contains? (:flags (z/get-object :lamp)) :onbit)))))

(deftest burned-out-lamp-does-not-light-room
  ;; Lamp with :rmungbit but no :onbit in inventory → room stays dark
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] conj :rmungbit)
  (is (not (z/lit?))))

(deftest tick-lamp-no-op-when-lamp-off
  ;; tick-lamp! must not print anything if lamp has no :onbit
  (reset! z/here :west-of-house)
  (let [out (output-of z/tick-lamp!)]
    (is (= "" out))))
