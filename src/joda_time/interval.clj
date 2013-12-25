(ns joda-time.interval
  (:refer-clojure :exclude [contains?])
  (:require [clojure.string :as string]
            [joda-time.core :as c]
            [joda-time.impl :as impl])
  (:import [org.joda.time ReadablePeriod ReadableDuration
            ReadableInstant Interval MutableInterval
            ReadableInterval

            DateTimeUtils ReadablePartial DateTimeFieldType]
           [org.joda.time.base AbstractPartial]))

(defprotocol ^:private AnyInterval
  (seq-move-start-by [i os]
                     "Prefer `move-start-by` with vararags.")
  (seq-move-end-by [i os]
                   "Prefer `move-end-by` with vararags.")
  (move-start-to [i new-start]
   "Moves the start instant of the interval to the given instant.

   (move-start-to (interval 0 10000) (instant 5000))
   => #<Interval 05.000/10.000>

  Fails if the new start instant falls after the end instant.

   (move-start-to (interval 0 10000) 15000)
   => IllegalArgumentException...")
  (move-end-to [i new-end]
   "Moves the end of the interval to the given instant/partial.

   (move-end-to (interval 0 10000) (instant 15000))
   => #<Interval 00.000/15.000>

  Fails if the new end instant/partial falls before the start instant.

   (move-end-to (interval 0 10000) -1)
   => IllegalArgumentException...")
  (start [i] "Gets the start of the interval.")
  (end [i] "Gets the end of the interval.")
  (contains? [i o] "True if the interval contains the given
                   instant/partial/interval.")
  (overlaps? [i oi] "True if this interval overlaps the other one.")
  (abuts? [i oi] "True if this interval abut with the other one.")
  (overlap [i oi] "Gets the overlap between this interval and the other one.")
  (gap [i oi] "Gets the gap between this interval and the other one."))

;;;;;;;;; PartialInterval

(declare partial-interval)

; The fact that PartialInterval is a record lets us pass it to time entity
; construction functions which expect a map with :start/:end keys. This is an
; unfortunate implementation detail. Interoperability should be achieved by
; adding custom converters through the ConverterManager.
;
; Please do not rely on the PartialInterval being a record.
(defrecord ^:private PartialInterval [^AbstractPartial start ^AbstractPartial end]
  AnyInterval
  (seq-move-start-by [_ os]
    (let [^ReadablePartial s (c/seq-plus start os)]
      (partial-interval s end)))
  (seq-move-end-by [_ os]
    (let [^ReadablePartial e (c/seq-plus end os)]
      (partial-interval start e)))
  (move-start-to [_ new-start]
    (partial-interval new-start end))
  (move-end-to [_ new-end]
    (partial-interval start new-end))
  (start [_] start)
  (end [_] end)
  (contains? [_ o]
    (if (c/partial? o)
      (and (.isAfter end o) (or (.isBefore start o) (.isEqual start o)))
      (and (.isAfter end (:start o))
           (or (.isAfter end (:end o))
               (.isEqual end (:end o)))
           (or (.isBefore start (:start o))
               (.isEqual start (:start o))))))
  (overlaps? [_ i]
    (and (.isBefore start (:end i))
         (.isAfter end (:start i))))
  (abuts? [_ i]
    (or (.isEqual start (:end i))
        (.isEqual end (:start i))))
  (overlap [self i]
    (when (overlaps? self i)
      (partial-interval (c/max start (:start i))
                        (c/min end (:end i)))))
  (gap [_ i]
    (cond (.isAfter start (:end i))
          (partial-interval (:end i) start)

          (.isBefore end (:start i))
          (partial-interval end (:start i))))

  c/Ordered
  (single-before? [self o]
    (if (c/partial? o)
      (or (.isBefore end o) (.isEqual end o))
      (c/single-before? self (start o))))
  (single-after? [_ o]
    (if (c/partial? o)
      (.isAfter start o)
      (or (.isEqual end (:start o))
          (.isAfter start (:end o))))))

(ns-unmap *ns* '->PartialInterval)
(ns-unmap *ns* 'map->PartialInterval)

(defn partial-interval?
  "True if the given object is an instance of `PartialInterval`."
  [x] (instance? PartialInterval x))

(defn- partial-type [^AbstractPartial p]
  ; list* needed to avoid LazySeq toString value
  (list* (map #(.getName ^DateTimeFieldType %) (seq (.getFieldTypes p)))))

(defn- illegal [& msgs]
  (throw (IllegalArgumentException. ^String (string/join msgs))))

(defn- partial-interval-from-map [{:keys [start end period]}]
  (cond (and start end)
        (PartialInterval. start end)

        (and period end)
        (seq-move-start-by (PartialInterval. end end) [(c/negate period)])

        (and start period)
        (seq-move-end-by (PartialInterval. start start) [period])))

(defn partial-interval
  "Constructs an interval from two partials or a map containing:

  * start and end keys
  * start and a period
  * end and a period

  Partials must have equal type (contain an equal set of
  DateTimeFieldTypes) and be contiguous:

    (partial-interval (local-date \"2010-01-01\")
                      (local-date \"2011-02-01\"))

  Intervals are inclusive of the start partial and exclusive of the end
  partial."
  ([o] (when-not (nil? o) (partial-interval-from-map o)))
  ([^AbstractPartial start ^AbstractPartial end]
   (if-not (= (seq (.getFieldTypes start)) (seq (.getFieldTypes end)))
     (illegal "Partial types must be equal: "
              (partial-type start) " and " (partial-type end))
     (if-not (DateTimeUtils/isContiguous start)
       (illegal "Partials must be contiguous: " (partial-type start))
       (if (.isAfter start end)
         (illegal "Partial " end " must follow or be the same as " start)
         (PartialInterval. start end))))))

;;;;;;;;;;;;; ReadableInterval

(defn- interval-from-map [{:keys [start end period duration]}]
  (let [start (impl/to-instant-if-number start)
        end (impl/to-instant-if-number end)]
    (cond (and start end)
          [start end]

          (and end duration)
          [duration end]

          (and start duration)
          [start duration]

          (and end period)
          [period end]

          (and start period)
          [start period])))

(defn mk-interval
  ([x y]
   (cond (and (c/instant? x) (c/instant? y))
         (Interval. ^ReadableInstant x ^ReadableInstant y)

         (c/duration? x)
         (Interval. ^ReadableDuration x ^ReadableInstant y)

         (c/duration? y)
         (Interval. ^ReadableInstant x ^ReadableDuration y)

         (c/period? x)
         (Interval. ^ReadablePeriod x ^ReadableInstant y)

         (c/period? y)
         (Interval. ^ReadableInstant x ^ReadablePeriod y))))

(defn ^Interval interval
  "Constructs an interval out of another interval, a string,
  start and end instants/date-times or a map with the
  following keys (where start/end may be instants, date-times
  or number of milliseconds):

  * start/end
  * start/period
  * period/end
  * start/duration
  * duration/end

    (j/interval \"2010/2013\")
    => #<Interval 2010-01-01T00:00:00.000+02:00/2013-01-01T00:00:00.000+02:00>

    (j/interval (j/date-time \"2010\") (j/date-time \"2013\"))
    => #<Interval 2010-01-01T00:00:00.000+02:00/2013-01-01T00:00:00.000+02:00>

    (j/interval {:start (j/date-time \"2010\"), :end (j/date-time \"2013\")})
    => #<Interval 2010-01-01T00:00:00.000+02:00/2013-01-01T00:00:00.000+02:00>

    (j/interval {:start (j/date-time \"2010\"), :period (j/years 3)})
    => #<Interval 2010-01-01T00:00:00.000+02:00/2013-01-01T00:00:00.000+02:00>"
  ([o]
   (cond (nil? o) nil
         (map? o) (apply mk-interval (interval-from-map o))
         :else (Interval. o)))
  ([start end] (mk-interval (impl/to-instant-if-number start)
                            (impl/to-instant-if-number end))))

(defn- to-millis-if-instant [o]
  (if (c/instant? o)
    (.getMillis ^ReadableInstant o)
    (long o)))

(defn- with-start [^Interval i ^long s]
  (.withStartMillis i s))

(defn- with-end [^Interval i ^long e]
  (.withEndMillis i e))

(extend-type Interval
  AnyInterval
  (seq-move-start-by [i os]
    (let [^ReadableInstant s (c/seq-plus (.getStart i) os)]
      (with-start i (.getMillis s))))
  (seq-move-end-by [i os]
    (let [^ReadableInstant e (c/seq-plus (.getEnd i) os)]
      (with-end i (.getMillis e))))
  (move-start-to [i new-start]
    (with-start i (to-millis-if-instant new-start)))
  (move-end-to [i new-end]
    (with-end i (to-millis-if-instant new-end)))
  (start [i] (.getStart i))
  (end [i] (.getEnd i))
  (contains? [i o] (if (c/instant? o)
                     (.contains i ^ReadableInstant o)
                     (.contains i ^ReadableInterval o)))
  (overlaps? [i oi] (.overlaps i oi))
  (abuts? [i oi] (.abuts i oi))
  (overlap [i oi] (.overlap i oi))
  (gap [i oi] (.gap i oi))

  c/Ordered
  (single-before? [i o] (if (c/instant? o)
                   (.isBefore i ^ReadableInstant o)
                   (.isBefore i ^ReadableInterval o)))
  (single-after? [i o] (if (c/instant? o)
                   (.isAfter i ^ReadableInstant o)
                   (.isAfter i ^ReadableInterval o))))

(defn move-start-by
  "Moves the start instant of the interval by the sum of given
  periods/durations/numbers of milliseconds:

    (move-start-by (interval 0 10000) 3000 (duration 1000) (seconds 1))
    => #<Interval 05.000/10.000>

  Accepts negative values:

    (move-start-by (interval 0 10000) -5000)
    => #<Interval 00.000/10.000>

  Fails if the new start instant falls after the end instant.

    (move-start-by (interval 0 10000) 11000)
    ; => IllegalArgumentException..."
  [i & os] (seq-move-start-by i os))

(defn move-end-by
  "Moves the end instant of the interval by the sum of given
  periods/durations/numbers of milliseconds.

    (move-end-by (interval 0 10000) 3000 (duration 1000) (seconds 1))
    => #<Interval 00.000/15.000>

  Accepts negative values:

    (move-end-by (interval 0 10000) -5000)
    => #<Interval 00.000/05.000>

  Fails if the new end instant falls before the start instant.

    (move-end-by (interval 0 10000) -11000)
    => IllegalArgumentException..."
  [i & os] (seq-move-end-by i os))

; Move-by is nice, but has a problem when adding periods. Periods added to the
; start of the interval might have a different duration from the ones added to
; the end of the interval. We either need to calculate the move duration from
; the start first and add it to the end (move-by-from-start), or vice versa
; (move-by-from-end).
;
; On the other hand, we may just disallow moving by periods (which will be
; inconsistent with the other move-'s).
;
;(defn ^ReadableInterval move-by
;  "Moves the interval (both start and end instants) by the sum of provided
;  periods/durations/numbers of milliseconds from the start:
;
;    (move-by (interval 0 10000) 3000 (seconds 1) (duration 1000))
;    ; => #<Interval 05.000/15.000>
;
;  Accepts negative values:
;
;    (move-by (interval 5000 10000) -2000)
;    ; => #<Interval 03.000/08.000>"
;  [^ReadableInterval i & os]
;  (let [^ReadableInstant s (seq-plus (.getStart i) os)
;        ^ReadableInstant e (seq-plus (.getEnd i) os)]
;    (if (instance? Interval i)
;      (Interval. s e)
;      (do (.setStart ^MutableInterval i s)
;          (.setEnd ^MutableInterval i e)))))

