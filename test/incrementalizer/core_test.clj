(ns incrementalizer.core-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [incrementalizer.core :as core]))

(deftest minimal-change
  (stest/instrument `core/minimal-change)

  (testing "when nothing is deployed"
    (is (= (yaml/parse-string (core/minimal-change {:constraints-path "resources/fixtures/constraints.edn"
                                                    :deployed-config-path "resources/fixtures/nothing.yml"
                                                    :desired-config-path "resources/fixtures/desired.yml"}))
           {:director-config {:properties-configuration {:director_configuration {:director_worker_count 5}}}
            :opsman-version "2.5.0"})))

  (testing "when the desired configuration would change multiple tiles"
    (is (= (yaml/parse-string (core/minimal-change {:constraints-path "resources/fixtures/constraints.edn"
                                                    :deployed-config-path "resources/fixtures/deployed.yml"
                                                    :desired-config-path "resources/fixtures/desired.yml"}))
           {:director-config {:properties-configuration {:director_configuration {:director_worker_count 5}}}
            :opsman-version "2.5.0"
            :products [{:product-name "cf"
                        :version "2.5.4"}]})))

  (testing "when a tile just needs an update"
    (is (= (yaml/parse-string (core/minimal-change {:constraints-path "resources/fixtures/constraints.edn"
                                                    :deployed-config-path "resources/fixtures/desired.yml"
                                                    :desired-config-path "resources/fixtures/desired2.yml"}))
           {:director-config {:properties-configuration {:director_configuration {:director_worker_count 5}}}
            :opsman-version "2.5.0"
            :products [{:product-name "p-redis"
                        :version "1.0.0"}
                       {:product-name "cf"
                        :version "2.5.5"}]})))
  (testing "when the desired is already deployed"
    (is (= (yaml/parse-string (core/minimal-change {:constraints-path "resources/fixtures/constraints.edn"
                                                    :deployed-config-path "resources/fixtures/desired2.yml"
                                                    :desired-config-path "resources/fixtures/desired2.yml"})))
        (yaml/parse-string (slurp "resources/fixtures/desired2.yml")))))
