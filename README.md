# Donatelo

## Installation

## Preliminary information
The code snippets used in this tutorial can be found in namespace ``donatelo.core-test``. The code snippets used in the rest of the tutorial use the data transport vectors as stored in ``dtvs`` and dictionaries as stored in ``dictionaries`` in the sample code below.

```clojure
(ns donatelo.core-test
  (:require [clojure.test :refer :all]
    [impp.donatelo.common :refer :all]
    [impp.donatelo.dictionary :refer :all]
    [impp.donatelo.donatelo :refer :all]
    [impp.donatelo.alpha :refer :all]
    ))

(defn split-by-white-space-and-punctuation [s]
  "This functions splits the passed string into word tokens which are separated by whitespaces or punctuations. Empty strings will be discarded."
  (filter not-empty (clojure.string/split s #"\s|\p{Punct}")))

(defn only-true-labels [labels]
  "This function assumes that the passed labels belongs to a document and get a membership value of 1.0."
  (zipmap labels (repeat 1.0)))

;;The text for these examples has been taken from the Oxford Language definitions provided via Google search on 06.20.2021.
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
  [["a heavily built omnivorous nocturnal mammal of the weasel family, typically having a grey and black coat."
    ["badger" "mammal" "animal" "weasel"]]
   ["a small, slender carnivorous mammal related to, but smaller than, the stoat."
    ["weasel" "mammal" "animal"]]
   ])

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
```

## Dynamic Token Vectors
The Dynamic Token Vector (DTV) is a special variation of a feature vector used in artificial neural networks. 
In our variation the DTV consists of three components, a collection of tokens (```documentTokens```) a map of labels 
(```labels```) and an optional ID (```id```). 

The ID (```id```) is optional, as it is not used in the training process, but as a convenience feature for easy management of the DTVs, and can consist of any data object that can uniquely assign the DTV.
Examples for this ID may be numbers, strings or even complex datastructures like lists, vectors, maps or objects.

The labels (```labels```) are encoded in a map, where the map key defines the label and the value defines a membership value. The membership value is a number between 0.0 and 1.0 that determines the membership of the DTV to the label. 0.0 means that the label does not belong to the DTV, 1.0 that the label belongs to this DTV, 0.5 that one does not know whether the label belongs to the DTV and all other numbers are representations about the "belief" that the label belongs to the DTV or not.
Membership values are actually used only to filter labels which have a value of 1.0, but in future iterations of this model it is planned to use membership values to adjust the weights during the learning phase.

The feature vector (```documentTokens```) is a generic collection (list or vector) with variable size. The content can consist of various data, including numbers, strings or more complex data structures such as lists, vectors, sets or maps.

```clojure
(->DynamicTokenVectorWithLabels 12 ["dog" "cat" ["canine" "dog"] #{"Felinae" #{"Leopardus" #{"Ocelot" "Oncilla"}} #{"Lynx" #{"Bobcat" "Canada lynx"}}}] {"animal" 1.0 "human" 0.0})
=>
#DynamicTokenVectorWithLabels{:id             12,
                              :documentTokens ["dog" "cat" ["canine" "dog"] #{"Felinae" #{"Leopardus" #{"Ocelot" "Oncilla"}} #{"Lynx" #{"Bobcat" "Canada lynx"}}}],
                              :labels         {"animal" 1.0, "human" 0.0}}
```
### Creation of DTVs
To simplify the creation of DTVs for large quantities, the ```create-dtv-vector``` function is available. This function expects as input the examples ```examples```, e.g. texts, a function that converts the examples into tokens ```token-fn```, a function which determines the labels and membershipvalues for the example ```label-fn``` and a function that determines the ID of the example ```id-fn```. Since the DTVs are created in parallel, the function allows the number of threads used for the process to be determined by ```:threads```.



The following example shows the created DTVs for two examples from the examples above. The tokens are created by splitting the text with punctuation and spaces, for the labels it is assumed that all the labels given belong to the examples and for simplicity the hash value is used as the ID.

