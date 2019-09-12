(ns incrementalizer.constraint
  (:require [clojure.spec.alpha :as s]
            [clj-semver.core :as semver]
            [foundation-lib.foundation-configuration :as foundation]))

(s/def ::product-name string?)

(s/def ::version semver/valid-format?)

(s/def ::selector (s/keys :req-un [::product-name
                                   ::version]))

(s/def ::requires (s/coll-of ::selector
                             :distinct true
                             :into #{}))

(s/def ::constraint (s/keys :req-un [::selector]
                            :opt-un [::requires]))

(s/def ::constraints (s/coll-of ::constraint
                                :distinct true
                                :into #{}))

(defn- constraint-satisfied?
  [desired-config constraint]
  (letfn [(satisfied? [selector]
            (let [deployed-version (if (re-matches (:product-name selector) "p-bosh")
                                     (if-not (empty? (:director-config desired-config))
                                       (:opsman-version desired-config))
                                     (some #(if (re-matches (:product-name selector) (:product-name %))
                                              (:version %))
                                           (:products desired-config)))]
              (and deployed-version
                   (not (semver/older? deployed-version (:version selector))))))]
    (every? satisfied? (:requires constraint))))

(defn- constraint-matching
  [product-name version constraints]
  (->> constraints
       (filter #(re-matches (:product-name (:selector %)) product-name))
       (sort-by :version semver/cmp)
       (reverse)
       (drop-while #(semver/newer? (:version (:selector %)) version))
       (first)))

(defn- valid-config-for-product?
  [constraints desired-config product-name]
  ; TODO: report the p-bosh/opsman version in the configuration.yml on check for the resource
  ;       and make the version of the foundation just the hash, without the opsman version pair
  (let [desired-version (if (= "p-bosh" product-name)
                          (:version (:director-config desired-config))
                          (some #(if (= product-name (:product-name %)) (:version %)) (:products desired-config)))
        matched-constraint (constraint-matching product-name desired-version constraints)]
    (if matched-constraint
      (constraint-satisfied? desired-config matched-constraint)
      true)))

(defn- process-selector
  [selector]
  (update selector :product-name re-pattern))

(defn- process-constraint
  [constraint]
  (-> constraint
      (update :selector process-selector)
      (update :requires #(map process-selector %))))

(defn valid-config?
  [constraints desired-config]
  (let [processed-constraints (map process-constraint constraints)
        product-names (cons "p-bosh" (map :product-name (:products desired-config)))]
    (every? (partial valid-config-for-product? processed-constraints desired-config) product-names)))

(s/fdef valid-config?
        :args (s/cat :constraints ::constraints
                     :desired-config ::foundation/desired-config)
        :ret boolean?)
