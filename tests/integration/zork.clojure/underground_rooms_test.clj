(ns underground-rooms-test
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
;;; Room existence — all 89 new rooms must be in the world
;;; ---------------------------------------------------------------------------

;;; Maze
(deftest maze-1-exists           (is (some? (z/get-room :maze-1))))
(deftest maze-2-exists           (is (some? (z/get-room :maze-2))))
(deftest maze-3-exists           (is (some? (z/get-room :maze-3))))
(deftest maze-4-exists           (is (some? (z/get-room :maze-4))))
(deftest maze-5-exists           (is (some? (z/get-room :maze-5))))
(deftest maze-6-exists           (is (some? (z/get-room :maze-6))))
(deftest maze-7-exists           (is (some? (z/get-room :maze-7))))
(deftest maze-8-exists           (is (some? (z/get-room :maze-8))))
(deftest maze-9-exists           (is (some? (z/get-room :maze-9))))
(deftest maze-10-exists          (is (some? (z/get-room :maze-10))))
(deftest maze-11-exists          (is (some? (z/get-room :maze-11))))
(deftest maze-12-exists          (is (some? (z/get-room :maze-12))))
(deftest maze-13-exists          (is (some? (z/get-room :maze-13))))
(deftest maze-14-exists          (is (some? (z/get-room :maze-14))))
(deftest maze-15-exists          (is (some? (z/get-room :maze-15))))
(deftest dead-end-1-exists       (is (some? (z/get-room :dead-end-1))))
(deftest dead-end-2-exists       (is (some? (z/get-room :dead-end-2))))
(deftest dead-end-3-exists       (is (some? (z/get-room :dead-end-3))))
(deftest dead-end-4-exists       (is (some? (z/get-room :dead-end-4))))
(deftest grating-room-exists     (is (some? (z/get-room :grating-room))))

;;; Cyclops area
(deftest cyclops-room-exists     (is (some? (z/get-room :cyclops-room))))
(deftest strange-passage-exists  (is (some? (z/get-room :strange-passage))))
(deftest treasure-room-exists    (is (some? (z/get-room :treasure-room))))

;;; Reservoir / stream
(deftest reservoir-south-exists  (is (some? (z/get-room :reservoir-south))))
(deftest reservoir-exists        (is (some? (z/get-room :reservoir))))
(deftest reservoir-north-exists  (is (some? (z/get-room :reservoir-north))))
(deftest stream-view-exists      (is (some? (z/get-room :stream-view))))
(deftest in-stream-exists        (is (some? (z/get-room :in-stream))))

;;; Mirror rooms and vicinity
(deftest mirror-room-1-exists    (is (some? (z/get-room :mirror-room-1))))
(deftest mirror-room-2-exists    (is (some? (z/get-room :mirror-room-2))))
(deftest small-cave-exists       (is (some? (z/get-room :small-cave))))
(deftest tiny-cave-exists        (is (some? (z/get-room :tiny-cave))))
(deftest cold-passage-exists     (is (some? (z/get-room :cold-passage))))
(deftest narrow-passage-exists   (is (some? (z/get-room :narrow-passage))))
(deftest winding-passage-exists  (is (some? (z/get-room :winding-passage))))
(deftest twisting-passage-exists (is (some? (z/get-room :twisting-passage))))
(deftest atlantis-room-exists    (is (some? (z/get-room :atlantis-room))))

;;; Round Room area
(deftest ew-passage-exists       (is (some? (z/get-room :ew-passage))))
(deftest round-room-exists       (is (some? (z/get-room :round-room))))
(deftest deep-canyon-exists      (is (some? (z/get-room :deep-canyon))))
(deftest damp-cave-exists        (is (some? (z/get-room :damp-cave))))
(deftest loud-room-exists        (is (some? (z/get-room :loud-room))))
(deftest ns-passage-exists       (is (some? (z/get-room :ns-passage))))
(deftest chasm-room-exists       (is (some? (z/get-room :chasm-room))))

;;; Hades
(deftest entrance-to-hades-exists    (is (some? (z/get-room :entrance-to-hades))))
(deftest land-of-living-dead-exists  (is (some? (z/get-room :land-of-living-dead))))

;;; Egypt / Dome / Temple
(deftest engravings-cave-exists  (is (some? (z/get-room :engravings-cave))))
(deftest egypt-room-exists       (is (some? (z/get-room :egypt-room))))
(deftest dome-room-exists        (is (some? (z/get-room :dome-room))))
(deftest torch-room-exists       (is (some? (z/get-room :torch-room))))
(deftest north-temple-exists     (is (some? (z/get-room :north-temple))))
(deftest south-temple-exists     (is (some? (z/get-room :south-temple))))

;;; Dam
(deftest dam-room-exists         (is (some? (z/get-room :dam-room))))
(deftest dam-lobby-exists        (is (some? (z/get-room :dam-lobby))))
(deftest maintenance-room-exists (is (some? (z/get-room :maintenance-room))))
(deftest dam-base-exists         (is (some? (z/get-room :dam-base))))

;;; River
(deftest river-1-exists          (is (some? (z/get-room :river-1))))
(deftest river-2-exists          (is (some? (z/get-room :river-2))))
(deftest river-3-exists          (is (some? (z/get-room :river-3))))
(deftest river-4-exists          (is (some? (z/get-room :river-4))))
(deftest river-5-exists          (is (some? (z/get-room :river-5))))
(deftest white-cliffs-north-exists (is (some? (z/get-room :white-cliffs-north))))
(deftest white-cliffs-south-exists (is (some? (z/get-room :white-cliffs-south))))
(deftest shore-exists            (is (some? (z/get-room :shore))))
(deftest sandy-beach-exists      (is (some? (z/get-room :sandy-beach))))
(deftest sandy-cave-exists       (is (some? (z/get-room :sandy-cave))))

;;; Falls / Rainbow / Canyon
(deftest aragain-falls-exists    (is (some? (z/get-room :aragain-falls))))
(deftest on-rainbow-exists       (is (some? (z/get-room :on-rainbow))))
(deftest end-of-rainbow-exists   (is (some? (z/get-room :end-of-rainbow))))
(deftest canyon-bottom-exists    (is (some? (z/get-room :canyon-bottom))))
(deftest cliff-middle-exists     (is (some? (z/get-room :cliff-middle))))
(deftest canyon-view-exists      (is (some? (z/get-room :canyon-view))))

;;; Mine
(deftest mine-entrance-exists    (is (some? (z/get-room :mine-entrance))))
(deftest squeeky-room-exists     (is (some? (z/get-room :squeeky-room))))
(deftest bat-room-exists         (is (some? (z/get-room :bat-room))))
(deftest shaft-room-exists       (is (some? (z/get-room :shaft-room))))
(deftest smelly-room-exists      (is (some? (z/get-room :smelly-room))))
(deftest gas-room-exists         (is (some? (z/get-room :gas-room))))
(deftest ladder-top-exists       (is (some? (z/get-room :ladder-top))))
(deftest ladder-bottom-exists    (is (some? (z/get-room :ladder-bottom))))
(deftest dead-end-5-exists       (is (some? (z/get-room :dead-end-5))))
(deftest timber-room-exists      (is (some? (z/get-room :timber-room))))
(deftest lower-shaft-exists      (is (some? (z/get-room :lower-shaft))))
(deftest machine-room-exists     (is (some? (z/get-room :machine-room))))
(deftest mine-1-exists           (is (some? (z/get-room :mine-1))))
(deftest mine-2-exists           (is (some? (z/get-room :mine-2))))
(deftest mine-3-exists           (is (some? (z/get-room :mine-3))))
(deftest mine-4-exists           (is (some? (z/get-room :mine-4))))
(deftest slide-room-exists       (is (some? (z/get-room :slide-room))))

;;; ---------------------------------------------------------------------------
;;; Exit traversal — inter-cluster connections
;;; ---------------------------------------------------------------------------

;;; maze-1 east → troll-room (troll-room east is permanently blocked by a string exit)
(deftest maze-1-east-to-troll-room
  (reset! z/here :maze-1)
  (do! "go east")
  (is (= :troll-room @z/here)))

(deftest maze-1-south-to-maze-2
  (reset! z/here :maze-1)
  (do! "go south")
  (is (= :maze-2 @z/here)))

(deftest maze-2-east-to-maze-3
  (reset! z/here :maze-2)
  (do! "go east")
  (is (= :maze-3 @z/here)))

;;; MAZE-DIODES — one-way exits hard-coded per ZIL comments
(deftest maze-2-down-to-maze-4
  (reset! z/here :maze-2)
  (do! "go down")
  (is (= :maze-4 @z/here)))

(deftest maze-7-down-to-dead-end-1
  (reset! z/here :maze-7)
  (do! "go down")
  (is (= :dead-end-1 @z/here)))

(deftest maze-9-down-to-maze-11
  (reset! z/here :maze-9)
  (do! "go down")
  (is (= :maze-11 @z/here)))

(deftest maze-12-down-to-maze-5
  (reset! z/here :maze-12)
  (do! "go down")
  (is (= :maze-5 @z/here)))

;;; Maze → grating-room
(deftest maze-11-ne-to-grating-room
  (reset! z/here :maze-11)
  (do! "go ne")
  (is (= :grating-room @z/here)))

(deftest grating-room-sw-to-maze-11
  (reset! z/here :grating-room)
  (do! "go sw")
  (is (= :maze-11 @z/here)))

;;; Grating-room → grating-clearing (requires grate open)
;;; MAZE-11-FCN M-ENTER clears :invisible on the grate when entering the room.
;;; Tests that directly set here bypass arrive!, so we clear it manually here.
(deftest grating-room-up-blocked-when-grate-closed
  (swap! z/world update-in [:objects :grate :flags] disj :invisible)
  (reset! z/here :grating-room)
  (let [out (output-of #(do! "go up"))]
    (is (clojure.string/includes? out "The grating is closed."))
    (is (= :grating-room @z/here))))

(deftest grating-room-up-when-grate-open
  (swap! z/world update-in [:objects :grate :flags] #(-> % (disj :invisible) (conj :openbit)))
  (reset! z/here :grating-room)
  (do! "go up")
  (is (= :grating-clearing @z/here)))

;;; :if-open with :invisible gating object falls through to default message, ignoring :else.
;;; This locks in the ZIL-faithful behavior that keeps the pre-rug trap-door exit correct.
(deftest if-open-invisible-object-ignores-else-string
  ;; Grate has :invisible set (default state — :else "The grating is closed." must be suppressed)
  (reset! z/here :grating-room)
  (let [out (output-of #(do! "go up"))]
    (is (clojure.string/includes? out "You can't go that way."))
    (is (not (clojure.string/includes? out "The grating is closed.")))
    (is (= :grating-room @z/here))))

;;; Maze → cyclops-room
(deftest maze-15-se-to-cyclops-room
  (reset! z/here :maze-15)
  (do! "go se")
  (is (= :cyclops-room @z/here)))

(deftest cyclops-room-nw-to-maze-15
  (reset! z/here :cyclops-room)
  (do! "go nw")
  (is (= :maze-15 @z/here)))

;;; Cyclops-room flag-gated exits
(deftest cyclops-room-up-blocked-without-cyclops-flag
  (reset! z/here :cyclops-room)
  (let [out (output-of #(do! "go up"))]
    (is (clojure.string/includes? out "The cyclops doesn't look like he'll let you past."))
    (is (= :cyclops-room @z/here))))

(deftest cyclops-room-up-when-cyclops-flag-set
  (swap! z/world assoc-in [:flags :cyclops-flag] true)
  (reset! z/here :cyclops-room)
  (do! "go up")
  (is (= :treasure-room @z/here)))

(deftest cyclops-room-east-blocked-without-magic-flag
  (reset! z/here :cyclops-room)
  (let [out (output-of #(do! "go east"))]
    (is (clojure.string/includes? out "The east wall is solid rock."))
    (is (= :cyclops-room @z/here))))

;;; Strange-passage → living-room
(deftest strange-passage-east-to-living-room
  (reset! z/here :strange-passage)
  (do! "go east")
  (is (= :living-room @z/here)))

;;; EW-passage connections
(deftest ew-passage-west-to-troll-room
  (reset! z/here :ew-passage)
  (do! "go west")
  (is (= :troll-room @z/here)))

(deftest ew-passage-east-to-round-room
  (reset! z/here :ew-passage)
  (do! "go east")
  (is (= :round-room @z/here)))

;;; Round Room cluster
(deftest round-room-east-to-loud-room
  (reset! z/here :round-room)
  (do! "go east")
  (is (= :loud-room @z/here)))

(deftest round-room-west-to-ew-passage
  (reset! z/here :round-room)
  (do! "go west")
  (is (= :ew-passage @z/here)))

(deftest round-room-north-to-ns-passage
  (reset! z/here :round-room)
  (do! "go north")
  (is (= :ns-passage @z/here)))

(deftest round-room-south-to-narrow-passage
  (reset! z/here :round-room)
  (do! "go south")
  (is (= :narrow-passage @z/here)))

(deftest round-room-se-to-engravings-cave
  (reset! z/here :round-room)
  (do! "go se")
  (is (= :engravings-cave @z/here)))

(deftest ns-passage-north-to-chasm-room
  (reset! z/here :ns-passage)
  (do! "go north")
  (is (= :chasm-room @z/here)))

;;; Chasm Room blocked exit
(deftest chasm-room-down-blocked
  (reset! z/here :chasm-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "Are you out of your mind?"))))

;;; Hades path
(deftest tiny-cave-down-to-entrance-to-hades
  (reset! z/here :tiny-cave)
  (do! "go down")
  (is (= :entrance-to-hades @z/here)))

(deftest entrance-to-hades-up-to-tiny-cave
  (reset! z/here :entrance-to-hades)
  (do! "go up")
  (is (= :tiny-cave @z/here)))

(deftest entrance-to-hades-blocked-without-lld-flag
  (reset! z/here :entrance-to-hades)
  (let [out (output-of #(do! "go south"))]
    (is (clojure.string/includes? out "Some invisible force prevents you from passing through the gate."))
    (is (= :entrance-to-hades @z/here))))

(deftest entrance-to-hades-south-when-lld-flag-set
  (swap! z/world assoc-in [:flags :lld-flag] true)
  (reset! z/here :entrance-to-hades)
  (do! "go south")
  (is (= :land-of-living-dead @z/here)))

;;; Temple cluster
(deftest north-temple-south-to-south-temple
  (reset! z/here :north-temple)
  (do! "go south")
  (is (= :south-temple @z/here)))

(deftest south-temple-north-to-north-temple
  (reset! z/here :south-temple)
  (do! "go north")
  (is (= :north-temple @z/here)))

(deftest north-temple-down-to-egypt-room
  (reset! z/here :north-temple)
  (do! "go down")
  (is (= :egypt-room @z/here)))

(deftest dome-room-down-blocked-without-dome-flag
  (reset! z/here :dome-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "You cannot go down without fracturing many bones."))
    (is (= :dome-room @z/here))))

;;; Reservoir — blocked exits
(deftest reservoir-down-blocked
  (reset! z/here :reservoir)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "The dam blocks your way."))))

(deftest reservoir-south-north-blocked-without-low-tide
  (reset! z/here :reservoir-south)
  (let [out (output-of #(do! "go north"))]
    (is (clojure.string/includes? out "You would drown."))
    (is (= :reservoir-south @z/here))))

(deftest reservoir-south-north-when-low-tide-set
  (swap! z/world assoc-in [:flags :low-tide] true)
  (reset! z/here :reservoir-south)
  (do! "go north")
  (is (= :reservoir @z/here)))

;;; Dam cluster
(deftest dam-room-north-to-dam-lobby
  (reset! z/here :dam-room)
  (do! "go north")
  (is (= :dam-lobby @z/here)))

(deftest dam-lobby-north-to-maintenance-room
  (reset! z/here :dam-lobby)
  (do! "go north")
  (is (= :maintenance-room @z/here)))

(deftest dam-lobby-south-to-dam-room
  (reset! z/here :dam-lobby)
  (do! "go south")
  (is (= :dam-room @z/here)))

;;; Canyon path (canyon-view was a dangling reference before this issue)
(deftest clearing-east-to-canyon-view
  (reset! z/here :clearing)
  (do! "go east")
  (is (= :canyon-view @z/here)))

(deftest canyon-view-down-to-cliff-middle
  (reset! z/here :canyon-view)
  (do! "go down")
  (is (= :cliff-middle @z/here)))

(deftest cliff-middle-down-to-canyon-bottom
  (reset! z/here :cliff-middle)
  (do! "go down")
  (is (= :canyon-bottom @z/here)))

(deftest canyon-bottom-north-to-end-of-rainbow
  (reset! z/here :canyon-bottom)
  (do! "go north")
  (is (= :end-of-rainbow @z/here)))

;;; Rainbow exits blocked without flag
(deftest aragain-falls-west-blocked-without-rainbow-flag
  (reset! z/here :aragain-falls)
  (let [out (output-of #(do! "go west"))]
    (is (clojure.string/includes? out "You can't go that way."))
    (is (= :aragain-falls @z/here))))

(deftest aragain-falls-west-when-rainbow-flag-set
  (swap! z/world assoc-in [:flags :rainbow-flag] true)
  (reset! z/here :aragain-falls)
  (do! "go west")
  (is (= :on-rainbow @z/here)))

;;; Shore / beach
(deftest shore-north-to-sandy-beach
  (reset! z/here :shore)
  (do! "go north")
  (is (= :sandy-beach @z/here)))

(deftest sandy-beach-ne-to-sandy-cave
  (reset! z/here :sandy-beach)
  (do! "go ne")
  (is (= :sandy-cave @z/here)))

;;; Mine entry
(deftest cold-passage-west-to-slide-room
  (reset! z/here :cold-passage)
  (do! "go west")
  (is (= :slide-room @z/here)))

(deftest slide-room-down-to-cellar
  (reset! z/here :slide-room)
  (do! "go down")
  (is (= :cellar @z/here)))

(deftest slide-room-north-to-mine-entrance
  (reset! z/here :slide-room)
  (do! "go north")
  (is (= :mine-entrance @z/here)))

(deftest mine-entrance-west-to-squeeky-room
  (reset! z/here :mine-entrance)
  (do! "go west")
  (is (= :squeeky-room @z/here)))

(deftest squeeky-room-north-to-bat-room
  (reset! z/here :squeeky-room)
  (do! "go north")
  (is (= :bat-room @z/here)))

(deftest bat-room-east-to-shaft-room
  (reset! z/here :bat-room)
  (do! "go east")
  (is (= :shaft-room @z/here)))

(deftest shaft-room-north-to-smelly-room
  (reset! z/here :shaft-room)
  (do! "go north")
  (is (= :smelly-room @z/here)))

(deftest smelly-room-down-to-gas-room
  (reset! z/here :smelly-room)
  (do! "go down")
  (is (= :gas-room @z/here)))

(deftest gas-room-east-to-mine-1
  (reset! z/here :gas-room)
  (do! "go east")
  (is (= :mine-1 @z/here)))

(deftest mine-4-down-to-ladder-top
  (reset! z/here :mine-4)
  (do! "go down")
  (is (= :ladder-top @z/here)))

(deftest ladder-top-down-to-ladder-bottom
  (reset! z/here :ladder-top)
  (do! "go down")
  (is (= :ladder-bottom @z/here)))

(deftest ladder-bottom-west-to-timber-room
  (reset! z/here :ladder-bottom)
  (do! "go west")
  (is (= :timber-room @z/here)))

;;; Timber-room EMPTY-HANDED blocked exits (permanently blocked until inventory check impl'd)
(deftest timber-room-west-blocked
  (reset! z/here :timber-room)
  (let [out (output-of #(do! "go west"))]
    (is (clojure.string/includes? out "You cannot fit through this passage with that load."))))

(deftest lower-shaft-south-to-machine-room
  (reset! z/here :lower-shaft)
  (do! "go south")
  (is (= :machine-room @z/here)))

(deftest machine-room-north-to-lower-shaft
  (reset! z/here :machine-room)
  (do! "go north")
  (is (= :lower-shaft @z/here)))

;;; Blocked string exits — spot-checks
(deftest damp-cave-south-blocked
  (reset! z/here :damp-cave)
  (let [out (output-of #(do! "go south"))]
    (is (clojure.string/includes? out "It is too narrow for most insects."))))

(deftest shaft-room-down-blocked
  (reset! z/here :shaft-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "You wouldn't fit and would die if you could."))))

(deftest torch-room-up-blocked
  (reset! z/here :torch-room)
  (let [out (output-of #(do! "go up"))]
    (is (clojure.string/includes? out "You cannot reach the rope."))))

(deftest white-cliffs-north-south-blocked-without-deflate-flag
  ;; DEFLATE condition (boat must be deflated) not yet implemented — always blocks.
  (reset! z/here :white-cliffs-north)
  (let [out (output-of #(do! "go south"))]
    (is (clojure.string/includes? out "The path is too narrow."))))
