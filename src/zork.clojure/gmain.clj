(ns gmain
  (:require [zork1.actions :as actions]
            [gparser :as parser]))

;;; ---------------------------------------------------------------------------
;;; Verb dispatch — mirrors PERFORM in GMAIN.ZIL
;;;
;;; Turn rule — ZIL MAIN-LOOP-1 (gmain.zil:169-172): once the parse succeeds,
;;; CLOCKER runs whether or not the action succeeded, except for meta-verbs.
;;; Handlers signal the two cases that skip it:
;;;   :parse-fail — the ZIL parser would have rejected the command (orphaned
;;;                 object, HAVE check), so P-WON is false
;;;   :clock-wait — the handler ran the clock itself (ZIL CLOCK-WAIT, V-WAIT)
;;; :quit ends the game. Any other return value is ignored for turn counting.
;;; ---------------------------------------------------------------------------

(def last-cmd (atom nil))

;;; ZIL: the no-CLOCKER verb list in MAIN-LOOP-1. Only these are implemented.
(def meta-verbs #{:score :quit})

(declare perform)

;;; ZIL parser orphan ("What do you want to take?") — P-WON is false.
(defn- orphan [msg]
  (println msg)
  :parse-fail)

(defn- dispatch [{:keys [verb obj dir] :as cmd}]
  (case verb
    :look      (if obj
                 (orphan "That sentence isn't one I recognize.")
                 (actions/v-look))
    :examine   (if obj
                 (actions/v-examine obj)
                 (orphan "What do you want to examine?"))
    :look-in   (if obj
                 (actions/v-look-inside obj)
                 (orphan "What do you want to look in?"))
    :open      (if obj
                 (actions/v-open obj)
                 (orphan "What do you want to open?"))
    :read      (if obj
                 (actions/v-read obj)
                 (orphan "What do you want to read?"))
    :take      (if obj
                 (actions/v-take obj)
                 (orphan "What do you want to take?"))
    :put       (if (and obj (:container cmd))
                 (actions/v-put obj (:container cmd))
                 (orphan "What do you want to put where?"))
    :drop      (if obj
                 (actions/v-drop obj)
                 (orphan "What do you want to drop?"))
    :close     (if obj
                 (actions/v-close obj)
                 (orphan "What do you want to close?"))
    :move      (if obj
                 (actions/v-move obj)
                 (orphan "What do you want to move?"))
    :climb     (actions/v-walk :up)
    :wait      (actions/v-wait)
    :diagnose  (actions/v-diagnose)
    :score     (actions/v-score)
    :inventory (actions/v-inventory)
    :go        (if dir
                 (actions/v-walk dir)
                 (orphan "Which direction?"))
    :lamp-on   (if obj
                 (actions/v-lamp-on obj)
                 (orphan "What do you want to light?"))
    :lamp-off  (if obj
                 (actions/v-lamp-off obj)
                 (orphan "What do you want to extinguish?"))
    :quit      (do (println "Your score is 0. Goodbye.") :quit)
    :unknown   (orphan "I don't understand that.")))

(defn perform [cmd]
  (cond
    ;; Unrecognised object word — short-circuit, no turn
    (= (:obj cmd) :not-found)
    (println "I don't see that here.")

    ;; Again — ZIL: replay last parsed command.
    ;; No prior command → "Beg pardon?" (ZIL: empty AGAIN-LEXV)
    ;; Prior parse failed (:unknown) → "That would just repeat a mistake."
    ;; Otherwise → replay (even if the prior action itself failed)
    (= (:verb cmd) :again)
    (cond
      (nil? @last-cmd)                    (println "Beg pardon?")
      (= (:verb @last-cmd) :unknown)      (println "That would just repeat a mistake.")
      :else                               (perform @last-cmd))

    ;; All other verbs — record for again (every parsed command, even failed
    ;; actions; only :unknown parse failures are excluded via AGAIN check),
    ;; dispatch, then run the clock per the turn rule above.
    :else
    (do
      (reset! last-cmd cmd)
      (let [result (dispatch cmd)]
        (when-not (or (#{:parse-fail :clock-wait :quit} result)
                      (meta-verbs (:verb cmd)))
          (actions/clocker!))
        result))))

;;; ---------------------------------------------------------------------------
;;; Main game loop — mirrors MAIN-LOOP in GMAIN.ZIL
;;; ---------------------------------------------------------------------------

(defn game-loop []
  (reset! last-cmd nil)
  (actions/load-world!)
  (println "ZORK I: The Great Underground Empire")
  (println "Copyright (c) 1981, 1982, 1983 Infocom, Inc. All rights reserved.")
  (println "ZORK is a registered trademark of Infocom, Inc.")
  (println "Revision 88 / Serial number 840726")
  (println)
  (actions/v-look)
  (loop []
    (println)
    (print "> ")
    (flush)
    (let [input (read-line)]
      (when (some? input)
        (let [cmd    (parser/parse input)
              result (perform cmd)]
          (when-not (= result :quit)
            (recur)))))))

(defn -main [& _]
  (game-loop))
