(ns joda-time.format-test
  (:require [clojure.test :refer :all]
            [joda-time :as j]
            [joda-time.format :as f]))

(deftest parses-date-entities
  (let [fmt (j/formatter "MM/dd/YYYY HH-mm-ss/SSS")
        iso-str "2010-10-01T10:20:30.500"
        fmt-str "10/01/2010 10-20-30/500"]
    (testing "date-times"
      (is (= (j/date-time iso-str) (j/parse-date-time fmt fmt-str)))
      (is (= (j/mutable-date-time iso-str) (j/parse-mutable-date-time fmt fmt-str))))
    (testing "locals"
      (is (= (j/local-date "2010-10-01") (j/parse-local-date fmt fmt-str)))
      (is (= (j/local-date-time iso-str) (j/parse-local-date-time fmt fmt-str)))))
  (testing "time"
    (let [fmt (j/formatter "HH-mm-ss/SSS")]
      (is (= (j/local-time "10:20:30.500") (j/parse-local-time fmt "10-20-30/500"))))))

(deftest creates-formatters
  (testing "from a pattern"
    (is (j/print (j/formatter "MM-YYYY") (j/date-time "2010-01")) "01-2010"))
  (testing "from an iso keyword"
    (is (j/print (j/formatter :basic-date) (j/date-time "2010-01")) "20100101"))
  (testing "from another formatter"
    (is (j/print (j/formatter (j/formatter "mm-YYYY")) (j/date-time "2010-01"))
        "01-2010"))
  (testing "from multiple arguments"
    (let [fmt (j/formatter :date)
          fmt-all (j/formatter "YYYY/MM/DD" :basic-date fmt)
          date (j/date-time "2010-01-10")]
      (is (= (j/print fmt-all date) "2010/01/10"))
      (is (= (j/parse-date-time fmt-all "2010/01/10") date))
      (is (= (j/parse-date-time fmt-all "20100110") date))
      (is (= (j/parse-date-time fmt-all "2010-01-10") date)))))

(deftest iso-formatters-present
  (is (= #{:basic-date
           :basic-date-time
           :basic-date-time-no-millis
           :basic-ordinal-date
           :basic-ordinal-date-time
           :basic-ordinal-date-time-no-millis
           :basic-time
           :basic-time-no-millis
           :basic-ttime
           :basic-ttime-no-millis
           :basic-week-date
           :basic-week-date-time
           :basic-week-date-time-no-millis
           :date
           :date-element-parser
           :date-hour
           :date-hour-minute
           :date-hour-minute-second
           :date-hour-minute-second-fraction
           :date-hour-minute-second-millis
           :date-optional-time-parser
           :date-parser
           :date-time
           :date-time-no-millis
           :date-time-parser
           :hour
           :hour-minute
           :hour-minute-second
           :hour-minute-second-fraction
           :hour-minute-second-millis
           :local-date-optional-time-parser
           :local-date-parser
           :local-time-parser
           :ordinal-date
           :ordinal-date-time
           :ordinal-date-time-no-millis
           :t-time
           :t-time-no-millis
           :time
           :time-element-parser
           :time-no-millis
           :time-parser
           :week-date
           :week-date-time
           :week-date-time-no-millis
           :weekyear
           :weekyear-week
           :weekyear-week-day
           :year
           :year-month
           :year-month-day} (set (keys @#'f/iso-formatters)))))
