(ns gmain
  (:require [zork1.actions :as actions]
            [gparser :as parser]))

;;; ---------------------------------------------------------------------------
;;; Verb dispatch — mirrors PERFORM in GMAIN.ZIL
;;;
;;; Returns :turn when the command consumed a move (successful state-changing
;;; action), :quit when the player quits, or nil otherwise.
;;; Increments actions/turns on :turn before returning.
;;; ---------------------------------------------------------------------------

(def last-cmd (atom nil))

(declare perform)

(defn- dispatch [{:keys [verb obj dir] :as cmd}]
  (case verb
    :look      (if obj
                 (println "That sentence isn't one I recognize.")
                 (actions/v-look))
    :examine   (if obj
                 (actions/v-examine obj)
                 (println "What do you want to examine?"))
    :look-in   (if obj
                 (actions/v-look-inside obj)
                 (println "What do you want to look in?"))
    :open      (if obj
                 (actions/v-open obj)
                 (println "What do you want to open?"))
    :read      (if obj
                 (actions/v-read obj)
                 (println "What do you want to read?"))
    :take      (if obj
                 (actions/v-take obj)
                 (println "What do you want to take?"))
    :put       (if (and obj (:container cmd))
                 (actions/v-put obj (:container cmd))
                 (println "What do you want to put where?"))
    :drop      (if obj
                 (actions/v-drop obj)
                 (println "What do you want to drop?"))
    :close     (if obj
                 (actions/v-close obj)
                 (println "What do you want to close?"))
    :move      (if obj
                 (actions/v-move obj)
                 (println "What do you want to move?"))
    :climb     (actions/v-walk :up)
    :wait      (actions/v-wait)
    :diagnose  (actions/v-diagnose)
    :score     (actions/v-score)
    :inventory (actions/v-inventory)
    :go        (if dir
                 (actions/v-walk dir)
                 (println "Which direction?"))
    :quit      (do (println "Your score is 0. Goodbye.") :quit)
    :unknown   (println "I don't understand that.")))

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
    ;; dispatch, then count the turn on success.
    :else
    (do
      (reset! last-cmd cmd)
      (let [result (dispatch cmd)]
        (when (= result :turn)
          (swap! actions/turns inc))
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
