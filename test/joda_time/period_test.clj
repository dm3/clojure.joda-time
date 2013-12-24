(ns joda-time.period-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time :as j])
  (:import [org.joda.time Period]))

(defn- coerce-to-long-if-number [o]
  (if (number? o) (long o) o))

; TODO: fails with {:end #<Partial -366T00>, :start #<Partial -225T11>}
; https://github.com/JodaOrg/joda-time/issues/90

; very costly to run
(defspec constructs-period-from-a-map-of-parameters 10
  (prop/for-all [params (gen/such-that not-empty jg/period-construction-params)]
                (let [period (j/period params)
                      {:keys [start end duration type chronology]} params
                      start (coerce-to-long-if-number start)
                      end (coerce-to-long-if-number end)
                      duration (coerce-to-long-if-number duration)]
                  (cond (and start end)
                        (if (number? start)
                          (= period (Period. start end type chronology))
                          (= period (Period. start end type)))

                        (and start duration)
                        (= period (Period. (if (j/partial? start) start (j/instant start))
                                           (j/duration duration) type))

                        (and end duration)
                        (= period (Period. (j/duration duration)
                                           (if (j/partial? end) end (j/instant end)) type))

                        :else (= period (Period. (:years params 0) (:months params 0)
                                                 (:weeks params 0) (:days params 0)
                                                 (:hours params 0) (:minutes params 0)
                                                 (:seconds params 0) (:millis params 0)
                                                 (or type (apply j/period-type (keys params)))))))))

(defspec type-of-a-period-is-the-same-as-properties 100
  (prop/for-all [period-map (jg/period-map)
                 period-fn (gen/elements [j/period j/mutable-period])]
                (let [period (period-fn period-map)]
                  (= (set (keys period-map))
                     (-> period j/properties keys set)
                     (-> period j/period-type j/period-type->seq set)))))

(defspec period-reconstructed-from-property-values 100
  (prop/for-all [period (jg/period)]
                (= (j/period (j/as-map period)) period)))

(defspec period-abs-law 100
  (prop/for-all [period (jg/period)]
                (= (j/abs period) (j/abs (j/negate period)))))

(deftest period-construction-test
  (testing "Recognizes period"
    (is (j/period? (j/years 1)))
    (is (j/period? (j/period {:days 5, :minutes 6}))))

  (testing "Empty period construction returns nil"
    (is (nil? (j/period nil))))

  (testing "Specific periods extract their part from a larger period"
    (is (= (j/period {:years 5}) (j/years (j/period {:years 5, :months 6}))))
    (is (= (j/period {:months 6}) (j/months (j/period {:years 5, :months 6})))))

  (testing "Specific periods get the number of units if possible"
    (let [now (j/date-time)
          four-min-interval (j/interval now (j/plus now (j/minutes 2) (j/seconds 120)))]
      (is (= 4 (.getMinutes (j/minutes four-min-interval))))))

  (testing "Period can be constructed from a map"
    (is (= (j/period {:years 1, :months 1, :weeks 1})
           (Period. 1 1 1 0 0 0 0 0 (j/period-type :years :months :weeks))))
    (is (= (j/period {:start 0, :end (BigDecimal. 100)})
           (j/period {:start (BigDecimal. 0), :end (int 100)})
           (j/period {:start 0, :end (j/instant 100)})
           (j/period {:start (j/instant 0), :end (j/instant 100)})
           (j/period {:start 0, :duration (j/duration 100)})
           (j/period {:start (j/instant 0), :duration (j/duration 100)})
           (j/period {:end 100, :duration 100})
           (j/period {:end (j/instant 100), :duration (j/duration 100)})
           (j/period {:millis 100} j/standard-period-type)
           (j/period 100 j/standard-period-type)
           (j/period 100 (j/period-type->seq j/standard-period-type)))))

  (testing "Standard period type contains all duration types"
    (is (= (j/period-type->seq j/standard-period-type)
           [:years :months :weeks :days :hours :minutes :seconds :millis]))))

(deftest period-operations-test
  (testing "Sums period together"
    (is (= (j/plus (j/years 1) (j/months 2) (j/days 3) (j/days 5))
           (j/period {:years 1, :months 2, :days 8}))))

  (testing "Sums together periods and numbers"
    (is (= (j/plus (j/years 2) 1) (j/years 3)))
    (is (= (j/plus (j/days 1) 2 2) (j/days 5))))

  (testing "Fails to sum period of a non-single type and numbers"
    (is (thrown? clojure.lang.ExceptionInfo
                 (j/plus (j/period {:years 1 :months 1}) 1))))

  (testing "PeriodType of sum only includes duration types present in summed periods"
    (is (= (j/period-type (j/plus (j/years 1) (j/months 2)))
           (j/period-type :years :months))))

  (testing "Negates a period"
    (is (= (j/negate (j/period {:years 2, :months 3}))
           (j/period {:years -2, :months -3}))))

  (testing "Computes absolute value of a period"
    (is (= (j/abs (j/period {:years -2, :months 3}))
           (j/period {:years 2, :months 3}))))

  (testing "Negated period has the same type and class as the original one"
    (is (= (j/period-type (j/negate (j/period {:years 2, :seconds 10})))
           (j/period-type (j/period {:years 2, :seconds 10}))))
    (are [c] (= (type (c 1)) (type (c -1)))
         j/years j/months j/weeks j/days
         j/hours j/minutes j/seconds j/millis))

  (testing "Minus with a single argument negates, same as clojure.core/-"
    (is (= (j/negate (j/years 5))
           (j/minus (j/years 5)))))

  (testing "Computes difference of periods"
    (is (= (j/minus (j/period {:years 5, :months 5}) (j/months 3) (j/years 2))
           (j/period {:years 3, :months 2}))))

  (testing "Subtracts numbers from a period"
    (is (= (j/minus (j/millis 1000) 100 100)
           (j/millis 800))))

  (testing "Merge overwrites values contrary to plus/minus"
    (is (= (j/merge (j/years 1) (j/months 2) (j/days 3) (j/days 5))
           (j/period {:years 1, :months 2, :days 5}))))

  (testing "PeriodType of merge only includes duration types present in merged periods"
    (is (= (j/period-type (j/merge (j/years 1) (j/months 2)))
           (j/period-type :years :months)))))

(defspec map-representation-of-a-merge-same-as-merged-properties 100
  (prop/for-all [periods (gen/not-empty (gen/vector (jg/period)))]
                (= (j/as-map (apply j/merge periods))
                   (reduce #(merge %1 (j/as-map %2)) {} periods))))

(defn sum-into [m pm]
  (into m (for [[k v] pm] [k (+ v (k m 0))])))

(defspec period-plus-minus-law-and-commutativity 100
  (prop/for-all [periods (gen/not-empty (gen/vector (jg/period)))]
                (= (j/as-map (apply j/plus periods))
                   (j/as-map (apply j/plus (reverse periods)))
                   (j/as-map (c/seq-minus (first periods) (map j/negate (rest periods))))
                   (reduce #(sum-into %1 (j/as-map %2)) {} periods))))

