(ns actions-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [zork1.actions :as z]))

(use-fixtures :each (fn [f] (z/load-world!) (f)))

(defn output-of [thunk]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw] (thunk))
    (str/trim (str sw))))

;;; ---------------------------------------------------------------------------
;;; dispatch-action — ZIL: object ACTION property dispatch (#48)
;;; Looks up :action on the object, calls the matching object-action handler.
;;; Truthy return = handler took over (ZIL RTRUE); nil = fall through to verb.
;;; ---------------------------------------------------------------------------

(deftest dispatch-returns-nil-for-action-without-handler
  ;; :sword has :action :sword-fcn but no defmethod → :default → nil (pass through)
  (testing "an :action value with no handler falls through"
    (is (nil? (z/dispatch-action :sword :take)))))

(deftest dispatch-returns-nil-for-unhandled-verb
  ;; lamp's :lantern handler handles :lamp-on/:lamp-off only; other verbs pass through
  (testing "a handler returns nil for a verb it does not handle"
    (is (nil? (z/dispatch-action :lamp :examine)))))

(deftest lantern-skips-lamp-on-when-burned-out
  (swap! z/world update-in [:objects :lamp :flags] conj :rmungbit)
  (let [result (atom nil)
        out    (output-of #(reset! result (z/dispatch-action :lamp :lamp-on)))]
    (testing "burned-out lamp-on returns truthy (RTRUE) and prints the message"
      (is @result)
      (is (str/includes? out "A burned-out lamp won't light.")))))

(deftest lantern-passes-through-lamp-on-when-healthy
  ;; lamp is not burned out → handler returns nil so V-LAMP-ON runs
  (testing "healthy lamp-on falls through to the verb handler"
    (is (not (z/flag? (z/get-object :lamp) :rmungbit)) "precondition: lamp not burned out")
    (is (nil? (z/dispatch-action :lamp :lamp-on)))))

(deftest lantern-skips-lamp-off-when-burned-out
  (swap! z/world update-in [:objects :lamp :flags] conj :rmungbit)
  (let [result (atom nil)
        out    (output-of #(reset! result (z/dispatch-action :lamp :lamp-off)))]
    (testing "burned-out lamp-off returns truthy (RTRUE) and prints the message"
      (is @result)
      (is (str/includes? out "The lamp has already burned out.")))))

(deftest mailbox-take-is-handled
  (let [result (atom nil)
        out    (output-of #(reset! result (z/dispatch-action :mailbox :take)))]
    (testing "mailbox :take returns truthy and prints the anchored message"
      (is @result)
      (is (str/includes? out "It is securely anchored.")))))

(deftest mailbox-passes-through-non-take-verb
  (testing "mailbox handler returns nil for verbs other than :take"
    (is (nil? (z/dispatch-action :mailbox :open)))))

(deftest mailbox-passes-through-take-when-not-prso
  ;; ZIL MAILBOX-F guards on <EQUAL? ,PRSO ,MAILBOX>: PERFORM also applies it
  ;; when the mailbox is PRSI (TAKE LEAFLET FROM MAILBOX), where it must not fire.
  (let [result (atom :unset)
        out    (output-of #(reset! result (z/object-action :mailbox-f
                                                           {:prsa :take
                                                            :prso :leaflet
                                                            :prsi :mailbox})))]
    (is (nil? @result))
    (is (= "" out))))

;;; ---------------------------------------------------------------------------
;;; handled — ZIL: <TELL ... CR> <RTRUE> pre-empt idiom (#48 review I-2)
;;; A handler that prints must also return truthy, or the verb runs as well.
;;; These tests register a throwaway handler on the sword (:trytakebit, no
;;; real handler) to show the contract end-to-end through v-take.
;;; ---------------------------------------------------------------------------

(deftest handled-prints-and-returns-truthy
  (let [result (atom nil)
        out    (output-of #(reset! result (z/handled "It is securely anchored.")))]
    (is @result)
    (is (= "It is securely anchored." out))))

(defn- with-test-handler
  "Point :sword at a throwaway :action value whose :take branch is `f`, run
  `thunk`, then unregister the method."
  [f thunk]
  (defmethod z/object-action ::test-handler [_ {:keys [prsa]}] (when (= prsa :take) (f)))
  (try
    (swap! z/world assoc-in [:objects :sword :action] ::test-handler)
    (thunk)
    (finally (remove-method z/object-action ::test-handler))))

(deftest handler-using-handled-pre-empts-take
  (with-test-handler #(z/handled "Blocked.")
    (fn []
      (is (= "Blocked." (output-of #(z/v-take :sword))))
      (is (not= :winner (get-in @z/world [:objects :sword :location]))))))

(deftest print-then-nil-handler-does-not-pre-empt-take
  ;; The hazard `handled` exists to prevent: printing without returning truthy
  ;; is RFALSE, so V-TAKE runs too — double output and the object is taken.
  (with-test-handler #(println "Blocked.")
    (fn []
      (is (= "Blocked.\nTaken." (output-of #(z/v-take :sword))))
      (is (= :winner (get-in @z/world [:objects :sword :location]))))))

;;; ---------------------------------------------------------------------------
;;; Handler/EDN consistency (#48 review S-1)
;;; Unhandled :action values route to :default and silently fall through, so a
;;; defmethod whose key doesn't match any :action value in 1dungeon.edn would
;;; never fire and nothing would complain. The EDN values are transcribed from
;;; ZIL; the defmethod keys are hand-typed — check the hand-typed side.
;;; ---------------------------------------------------------------------------

(deftest every-object-action-handler-matches-an-edn-action
  (let [edn-actions (set (keep :action (vals (:objects @z/world))))
        handlers    (disj (set (keys (methods z/object-action))) :default)]
    (is (seq handlers))
    (doseq [h handlers]
      (is (contains? edn-actions h)
          (str "object-action handler " h " matches no :action in 1dungeon.edn")))))
