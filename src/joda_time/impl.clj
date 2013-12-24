(ns joda-time.impl
  "Internal implementation.
  Anything in this namespace is subject to change without warnings!"
  (:require [clojure.string :as s])
  (:import [org.joda.time Instant DateTimeFieldType DurationFieldType]))

(defn dashize [camelcase]
  (let [words (re-seq #"([^A-Z]+|[A-Z]+[^A-Z]*)" camelcase)]
    (s/join "-" (map (comp s/lower-case first) words))))

(defn- as-field-types [type-symbol names]
  (reduce (fn [result field-name]
            (assoc result (name field-name) (eval `(. ~type-symbol ~field-name))))
          {} names))

(def date-time-field-type-names
  ['era 'centuryOfEra 'year 'yearOfCentury 'weekyear 'yearOfEra
   'weekyearOfCentury 'monthOfYear 'weekOfWeekyear 'dayOfYear 'dayOfMonth
   'dayOfWeek 'halfdayOfDay 'hourOfDay 'clockhourOfDay 'clockhourOfHalfday
   'hourOfHalfday 'minuteOfDay 'minuteOfHour 'secondOfDay
   'secondOfMinute 'millisOfDay 'millisOfSecond])

(def duration-field-type-names
  ['eras 'centuries 'years 'months 'weeks 'weekyears 'days 'halfdays 'hours
   'minutes 'seconds 'millis])

(def date-time-field-types
  (as-field-types DateTimeFieldType date-time-field-type-names))

(def duration-field-types
  (as-field-types DurationFieldType duration-field-type-names))

(defn reduce-date-time-fields [f initial]
  (reduce
    (fn [res n] (f initial [(keyword n) (date-time-field-types n)]))
    date-time-field-type-names))

;;;; Conversions

(defn to-instant-if-number [d]
  (if (number? d) (Instant. (long d)) d))
