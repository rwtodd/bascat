(defproject bascat "1.1.0"
  :description "Converts tokenized GWBASIC/BASICA files to plain text"
  :url "https://github.com/rwtodd/Clojure.bascat"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :main ^:skip-aot org.rwtodd.bascat.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
