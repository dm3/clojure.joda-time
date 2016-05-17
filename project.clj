(defproject clojure.joda-time "0.7.0-SNAPSHOT"
  :description "Idiomatic Clojure wrapper for Joda-Time"
  :url "http://github.com/dm3/clojure.joda-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.joda-time"}
  :dependencies [[joda-time/joda-time "2.9.3"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/test.check "0.9.0"]
                                  [criterium "0.4.4"]]
                   :plugins [[codox "0.8.13"]]
                   :codox {:include [joda-time joda-time.purgatory joda-time.accessors]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.7,default" "test"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]])
