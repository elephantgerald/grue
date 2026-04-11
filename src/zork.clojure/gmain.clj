(ns gmain
  (:require [zork1.actions :as actions]
            [gparser :as parser]))

;;; ---------------------------------------------------------------------------
;;; Verb dispatch — mirrors PERFORM in GMAIN.ZIL
;;; ---------------------------------------------------------------------------

(def last-cmd (atom nil))

(defn perform [{:keys [verb obj dir] :as cmd}]
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
    :again     (if @last-cmd
                 (perform @last-cmd)
                 (println "You haven't done anything yet."))
    :quit      (do (println "Your score is 0. Goodbye.") :quit)
    :unknown   (println "I don't understand that.")))

;;; ---------------------------------------------------------------------------
;;; Main game loop — mirrors MAIN-LOOP in GMAIN.ZIL
;;; ---------------------------------------------------------------------------

(defn game-loop []
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
              _      (when-not (= (:verb cmd) :again)
                       (reset! last-cmd cmd))
              result (perform cmd)]
          (when-not (= result :quit)
            (recur)))))))

(defn -main [& _]
  (game-loop))
