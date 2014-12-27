(defproject clojure.joda-time "0.3.0"
  :description "Idiomatic Clojure wrapper for Joda-Time"
  :url "http://github.com/dm3/clojure.joda-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.joda-time"}
  :dependencies [[joda-time/joda-time "2.6"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/test.check "0.5.8"]
                                  [criterium "0.4.2"]]
                   :plugins [[codox "0.8.9"]]
                   :codox {:include [joda-time joda-time.purgatory]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7 {:repositories [["snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"]]
                   :dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}}
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.4,default:dev,1.5,default:dev,1.7,default" "test"]}
  :deploy-repositories [["releases" :clojars]])
