(ns house-interior-test
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
;;; Room existence — all 8 rooms must be in the world
;;; ---------------------------------------------------------------------------

(deftest kitchen-exists
  (is (some? (z/get-room :kitchen))))

(deftest attic-exists
  (is (some? (z/get-room :attic))))

(deftest living-room-exists
  (is (some? (z/get-room :living-room))))

(deftest cellar-exists
  (is (some? (z/get-room :cellar))))

(deftest troll-room-exists
  (is (some? (z/get-room :troll-room))))

(deftest east-of-chasm-exists
  (is (some? (z/get-room :east-of-chasm))))

(deftest gallery-exists
  (is (some? (z/get-room :gallery))))

(deftest studio-exists
  (is (some? (z/get-room :studio))))

;;; ---------------------------------------------------------------------------
;;; Kitchen — ZIL: KITCHEN-FCN M-LOOK
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest kitchen-description-window-ajar
  ;; Kitchen window starts without :openbit — "slightly ajar"
  (reset! z/here :kitchen)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "You are in the kitchen of the white house."))
    (is (clojure.string/includes? out "slightly ajar."))))

(deftest kitchen-description-window-open
  (swap! z/world update-in [:objects :kitchen-window :flags] conj :openbit)
  (reset! z/here :kitchen)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "open."))))

;;; ---------------------------------------------------------------------------
;;; Attic — ZIL: ATTIC (LDESC only, no action handler)
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest attic-description
  (reset! z/here :attic)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "This is the attic. The only exit is a stairway leading down."))))

;;; ---------------------------------------------------------------------------
;;; Living room — ZIL: LIVING-ROOM-FCN M-LOOK
;;; Four description variants based on rug-moved and trap-door state.
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest living-room-description-base-state
  ;; Rug not moved, trap door not open (initial state)
  (reset! z/here :living-room)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "You are in the living room."))
    (is (clojure.string/includes? out "a large oriental rug in the center of the room."))))

(deftest living-room-description-rug-moved-door-closed
  (reset! z/rug-moved true)
  (reset! z/here :living-room)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "a closed trap door at your feet."))))

(deftest living-room-description-rug-moved-door-open
  (reset! z/rug-moved true)
  (swap! z/world update-in [:objects :trap-door :flags] conj :openbit)
  (reset! z/here :living-room)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "a rug lying beside an open trap door."))))

(deftest living-room-description-door-open-rug-not-moved
  ;; Trap door open but rug hasn't been explicitly moved (unusual state)
  (swap! z/world update-in [:objects :trap-door :flags] conj :openbit)
  (reset! z/here :living-room)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "an open trap door at your feet."))))

;;; ---------------------------------------------------------------------------
;;; Cellar — ZIL: CELLAR-FCN M-LOOK
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest cellar-description
  ;; Cellar is dark — give player a lit lamp so the description is visible.
  (reset! z/here :cellar)
  (swap! z/world update-in [:objects :lamp :location] (constantly :adventurer))
  (swap! z/world update-in [:objects :lamp :flags] conj :onbit)
  (z/update-lit!)
  (let [out (output-of z/v-look)]
    (is (clojure.string/includes? out "You are in a dark and damp cellar with a narrow passageway leading"))
    (is (clojure.string/includes? out "north, and a crawlway to the south."))))

;;; ---------------------------------------------------------------------------
;;; Exit traversal — basic room-to-room movement
;;; ---------------------------------------------------------------------------

(deftest kitchen-to-attic
  (reset! z/here :kitchen)
  (do! "go up")
  (is (= :attic @z/here)))

(deftest attic-to-kitchen
  (reset! z/here :attic)
  (do! "go down")
  (is (= :kitchen @z/here)))

(deftest kitchen-to-living-room
  (reset! z/here :kitchen)
  (do! "go west")
  (is (= :living-room @z/here)))

(deftest living-room-to-kitchen
  (reset! z/here :living-room)
  (do! "go east")
  (is (= :kitchen @z/here)))

(deftest kitchen-east-blocked-when-window-closed
  ;; Window closed — player should not move
  (reset! z/here :kitchen)
  (do! "go east")
  (is (= :kitchen @z/here)))

(deftest kitchen-east-when-window-open
  (swap! z/world update-in [:objects :kitchen-window :flags] conj :openbit)
  (reset! z/here :kitchen)
  (do! "go east")
  (is (= :east-of-house @z/here)))

(deftest living-room-down-blocked-when-trap-door-invisible
  ;; Trap door is invisible before rug is moved — player sees "can't go that way"
  ;; Verified against DOSBox r88/840726
  (reset! z/here :living-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "You can't go that way."))))

(deftest living-room-down-blocked-when-trap-door-closed-but-visible
  ;; Trap door visible (rug moved) but still closed — "The trap door is closed."
  ;; Verified against DOSBox r88/840726
  (reset! z/rug-moved true)
  (swap! z/world update-in [:objects :trap-door :flags] disj :invisible)
  (reset! z/here :living-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "The trap door is closed."))))