```clojure
(create-dtv-vector (take 2 examples)
                   #(split-by-white-space-and-punctuation (first %1))
                   #(only-true-labels (second %1))
                   #(hash %1)
                   :threads 4)

=>
(#DynamicTokenVectorWithLabels{:id             -2043495102,
                               :documentTokens ("a" "domesticated" "carnivorous" "mammal"
                                                 "that" "typically" "has" "a" "long"
                                                 "snout" "an" "acute" "sense" "of" "smell"
                                                 "non" "retractable" "claws" "and" "a"
                                                 "barking" "howling" "or" "whining" "voice"),
                               :labels         {"dog" 1.0, "animal" 1.0, "mammal" 1.0, "canine" 1.0}}
  #DynamicTokenVectorWithLabels{:id             795265975,
                                :documentTokens ("a" "small" "domesticated" "carnivorous"
                                                  "mammal" "with" "soft" "fur" "a"
                                                  "short" "snout" "and" "retractable"
                                                  "claws" "is" "widely" "kept" "as"
                                                  "a" "pet" "or" "for" "catching"
                                                  "mice" "and" "many" "breeds"
                                                  "have" "been" "developed"),
                                :labels         {"cat" 1.0, "animal" 1.0, "feline" 1.0, "mammal" 1.0}})

```

## Dictionaries
There is a dictionary for each label. A dictionary contains the tokens that were found in the context of this label, as well as their weighting. All these dictionaries are then combined in a map in which the label is the key and the dictionary for the label is the value.
Below you will find the dictionary for the label "animal".  
```clojure
{"fins" 0.0625, "predatory" 0.0625, "nocturnal" 0.0625, "Africa" 0.0625, "ivory" 0.0625,
 "snout" 0.125, "are" 0.0625, "very" 0.0625, "stoat" 0.0625, "of" 0.125, "limbless" 0.0625,
 "kept" 0.0625, "smaller" 0.0625, "Asia" 0.0625, "chiefly" 0.0625, "grey" 0.0625, "coat" 0.0625,
 "toothlike" 0.0625, "slender" 0.0625, "water" 0.0625, "weasel" 0.0625, "long" 0.1875, "cold" 0.0625,
 "land" 0.0625, "tusks" 0.0625, "is" 0.125, "although" 0.0625, "smell" 0.0625, "skeleton" 0.0625,
 "native" 0.0625, "vertebrate" 0.0625, "related" 0.0625, "bodied" 0.0625, "than" 0.0625, "small" 0.125,
 "animal" 0.125, "for" 0.0625, "It" 0.125, "short" 0.0625, "plankton" 0.0625, "gills" 0.0625, "marine" 0.0625,
 "can" 0.0625, "scales" 0.0625, "that" 0.0625, "blooded" 0.0625, "curved" 0.0625, "prominent" 0.0625, 
 "an" 0.0625, "mammal" 0.3125, "or" 0.125, "largest" 0.125, "have" 0.0625, "a" 1.0, "mice" 0.0625, "many" 0.0625,
 "on" 0.0625, "but" 0.0625, "domesticated" 0.125, "cartilaginous" 0.0625, "catching" 0.0625, "carnivorous" 0.1875,
 "kinds" 0.0625, "Most" 0.0625, "whining" 0.0625, "having" 0.0625, "non" 0.0625, "and" 0.5625, "trunk" 0.0625,
 "black" 0.0625, "built" 0.0625, "family" 0.0625, "wholly" 0.0625, "pet" 0.0625, "acute" 0.0625, "claws" 0.125,
 "barking" 0.0625, "living" 0.125, "voice" 0.0625, "ears" 0.0625, "fin" 0.0625, "with" 0.25, "widely" 0.0625,
 "soft" 0.0625, "typically" 0.125, "some" 0.0625, "developed" 0.0625, "howling" 0.0625, "feed" 0.0625,
 "large" 0.1875, "size" 0.0625, "has" 0.0625, "sense" 0.0625, "eating" 0.0625, "to" 0.1875, "retractable" 0.125,
 "breeds" 0.0625, "as" 0.0625, "heavily" 0.0625, "southern" 0.0625, "prehensile" 0.0625, "grow" 0.0625,
 "the" 0.25, "dorsal" 0.0625, "fur" 0.0625, "plant" 0.0625, "been" 0.0625, "omnivorous" 0.0625, "sharks" 0.0625,
 "in" 0.0625, "fish" 0.0625},

```
The function ```create-multi-label-category-dictionaries``` is available to initialise dictionaries on the basis of DTVs.
To initialise the dictionary, the augmented term frequency (https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency) is calculated for each token.

