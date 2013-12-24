(ns joda-time.duration
  (:require [joda-time.impl :as impl]
            [joda-time.core :as c])
  (:import [org.joda.time Period Duration ReadableDuration
            ReadableInstant]
           [org.joda.time.base BasePeriod]))

(defn- ^BasePeriod to-base-period [p]
  (if (instance? BasePeriod p) p (Period. p)))

(defn- duration-from-map [{:keys [start end period] :as m}]
  (cond (and (and start end) (number? start) (number? end))
        (Duration. (long start) (long end))

        (and start end)
        (Duration. ^ReadableInstant (impl/to-instant-if-number start)
                   ^ReadableInstant (impl/to-instant-if-number end))


        (and start period)
        (.toDurationFrom (to-base-period period)
                         (impl/to-instant-if-number start))

        (and end period)
        (.toDurationTo (to-base-period period)
                       (impl/to-instant-if-number end))

        :else (throw (ex-info (str "Cannot construct duration from " m)
                              {:data m}))))

(defn ^Duration duration
  "Constructs a Duration out of a number, interval, string, other duration or a
  map containing:

    - start and end instants
    - start instant and a period
    - end instant and a period

    (duration 1000)
    => #<Duration PT1S>

    (duration (interval 0 1000))
    => #<Duration PT1S>

    (duration \"PT1S\")
    => #<Duration PT1S>

    (duration (duration 1000))
    => #<Duration PT1S>

    (duration {:start 0, :end 1000})
    => #<Duration PT1S>

    (duration {:start 0, :period (seconds 1)})
    => #<Duration PT1S>

    (duration {:end 0, :period (seconds 1)})
    => #<Duration PT1S>"
  [o] (if (nil? o) nil
        (cond (number? o) (Duration. (long o))
              (map? o) (if (empty? o) nil (duration-from-map o))
              :else (Duration. o))))

(extend-type Duration
  c/Plusable
  (seq-plus [d durations]
    (reduce #(if (number? %2)
               (.plus ^Duration %1 (long %2))
               (.plus ^Duration %1 ^ReadableDuration %2)) d durations))

  c/Minusable
  (seq-minus [d durations]
    (reduce #(if (number? %2)
               (.minus ^Duration %1 (long %2))
               (.minus ^Duration %1 ^ReadableDuration %2)) d durations))

  c/HasSign
  (negate [d]
    (Duration. (- (.getMillis d))))
  (abs [d]
    (Duration. (Math/abs (.getMillis d)))))
