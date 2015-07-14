(ns joda-time.accessors-test
  (:require [clojure.test :refer :all]
            [joda-time.accessors :as ja]
            [joda-time :as j]))

(deftest accesses-properties
  (doseq [dt [(j/date-time 2013 1 1)
              (j/local-date 2013 1 1)
              (j/year-month 2013 1)]]
    (is (= (j/property dt :year) (ja/year-prop dt)))
    (is (= (j/value (j/property dt :year)) (ja/year dt) 2013))
    (is (= (j/min-value (j/property dt :year)) (ja/min-year dt)))
    (is (= (j/max-value (j/property dt :year)) (ja/max-year dt)))
    (is (= (j/with-min-value (j/property dt :year)) (ja/with-min-year dt)))
    (is (= (j/with-max-value (j/property dt :year)) (ja/with-max-year dt)))
    (is (= (j/with-value (j/property dt :year) 2000) (ja/with-year dt 2000))))

  (let [p (j/period {:years 2, :millis 5})]
    (is (= (j/property p :years) (ja/years-prop p)))
    (is (= (j/value (j/property p :years)) (ja/years p) 2))
    (is (= (j/with-value (j/property p :years) 5) (ja/with-years p 5) (j/period {:years 5, :millis 5})))))
