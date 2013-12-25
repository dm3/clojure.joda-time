(ns joda-time.interval-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time :as j]))

;;;;;; PartialInterval

(def ld-2005-01 (j/local-date "2005-01-01"))
(def ld-2010-01 (j/local-date "2010-01-01"))

(deftest partial-interval-construction-test
  (testing "Nil returns nil"
    (is (nil? (j/partial-interval nil))))

  (testing "Supports construction with the map parameter"
    (are [m] (= (j/partial-interval ld-2005-01 ld-2010-01) (j/partial-interval m))
         {:start ld-2005-01, :end ld-2010-01}
         {:start ld-2005-01, :period (j/years 5)}
         {:period (j/years 5), :end ld-2010-01}))

  (testing "Only allows partials of equal type"
    (is (thrown? IllegalArgumentException
                 (j/partial-interval (j/local-date "2010-01-01")
                                     (j/local-date-time "2010-01-01T15:40")))))

  (testing "Only allows contiguous partials"
    (is (thrown? IllegalArgumentException
                 (j/partial-interval (j/partial {:year 2010 :secondOfMinute 50})
                                     (j/partial {:year 2010 :secondOfMinute 50})))))

  (testing "Only allows valid intervals"
    (is (thrown? IllegalArgumentException
                 (j/partial-interval (j/local-date "2009")
                                     (j/local-date "2001"))))))

(defn- hour-minute [s]
  (let [t (j/local-time s)]
    (j/partial (select-keys (j/as-map t) [:hourOfDay :minuteOfHour]))))

(defn- pinterval [s e]
  (j/partial-interval (hour-minute s)
                      (hour-minute e)))

(deftest partial-interval-operations
  (testing "Contains partial"
    (is (j/contains? (pinterval "09:00" "10:00")
                     (hour-minute "09:00")))
    (is (not (j/contains? (pinterval "09:00" "10:00")
                          (hour-minute "10:00"))))
    (is (not (j/contains? (pinterval "09:00" "09:00")
                          (hour-minute "09:00")))))

  (testing "Moves the end by"
    (is (= (j/move-end-by (pinterval "09:00" "09:30") (j/minutes 30))
           (pinterval "09:00" "10:00"))))

  (testing "Moves the start by"
    (is (= (j/move-start-by (pinterval "09:00" "09:30") (j/minutes 30))
           (pinterval "09:30" "09:30"))))

  (testing "Moves the start to"
    (is (= (j/move-start-to (pinterval "09:00" "09:30") (hour-minute "09:20"))
           (pinterval "09:20" "09:30"))))

  (testing "Moves the end to"
    (is (= (j/move-end-to (pinterval "09:00" "09:30") (hour-minute "09:40"))
           (pinterval "09:00" "09:40"))))

  (testing "Contains interval"
    (are [a b]
         (j/contains? (pinterval (first a) (second a))
                      (pinterval (first b) (second b)))
         ["09:00" "10:00"] ["09:00" "10:00"]
         ["09:00" "10:00"] ["09:00" "09:30"]
         ["09:00" "10:00"] ["09:30" "10:00"]
         ["09:00" "10:00"] ["09:15" "09:45"]
         ["09:00" "10:00"] ["09:00" "09:00"])

    (are [a b]
         (not (j/contains? (pinterval (first a) (second a))
                           (pinterval (first b) (second b))))
         ["09:00" "10:00"] ["08:59" "10:00"]
         ["09:00" "10:00"] ["09:00" "10:01"]
         ["09:00" "10:00"] ["10:00" "10:00"]
         ["09:00" "09:00"] ["09:00" "09:00"]))

  (testing "Overlaps with interval"
    (are [a b]
         (j/overlaps? (pinterval (first a) (second a))
                      (pinterval (first b) (second b)))
         ["09:00" "10:00"] ["08:00" "09:30"]
         ["09:00" "10:00"] ["08:00" "10:00"]
         ["09:00" "10:00"] ["08:00" "11:00"]
         ["09:00" "10:00"] ["09:00" "09:30"]
         ["09:00" "10:00"] ["09:00" "10:00"]
         ["09:00" "10:00"] ["09:00" "11:00"]
         ["09:00" "10:00"] ["09:30" "09:30"]
         ["09:00" "10:00"] ["09:30" "10:00"]
         ["09:00" "10:00"] ["09:30" "11:00"]
         ["14:00" "14:00"] ["13:00" "15:00"])

    (are [a b]
         (not (j/overlaps? (pinterval (first a) (second a))
                           (pinterval (first b) (second b))))
         ["09:00" "10:00"] ["08:00" "08:30"]
         ["09:00" "10:00"] ["08:00" "09:00"]
         ["09:00" "10:00"] ["09:00" "09:00"]
         ["09:00" "10:00"] ["10:00" "10:00"]
         ["09:00" "10:00"] ["10:00" "11:00"]
         ["09:00" "10:00"] ["10:30" "11:00"]
         ["14:00" "14:00"] ["14:00" "14:00"]))

  (testing "Abuts with interval"
    (are [a b _ result]
         (= (j/abuts? (pinterval (first a) (second a))
                      (pinterval (first b) (second b))) result)
         ["09:00" "10:00"] ["08:00" "08:30"] -> false
         ["09:00" "10:00"] ["08:00" "09:00"] -> true
         ["09:00" "10:00"] ["08:00" "09:01"] -> false
         ["09:00" "10:00"] ["09:00" "09:00"] -> true
         ["09:00" "10:00"] ["09:00" "09:01"] -> false
         ["09:00" "10:00"] ["10:00" "10:00"] -> true
         ["09:00" "10:00"] ["10:00" "10:30"] -> true
         ["09:00" "10:00"] ["10:30" "11:00"] -> false
         ["14:00" "14:00"] ["14:00" "14:00"] -> true
         ["14:00" "14:00"] ["14:00" "15:00"] -> true
         ["14:00" "14:00"] ["13:00" "14:00"] -> true))

  (testing "Calculates an overlap"
    (are [a b _ r]
         (= (j/overlap (pinterval (first a) (second a))
                       (pinterval (first b) (second b)))
            (when r (pinterval (first r) (second r))))
         ["09:00" "10:00"] ["08:00" "09:30"] -> ["09:00" "09:30"]
         ["09:00" "10:00"] ["08:00" "10:00"] -> ["09:00" "10:00"]
         ["09:00" "10:00"] ["08:00" "11:00"] -> ["09:00" "10:00"]
         ["09:00" "10:00"] ["09:00" "09:30"] -> ["09:00" "09:30"]
         ["09:00" "10:00"] ["09:00" "10:00"] -> ["09:00" "10:00"]
         ["09:00" "10:00"] ["09:00" "11:00"] -> ["09:00" "10:00"]
         ["09:00" "10:00"] ["09:30" "09:30"] -> ["09:30" "09:30"]
         ["09:00" "10:00"] ["09:30" "10:00"] -> ["09:30" "10:00"]
         ["09:00" "10:00"] ["09:30" "11:00"] -> ["09:30" "10:00"]
         ["14:00" "14:00"] ["13:00" "15:00"] -> ["14:00" "14:00"]
         ["09:00" "10:00"] ["11:00" "12:00"] -> nil
         ["11:00" "12:00"] ["02:00" "03:00"] -> nil))

  (testing "Calculates a gap"
    (are [a b _ r]
         (= (j/gap (pinterval (first a) (second a))
                   (pinterval (first b) (second b)))
            (when r (pinterval (first r) (second r))))
         ["09:00" "10:00"] ["11:00" "12:30"] -> ["10:00" "11:00"]
         ["09:00" "10:00"] ["08:00" "08:30"] -> ["08:30" "09:00"]
         ["09:00" "10:00"] ["08:00" "10:00"] -> nil
         ["09:00" "10:00"] ["08:00" "11:00"] -> nil
         ["09:00" "10:00"] ["09:00" "10:30"] -> nil)))

