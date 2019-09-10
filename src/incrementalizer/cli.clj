(ns incrementalizer.cli
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.data.json :as json]
            [incrementalizer.core :as core]))

(set! *warn-on-reflection* true)

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread throwable]
     (println (.getMessage throwable))
     (System/exit 1))))

(def cli-options
  [["-h" "--help"]
   [nil  "--debug" "Print additional output"]])

(defn usage [options-summary]
  (->> ["Usage: incrementalizer [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        " min    Compute a valid minimal change"]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (and (= 3 (count arguments))
           (#{"min"} (first arguments)))
      {:action core/minimal-change :options (assoc options
                                                   :deployed-config-path (nth arguments 1)
                                                   :desired-config-path (nth arguments 2))}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try
        (println (action options))
        (catch Exception e
          (if (:debug options) (.printStackTrace e))
          (exit 1 (str "\nERROR: " e)))))))
