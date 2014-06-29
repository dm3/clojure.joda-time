(ns joda-time.partial-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time.impl :as impl]
            [joda-time :as j])
  (:import [java.util Date Calendar]
           [org.joda.time ReadablePartial DateTime Partial LocalDate LocalTime
            LocalDateTime YearMonth MonthDay DateTimeUtils]
           [org.joda.time.chrono GJChronology]))

;TODO: currently fails due to https://github.com/JodaOrg/joda-time/issues/96
#_(defspec partial-reconstructed-from-map-representation 100
  (prop/for-all [^ReadablePartial part jg/any-partial]
                (= (j/partial (assoc (j/as-map part)
                                     :chronology (.getChronology part)))
                   part)))

;TODO: currently fails due to https://github.com/JodaOrg/joda-time/issues/96
#_(defspec map-of-merged-partials-same-as-merged-maps 100
  (prop/for-all [partials (gen/not-empty (gen/vector jg/any-partial))]
                (try
                  (= (apply j/merge partials)
                     (j/partial (reduce #(merge %1 (j/as-map %2)) {} partials)))
                  ; Fails if merged partial is invalid, that's expected
                  (catch IllegalArgumentException e true))))

(deftest before-after-test
  (is (j/before? (j/local-date "2010") (j/local-date "2011") (j/local-date "2012")))
  (is (j/after? (j/local-date "2012") (j/local-date "2011") (j/local-date "2010"))))

(deftest partial-construction-test
  (testing "Returns nil for nil"
    (is (nil? (j/partial nil))))

  (testing "Empty partial from an empty map"
    (is (= (Partial.) (j/partial)))
    (is (= (Partial.) (j/partial {}))))

  (testing "Partial from a map"
    (is (= (j/partial {:year 2010, :monthOfYear 10, :dayOfMonth 2})
           (j/local-date "2010-10-02")))))

(def ^:private date-times-for-durations
  {:eras :era
   :centuries :centuryOfEra
   :years :year
   :months :monthOfYear
   :weeks :weekOfWeekyear
   :weekyears :weekyear
   :days :dayOfMonth
   :halfdays :halfdayOfDay
   :hours :hourOfDay
   :minutes :minuteOfHour
   :seconds :secondOfMinute
   :millis :millisOfSecond})

(defn partial-of-type [periods & {:keys [default]}]
  (if (empty? periods)
    (Partial.)
    (let [duration-type-names (keys (j/as-map (apply j/merge periods)))]
      (reduce #(.with %1 (@#'impl/date-time-field-types
                           (name (%2 date-times-for-durations))) default)
              (Partial.) duration-type-names))))

(defspec partial-plus-commutative 100
  (prop/for-all [periods (gen/vector (jg/period :min 1 :max 2) 4)]
                (let [part (partial-of-type periods :default 1)]
                  (= (c/seq-plus part periods)
                     (c/seq-plus part (reverse periods))))))

(defspec partial-plus-minus-law-holds 100
  (prop/for-all [periods (gen/vector (jg/period :min -2 :max -1) 4)]
                (let [part (partial-of-type periods :default 1)]
                  (= (c/seq-minus part periods)
                     (c/seq-plus part (map j/negate periods))))))

(defspec partial-properties-return-same-values-as-map-representation 100
  (prop/for-all [p jg/any-partial]
                (= (dissoc (j/as-map p) :chronology)
                   (into {} (for [[k v] (j/properties p)]
                              [k (j/value v)])))))

(defspec properties-is-the-same-as-single-property 100
  (prop/for-all [partial (jg/multi-field-partial)]
                (reduce #(and %1 (= (second %2) (j/property partial (first %2))))
                        (j/properties partial))))

(deftest partial-operations-test
  (testing "Sums partial and periods together"
    (is (= (j/plus (j/partial {:year 2010, :monthOfYear 5}) (j/years 5) (j/months 6))
           (j/partial {:year 2015, :monthOfYear 11}))))

  (testing "Doesn't add unsupported duration fields"
    (is (= (j/plus (j/local-date "2010-01-01") (j/hours 48))
           (j/local-date "2010-01-01"))

        (= (j/plus (j/partial {:year 2010, :monthOfYear 1, :dayOfMonth 1})
                   (j/hours 24))
           (j/partial {:year 2010, :monthOfYear 1, :dayOfMonth 1}))))

  (testing "Subtracts periods from a partial"
    (is (= (j/minus (j/partial {:year 2010, :monthOfYear 7}) (j/years 2) (j/months 6))
           (j/partial {:year 2008, :monthOfYear 1}))))

  (testing "Doesn't subtract unsupported duration fields"
    (is (= (j/minus (j/partial {:year 2010}) (j/months 5))
           (j/partial {:year 2010})))))

