(ns joda-time.core-test
  (:require [clojure.test :refer :all]
            [joda-time :as j])
  (:import [org.joda.time DateTimeZone Chronology]
           [org.joda.time.chrono GJChronology ISOChronology]))

(deftest min-max-test
  (testing "min returns the minimum"
    (is (= (j/min (j/date-time "2010") (j/date-time "2011"))
           (j/date-time "2010")))
    (is (= (j/min (j/local-date-time "2010") (j/local-date-time "2011"))
           (j/local-date-time "2010")))
    (is (= (j/years 10) (j/min (j/years 10) (j/years 20)))))

  (testing "max returns the maximum"
    (is (= (j/max (j/date-time "2010") (j/date-time "2011"))
           (j/date-time "2011")))
    (is (= (j/max (j/local-date-time "2010") (j/local-date-time "2011"))
           (j/local-date-time "2011")))
    (is (= (j/years 20) (j/max (j/years 10) (j/years 20))))))

(deftest timezone-construction-test
  (testing "Default"
    (is (= (DateTimeZone/getDefault) (j/timezone))))

  (testing "From another timezone"
    (let [tz (j/timezone :UTC)]
      (is (= tz (j/timezone tz)))))

  (testing "From another timezone"
    (let [tz (java.util.SimpleTimeZone. 0 "UTC")]
      (is (= (j/timezone :UTC) (j/timezone tz)))))

  (testing "From keyword"
    (is (instance? DateTimeZone (j/timezone :UTC))))
  (testing "From string"
    (is (instance? DateTimeZone (j/timezone "UTC"))))
  (testing "From symbol"
    (is (instance? DateTimeZone (j/timezone 'UTC))))

  (testing "Is case sensitive"
    (is (thrown? Exception (j/timezone :utc)))))

(deftest chronology-test
  (testing "Gets valid chronology"
    (is (instance? GJChronology (j/chronology :gj)))
    (is (instance? GJChronology (j/chronology :gj :UTC)))
    (is (instance? ISOChronology (j/chronology :iso))))

  (testing "Is case sensitive"
    (is (thrown? Exception (j/chronology :ISO)))))