;;;;;; Interval

(def d-2005-01 (j/date-time "2005-01-01"))
(def d-2015-02 (j/date-time "2015-02-01"))

(def d-2009-01 (j/date-time "2009-01-01"))

(def d-2010-01 (j/date-time "2010-01-01"))
(def d-2020-02 (j/date-time "2020-02-01"))

(deftest interval-construction-test
  (testing "Nil returns nil"
    (is (nil? (j/interval nil))))

  (testing "Mimics Joda-Time constructors through map with parameters"
    (are [m] (= (j/interval d-2005-01 d-2010-01) (j/interval m))
         ; Chronology is looked up from the start instant,
         ; so the following doesn't work:
         #_{:start (.getMillis d-2005-01) :end d-2010-01}
         #_{:start (.getMillis d-2005-01) :end (.getMillis d-2010-01)}

         {:start d-2005-01, :end d-2010-01}
         {:start d-2005-01, :end (.getMillis d-2010-01)}
         {:start d-2005-01, :period (j/years 5)}
         {:period (j/years 5), :end d-2010-01}

         {:start d-2005-01,
          :duration (j/duration {:start d-2005-01, :period (j/years 5)})}

         {:duration (j/duration {:start d-2005-01, :period (j/years 5)}),
          :end d-2010-01})))

(deftest interval-operations-test
  (testing "Moves start by a specified duration/period/number of millis"
    (is (= (j/move-start-by (j/interval d-2005-01 d-2015-02)
                            (j/years 4)
                            (j/duration {:start d-2009-01, :period (j/years 1)}))
           (j/interval d-2010-01 d-2015-02))))

  (testing "Moves end by a specified duration/period/number of millis"
    (is (= (j/move-end-by (j/interval d-2005-01 d-2015-02)
                          (j/years 4)
                          (j/duration {:start d-2009-01, :period (j/years 1)}))
           (j/interval d-2005-01 d-2020-02))))

  (testing "Moves start to the specified instant"
    (is (= (j/move-start-to (j/interval d-2005-01 d-2015-02) d-2010-01)
           (j/interval d-2010-01 d-2015-02))))

  (testing "Moves end to the specified instant"
    (is (= (j/move-end-to (j/interval d-2005-01 d-2010-01) d-2015-02)
           (j/interval d-2005-01 d-2015-02))))

  (testing "Interval before instant"
    (is (j/before? (j/interval d-2005-01 d-2010-01) d-2015-02)))

  (testing "Interval before other interval"
    (is (j/before? (j/interval d-2005-01 d-2010-01)
                   (j/interval d-2015-02 d-2020-02))))

  (testing "Interval after instant"
    (is (j/after? (j/interval d-2015-02 d-2020-02) d-2010-01)))

  (testing "Interval after other interval"
    (is (j/after? (j/interval d-2015-02 d-2020-02)
                  (j/interval d-2005-01 d-2010-01)))))

;;;;;;;;; Both

(defspec before-after-test 100
  (prop/for-all [i jg/any-interval]
                (and (j/before? i (j/end i))
                     (j/before? i (j/plus (j/end i) (j/years 1)))
                     (if-not (= (j/start i) (j/end i))
                       (not (j/before? i (j/start i))) true)
                     (j/after? i (j/minus (j/start i) (j/years 1)))
                     (not (j/after? i (j/end i)))
                     (not (j/after? i (j/start i))))))
