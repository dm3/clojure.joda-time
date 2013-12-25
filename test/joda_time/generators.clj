(ns joda-time.generators
  (:refer-clojure :exclude (partial))
  (:require [simple-check.generators :as g]
            [joda-time.core :as c]
            [joda-time :as j]
            [clojure.set :as sets])
  (:import [org.joda.time.chrono GJChronology ISOChronology
            BuddhistChronology IslamicChronology JulianChronology
            GregorianChronology EthiopicChronology CopticChronology]
           [org.joda.time Chronology IllegalFieldValueException
            DateTimeUtils Period Instant Partial MutableInterval
            DateTimeFieldType DateTimeField ReadableInstant ReadablePartial
            LocalDate LocalDateTime LocalTime YearMonth MonthDay
            DateTime MutableDateTime]))

(defn- utc-instances-of [chrono-types]
  (for [^Class tp chrono-types]
    (eval `(. ~(symbol (.getSimpleName tp)) getInstanceUTC))))

(def utc-chronology
  (g/elements (utc-instances-of [GJChronology ISOChronology BuddhistChronology
                                 IslamicChronology JulianChronology
                                 GregorianChronology EthiopicChronology
                                 CopticChronology])))

(def default-chronology (ISOChronology/getInstanceUTC))

(def year-of-century
  (g/choose 1 100))

(def year-of-era
  (g/choose 1 10000))

(def month-of-year
  (g/choose 1 12))

(def day-of-week
  (g/choose 1 7))

(def day-of-month
  (g/choose 1 28))

(def hour-of-day
  (g/choose 0 23))

(def minute-of-hour
  (g/choose 0 59))

(def second-of-minute
  (g/choose 0 59))

(def millis-of-second
  (g/choose 0 999))

(defn positive [g] (g/fmap #(c/abs %) g))

;;;;;;;;;; Instants

(def ^:private choosable-instant-range {:min 0, :max (/ Integer/MAX_VALUE 2)})

(def instant-number
  (g/choose (:min choosable-instant-range)
            (:max choosable-instant-range)))

(def ^:private duration-number
  (g/choose (/ Integer/MIN_VALUE 4) (/ Integer/MAX_VALUE 4)))

(def number
  (g/bind instant-number
          (fn [n] (g/one-of [(g/return (BigDecimal/valueOf n))
                             (g/return (BigInteger/valueOf n))
                             (g/return (int n))
                             (g/return (long n))
                             (g/return (float n))
                             (g/return (java.util.concurrent.atomic.AtomicInteger. n))]))))

(def instant
  (g/fmap #(Instant. %) instant-number))

(defn instant-from [from]
  {:pre [(<= (:min choosable-instant-range) from (:max choosable-instant-range))]}
  (g/fmap #(Instant. %) (g/choose from (:max choosable-instant-range))))

;;;;;;;;;; Intervals

(def interval (g/fmap (clojure.core/partial apply j/interval)
                      (g/bind instant-number
                              #(g/tuple (g/return %)
                                        (g/choose % (:max choosable-instant-range))))))

(def mutable-interval
  (g/fmap #(MutableInterval. %) interval))

(def partial-interval
  (g/fmap #(j/partial-interval ((second %) (j/start (first %)))
                               ((second %) (j/end (first %))))
          (g/tuple interval (g/one-of [(g/return j/local-date)
                                       (g/return j/local-date-time)
                                       (g/return j/year-month)]))))

(def any-interval
  (g/one-of [interval partial-interval]))

;;;;;;;;;; Date-Times

(defn- date-time-tuple [& {:keys [chrono] :or {chrono default-chronology}}]
  (g/tuple year-of-era month-of-year day-of-month
           hour-of-day minute-of-hour second-of-minute
           millis-of-second (g/return chrono)))

(defn date-time [& {:keys [chrono] :or {chrono default-chronology}}]
  (g/fmap (clojure.core/partial apply #(DateTime. %1 %2 %3 %4 %5 %6 %7 %8))
          (date-time-tuple :chrono chrono)))

;;;;;;;;;; Periods

(def valid-period-field-types
  [:years :months :weeks :days :hours :minutes :seconds :millis])

(def period-field-type
  (g/elements valid-period-field-types))

(def period-field-types
  (g/fmap set (g/vector period-field-type)))

(def period-type
  (g/fmap (clojure.core/partial apply j/period-type) period-field-types))

(def valid-partial-field-types
  [:era :centuryOfEra :yearOfEra :yearOfCentury :year :monthOfYear
   :weekyearOfCentury :weekOfWeekyear :weekyear :dayOfYear :dayOfMonth
   :dayOfWeek :halfdayOfDay :hourOfDay :clockhourOfDay :hourOfHalfday
   :clockhourOfHalfday :minuteOfDay :millisOfDay :minuteOfHour
   :secondOfDay :secondOfMinute :millisOfSecond])

(def date-time-field-name (g/elements valid-partial-field-types))

(defn- get-date-time-field [chrono field-name]
  (clojure.lang.Reflector/invokeInstanceMethod chrono (name field-name)
                                               (object-array 0)))

(defn date-time-field [& {:keys [chrono fields]
                          :or {chrono default-chronology
                               fields valid-partial-field-types}}]
  (g/bind
    (g/elements (vec fields))
    (fn [field-name]
      (let [field (get-date-time-field chrono field-name)
            max-val (.getMaximumValue field)
            min-val (.getMinimumValue field)]
        (g/tuple (g/return field)
                 (g/choose (max min-val (:min choosable-instant-range))
                           (min max-val (:max choosable-instant-range))))))))

(defn single-field-partial [& {:keys [chrono allowed-fields]
                               :or {chrono default-chronology
                                    allowed-fields valid-partial-field-types}}]
  (g/fmap
    (fn [[field value]]
      (Partial. (.getType field) (int value)))
    (date-time-field :chrono chrono :fields allowed-fields)))

(def ^:private duplicated-partial-field-types
  #{:clockhourOfDay :clockhourOfHalfday})

;TODO: remove when upgrading to Joda 2.4
(def ^:private bug-joda-partial-field-types
  #{:era :centuryOfEra
    :weekyearOfCentury :yearOfEra :yearOfCentury :weekyear})

(def ^:private allowed-generated-partial-fields
  (remove bug-joda-partial-field-types
          (remove duplicated-partial-field-types
                  valid-partial-field-types)))

(defn multi-field-partial [& {:keys [chrono allowed-fields required-fields]
                              :or {chrono default-chronology
                                   allowed-fields allowed-generated-partial-fields}}]
  {:pre [(sets/subset? (set required-fields) (set allowed-fields))]}
  (g/such-that #(and (not (nil? %))
                     (sets/subset? (set required-fields)
                                   (set (keys (c/properties %)))))
    (g/bind (g/not-empty (g/vector
      (single-field-partial :chrono chrono :allowed-fields allowed-fields)))
      (fn [parts]
        (g/return
          (try ; This might fail if the resulting partial is invalid, e.g.:
               ; 2010-02-29
               (apply c/merge parts)
               (catch IllegalFieldValueException e)))))))

(defn contiguous-multi-field-partial [& {:keys [chrono allowed-fields required-fields]
                                         :or {chrono default-chronology
                                              allowed-fields allowed-generated-partial-fields}}]
  (g/such-that (fn [p] (DateTimeUtils/isContiguous p))
               (multi-field-partial :chrono chrono
                                    :allowed-fields allowed-fields
                                    :required-fields required-fields)))

(def local-date
  (g/fmap #(LocalDate. %)
          (multi-field-partial
            :required-fields [:year :monthOfYear :dayOfMonth]
            :allowed-fields [:year :monthOfYear :dayOfMonth])))

(def local-date-time
  (g/fmap #(LocalDateTime. %)
          (multi-field-partial
            :required-fields [:year :monthOfYear :dayOfMonth :millisOfDay]
            :allowed-fields [:year :monthOfYear :dayOfMonth :millisOfDay])))

(def local-time
  (g/fmap #(LocalTime. %)
          (multi-field-partial
            :required-fields [:millisOfSecond :secondOfMinute :minuteOfHour :hourOfDay]
            :allowed-fields [:millisOfSecond :secondOfMinute :minuteOfHour :hourOfDay])))

(def year-month
  (g/fmap #(YearMonth. %)
          (multi-field-partial
            :required-fields [:year :monthOfYear]
            :allowed-fields [:year :monthOfYear])))

(def month-day
  (g/fmap #(MonthDay. %)
          (multi-field-partial
            :required-fields [:monthOfYear :dayOfMonth]
            :allowed-fields [:monthOfYear :dayOfMonth])))

(def any-partial
  (g/one-of [(multi-field-partial) local-date local-date-time local-time
             year-month month-day]))

(def duration (g/fmap j/duration duration-number))

(defn maybe
  "Produces A generator which either returns a `nil` or values from the
  provided generator."
  [gen]
  (g/one-of [(g/return nil) gen]))

(defn- partial-fields-of [part]
  (keys (c/properties part)))

(defn- same-type-after [date]
  (cond (number? date)
        (g/choose date (:max choosable-instant-range))

        (instance? ReadableInstant date)
        (instant-from (.getMillis date))

        (instance? ReadablePartial date)
        (let [c (.getChronology date)
              fields (partial-fields-of date)]
          (g/such-that #(or (.isAfter % date) (.isEqual % date))
                       (multi-field-partial :chrono c
                                            :required-fields fields
                                            :allowed-fields fields)))))

(defn- same-chronology [date]
  (cond (instance? ReadableInstant date)
        (g/return (.getChronology date))

        (instance? ReadablePartial date)
        (g/return (.getChronology date))

        :else utc-chronology))

(defn period-map [& {:keys [min max] :or {min -1000, max 1000}}]
  (g/not-empty (g/map period-field-type (g/choose min max))))

(defn period [& {:keys [min max] :or {min -1000, max 1000}}]
  (g/fmap j/period (period-map :min min :max max)))

(def period-construction-map
  (g/bind
    (g/one-of [instant-number instant (contiguous-multi-field-partial)
               (period-map :min 1 :max 1000)])
    (fn [start]
      (cond (map? start) (g/return start)

            (c/partial? start)
            (g/hash-map :start (maybe (g/return start))
                        :end (maybe (same-type-after start))
                        :chronology (maybe (same-chronology start)))

            :else
            (g/hash-map :start (maybe (g/return start))
                        :end (maybe (same-type-after start))
                        :duration (maybe (g/one-of [duration duration-number]))
                        :chronology (maybe (same-chronology start)))))))

(def period-construction-params
  "Generates maps of parameters which produce a valid Period when constructed
  via the `joda-time.core/period` function."
  (g/such-that #(or (and (:duration %) (:start %))
                    (and (:duration %) (:end %))
                    (and (:start %) (:end %))
                    (some (set valid-period-field-types) (keys %)))
               period-construction-map))
