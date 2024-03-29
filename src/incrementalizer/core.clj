(ns incrementalizer.core
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.math.combinatorics :as combo]
            [clj-yaml.core :as yaml]
            [clj-semver.core :as semver]
            [foundation-lib.query :as query]
            [foundation-lib.util :as util]
            [foundation-lib.deployed-configuration :as deployed-configuration]
            [foundation-lib.desired-configuration :as desired-configuration]
            [incrementalizer.constraint :as constraint]))

(defn- paired-product-configs
  [deployed-products desired-products]
  (let [deployed-by-name (reduce #(assoc %1 (:product-name %2) %2) {} deployed-products)
        desired-by-name (reduce #(assoc %1 (:product-name %2) %2) {} desired-products)
        product-names (set (concat (keys deployed-by-name) (keys desired-by-name)))]
    (map #(hash-map :name %
                    :deployed (deployed-by-name %)
                    :desired (desired-by-name %)) product-names)))

(defn- single-product-change
  [deployed-config changed-product-pair]
  (let [{:keys [name deployed desired]} changed-product-pair]
    (match [deployed desired]
      [nil _] (update deployed-config :products #(conj % desired))
      [_ nil] (update deployed-config :products #(disj % deployed))
      :else (update deployed-config
                    :products #(-> %
                                   (disj deployed)
                                   (conj desired))))))

(defn- selective-deploy
  [deployed-config products]
  (let [[product & rest] products]
    (match [product]
      [nil]              deployed-config
      [{:name "p-bosh"}] (selective-deploy (assoc deployed-config :director-config (:desired product)) rest)
      [_]                (selective-deploy (single-product-change deployed-config product) rest))))

(defn- all-combinations
  [list]
  (apply concat (map #(combo/combinations list (inc %)) (range (count list)))))

(defn- possible-changes
  [cli-options deployed-config desired-config]
  (let [product-pairs (paired-product-configs (:products deployed-config) (:products desired-config))
        changed-products (cond->> (filter #(query/product-requires-changes? (:deployed %) (:desired %)) product-pairs)
                           (query/director-requires-changes? (:director-config deployed-config)
                                                             (:director-config desired-config)) (cons {:name "p-bosh"
                                                                                                       :deployed (:director-config deployed-config)
                                                                                                       :desired (:director-config desired-config)}))]
    (when (:debug cli-options)
      (binding [*out* *err*]
        (println "Found changes in the following products: " (map :name changed-products))))
    (map #(selective-deploy deployed-config %) (all-combinations changed-products))))

(defn minimal-change
  [cli-options]
  (let [{:keys [constraints-path deployed-config-path desired-config-path]} cli-options
        raw-constraints (clojure.edn/read-string (slurp (io/file constraints-path)))
        raw-deployed-config (yaml/parse-string (slurp (io/file deployed-config-path)))
        raw-desired-config (yaml/parse-string (slurp (io/file desired-config-path)))
        constraints (s/conform ::constraint/constraints raw-constraints)
        deployed-config (s/conform ::deployed-configuration/deployed-config raw-deployed-config)
        desired-config (s/conform ::desired-configuration/desired-config raw-desired-config)]

    (when (= ::s/invalid constraints)
      (binding [*out* *err*]
        (println "The constraints are not valid")
        (s/explain ::constraint/constraints raw-constraints)
        (println))
      (throw (ex-info "The constraints are not valid" {})))

    (when (= ::s/invalid deployed-config)
      (binding [*out* *err*]
        (println "The deployed foundation configuration is not valid")
        (s/explain ::deployed-configuration/deployed-config raw-deployed-config)
        (println))
      (throw (ex-info "The deployed foundation configuration is not valid" {})))

    (when (= ::s/invalid desired-config)
      (binding [*out* *err*]
        (println "The desired foundation configuration is not valid")
        (s/explain ::desired-configuration/desired-config raw-desired-config)
        (println))
      (throw (ex-info "The desired foundation configuration is not valid" {})))

    (let [extra-config (util/non-specd ::desired-configuration/desired-config desired-config)]
      (when-not (empty? extra-config)
        (binding [*out* *err*]
          (println "The desired foundation configuration contains extraneous data")
          (pprint extra-config)
          (println))
        (throw (ex-info "The desired foundation configuration contains extraneous data" extra-config))))

    (if (= deployed-config desired-config)
      (yaml/generate-string desired-config)
      (if-let [incremental-config (first (filter (partial constraint/valid-config? constraints) (possible-changes cli-options deployed-config desired-config)))]
        (yaml/generate-string incremental-config)
        (throw (ex-info "Could not compute a valid configuration" {}))))))

(s/def ::deployed-config-path string?)

(s/def ::desired-config-path string?)

(s/fdef minimal-change
        :args (s/cat :cli-options (s/keys :req-un [::deployed-config-path ::desired-config-path]))
        :ret string?)
