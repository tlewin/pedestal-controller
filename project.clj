(defproject pedestal-controller "0.1.0-SNAPSHOT"
  :description "Simple controller mechanism for Pedestal applications"
  :url "https://github.com/tlewin/pedestal-controller"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :aliases {"test-all" ["with-profile" "base:+1.9:+1.10" "test"]}
  :profiles {:dev  {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}})
