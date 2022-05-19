(ns impp.donatelo.donatelo
  (:use [clojure.test]
        [util.helper]
        [impp.vdnntl.alpha]
        [impp.vdnntl.common])
  (:require [clojure.tools.logging :as LOG]
            [com.climate.claypoole :as CP]))

;; Weight functions
(defn- get-weight-sum
  "Calculates the sum of the values for the passed `tokens` in the given `dictionary`"
  [dictionary tokens]
  (reduce #(+ %1 (get dictionary %2 0.0)) 0.0 tokens))

(defn- get-weight-value-for
  "Calculates the sum of the values of the passed `tokens` in the given `dictionary-kv`.
  The return value is a vector where the first entry is the label and the second entry the calculated sum."
  [dictionary-kv tokens]
  {:pre [(is (map-entry? dictionary-kv) (str dictionary-kv " is not a map entry!"))]}
  (vector (key dictionary-kv) (get-weight-sum (val dictionary-kv) tokens)))

(defn predict
  "Returns for every label in the passed `dictionaries` the value for the given `tokens`.
  The result is a collections of sorted vectors where the first entry is the label and the second entry is the value.
  Supported options:
  | key           | description    |
  |---------------|----------------|
  | `:normalize?` | Defines whether the values should be scaled to a value between 0.0 and 1.0. Standard value is false.
  | `:threads`    | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [dictionaries tokens & {:keys [threads normalize?] :or {threads std-threads normalize? false}}]
  (let [result (sort-by second > (CP/pmap threads #(get-weight-value-for %1 tokens) dictionaries))
        sum (if normalize? (apply + (map second result)) 1)]
    (if normalize?
      (doall (map #(assoc %1 1 (/ (get %1 1) sum)) result))
      result)))

(defn- skip-update?
  "Checks whether the update process of the weights can be skipped.
  The update can be skipped if the passed `token` has been added in a previous update step or the `token` is not contained
  in the dictionary and the update is for a wrong prediction, indicated by a negative `alpha`."
  [actual-dictionary-state alpha token]
  (or (contains? (:added actual-dictionary-state) token)
      (and (not (contains? (:dic actual-dictionary-state) token))
           (neg? alpha))))

(defn- update-weights
  "Returns an updated dictionary based on the passed `selected-tokens` and the given `dictionary-kv`.
  If `alpha`is not 1.0 than the values in the dictionary are updated with the passed 'alpha' which can be adjusted with
  a given `:alpha-adjust-fn`. If a token does not exists in the passed `dictionary-kv`it is initialized with `beta`.
  Supported options:
  | key                | description    |
  |--------------------|----------------|
  | `:alpha-adjust-fn` | Function which receives a `token`,`label` and `alpha` and returns an adjusted alpha-value. Standard value is [[standard-alpha-adjustment]]."
  [dictionary-kv alpha selected-tokens beta & {:keys [alpha-adjust-fn]
                                               :or   {alpha-adjust-fn standard-alpha-adjustment}}]
  (let [label-id (key dictionary-kv)]
    (hash-map label-id (if (== (Double/POSITIVE_INFINITY) alpha)
                         (merge (val dictionary-kv)
                                (zipmap
                                  (clojure.set/difference (apply hash-set selected-tokens)
                                                          (apply hash-set (keys (val dictionary-kv))))
                                  (repeat beta)))
                         (:dic (reduce #(if (skip-update? %1 alpha %2)
                                          %1
                                          (update-in
                                            (assoc-in %1 [:dic %2]
                                                      (+ (get-in %1 [:dic %2] beta)
                                                         (* (alpha-adjust-fn label-id %2 alpha)
                                                            (get-in %1 [:dic %2] 0))))
                                            [:added] conj %2))
                                       {:dic   (val dictionary-kv)
                                        :added #{}}
                                       selected-tokens))))))
;; Training functions
(def ^:dynamic *accuracy-sum* 0)
(def ^:dynamic *accuracy-history* [])

(defn- get-expected-labels [example]
  (into #{} (map key (filter #(== 1.0 (val %1)) (:labels example)))))

(defn- update-dictionaries-for-example
  "Returns updated dictionaries based on the passed `example` and the given `dictionaries`.
  Supported options:
  | key                | description    |
  |--------------------|----------------|
  | `:alpha`           | Update rate parameter, standard value is 0.5.
  | `:beta`            | Learning rate parameter to initialize weights for unknown tokens, standard value is 0.3.
  | `:alpha-adjust-fn` | Function which receives a `token`,`label` and `alpha` and returns an adjusted alpha-value. Standard value is [[standard-alpha-adjustment]].
  | `:threads`         | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [dictionaries example & {:keys [alpha beta alpha-adjust-fn threads]
                           :or   {alpha           0.5
                                  beta            0.3
                                  threads         std-threads
                                  alpha-adjust-fn standard-alpha-adjustment}}]
  (let [expected-labels (get-expected-labels example)
        number-expected-labels (count expected-labels)
        predictions (take number-expected-labels (predict dictionaries (:documentTokens example) :threads threads))
        correct-labels (clojure.set/intersection (apply hash-set (map first predictions))
                                                 expected-labels)
        new-label-dictionaries (zipmap
                                 (clojure.set/difference expected-labels (apply hash-set (keys dictionaries)))
                                 (repeat {}))

        correct-count (count correct-labels)
        accuracy (if (== 0 correct-count) 0.0
                                          (/ correct-count number-expected-labels))]
    (do
      (set! *accuracy-sum* (+ *accuracy-sum* accuracy))
      (if (== 1.0 accuracy) dictionaries
                            (apply merge (CP/pmap threads #(update-weights %1
                                                                           (cond
                                                                             (contains? correct-labels (key %1)) (Double/POSITIVE_INFINITY)
                                                                             (contains? expected-labels (key %1)) alpha
                                                                             :else (* -1.0 alpha))
                                                                           (:documentTokens example)
                                                                           beta
                                                                           :alpha-adjust-fn alpha-adjust-fn)
                                                  (merge dictionaries new-label-dictionaries)))))))

(defn single-train
  "Returns updated dictionaries based on the passed `example` and the given `dictionaries`.
  Supported options:
  | key                | description    |
  |--------------------|----------------|
  | `:alpha`           | Update rate parameter, standard value is 0.5.
  | `:beta`            | Learning rate parameter to initialize weights for unknown tokens, standard value is 0.3.
  | `:alpha-adjust-fn` | Function which receives a `token`,`label` and `alpha` and returns an adjusted alpha-value. Standard value is [[standard-alpha-adjustment]].
  | `:threads`         | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [dictionaries example & {:keys [alpha beta alpha-adjust-fn threads]
                           :or   {alpha           0.5
                                  beta            0.3
                                  threads         std-threads
                                  alpha-adjust-fn standard-alpha-adjustment}}]

  (binding [*accuracy-sum* 0]
    (with-meta (update-dictionaries-for-example dictionaries example
                                                :alpha alpha :beta beta :threads threads
                                                :alpha-adjust-fn alpha-adjust-fn)
               {:accuracy *accuracy-sum*})))

(defn train
  "Starts the training process with the passed `dictionaries` with the given `examples`.
  Finish conditions for the training are either a minimal change in the accuracy or a maximum number of epochs.
  The result of the training is an updated `dictionaries`
  Supported options:
  | key                | description    |
  |--------------------|----------------|
  | `:alpha`           | Learning rate parameter, standard value is 0.5.
  | `:beta`            | Update rate parameter to initialize weights for unknown tokens, standard value is 0.3.
  | `:epochs`          | Maximum number of learning epochs, standard value is 8.
  | `:epsilon`         | Threshold for accuracy difference during the epochs, if the change is lower than this value the learning process is stopped. Standard value is 0.001.
  | `:alpha-adjust-fn` | Function which receives a `token`,`label` and `alpha` and returns an adjusted alpha-value. Standard value is [[standard-alpha-adjustment]].
  | `:threads`         | Number of used threads during the creation process. Standard value is [[std-threads]].
  | `:shuffle?`        | If true then `examples` are shuffled for every learning epoch. Standard value is false."
  [dictionaries examples & {:keys [alpha beta epochs epsilon alpha-adjust-fn threads shuffle?]
                            :or   {alpha           0.5
                                   beta            0.3
                                   epochs          8
                                   epsilon         0.001
                                   shuffle?        false
                                   threads         std-threads
                                   alpha-adjust-fn standard-alpha-adjustment}}]
  (binding [*accuracy-sum* 0
            *accuracy-history* [1.0 0]]
    (LOG/info (str "Start learning with " (count examples) " examples, threads " threads ", shuffle?: " shuffle? ", alpha "
                   alpha ", alpha adjustment function:" (get-description alpha-adjust-fn) ", beta " beta ", epsilon " epsilon " and epochs " epochs))
    (loop [used-dictionaries dictionaries epoch 0 start-time (System/currentTimeMillis)]
      (if (or
            (> epsilon (apply (comp abs -) (take-last 2 *accuracy-history*)))
            (= epoch epochs))
        used-dictionaries
        (do (LOG/info (str "Start learning epoch " (inc epoch) " of " epochs " epochs."))
            (set! *accuracy-sum* 0)
            (let [new-dictionaries (reduce #(update-dictionaries-for-example %1 %2
                                                                             :alpha alpha :beta beta :threads threads
                                                                             :alpha-adjust-fn alpha-adjust-fn)
                                           used-dictionaries (if shuffle? (shuffle examples) examples))
                  accuracy (double (/ *accuracy-sum* (count examples)))]
              (do
                (set! *accuracy-history* (conj *accuracy-history* accuracy))
                (LOG/info (str "Accuracy: " accuracy))
                (LOG/info (str "Diff: " (apply (comp abs -) (take-last 2 *accuracy-history*))))
                (LOG/info (str "Accuracy-history: " (rest *accuracy-history*)))
                (recur new-dictionaries
                       (inc epoch)
                       (do (LOG/info (str "Finished learning epoch " (inc epoch) " after " (- (System/currentTimeMillis) start-time)) " milliseconds.")
                           (System/currentTimeMillis))))))))))