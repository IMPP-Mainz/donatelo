(ns impp.donatelo.alpha
  "This namespace contains functions to adjust the `alpha` value during the training process based on additional/extrenal
  knowledge."
  (:use [util.helper]))

(defn standard-alpha-adjustment
  "This function returns always the passed `alpha` value independent of the passed `label` or `token`"
  [label token alpha] alpha)

(defn create-new-alpha-adjustment-fn
  "Creates a function which returns an adjusted `alpha` based on the passe `label` and `token`.
   The adjustment is based on the information whether a token `token` is contained in the set of 'important' tokens for
   a `label` which are contained in passed `label-token-map`. If `token`is contained in `label-token-map` and `alpha` is
   positive then `alpha` is multiplied with `pos-multiplier` or if `alpha`is negative then `alpha` is multiplied with
   `neg-multiplier` else `alpha`is unchanged."
  [label-token-map pos-multiplier neg-multiplier]
  (with-meta (fn [label token alpha]
               (if (contains? (get label-token-map label #{}) token)
                 (* alpha (if (pos? alpha) pos-multiplier neg-multiplier))
                 alpha))
             {:pos-multiplier pos-multiplier
              :neg-multiplier neg-multiplier
              :source         (get-description label-token-map)}
             ))