(ns joda-time.instant-test
  (:require [clojure.test :refer :all]
            [simple-check.properties :as prop]
            [simple-check.generators :as gen]
            [simple-check.clojure-test :refer (defspec)]
            [joda-time.generators :as jg]
            [joda-time.core :as c]
            [joda-time :as j])
  (:import [org.joda.time DateTime Instant]))

(defspec date-time-reconstructed-from-map-representation 100
  (prop/for-all [^DateTime dt (jg/date-time)]
                (= (j/date-time (assoc (j/as-map dt) :chronology (.getChronology dt)))
                   dt)))

(defspec instant-reconstructed-from-map-representation 100
  (prop/for-all [^Instant inst jg/instant]
                (= (j/instant (assoc (j/as-map inst) :chronology (.getChronology inst)))
                   inst)))

(defspec date-time-plus-minus-law-holds 100
  (prop/for-all [periods-durations (gen/vector (gen/one-of [(jg/period) jg/duration]))
                 date-time (jg/date-time)]
                (= (c/seq-minus date-time periods-durations)
                   (c/seq-plus date-time (map j/negate periods-durations)))))

(deftest before-after-test
  (is (j/before? (j/date-time "2010") (j/date-time "2011") (j/date-time "2012")))
  (is (j/after? (j/date-time "2012") (j/date-time "2011") (j/date-time "2010"))))
