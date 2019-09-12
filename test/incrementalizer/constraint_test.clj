(ns incrementalizer.constraint-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [incrementalizer.constraint :as constraint]))

(deftest valid-config?
  (stest/instrument `constraint/valid-config?)

  (testing "a pair of products"
    (let [constraints [{:selector {:product-name ".-metrics"
                                   :version "1.1.5"}
                        :requires [{:product-name "cf"
                                    :version "1.0.0"}]}]]
      (is (s/valid? ::constraint/constraints constraints))
      (is (constraint/valid-config? constraints {:director-config {}
                                                 :opsman-version "2.0.0"
                                                 :products [{:product-name "p-metrics"
                                                             :version "1.2.0"}
                                                            {:product-name "cf"
                                                             :version "2.0.0"}]}))
      (is (not (constraint/valid-config? constraints {:director-config {}
                                                      :opsman-version "2.0.0"
                                                      :products [{:product-name "p-metrics"
                                                                  :version "1.2.0"}]}))))))