The optional key ``:threads`` is used to specify the number of threads used for the creation process and ``:k`` is the smoothing factor for double normalisation.
```clojure
(create-multi-label-category-dictionaries dtvs)
=>
{"dog" {"snout" 0.3333333333333333, "of" 0.3333333333333333, "long" 0.3333333333333333,
        ...},
 "shark" {"predatory" 0.25, "chiefly" 0.25, "toothlike" 0.25,
          ...},
 "animal" {"fins" 0.0625, "nocturnal" 0.0625, "Africa" 0.0625,
           ...},
 "mammal" {"nocturnal" 0.09090909090909091, "Africa" 0.09090909090909091, "ivory" 0.09090909090909091,
           ...},
 "elephant" {"Africa" 0.5, "ivory" 0.5,"very" 0.5,
             ...},
 "cat" {"snout" 0.3333333333333333, "kept" 0.3333333333333333, "small" 0.3333333333333333,
        ...},
 "canine" {"snout" 0.3333333333333333, "of" 0.3333333333333333, "long" 0.3333333333333333,
           ...},
 "fish" {"fins" 0.2, "predatory" 0.2, "are" 0.2,
         ...},
 "feline" {"snout" 0.3333333333333333, "kept" 0.3333333333333333, "is" 0.3333333333333333,
           ...}}

```
### Dictionary analysis
The weight of the token in the dictionary also indicates the importance of the token in the context of the label. Thus, there is the possibility to give a reason for the result and also to estimate the influence of a token in general.

