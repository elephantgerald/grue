(ns darkness-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [zork1.actions :as z]
            [gparser :as parser]))

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

;;; ---------------------------------------------------------------------------
;;; v-lamp-on — ZIL V-LAMP-ON (gverbs.zil:786) + LANTERN (1actions.zil:2237)
;;; ---------------------------------------------------------------------------

(defn- setup-lamp-off-in-inventory! []
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] disj :onbit))

(deftest lamp-on-lights-lamp
  ;; Basic case: lamp in inventory, off → :onbit set, message printed, :turn returned
  (reset! z/here :west-of-house)
  (setup-lamp-off-in-inventory!)
  (let [out (output-of #(is (= :turn (z/v-lamp-on :lamp))))]
    (is (str/includes? out "The brass lantern is now on."))
    (is (contains? (:flags (z/get-object :lamp)) :onbit))))

(deftest lamp-on-resets-lamp-power
  ;; v-lamp-on must reset lamp-power to 185 before setting :onbit (tick-lamp! contract)
  (reset! z/here :west-of-house)
  (setup-lamp-off-in-inventory!)
  (z/v-lamp-on :lamp)
  (is (= 185 @z/lamp-power)))

(deftest lamp-on-already-on
  ;; Lamp already has :onbit → "It is already on." — not a turn
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (let [out (output-of #(is (nil? (z/v-lamp-on :lamp))))]
    (is (str/includes? out "It is already on."))))

(deftest lamp-on-burned-out
  ;; Lamp has :rmungbit → "A burned-out lamp won't light." — not a turn
  (reset! z/here :west-of-house)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] #(-> % (disj :onbit) (conj :rmungbit)))
  (let [out (output-of #(is (nil? (z/v-lamp-on :lamp))))]
    (is (str/includes? out "A burned-out lamp won't light."))))

(deftest lamp-on-lights-dark-room
  ;; Turning on lamp in a dark room triggers update-lit! and v-look (room desc shown)
  (reset! z/here :cellar)
  (setup-lamp-off-in-inventory!)
  (z/update-lit!)  ; ensure @lit is false before lighting
  (let [out (output-of #(z/v-lamp-on :lamp))]
    (is (str/includes? out "You are in a dark and damp cellar"))
    (is @z/lit)))

(deftest lamp-on-non-lightbit-object
  ;; Object without :lightbit → "You can't turn that on."
  (reset! z/here :west-of-house)
  (let [out (output-of #(z/v-lamp-on :sword))]
    (is (str/includes? out "You can't turn that on."))))

(deftest lamp-on-burnbit-object
  ;; Object with :burnbit but no :lightbit → "If you wish to burn the X, you should say so."
  ;; (gverbs.zil:797-799 — :advertisement has :burnbit, desc "leaflet")
  (reset! z/here :west-of-house)
  (let [out (output-of #(z/v-lamp-on :advertisement))]
    (is (str/includes? out "If you wish to burn the leaflet, you should say so."))))

;;; ---------------------------------------------------------------------------
;;; v-lamp-off — ZIL V-LAMP-OFF (gverbs.zil:771) + LANTERN (1actions.zil:2243)
;;; ---------------------------------------------------------------------------

(deftest lamp-off-extinguishes-lamp
  ;; Lamp on in inventory → :onbit cleared, message printed, :turn returned
  (reset! z/here :west-of-house)
  (setup-lamp-on-in-inventory!)
  (z/update-lit!)
  (let [out (output-of #(is (= :turn (z/v-lamp-off :lamp))))]
    (is (str/includes? out "The brass lantern is now off."))
    (is (not (contains? (:flags (z/get-object :lamp)) :onbit)))))

(deftest lamp-off-already-off
  ;; Lamp not :onbit → "It is already off." — not a turn
  (reset! z/here :west-of-house)
  (setup-lamp-off-in-inventory!)
  (let [out (output-of #(is (nil? (z/v-lamp-off :lamp))))]
    (is (str/includes? out "It is already off."))))

(deftest lamp-off-burned-out
  ;; Lamp has :rmungbit → "The lamp has already burned out." — not a turn
  (reset! z/here :west-of-house)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] #(-> % (disj :onbit) (conj :rmungbit)))
  (let [out (output-of #(is (nil? (z/v-lamp-off :lamp))))]
    (is (str/includes? out "The lamp has already burned out."))))

(deftest lamp-off-now-dark
  ;; Lamp off in underground room → room goes dark, "It is now pitch black."
  (reset! z/here :cellar)
  (setup-lamp-on-in-inventory!)
  (z/update-lit!)
  (let [out (output-of #(z/v-lamp-off :lamp))]
    (is (str/includes? out "It is now pitch black."))
    (is (not @z/lit))))

(deftest lamp-off-non-lightbit-object
  ;; Object without :lightbit → "You can't turn that off."
  (reset! z/here :west-of-house)
  (let [out (output-of #(z/v-lamp-off :sword))]
    (is (str/includes? out "You can't turn that off."))))

;;; ---------------------------------------------------------------------------
;;; Parser — verb mappings for light/extinguish/douse (gsyntax.zil:286/211/213)
;;; ---------------------------------------------------------------------------

(deftest parser-light-maps-to-lamp-on
  ;; "light" verb string → :lamp-on action (gsyntax.zil:286)
  (is (= :lamp-on (:verb (parser/parse "light lamp")))))

(deftest parser-extinguish-maps-to-lamp-off
  ;; "extinguish" verb string → :lamp-off action (gsyntax.zil:211)
  (is (= :lamp-off (:verb (parser/parse "extinguish lamp")))))

(deftest parser-douse-maps-to-lamp-off
  ;; "douse" verb string → :lamp-off action (gsyntax.zil:213)
  (is (= :lamp-off (:verb (parser/parse "douse lamp")))))
