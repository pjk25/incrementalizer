(ns user
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [run-tests run-all-tests]]))

(require '[clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]])

(set-refresh-dirs "src" "test")

(defn run-my-tests
  []
  (refresh)
  (run-all-tests #"incrementalizer.*-test"))

(defn run-some-tests
  [selector]
  (refresh)
  (run-all-tests (re-pattern (str "incrementalizer." selector "-test"))))