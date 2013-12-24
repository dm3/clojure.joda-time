(ns joda-time.duration-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time :as j])
  (:import [org.joda.time Duration]))

(defspec duration-abs-law 100
  (prop/for-all [duration jg/duration]
                (= (j/abs duration) (j/abs (j/negate duration)))))

(defspec duration-plus-commutative 100
  (prop/for-all [first-duration jg/duration
                 durations (gen/vector (gen/one-of [jg/duration jg/number]))]
                (= (c/seq-plus first-duration durations)
                   (c/seq-plus first-duration (reverse durations))
                   (c/seq-minus first-duration (map j/negate durations)))))

(deftest duration-construction-test
  (testing "Nil results in empty duration"
    (is (not (j/duration nil)))
    (is (not (j/duration {}))))

  (testing "Constructs duration out of a number, string, interval or map"
    (is (= (j/duration 1000)
           (j/duration (j/interval 0 1000))
           (j/duration "PT1S")
           (j/duration {:start 0, :end (BigDecimal. 1000)})
           (j/duration {:start (int 0), :end 1000})
           (j/duration {:start (j/instant 0), :end (j/instant 1000)})
           (j/duration {:start (j/instant 0), :period (j/seconds 1)})
           (j/duration {:end (j/instant 0), :period (j/seconds 1)})
           (Duration. 1000))))

  (testing "Recognizes durations"
    (is (j/duration? (j/duration 1)))
    (is (not (j/duration? (j/interval 0 1000))))))

(deftest duration-operations-test
  (testing "Sums durations together"
    (is (j/plus (j/duration 1000) (j/duration 1000))
        (j/duration 2000)))

  (testing "Sums durations with numbers"
    (is (j/plus (j/duration 1000) (BigDecimal. 1000) 1000)
        (j/duration 3000)))

  (testing "Negates durations"
    (is (j/negate (j/duration 1000))
        (j/duration -1000)))

  (testing "Computes absolute value of the duration"
    (is (j/abs (j/duration -1000))
        (j/duration 1000))))