For simple overviews, the functions ```get-top-x-tokens-from``` and ```get-last-x-tokens-from``` are offered, which display the x tokens with the highest and lowest weight in the dictionaries.
```clojure
(get-top-x-tokens-from dictionaries 10)
=>
{"animal"   (["a" 1.0] ["and" 0.5625] ["mammal" 0.3125] ["with" 0.25] ["the" 0.25]
             ["long" 0.1875] ["carnivorous" 0.1875] ["large" 0.1875] ["to" 0.1875] ["snout" 0.125]),
 "canine"   (["a" 1.0] ["snout" 0.3333333333333333] ["of" 0.3333333333333333] ["long" 0.3333333333333333] ["smell" 0.3333333333333333]
             ["that" 0.3333333333333333] ["an" 0.3333333333333333] ["mammal" 0.3333333333333333] ["or" 0.3333333333333333] ["domesticated" 0.3333333333333333]),
 "cat"      (["a" 1.0] ["and" 0.6666666666666666] ["snout" 0.3333333333333333] ["kept" 0.3333333333333333] ["is" 0.3333333333333333]
             ["small" 0.3333333333333333] ["for" 0.3333333333333333] ["It" 0.3333333333333333] ["short" 0.3333333333333333] ["mammal" 0.3333333333333333]),
 "dog"      (["a" 1.0] ["snout" 0.3333333333333333] ["of" 0.3333333333333333] ["long" 0.3333333333333333] ["smell" 0.3333333333333333]
             ["that" 0.3333333333333333] ["an" 0.3333333333333333] ["mammal" 0.3333333333333333] ["or" 0.3333333333333333] ["domesticated" 0.3333333333333333]),
 "elephant" (["a" 1.0] ["and" 1.0] ["large" 1.0] ["Africa" 0.5] ["ivory" 0.5]
             ["very" 0.5] ["Asia" 0.5] ["long" 0.5] ["land" 0.5] ["tusks" 0.5]),
 "feline"   (["a" 1.0] ["and" 0.6666666666666666] ["snout" 0.3333333333333333] ["kept" 0.3333333333333333] ["is" 0.3333333333333333]
             ["small" 0.3333333333333333] ["for" 0.3333333333333333] ["It" 0.3333333333333333] ["short" 0.3333333333333333] ["mammal" 0.3333333333333333]),
 "fish"     (["a" 1.0] ["and" 0.6] ["with" 0.4] ["fins" 0.2] ["predatory" 0.2]
             ["are" 0.2] ["limbless" 0.2] ["chiefly" 0.2] ["toothlike" 0.2] ["water" 0.2]),
 "mammal"   (["a" 1.0] ["and" 0.5454545454545454] ["mammal" 0.45454545454545453] ["carnivorous" 0.2727272727272727] ["the" 0.2727272727272727]
             ["snout" 0.18181818181818182] ["of" 0.18181818181818182] ["long" 0.18181818181818182] ["is" 0.18181818181818182] ["small" 0.18181818181818182]),
 "shark"    (["a" 1.0] ["and" 0.5] ["predatory" 0.25] ["are" 0.25] ["chiefly" 0.25]
             ["toothlike" 0.25] ["long" 0.25] ["although" 0.25] ["skeleton" 0.25] ["bodied" 0.25])}


(get-last-x-tokens-from dictionaries 10)
=>
{"animal"   (["prehensile" 0.0625] ["grow" 0.0625] ["dorsal" 0.0625] ["fur" 0.0625] ["plant" 0.0625]
             ["been" 0.0625] ["omnivorous" 0.0625] ["sharks" 0.0625] ["in" 0.0625] ["fish" 0.0625]),
 "canine"   (["and" 0.3333333333333333] ["acute" 0.3333333333333333] ["claws" 0.3333333333333333] ["barking" 0.3333333333333333] ["voice" 0.3333333333333333]
             ["typically" 0.3333333333333333] ["howling" 0.3333333333333333] ["has" 0.3333333333333333] ["sense" 0.3333333333333333] ["retractable" 0.3333333333333333]),
 "cat"      (["claws" 0.3333333333333333] ["with" 0.3333333333333333] ["widely" 0.3333333333333333] ["soft" 0.3333333333333333] ["developed" 0.3333333333333333]
             ["retractable" 0.3333333333333333] ["breeds" 0.3333333333333333] ["as" 0.3333333333333333] ["fur" 0.3333333333333333] ["been" 0.3333333333333333]),
 "dog"      (["and" 0.3333333333333333] ["acute" 0.3333333333333333] ["claws" 0.3333333333333333] ["barking" 0.3333333333333333] ["voice" 0.3333333333333333]
             ["typically" 0.3333333333333333] ["howling" 0.3333333333333333] ["has" 0.3333333333333333] ["sense" 0.3333333333333333] ["retractable" 0.3333333333333333]),
 "elephant" (["trunk" 0.5] ["living" 0.5] ["ears" 0.5] ["with" 0.5] ["eating" 0.5]
             ["to" 0.5] ["southern" 0.5] ["prehensile" 0.5] ["the" 0.5] ["plant" 0.5]),
 "feline"   (["claws" 0.3333333333333333] ["with" 0.3333333333333333] ["widely" 0.3333333333333333] ["soft" 0.3333333333333333] ["developed" 0.3333333333333333]
             ["retractable" 0.3333333333333333] ["breeds" 0.3333333333333333] ["as" 0.3333333333333333] ["fur" 0.3333333333333333] ["been" 0.3333333333333333]),
 "fish"     (["feed" 0.2] ["large" 0.2] ["size" 0.2] ["to" 0.2] ["grow" 0.2]
             ["the" 0.2] ["dorsal" 0.2] ["sharks" 0.2] ["in" 0.2] ["fish" 0.2]),
 "mammal"   (["eating" 0.09090909090909091] ["breeds" 0.09090909090909091] ["as" 0.09090909090909091] ["heavily" 0.09090909090909091] ["southern" 0.09090909090909091]
             ["prehensile" 0.09090909090909091] ["fur" 0.09090909090909091] ["plant" 0.09090909090909091] ["been" 0.09090909090909091] ["omnivorous" 0.09090909090909091]),
 "shark"    (["some" 0.25] ["feed" 0.25] ["large" 0.25] ["size" 0.25] ["to" 0.25]
             ["grow" 0.25] ["the" 0.25] ["dorsal" 0.25] ["sharks" 0.25] ["fish" 0.25])}

```
## Prediction
The function ```predict``` performs the calculation which label(s) fits better to the passed feature vector. Besides the feature vector the function accepts the optional parameters ```:normalize?``` and ```:threads ```.
if ```:normalize?``` is ```true``` the calculated values will be scaled between 0.0 and 1.0 and ```:threads``` allows the user to define the number of threads which are used for the parallel calculation of the values.

