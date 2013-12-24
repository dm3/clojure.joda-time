(ns joda-time.property-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time :as j]))

(def thing-with-properties
  (gen/one-of [jg/any-partial (jg/date-time) jg/instant (jg/period)]))

(defspec value-of-property-between-min-and-max 100
  (prop/for-all [o thing-with-properties]
                (every? true?
                  (for [[k p] (j/properties o)]
                    (>= (j/max-value p) (j/value p) (j/min-value p))))))

(defspec properties-is-the-same-as-single-property 100
  (prop/for-all [o thing-with-properties]
                (reduce #(and %1 (= (second %2) (j/property o (first %2))))
                        (j/properties o))))

(defspec original-between-with-min-and-with-max 100
  (prop/for-all [o thing-with-properties]
                (every? true?
                  (for [[k p] (j/properties o)]
                    ; TODO: https://github.com/JodaOrg/joda-time/issues/102
                    (if-not (= k :centuryOfEra)
                      (>= (j/value (j/property (j/with-max-value p) k))
                          (j/value p)
                          (j/value (j/property (j/with-min-value p) k)))
                      true)))))

(defspec value-set-to-the-specified-property-value 100
  (prop/for-all [o thing-with-properties
                 value jg/number]
                (every? true?
                  (for [[k p] (j/properties o)]
                    (let [new-value (max (j/min-value p) (min value (j/max-value p)))]
                      ; TODO: https://github.com/JodaOrg/joda-time/issues/102
                      (if-not (= k :centuryOfEra)
                        (= (j/value (j/property (j/with-value p new-value) k))
                           (int new-value))
                        true))))))

(deftest test-property-values
  (testing "Gets value of a property"
    (is (= (-> (j/date-time "2010-03") (j/property :monthOfYear) j/value)
           (-> (j/date-time "2010-03") j/properties :monthOfYear j/value)
           3)))

  (testing "Gets maximum value of a property"
    (is (= (j/max-value (j/property (j/date-time "2010-03") :monthOfYear))
           12)))

  (testing "Gets minimum value of a property"
    (is (= (j/min-value (j/property (j/date-time "2010-03") :monthOfYear))
           1)))

  (testing "Date with a maximum value of a property"
    (is (= (j/with-max-value (j/property (j/date-time "2010-03") :monthOfYear))
           (j/date-time "2010-12"))))

  (testing "Date with a minimum value of a property"
    (is (= (j/with-min-value (j/property (j/date-time "2010-03") :monthOfYear))
           (j/date-time "2010-01"))))

  (testing "Date with a specified value of a property"
    (is (= (j/with-value (j/property (j/date-time "2010-03") :monthOfYear) 12)
           (j/date-time "2010-12")))))
