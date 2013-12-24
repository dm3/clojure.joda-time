(ns joda-time.core
  (:refer-clojure :exclude [merge max min])
  (:require [clojure.string :as string])
  (:import [org.joda.time ReadablePeriod ReadableDuration
            ReadablePartial ReadableInterval ReadableInstant
            ReadableDateTime Chronology DateTimeZone]
           [org.joda.time.chrono GJChronology ISOChronology
            BuddhistChronology IslamicChronology JulianChronology
            GregorianChronology EthiopicChronology CopticChronology]))

(defprotocol Plusable
  (seq-plus [o objects] "Internal use only."))

(defprotocol Minusable
  (seq-minus [o objects] "Internal use only."))

(defprotocol Mergeable
  (seq-merge [o objects] "Internal use only."))

(defprotocol HasSign
  (negate [o]
    "Negates a period or a duration.

      (negate (negate x)) == x")
  (abs [o]
    "Returns the absolute value of a period or a duration.

      (abs (negate x)) == (abs x)"))

(defprotocol HasProperties
  (properties [o]
    "Retrieves properties of a Partial or a DateTime.

    For example, get a date with the last day of month:

      (-> (properties date) :dayOfMonth with-max-value)

    or get maximum values for all of the properties:

      (->> (properties date)
           (map (comp max-value val))")
  (property [o key]
    "Retrieves a property with the given key. Nil if the property doesn't
    exist.

    For example:

      (-> date properties :dayOfMonth value)

    can be achieved efficiently by:

      (-> date (property :dayOfMonth) value)"))

(defn plus
  "Sums two or more time entities together. Plus is defined for the following
  entities:

  * periods: period + {period, number}
  * durations: duration + {duration, number}
  * partials: partial + period
  * instants: instant + {duration, period}

  ## Periods

  Sums periods and numbers together to produce a Period. No period
  normalization or field overflows happen in the `plus` function.

    (plus (years 1) (years 1) (months 1))
    => #<Period P2Y1M>

  Numbers are only allowed if type of the sum of the periods preceding the
  number is single (e.g. years or months).

    (plus (years 1) 10)
    => #<Period P11Y>

    (plus (years 1) (months 1) 10)
    => Exception ...

    (plus (years 1) 10 (months 1))
    => #<Period P11Y1M>

  Will always return the result of type `Period`, albeit with the most specific
  `PeriodType`.

  ## Durations

  Sums durations and numbers of milliseconds together to produce a Duration.

    (plus (duration 10) (duration 10) 10)
    => #<Duration PT0.030S>

  ## Partials

  Sums a partial and periods together to produce a partial. Produces the most
  specific type:

    (plus (local-date \"2010-01-01\") (years 2))
    => #<LocalDate 2012-01-01>

    (plus (partial {:year 2010}) (years 2))
    => #<Partial 2012>

  Discards periods which aren't supported by the partial:

    (plus (partial {:year 2010}) (days 500))
    => #<Partial 2010>

  ## Instants

  Sums an instant and periods, durations and numbers of milliseconds together
  to produce an instant. Works with Instants and DateTimes.  Returns the most
  specific type:

    (plus (date-time 0) 1000 (duration 1000) (years 40))
    => #<DateTime 2010-01-01T03:00:02.000+02:00>

    (plus (instant 0) 1000 (duration 1000) (years 40))
    => #<Instant 2010-01-01T00:00:02.000Z>"
  [o & os]
  (seq-plus o os))

(defn minus
  "Subtracts one or more time entity from the first entity. Minus is defined
  for the following entities:

  * periods: period - {period, number}
  * durations: duration - {duration, number}
  * partials: partial - period
  * instants: instant - {duration, period}

  Calling `minus` with a single argument has the same effect as `negate` (for
  durations and periods).  `(minus x y z)` has the same effect as `(plus x
  (negate y) (negate z))`."
  [o & os]
  (if (seq os)
    (seq-minus o os)
    (negate o)))

(defn merge
  "Merges two or more periods or partials together.

  ## Periods

  Merges periods together to produce a Period. Type of the resulting period is
  an aggregate of all period types participating in a merge.

  Periods further in the arglist will overwrite earlier ones having overlapping
  period types.

    (merge (years 2) (months 3))
    => #<Period P2Y3M>

    (merge (years 2) (years 3))
    => #<Period P3Y>

  ## Partials

  Merges partials into an instance of Partial using the chronology of the
  first partial:

    (merge (partial {:year 5}) (partial {:year 3, :monthOfYear 4}))
    => #<Partial 0003-04>

  Will throw an exception if the resulting partial is invalid:

    (merge (local-date \"2008-02-29\") (partial {:year 2010}))
    => Exception ..."
  [o & os]
  (seq-merge o os))

(defprotocol Ordered
  (single-before? [a b] "Implementation details")
  (single-after? [a b] "Implementation details"))

(defprotocol HasZone
  (with-zone [o zone]
    "Set the time zone for the given date-time or a formatter.
    Argument `zone` might be a `DateTimeZone` or a (case-sensitive)
    name of the time zone."))

(defn max
  "Maximum of the given date-times/instants/partials/periods/intervals."
  [o & os]
  (last (sort (cons o os))))

(defn min
  "Minimum of the given date-times/instants/partials/periods/intervals."
  [o & os]
  (first (sort (cons o os))))

(defn before?
  "Returns non-nil if time entities are ordered from the earliest to the latest
  (same as `<`):

    (before? (date-time \"2009\") (date-time \"2010\") (date-time \"2011\"))
    => truthy...

    (before? (interval (date-time \"2009\") (date-time \"2010\"))
             (date-time \"2011\"))
    => truthy...

  Works with instants, date-times, partials and intervals."
  ([x] true)
  ([x y] (single-before? x y))
  ([x y & more]
   (if (before? x y)
     (if (next more)
       (recur y (first more) (next more))
       (before? y (first more)))
     false)))

(defn after?
  "Returns non-nil if time entities are ordered from the latest to the earliest
  (same as `>`):

    (after? (date-time \"2011\") (date-time \"2010\") (date-time \"2009\"))
    => true

    (after? (interval (date-time \"2009\") (date-time \"2010\"))
            (date-time \"2008\"))
    => truthy...

  Works with instants, date-times, partials and intervals."
  ([x] true)
  ([x y] (single-after? x y))
  ([x y & more]
   (if (after? x y)
     (if (next more)
       (recur y (first more) (next more))
       (after? y (first more)))
     false)))

(extend-type nil
  Mergeable (seq-merge [o os] nil)
  Plusable (seq-plus [o os] nil)
  Minusable (seq-minus [o os] nil)
  HasSign
  (negate [o] nil)
  (abs [o] nil)
  HasProperties (properties [o] nil))

(extend-type Number
  Plusable (seq-plus [o os] (+ o (apply + os)))
  Minusable (seq-minus [o os] (- o (apply - os)))
  HasSign
  (negate [o] (- o))
  (abs [o] (cond (instance? BigDecimal o)
                 (.abs ^BigDecimal o)

                 (instance? BigInteger o)
                 (.abs ^BigInteger o)

                 :else (Math/abs (long o)))))

;;;;;;;;;;;; Predicates

(defn duration?
  "True if the given object is an instance of `ReadableDuration`."
  [x] (instance? ReadableDuration x))

(defn interval?
  "True if the given object is an instance of `ReadableInterval`."
  [x] (instance? ReadableInterval x))

(defn period?
  "True if the given object is an instance of `ReadablePeriod`."
  [x] (instance? ReadablePeriod x))

(defn instant?
  "True if the given object is an instance of `ReadableInstant`."
  [x] (instance? ReadableInstant x))

(defn partial?
  "True if the given object is an instance of `ReadablePartial` (includes local
  dates/times)."
  [x] (instance? ReadablePartial x))

;;;;;;;;;;;;; Chronologies and time-zones

(defn ^DateTimeZone timezone
  "Produces a `DateTimeZone` out of a `java.util.TimeZone` or a timezone ID - a
  case-sensitive string, keyword or symbol:

    (timezone :UTC)
    => #<FixedDateTimeZone UTC>

  Default time zone is returned when called with no arguments:

    (timezone)
    => #<CachedDateTimeZone Europe/Vilnius>"
  ([] (DateTimeZone/getDefault))
  ([tz] (cond (instance? DateTimeZone tz) tz
              (instance? java.util.TimeZone tz) (DateTimeZone/forTimeZone tz)
              :else (DateTimeZone/forID (name tz)))))

(def ^:private chronologies
  (reduce
    (fn [result chrono-type]
      (let [type-str (str chrono-type)
            chrono-key (-> type-str (string/replace-first "Chronology" "")
                           string/lower-case)
            default-fn (eval `(fn [] (. ~chrono-type (getInstance))))
            tz-fn (eval `(fn [tz#] (. ~chrono-type (getInstance (timezone tz#)))))]
        (assoc result chrono-key {:default default-fn, :tz tz-fn})))
    {} ['GJChronology 'ISOChronology 'BuddhistChronology 'IslamicChronology
        'JulianChronology 'GregorianChronology 'EthiopicChronology
        'CopticChronology]))

(defn ^Chronology chronology
  "Produces a chronology of the specified type (lowercase) and the given time
  zone (optional):

    (chronology :coptic)
    => #<CopticChronology CopticChronology [Europe/Vilnius]>

    (chronology :coptic :UTC)
    => #<CopticChronology CopticChronology [UTC]>

  Time zones are resolved through the `timezone` function."
  ([nm] ((get-in chronologies [(name nm) :default])))
  ([nm tz] ((get-in chronologies [(name nm) :tz]) tz)))
