(ns impp.donatelo.dictionary
  "Namespace to create and analyse dictionaries."
  (:use [util.helper]
        [impp.vdnntl.common])
  (:require [clojure.tools.logging :as LOG]
            [com.climate.claypoole :as CP]))

;; Dictionary creation functions
(defn- count-tokens [token-collection]
  (reduce #(update %1 %2 (fnil inc 0.0)) {} token-collection))

(defn create-dictionary
  "Creates a dictionary for the learning process based on the passed collections of tokens in `token-collections`.
  The dictionary is a map with the token as key and the term frequency (https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency)
  of this token as value.
  Supported options:
  | key        | description    |
  |------------|----------------|
  | `:threads` | Number of used threads during the creation process. Standard value is [[std-threads]].
  | `:k`       | Smoothing term. Standard value is 0.0."
  [token-collections & {:keys [threads k]
                        :or   {threads std-threads
                               k       0.0}}]
  (let [dictionary (apply merge-with + (CP/pmap threads #(count-tokens %1) token-collections))
        max-frequency (apply max 1 (vals dictionary))]
    (reduce #(assoc %1 (key %2)
                       (+ k
                          (* (- 1 k)
                             (/ (val %2) max-frequency))))
            {} dictionary)))

(defn- add-example-to-label-group [label-groups example]
  "Adds the `example` to the labels in `label-groups` based on the given labels in `example`."
  (reduce #(update-in %1 [%2] (fnil conj []) example) label-groups
          (keys (:labels example))))

(defn create-multi-label-category-dictionaries
  "Creates dictionaries for the learning process based on the passed `examples`.
  The returned dictionaries are nested maps where the key is label und the value is a map with a token as key and the
  term frequency (https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency) of the token for all `examples` which
  have the same label as the key. For every label one dictionary is returned.
  Supported options:
  | key        | description    |
  |------------|----------------|
  | `:threads` | Number of used threads during the creation process. Standard value is [[std-threads]].
  | `:k`       | Smoothing term. Standard value is 0.0."
  [examples & {:keys [threads k] :or {threads std-threads k 0.0}}]
  (LOG/info "Start creating multi-label dictionaries.")
  (apply merge (CP/pmap threads #(do (LOG/info "Start creating dictionary for label '" (key %1) "'.")
                                     (hash-map (key %1)
                                               (create-dictionary (map :documentTokens (val %1)) :k k)))
                        (reduce #(add-example-to-label-group %1 %2) {} examples))))

(defn get-top-x-tokens-from
  "Returns the 'x' tokens with the highest values for every label in the passed 'dictionaries'.
  Supported options:
  | key        | description    |
  |------------|----------------|
  | `:threads` | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [dictionaries x & {:keys [threads] :or {threads std-threads}}]
  (apply merge (CP/pmap threads #(sorted-map
                                   (key %1)
                                   (take x (sort-by second > (val %1)))) dictionaries)))

(defn get-last-x-tokens-from
  "Returns the 'x' tokens with the lowest values for every label in the passed 'dictionaries'.
  Supported options:
  | key        | description    |
  |------------|----------------|
  | `:threads` | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [dictionaries x & {:keys [threads] :or {threads std-threads}}]
  (apply merge (CP/pmap threads #(sorted-map
                                   (key %1)
                                   (take-last x (sort-by second > (val %1)))) dictionaries)))