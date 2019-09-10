(ns incrementalizer.core-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [incrementalizer.core :as core]))

(deftest minimal-change
  (stest/instrument `core/minimal-change)

  (testing "when nothing is deployed"
    (is (= (yaml/parse-string (core/minimal-change {:deployed-config-path "resources/fixtures/nothing.yml"
                                                    :desired-config-path "resources/fixtures/desired.yml"}))
           {:director-config {:properties-configuration {:director_configuration {:director_worker_count 5}}}})))

  (testing "when the desired configuration would change multiple tiles"
    (is (= (yaml/parse-string (core/minimal-change {:deployed-config-path "resources/fixtures/deployed.yml"
                                                    :desired-config-path "resources/fixtures/desired.yml"}))
           {:director-config {:properties-configuration {:director_configuration {:director_worker_count 5}}}
            :products [{:product-name "cf"
                        :version "2.5.4"}]}))))