The result of ```predict``` is a collection of vectors, where the first element is the label and the second the prediction value for the label and the passed feature vector. The elements of these collection are sorted in a descending order by the prediction value.

```clojure
(predict dictionaries (:documentTokens (first dtvs)))

=>
(["dog" 10.333333333333334]
 ["canine" 10.333333333333334]
 ["mammal" 6.727272727272725]
 ["cat" 6.0]
 ["feline" 6.0]
 ["animal" 5.8125]
 ["elephant" 5.0]
 ["fish" 3.8000000000000003]
 ["shark" 3.75])

(predict dictionaries (:documentTokens (first dtvs)) :normalize? true)

=>
(["dog" 0.15250702997031482]
 ["canine" 0.15250702997031482]
 ["mammal" 0.09928610162290286]
 ["cat" 0.0885524690150215]
 ["feline" 0.0885524690150215]
 ["animal" 0.08578520435830209]
 ["elephant" 0.07379372417918459]
 ["fish" 0.056083230376180286]
 ["shark" 0.05534529313438844])
```
To have an overview of labels that have the same prediction value, you can group them by prediction value.
```clojure
(group-by second (predict dictionaries (:documentTokens (first dtvs)) :normalize? true))

=>
{0.15250702997031482  [["dog" 0.15250702997031482] ["canine" 0.15250702997031482]],
 0.09928610162290286  [["mammal" 0.09928610162290286]],
 0.0885524690150215   [["cat" 0.0885524690150215] ["feline" 0.0885524690150215]],
 0.08578520435830209  [["animal" 0.08578520435830209]],
 0.07379372417918459  [["elephant" 0.07379372417918459]],
 0.056083230376180286 [["fish" 0.056083230376180286]],
 0.05534529313438844  [["shark" 0.05534529313438844]]}
```

## Training
The training is carried out using the ``train`` function.  The training modifies the passed dictionaries ``dictionaries`` based on the passed DTVs of the sample ``examples``. In addition to these parameters, one can optionally specify whether the examples should be shuffled on each run ``shuffle?``, the maximum number of epochs to run ``epochs``, the maximum difference in accuracy at which the training can be terminated ``epsilon``,how many threads should be used for parallel processing ``threads``, the value for the learning parameter alpha ``alpha`` and the initialisation parameter beat ``beta`` as well as a function that can adjust the learning parameter alpha depending on the situation ``alpha-adjust-fn``.
The result of the training and returned values are adjusted ``dictionaries``. 