(deftest living-room-to-cellar-when-trap-door-open
  ;; Note: ZIL TRAP-DOOR-EXIT also requires RUG-MOVED (1actions.zil:567).
  ;; rug-moved guard not yet implemented — this test will need updating
  ;; when the rug puzzle is translated.
  (swap! z/world update-in [:objects :trap-door :flags] conj :openbit)
  (reset! z/here :living-room)
  (do! "go down")
  (is (= :cellar @z/here)))

(deftest cellar-to-living-room-when-trap-door-open
  (swap! z/world update-in [:objects :trap-door :flags] conj :openbit)
  (reset! z/here :cellar)
  (do! "go up")
  (is (= :living-room @z/here)))

(deftest cellar-up-blocked-when-trap-door-closed
  ;; Simulate: rug moved (:invisible cleared), trap door opened, player descends.
  ;; CELLAR-FCN M-ENTER closes the door (:openbit removed, :touchbit set).
  ;; Going back up should be blocked with "The trap door is closed."
  ;; Verified against DOSBox r88/840726
  (swap! z/world update-in [:objects :trap-door :flags] #(-> % (disj :invisible) (conj :openbit)))
  (reset! z/here :living-room)
  (do! "go down")
  (let [out (output-of #(do! "go up"))]
    (is (clojure.string/includes? out "The trap door is closed."))
    (is (= :cellar @z/here))))

;;; ---------------------------------------------------------------------------
;;; Lower room graph — exit traversal
;;; ---------------------------------------------------------------------------

(deftest cellar-to-troll-room
  (reset! z/here :cellar)
  (do! "go north")
  (is (= :troll-room @z/here)))

(deftest troll-room-to-cellar
  (reset! z/here :troll-room)
  (do! "go south")
  (is (= :cellar @z/here)))

(deftest cellar-to-east-of-chasm
  (reset! z/here :cellar)
  (do! "go south")
  (is (= :east-of-chasm @z/here)))

(deftest east-of-chasm-to-gallery
  (reset! z/here :east-of-chasm)
  (do! "go east")
  (is (= :gallery @z/here)))

(deftest gallery-to-studio
  (reset! z/here :gallery)
  (do! "go north")
  (is (= :studio @z/here)))

(deftest studio-to-gallery
  (reset! z/here :studio)
  (do! "go south")
  (is (= :gallery @z/here)))

;;; ---------------------------------------------------------------------------
;;; Cellar M-ENTER — trap door closes automatically on entry
;;; ZIL: CELLAR-FCN M-ENTER (gverbs.zil:1855)
;;; Verified against DOSBox r88/840726
;;; ---------------------------------------------------------------------------

(deftest cellar-entry-closes-trap-door
  ;; Open the trap door and enter cellar — it should auto-close
  (swap! z/world update-in [:objects :trap-door :flags] conj :openbit)
  (reset! z/here :living-room)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "The trap door crashes shut, and you hear someone barring it.")))
  (is (not (contains? (get-in @z/world [:objects :trap-door :flags]) :openbit))))

;;; ---------------------------------------------------------------------------
;;; Object existence — trophy-case, trap-door, map
;;; ---------------------------------------------------------------------------

(deftest trophy-case-exists-in-living-room
  (is (= :living-room (get-in @z/world [:objects :trophy-case :location]))))

(deftest trap-door-exists-in-living-room
  (is (= :living-room (get-in @z/world [:objects :trap-door :location]))))

(deftest map-exists-in-trophy-case
  (is (= :trophy-case (get-in @z/world [:objects :map :location]))))

(deftest map-starts-invisible
  (is (contains? (get-in @z/world [:objects :map :flags]) :invisible)))

(deftest trap-door-starts-invisible
  (is (contains? (get-in @z/world [:objects :trap-door :flags]) :invisible)))

(deftest trophy-case-is-container
  (is (z/flag? (z/get-object :trophy-case) :contbit)))

;;; ---------------------------------------------------------------------------
;;; Blocked-string exits — rooms that produce a specific message
;;; Consistent with around_house_test.clj pattern
;;; ---------------------------------------------------------------------------

(deftest living-room-west-blocked
  (reset! z/here :living-room)
  (let [out (output-of #(do! "go west"))]
    (is (clojure.string/includes? out "The door is nailed shut."))))

(deftest kitchen-down-blocked
  (reset! z/here :kitchen)
  (let [out (output-of #(do! "go down"))]
    (is (clojure.string/includes? out "Only Santa Claus climbs down chimneys."))))

(deftest cellar-west-blocked
  (reset! z/here :cellar)
  (let [out (output-of #(do! "go west"))]
    (is (clojure.string/includes? out "You try to ascend the ramp, but it is impossible, and you slide back down."))))

(deftest troll-room-east-blocked
  (reset! z/here :troll-room)
  (let [out (output-of #(do! "go east"))]
    (is (clojure.string/includes? out "The troll fends you off with a menacing gesture."))))

(deftest troll-room-west-blocked
  (reset! z/here :troll-room)
  (let [out (output-of #(do! "go west"))]
    (is (clojure.string/includes? out "The troll fends you off with a menacing gesture."))))
