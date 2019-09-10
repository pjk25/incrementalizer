(ns incrementalizer.core
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [foundation-lib.foundation-configuration :as foundation]))

(defn- paired-product-configs
  [deployed-products desired-products]
  (let [deployed-by-name (reduce #(assoc %1 (:product-name %2) %2) {} deployed-products)
        desired-by-name (reduce #(assoc %1 (:product-name %2) %2) {} desired-products)
        product-names (set (concat (keys deployed-by-name) (keys desired-by-name)))]
    (map #({:name %
            :deployed (deployed-by-name %)
            :desired (desired-by-name %)}) product-names)))

(defn- product-index
  [products product-name]
  (first (keep-indexed #(if (= product-name (:product-name %2))
                          %1)
                       products)))

(defn- single-product-change
  [deployed-config desired-config changed-product-pair]
  (let [{:keys [name deployed desired]} changed-product-pair]
    (match [deployed desired]
      [nil _] (update deployed-config :products #(conj desired %))
      [_ nil] (update deployed-config :products #(remove (partial = deployed) %))
      :else (let [index (product-index (:products deployed-config) name)]
              (assoc-in deployed-config [:products index] desired)))))

(defn- single-tile-changes
  [deployed-config desired-config]
  (let [product-pairs (paired-product-configs (:products deployed-config) (:products desired-config))
        changed-products (filter #(foundation/requires-changes? (:deployed %) (:desired %)) product-pairs)
        single-product-changes (map #(single-product-change deployed-config desired-config %)
                                    changed-products)]
    (if (foundation/requires-changes? (:director-config deployed-config) (:director-config desired-config))
      (let [director-changes (assoc deployed-config :director-config (:director-config desired-config))]
        (cons director-changes single-product-changes))
      single-product-changes)))

(defn- possible-changes
  [deployed-config desired-config]
  ; TODO: incorporate the multi-tile changes resulting from merging n single tile changes
  (single-tile-changes deployed-config desired-config))

(defn- valid-config?
  [desired-config]
  ; TODO: we want to read an edn which specifies version based dependencies
  true)

(defn minimal-change
  [cli-options]
  (let [{:keys [deployed-config-path desired-config-path]} cli-options
        raw-deployed-config (yaml/parse-string (slurp (io/file deployed-config-path)))
        raw-desired-config (yaml/parse-string (slurp (io/file desired-config-path)))
        deployed-config (s/conform ::foundation/config raw-deployed-config)
        desired-config (s/conform ::foundation/config raw-desired-config)]
    (if-let [incremental-config (first (filter valid-config? (possible-changes deployed-config desired-config)))]
      (println (yaml/generate-string incremental-config))
      (throw (ex-info "Could not compute a valid configuration" {})))))

(s/def ::deployed-config-path string?)

(s/def ::desired-config-path string?)

(s/fdef minimal-change
        :args (s/cat :cli-options (s/keys :req-un [::deployed-config-path ::desired-config-path]))
        :ret nil?)