```clojure
(def result (train dictionaries dtvs))
20.07.2021 15:01:25,071 INFO  donatelo:326 - Start learning with 5 examples, threads 6, shuffle?: false, alpha 0.5, alpha adjustment function:alpha$standard_alpha_adjustment, beta 0.3, epsilon 0.001 and epochs 8
20.07.2021 15:01:25,072 INFO  donatelo:326 - Start learning epoch 1 of 8 epochs.
20.07.2021 15:01:25,088 INFO  donatelo:326 - Accuracy: 0.9
20.07.2021 15:01:25,088 INFO  donatelo:326 - Diff: 0.9
20.07.2021 15:01:25,089 INFO  donatelo:326 - Accuracy-history: (0 0.9)
20.07.2021 15:01:25,090 INFO  donatelo:326 - Finished learning epoch 1 after 18  milliseconds.
20.07.2021 15:01:25,091 INFO  donatelo:326 - Start learning epoch 2 of 8 epochs.
20.07.2021 15:01:25,113 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:01:25,114 INFO  donatelo:326 - Diff: 0.09999999999999998
20.07.2021 15:01:25,114 INFO  donatelo:326 - Accuracy-history: (0 0.9 1.0)
20.07.2021 15:01:25,115 INFO  donatelo:326 - Finished learning epoch 2 after 24  milliseconds.
20.07.2021 15:01:25,115 INFO  donatelo:326 - Start learning epoch 3 of 8 epochs.
20.07.2021 15:01:25,132 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:01:25,133 INFO  donatelo:326 - Diff: 0.0
20.07.2021 15:01:25,133 INFO  donatelo:326 - Accuracy-history: (0 0.9 1.0 1.0)
20.07.2021 15:01:25,134 INFO  donatelo:326 - Finished learning epoch 3 after 18  milliseconds.
=> #'donatelo.core-test/result

(def result (train dictionaries dtvs :alpha 0.5 :beta 0.3 :shuffle? true :epochs 10 :epsilon 0.01 :threads 2))

20.07.2021 15:02:04,789 INFO  donatelo:326 - Start learning with 5 examples, threads 2, shuffle?: true, alpha 0.5, alpha adjustment function:alpha$standard_alpha_adjustment, beta 0.3, epsilon 0.01 and epochs 10
20.07.2021 15:02:04,791 INFO  donatelo:326 - Start learning epoch 1 of 10 epochs.
20.07.2021 15:02:04,805 INFO  donatelo:326 - Accuracy: 0.8666666666666667
20.07.2021 15:02:04,805 INFO  donatelo:326 - Diff: 0.8666666666666667
20.07.2021 15:02:04,805 INFO  donatelo:326 - Accuracy-history: (0 0.8666666666666667)
20.07.2021 15:02:04,806 INFO  donatelo:326 - Finished learning epoch 1 after 15  milliseconds.
20.07.2021 15:02:04,806 INFO  donatelo:326 - Start learning epoch 2 of 10 epochs.
20.07.2021 15:02:04,816 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:02:04,817 INFO  donatelo:326 - Diff: 0.1333333333333333
20.07.2021 15:02:04,817 INFO  donatelo:326 - Accuracy-history: (0 0.8666666666666667 1.0)
20.07.2021 15:02:04,817 INFO  donatelo:326 - Finished learning epoch 2 after 11  milliseconds.
20.07.2021 15:02:04,818 INFO  donatelo:326 - Start learning epoch 3 of 10 epochs.
20.07.2021 15:02:04,826 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:02:04,827 INFO  donatelo:326 - Diff: 0.0
20.07.2021 15:02:04,827 INFO  donatelo:326 - Accuracy-history: (0 0.8666666666666667 1.0 1.0)
20.07.2021 15:02:04,827 INFO  donatelo:326 - Finished learning epoch 3 after 9  milliseconds.
=> #'donatelo.core-test/result
```

Please note that the original dictionaries are not changed. The dictionaries returned by the ``train`` function are modified copies of the originals.

```clojure
(= (get result "dog") (get dictionaries "dog"))
=> false
```

### Alpha adjustment function
By default, the update rate parameter alpha is used unchanged to adjust the weights in the dictionary. However, the function ``train`` can still be passed a function that adjusts the alpha depending on the label and the current token ``alpha-adjustment-fn``. This opens up the possibility, for example, of tapping "knowledge" from other sources for the training.
An example for the use of this possibility is the adjustment of the alpha value depending on whether a token is specified by another knowledge source as "important" for the label.
The function ``impp.donatelo.alpha/create-new-alpha-adjustment-fn``, for example, creates such a function that modifies the alpha based on the two multipliers, ``pos-multiplier`` if the prediction was correct and ``neg-multiplier`` if the prediction was wrong, if the current token is in the set of "important" tokens for that label.

In the example below, the alpha value of an "important" token is multiplied by 1.25 in the case of a correct prediction, while it remains unchanged in the case of an incorrect prediction.

