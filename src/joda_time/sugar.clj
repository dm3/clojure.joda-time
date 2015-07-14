(ns joda-time.sugar
  "Various convenience functions which were deemed worthy."
  (:require [clojure.string :as string]
            [joda-time.property :as prop]
            [joda-time.period :as period]
            [joda-time.interval :as interval]
            [joda-time.impl :as impl]
            [joda-time.core :as c]))

(defn- values-of [props]
  (into {} (for [[k p] props] [k (prop/value p)])))

(defn as-map
  "Converts a period or a partial into a map representation.

  A period is converted into a map where keys correspond to names of
  `DurationFieldTypes`. Only durations supported by the provided period are
  included in the result.

  A partial is converted into a map where keys correspond to
  `DateTimeFieldTypes`.

  An instant/date-time is converted into a map where keys correspond to
  `DateTimeFieldTypes`. Every date-time field type will be present in the
  result."
  [o] (values-of (c/properties o)))

(def ^:private handles-duration #{'hours 'minutes 'seconds 'millis})

(doseq [duration-type ['years 'months 'weeks 'days 'hours 'minutes
                       'seconds 'millis]]
  (let [fn-name (symbol (str duration-type '-in))]
    (eval `(defn ~fn-name
             ~(str "Number of " duration-type " in the given period/interval/pair of\n"
                   "instants, date-times or partials."
                   (when (handles-duration duration-type) " Also handles durations."))
             ([o#]
              (~(symbol (str '.get (string/capitalize (str duration-type))))
                        (~(symbol "period" (str duration-type)) o#)))
             ([x# y#]
              (if (c/instant? x#)
                (~fn-name (interval/interval x# y#))
                (~fn-name (interval/partial-interval x# y#))))))))

(doseq [[day-name day-number]
        [['monday 1] ['tuesday 2] ['wednesday 3] ['thursday 4] ['friday 5]
         ['saturday 6] ['sunday 7]]]
  (eval `(defn ~(symbol (str day-name '?))
           ~(str "Returns true if the given instant/date-time/partial with the\n"
                 "  `dayOfWeek` property represents a " day-name ".")
           [o#] (= (prop/value (c/property o# :dayOfWeek)) ~day-number))))

(defn weekend? [dt]
  (or (saturday? dt) (sunday? dt)))

(defn weekday? [dt]
  (not (weekend? dt)))
