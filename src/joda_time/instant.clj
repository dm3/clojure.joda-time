(ns joda-time.instant
  (:require [joda-time.core :as c]
            [joda-time.impl :as impl]
            [joda-time.property :as property])
  (:import [org.joda.time ReadablePeriod ReadableDuration
            ReadableInstant Instant DateTime MutableDateTime
            Chronology DateTimeField DateTimeFieldType]
           [org.joda.time.base AbstractInstant]))

; DateTime and Partial construction could benefit from having default aliases
; for DateTimeFieldTypes, such as:
;
;   (def ^:private date-time-field-type-aliases
;     {:month 'monthOfYear
;      :day 'dayOfMonth
;      :hour 'hourOfDay
;      :minute 'minuteOfHour
;      :second 'secondOfMinute
;      :millis 'millisOfSecond})
;
; These aliases would only be triggered when constructing a date-time or a
; partial. However, no other places in code would use aliases which makes me
; hesitant to add them.

(extend-type AbstractInstant
  c/Ordered
  (single-before? [i o]
    (.isBefore i ^ReadableInstant (impl/to-instant-if-number o)))
  (single-after? [i o]
    (.isAfter i ^ReadableInstant (impl/to-instant-if-number o))))

(defn- date-time-ctor-from-map
  [{:keys [year yearOfEra weekyear monthOfYear dayOfMonth hourOfDay
           minuteOfHour secondOfMinute millisOfSecond chronology]}]
  (let [year (or year yearOfEra weekyear)]
    [year monthOfYear dayOfMonth hourOfDay minuteOfHour secondOfMinute
     millisOfSecond chronology]))

(doseq [date-time-type ['DateTime 'MutableDateTime]]
  (let [fn-name (symbol (impl/dashize (str date-time-type)))
        ctor-fn (symbol (str 'mk- fn-name))]
    (eval `(defn- ~ctor-fn [y# m# d# h# mm# s# mmm# chrono#]
             (~(symbol (str date-time-type '.)) (int y#) (int m#) (int d#) (int h#)
                       (int mm#) (int s#) (int mmm#) ^Chronology chrono#)))
    (eval `(defn ~(with-meta fn-name {:tag date-time-type})
             ~(str "Constructs a " date-time-type " out of:\n\n"
                   "  * another instant or a number of milliseconds\n"
                   "  * a java (util/sql) Date/Timestamp or a Calendar\n"
                   "  * an ISO formatted string\n"
                   "  * a map with keys corresponding to the names of date-time field types\n"
                   "    and an (optional) chronology.\n\n"
                   "  When called with no arguments produces a value of `" date-time-type "/now`.")
             ([] (. ~date-time-type now))
             ([o#]
              (cond
                (nil? o#) nil
                (map? o#) (apply ~ctor-fn (date-time-ctor-from-map o#))
                :else (~(symbol (str date-time-type '.)) o#)))))))

(defn- mseq-minus [^MutableDateTime d objs]
  (doseq [o objs]
    (cond (number? o)
          (.add d (- (long o)))

          (c/period? o)
          (.add d ^ReadablePeriod o -1)

          :else
          (.add d ^ReadableDuration o -1)))
  d)

(defn- mseq-plus [^MutableDateTime d objs]
  (doseq [o objs]
    (cond (number? o)
          (.add d (long o))

          (c/period? o)
          (.add d ^ReadablePeriod o)

          :else
          (.add d ^ReadableDuration o)))
  d)

(extend-type DateTime
  c/Plusable
  (seq-plus [d objs]
    (let [md (MutableDateTime. d)]
      (DateTime. ^MutableDateTime (mseq-plus md objs))))

  c/Minusable
  (seq-minus [d objs]
    (let [md (MutableDateTime. d)]
      (DateTime. ^MutableDateTime (mseq-minus md objs))))

  c/HasProperties
  (properties [d]
    (reduce #(assoc %1 (keyword (key %2)) (.property d (val %2)))
            {} impl/date-time-field-types))
  (property [d k]
    (.property d (impl/date-time-field-types (name k))))

  c/HasZone
  (with-zone [dt zone]
    (.withZone dt (c/timezone zone))))

;;;;;;;; Instant

(defn ^Instant instant
  "Constructs an Instant out of another instant, java date, calendar, number of
  millis or a formatted string:

    (instant)
    => #<Instant ...now...>

    (instant (java.util.Date.))
    => #<Instant ...now...>

    (instant 1000)
    => #<Instant 1970-01-01T00:00:01.000Z>

    (j/instant \"1970-01-01T00:00:01.000Z\")
    => #<Instant 1970-01-01T00:00:01.000Z>"
  ([] (Instant.))
  ([o] (cond (nil? o) nil
             (map? o) (.toInstant ^DateTime (date-time o))
             :else (Instant. o))))

(defn- to-number-if-duration [d]
  (cond (number? d) (long d)
        (c/duration? d) (.getMillis ^ReadableDuration d)))

(defn- new-millis [^Chronology chrono current-ms to-add ^long scalar]
  (cond (c/period? to-add)
        (.add chrono ^ReadablePeriod to-add ^long current-ms scalar)

        :else
        (.add chrono ^long current-ms ^long (to-number-if-duration to-add) scalar)))

(defn- add-to-instant [^Instant inst o scalar]
  (let [chrono (.getChronology inst)
        ms (.getMillis inst)]
    (.withMillis inst ^long (new-millis chrono ms o scalar))))

(extend-type Instant
  c/Plusable
  (seq-plus [d objs]
    (reduce #(add-to-instant %1 %2 1) d objs))

  c/Minusable
  (seq-minus [d objs]
    (reduce #(add-to-instant %1 %2 -1) d objs)))

(defrecord ^:private InstantProperty [^ReadableInstant inst ^DateTimeField field]
  property/Property
  (value [_] (.get field (.getMillis inst)))
  (max-value [_] (.getMaximumValue field (.getMillis inst)))
  (min-value [_] (.getMinimumValue field (.getMillis inst)))
  (with-max-value [self]
    (property/with-value self (property/max-value self)))
  (with-min-value [self]
    (property/with-value self (property/min-value self)))
  (with-value [_ value]
    (Instant. (.set field (.getMillis inst) (int value)))))

(ns-unmap *ns* '->InstantProperty)
(ns-unmap *ns* 'map->InstantProperty)

(extend-type Instant
  c/HasProperties
  (properties [d]
    (reduce #(assoc %1 (keyword %2) (c/property d %2))
            {} impl/date-time-field-type-names))
  (property [d field-name]
    (let [^DateTimeFieldType t (impl/date-time-field-types (name field-name))]
      (InstantProperty. d (.getField t (.getChronology d))))))
