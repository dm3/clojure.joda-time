(ns joda-time.purgatory
  "A place for Vars to suffer while they are evaluated for worthiness.

  Experiments and alpha quality code belongs here. No vars from this namespaces
  are imported into the global `joda-time` ns."
  (:require [joda-time.impl :as impl]
            [joda-time.core :as c]
            [joda-time.format :as f])
  (:import [org.joda.time DateTime]))

(defn show-formatters
  "Shows how a given instant/date-time/partial, or by default the current time,
  would be formatted with each of the available printing formatters.

  Inspired by `clj-time`."
  ([] (show-formatters (DateTime.)))
  ([dt]
    (doseq [[k p] (sort @#'f/iso-formatters)]
      (try (printf "%-40s%s\n" k (f/print p dt))
           (catch Exception _)))))

(doseq [date-time-field-type-name impl/date-time-field-type-names]
  (let [fn-name (symbol (impl/dashize (str date-time-field-type-name)))]
    (eval `(defn ~fn-name
             [o#] (c/property o# ~(str date-time-field-type-name))))))