```clojure
(def my-important-words {"dog"    #{"mammal" "snout" "smell" "carnivorous" "barking" "howling"}
                         "cat"    #{"mammal" "carnivorous"}
                         "feline" #{"mammal" "carnivorous"}
                         "animal" #{"ears" "snout" "claws" "mammal"}
                         })

(def result (train dictionaries dtvs :alpha-adjust-fn (create-new-alpha-adjustment-fn my-important-words 1.25 1.0)))

20.07.2021 15:02:28,021 INFO  donatelo:326 - Start learning with 5 examples, threads 6, shuffle?: false, alpha 0.5, alpha adjustment function:{:pos-multiplier 1.25, :neg-multiplier 1.0, :source "PersistentArrayMap"}, beta 0.3, epsilon 0.001 and epochs 8
20.07.2021 15:02:28,022 INFO  donatelo:326 - Start learning epoch 1 of 8 epochs.
20.07.2021 15:02:28,038 INFO  donatelo:326 - Accuracy: 0.9
20.07.2021 15:02:28,039 INFO  donatelo:326 - Diff: 0.9
20.07.2021 15:02:28,039 INFO  donatelo:326 - Accuracy-history: (0 0.9)
20.07.2021 15:02:28,040 INFO  donatelo:326 - Finished learning epoch 1 after 19  milliseconds.
20.07.2021 15:02:28,040 INFO  donatelo:326 - Start learning epoch 2 of 8 epochs.
20.07.2021 15:02:28,054 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:02:28,054 INFO  donatelo:326 - Diff: 0.09999999999999998
20.07.2021 15:02:28,055 INFO  donatelo:326 - Accuracy-history: (0 0.9 1.0)
20.07.2021 15:02:28,055 INFO  donatelo:326 - Finished learning epoch 2 after 15  milliseconds.
20.07.2021 15:02:28,056 INFO  donatelo:326 - Start learning epoch 3 of 8 epochs.
20.07.2021 15:02:28,070 INFO  donatelo:326 - Accuracy: 1.0
20.07.2021 15:02:28,071 INFO  donatelo:326 - Diff: 0.0
20.07.2021 15:02:28,072 INFO  donatelo:326 - Accuracy-history: (0 0.9 1.0 1.0)
20.07.2021 15:02:28,072 INFO  donatelo:326 - Finished learning epoch 3 after 16  milliseconds.
=> #'donatelo.core-test/result

```

### Online learning
Dictionaries can also be adapted for individual examples. For this purpose, the function ``single-train`` is available, which expects the dictionaries and the DTV for the example as input. Analogous to ``train``, the learning parameter alpha ``alpha`` and its adjustment function ``alpha-adjust-fn``, initialisation parameter beta ``beta`` and the number of threads used ``:threads`` can be passed. Further information, such as the accuracy, can be found in the metadata.
```clojure
(meta (single-train dictionaries (rand-nth dtvs)))

=> {:accuracy 2/3}
```
### New examples
If the training contains examples with labels that are not yet known, the training is continued using these examples and the new labels are added to the dictionaries.

```clojure
(def additional-examples
  [["a heavily built omnivorous nocturnal mammal of the weasel family, typically having a grey and black coat."
    ["badger" "mammal" "animal" "weasel"]]
   ["a small, slender carnivorous mammal related to, but smaller than, the stoat."
    ["weasel" "mammal" "animal"]]
   ])

(def additional-dtvs (create-dtv-vector additional-examples
                                        #(split-by-white-space-and-punctuation (first %1))
                                        #(only-true-labels (second %1))
                                        #(hash %1)))

;; Labels in the initialized dictionaries.
(keys dictionaries )
=> ("dog" "shark" "animal" "mammal" "elephant" "cat" "canine" "fish" "feline")

;; Labels in the dictionaries which has been updated with the same examples which are used for the initialization.
(keys (train dictionaries dtvs))
=> ("dog" "shark" "animal" "mammal" "elephant" "cat" "canine" "fish" "feline")

;; Labels in the dictionaries which has been updated with the same examples which are used for the initialization and new unknown examples.
(keys (train dictionaries (concat dtvs additional-dtvs)))
=> ("dog" "badger" "weasel" "shark" "animal" "mammal" "elephant" "cat" "canine" "fish" "feline")
```
## TODOs
- [ ] Using membership values in learning process

## License
Copyright (C) 2021 Institut für medizinische und pharmazeutische Prüfungsfragen.
Licensed under the [GNU General Public License 3](https://www.gnu.org/licenses/gpl-3.0.de.html).

Please quote as: Núñez, A.; Lindner, M. (2022): Donatelo, https://github.com/IMPP-Mainz/donatelo, DOI: <DOI-Nummer>
