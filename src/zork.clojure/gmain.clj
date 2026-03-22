(ns gmain
  (:require [zork1.actions :as actions]
            [gparser :as parser]))

;;; ---------------------------------------------------------------------------
;;; Verb dispatch — mirrors PERFORM in GMAIN.ZIL
;;; ---------------------------------------------------------------------------

(defn perform [{:keys [verb obj dir]}]
  (case verb
    :look    (actions/v-look)
    :examine (if obj
               (actions/v-examine obj)
               (println "What do you want to examine?"))
    :open    (if obj
               (actions/v-open obj)
               (println "What do you want to open?"))
    :read    (if obj
               (actions/v-read obj)
               (println "What do you want to read?"))
    :go      (if dir
               (actions/v-walk dir)
               (println "Which direction?"))
    :quit    (do (println "Your score is 0. Goodbye.") :quit)
    :unknown (println (str "I don't understand that."))))

;;; ---------------------------------------------------------------------------
;;; Main game loop — mirrors MAIN-LOOP in GMAIN.ZIL
;;; ---------------------------------------------------------------------------

(defn game-loop []
  (actions/load-world!)
  (println "ZORK I: The Great Underground Empire")
  (println "Copyright (c) 1981 Infocom, Inc.")
  (println)
  (actions/v-look)
  (loop []
    (println)
    (print "> ")
    (flush)
    (let [input (read-line)]
      (when (some? input)
        (let [result (perform (parser/parse input))]
          (when-not (= result :quit)
            (recur)))))))

(defn -main [& _]
  (game-loop))
