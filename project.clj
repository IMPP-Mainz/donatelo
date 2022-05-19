(defproject impp-mainz/donatelo "0.1"
  :description "FIXME: write description"
  :url "https://github.com/IMPP-Mainz/vdnntl"
  :license {:name "GPL-3.0"
            :url  "https://choosealicense.com/licenses/gpl-3.0"
            :comment "GNU General Public License v3.0"
            :year 2021
            :key "gpl-3.0"}
  :copyright "Copyright (C) 2021 Institut für medizinische und pharmazeutische Prüfungsfragen."
  :authors [{:name   "Alexandra Núñez"
             :github "AlexaKekin"
             :email  ["anunez@impp.de" "anunyes@gmail.com"]}
            {:name   "Marcus Lindner"
             :github "Goldritter"
             :email  ["mlindner@impp.de" "marcus.goldritter.lindner@gmail.com"]}]
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.climate/claypoole "1.1.4"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.apache.logging.log4j/log4j-core "2.13.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.13.0"]
                 ]
  :plugins [[lein-licenses "0.2.2"]
            [lein-license "1.0.0"]
            ]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
