(ns user
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]

    [simple-check.generators :as g]
    [joda-time :as j]
    [joda-time.purgatory :as p]
    [joda-time.generators :as jg]

    [joda-time.duration-test :as dt]
    [joda-time.instant-test :as it]
    [joda-time.interval-test :as intt]
    [joda-time.period-test :as pet]
    [joda-time.partial-test :as pt]))
