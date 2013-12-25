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

(def java-date (gen/fmap #(.toDate ^org.joda.time.DateTime %) (jg/date-time)))
(def sql-date (gen/fmap #(java.sql.Date. (.getTime ^Date %)) java-date))
(def sql-timestamp (gen/fmap #(java.sql.Timestamp. (.getTime ^Date %)) java-date))

(defspec convert-to-java-date-returns-same-millis 100
  (prop/for-all [date (gen/one-of [jg/instant (jg/date-time)
                                   jg/local-date jg/local-date-time
                                   jg/year-month java-date sql-date sql-timestamp
                                   jg/instant-number])]
                (= (j/to-millis-from-epoch date)
                   (.getTime (j/to-java-date date))
                   (.getTime (j/to-sql-date date))
                   (.getTime (j/to-sql-timestamp date)))))

(deftest converts-between-dates
  (testing "To java date from complete dates"
    ; Different timezones will produce different milliseconds
    (testing "in the default timezone"
      (let [^Date date (Date. ^long (j/to-millis-from-epoch "2013-12-10"))]
        (is (nil? (j/to-java-date nil)))
        (are [d] (= (j/to-java-date d) date)
             (j/to-millis-from-epoch "2013-12-10")
             date
             (java.sql.Date. (.getTime date))
             (java.sql.Timestamp. (.getTime date))
             (j/date-time (j/to-millis-from-epoch "2013-12-10"))
             (j/date-time "2013-12-10")
             (j/local-date "2013-12-10")
             (j/local-date-time "2013-12-10T00:00:00.000")))))

  (testing "To java date from partial dates"
    (is (= (j/to-java-date (j/date-time "1970-12-10"))
           (j/to-java-date (j/month-day "1970-12-10"))))
    (is (= (j/to-java-date (j/date-time "2013-12-01"))
           (j/to-java-date (j/year-month "2013-12")))))

  (testing "Conversion back and forth preserves timezone"
    (let [ld (j/local-date)]
      (is (= (j/local-date (j/to-java-date ld)) ld)))
    (let [ldt (j/local-date-time)]
      (is (= (j/local-date-time (j/to-java-date ldt)) ldt)))
    (let [dt (j/date-time)]
      (is (= (j/date-time (j/to-java-date dt)) dt)))))