(def millis-1970-01-12 (* 1000 1000 1000))
(def millis-1970-01-12-164640 (* 1000 1000 1000))

(deftest test-specific-partial-construction
  (doseq [[ctor-fn partial-type java-ctor-fn]
          [[j/local-date LocalDate
            (fn [& chrono] (LocalDate. millis-1970-01-12 (first chrono)))]
           [j/local-time LocalTime
            (fn [& chrono] (LocalTime. millis-1970-01-12-164640 (first chrono)))]
           [j/local-date-time LocalDateTime
            (fn [& chrono] (LocalDateTime. millis-1970-01-12-164640 (first chrono)))]
           [j/month-day MonthDay
            (fn [& chrono] (MonthDay. millis-1970-01-12-164640 (first chrono)))]
           [j/year-month YearMonth
            (fn [& chrono] (YearMonth. millis-1970-01-12-164640 (first chrono)))]]]

    (testing "Partial is of the specified type"
      (is (= (type (ctor-fn)) partial-type)))

    (testing "Operations return the specified type"
      (is (= (type (j/plus (ctor-fn) (j/days 1) (j/minutes 1))) partial-type))
      (is (= (type (j/minus (ctor-fn) (j/days 1) (j/minutes 1))) partial-type)))

    (testing "Constructs a current partial"
      (try
        (DateTimeUtils/setCurrentMillisFixed millis-1970-01-12-164640)
        (is (= (ctor-fn) (ctor-fn millis-1970-01-12-164640)))
        (finally
          (DateTimeUtils/setCurrentMillisSystem))))

    (testing "Nil is always a nil - different from Joda-Time behaviour"
      (is (nil? (ctor-fn nil))))

    (testing "Int and long treated as millis"
      (let [millis millis-1970-01-12-164640]
        (are [d] (= (java-ctor-fn) d)
             (ctor-fn (int millis))
             (ctor-fn (long millis))
             (ctor-fn (BigDecimal. millis)))))

    (testing "Partials converted same as in Joda-Time"
      (are [d] (= (java-ctor-fn) d)
           (ctor-fn (LocalDateTime. millis-1970-01-12-164640))))

    (testing "Instants converted same as in Joda-Time"
      (are [d] (= (java-ctor-fn) d)
           (ctor-fn (DateTime. millis-1970-01-12-164640))))

    (testing "Sql and Util Dates converted same as in Joda-Time"
      (let [millis millis-1970-01-12-164640]
        (are [d] (= (java-ctor-fn) d)
             (ctor-fn (Date. millis))
             (ctor-fn (java.sql.Timestamp. millis))
             (ctor-fn (java.sql.Date. millis)))))

    (testing "Calendar converted with its own Chronology, as in Joda-Time"
      (let [cal (doto (Calendar/getInstance)
                  (.setTimeInMillis millis-1970-01-12-164640))
            chrono (GJChronology/getInstance)]
        (is (= (java-ctor-fn chrono) (ctor-fn cal)))))))

(deftest test-string-format-construction
  (testing "LocalDate from string"
    (let [d (LocalDate. millis-1970-01-12)]
      (is (= d (j/local-date (str d))))
      (is (= d (j/local-date "1970-01-12")))
      (are [s] (thrown? IllegalArgumentException (j/local-date s))
           "1970/01/12"
           "1/12/1970"
           "1970-01-12 22:30")))

  (testing "LocalDateTime from string"
    (let [d (LocalDateTime. 1970 1 12 16 46 40)]
      (is (= d (j/local-date-time (str d))))
      (is (= d (j/local-date-time "1970-01-12T16:46:40.000")))
      (are [s] (thrown? IllegalArgumentException (j/local-date-time s))
           "1970/01/12 16:46:40"
           "1/12/1970 16:46:40"
           "1970-01-12 22:30"))))
