(ns donatelo.core-test
  (:require [clojure.test :refer :all]
            [impp.donatelo.common :refer :all]
            [impp.donatelo.dictionary :refer :all]
            [impp.donatelo.vdnntl :refer :all]
            [impp.donatelo.alpha :refer :all]
            ))

;;The text for these examples has been taken from the Oxford Language definitions provided via Google search.
(def examples [["a domesticated carnivorous mammal that typically has a long snout, an acute sense of smell, non-retractable claws, and a barking, howling, or whining voice."
                ["dog" "mammal" "canine" "animal"]]
               ["a small domesticated carnivorous mammal with soft fur, a short snout, and retractable claws. It is widely kept as a pet or for catching mice, and many breeds have been developed."
                ["cat" "feline" "mammal" "animal"]]
               ["a very large plant-eating mammal with a prehensile trunk, long curved ivory tusks, and large ears, native to Africa and southern Asia. It is the largest living land animal."
                ["elephant" "mammal" "animal"]]
               ["a limbless cold-blooded vertebrate animal with gills and fins living wholly in water."
                ["fish" "animal"]]
               ["a long-bodied chiefly marine fish with a cartilaginous skeleton, a prominent dorsal fin, and toothlike scales. Most sharks are predatory, although the largest kinds feed on plankton, and some can grow to a large size."
                ["shark" "fish" "animal"]]
               ])

(def additional-examples
  [["a heavily built omnivorous nocturnal mammal of the weasel family, typically having a grey and black coat. "
    ["badger" "mammal" "animal" "weasel"]]
   ["a small, slender carnivorous mammal related to, but smaller than, the stoat. "
    ["weasel" "mammal" "animal"]]
   ])

(defn split-by-white-space-and-punctuation [s]
  " This functions splits the passed string into word tokens which are separated by whitespaces or punctuations. Empty strings will be discarded. "
  (filter not-empty (clojure.string/split s #" \s| \p {Punct} ")))

(defn only-true-labels [labels]
  "This function assumes that the passed labels belongs to a document and get a membership value of 1.0."
  (zipmap labels (repeat 1.0)))

;; From the examples created data transfer vectors.
(def dtvs (create-dtv-vector examples
                             #(split-by-white-space-and-punctuation (first %1))
                             #(only-true-labels (second %1))
                             #(hash %1)))

(def additional-dtvs (create-dtv-vector additional-examples
                                        #(split-by-white-space-and-punctuation (first %1))
                                        #(only-true-labels (second %1))
                                        #(hash %1)))

;; From the examples initialized dictionaries
(def dictionaries (create-multi-label-category-dictionaries dtvs))

(def my-important-words {"dog"    #{"mammal" "snout" "smell" "carnivorous" "barking" "howling"}
                         "cat"    #{"mammal" "carnivorous"}
                         "feline" #{"mammal" "carnivorous"}
                         "animal" #{"ears" "snout" "claws" "mammal"}
                         })

(defn create-dtvs-from-definitions []
  (create-dtv-vector examples
                     #(split-by-white-space-and-punctuation (first %1))
                     #(only-true-labels (second %1))
                     #(hash %1)))