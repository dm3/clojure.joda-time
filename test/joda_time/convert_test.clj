(ns joda-time.convert-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time :as j])
  (:import [java.util Date]
           [java.sql Timestamp]))

(defspec joda-millis-equal-to-java-millis 100
  (prop/for-all [joda-date (gen/one-of [jg/any-instant jg/local-date jg/local-date-time])]
                (= (j/to-millis-from-epoch joda-date)
                   (.getTime (j/to-java-date joda-date)))))

(def millis-2013-12-10 1386626400000)

(deftest converts-between-dates
  (doseq [[java-date convert-fn]
          [[(java.util.Date. ^long millis-2013-12-10) j/to-java-date]
           [(java.sql.Date. ^long millis-2013-12-10) j/to-sql-date]
           [(java.sql.Timestamp. ^long millis-2013-12-10) j/to-sql-timestamp]
           [millis-2013-12-10 j/to-millis-from-epoch]]]
    (testing "To " (type java-date)
      (is (nil? (convert-fn nil)))
      (are [ctor] (= java-date (convert-fn (ctor millis-2013-12-10)))
           j/date-time
           j/instant
           j/mutable-date-time
           j/local-date
           j/local-date-time))))
