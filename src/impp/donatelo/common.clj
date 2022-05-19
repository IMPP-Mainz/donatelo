(ns impp.donatelo.common
  (:require [com.climate.claypoole :as CP]
            [clojure.tools.logging :as LOG]))

(def std-threads (+ 2 (CP/ncpus)))

(defrecord DynamicTokenVectorWithLabels [id documentTokens labels])

(defn create-dtv-vector
  "Transforms the passed `examples` into Dynamic Token Vectors (DTV) which are needed for the learning process.
  For every example one [[DynamicTokenVectorWithLabels]] is returned, where :id is defined by the passed 'id-fn',
  :documentTokens by the passed `token-fn' and :labels by the passed 'label-fn'.
  Supported options:
  | key        | description    |
  |------------|----------------|
  | `:threads` | Number of used threads during the creation process. Standard value is [[std-threads]]."
  [examples token-fn label-fn id-fn & {:keys [threads] :or {threads std-threads}}]
  (LOG/info "Create " (count examples) "DTV-vectors with " threads " threads.")
  (doall (CP/pmap threads #(do
                             (System/gc)
                             (->DynamicTokenVectorWithLabels (id-fn %1) (token-fn %1) (label-fn %1)))
                  examples)))
