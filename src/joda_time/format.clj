(ns joda-time.format
  (:refer-clojure :exclude [print])
  (:require [joda-time.impl :as impl]
            [joda-time.core :as c])
  (:import [org.joda.time.format DateTimeFormat DateTimeFormatter
            ISODateTimeFormat DateTimeFormatterBuilder DateTimePrinter
            DateTimeParser]
           [org.joda.time LocalDate LocalDateTime DateTime LocalTime
            MutableDateTime]))

(defn- select-formatters []
  (let [fmts (filter (fn [^java.lang.reflect.Method m]
                       (-> m .getReturnType (= DateTimeFormatter)))
                     (.getDeclaredMethods ISODateTimeFormat))]
    (for [^java.lang.reflect.Method m fmts
          :let [fmt (try (.invoke m nil nil)
                         (catch Exception _ nil))]
          :when fmt]
      [(keyword (impl/dashize (.getName m))) fmt])))

(def ^:private iso-formatters
  "All of the formatters defined in the static methods of `ISODateTimeFormat`."
  (reduce (fn [result [fmt-key ^DateTimeFormatter fmt]]
            (assoc result fmt-key fmt)) {} (select-formatters)))

(defprotocol ^:private Printable
  (print-date [dt f] "Internal use only"))

(defn print
  "Prints a date entity using the provided formatter. Without the formatter
  prints in ISO format."
  ([dt] (str dt))
  ([^DateTimeFormatter df dt] (print-date dt df)))

(doseq [t ['LocalDate 'LocalDateTime 'LocalTime 'DateTime 'MutableDateTime]]
  (let [time-entity-name (impl/dashize (str t))
        fn-name (symbol (str 'parse- time-entity-name))]
    (eval `(defn ~fn-name
             ~(str "Parses an instance of " t " with the given parsing formatter.\n"
                   "  To parse an ISO-formatted string you can simply invoke the `"
                   time-entity-name "` constructor.")
             [f# dt-str#]
             (. ^DateTimeFormatter f# ~(symbol (str 'parse t)) dt-str#))))
  (eval `(extend-type ~t
           Printable
           (print-date [dt# f#] (.print ^DateTimeFormatter f# dt#)))))

(defn- as-formatter [fmt]
  (cond (instance? DateTimeFormatter fmt) fmt
        (string? fmt) (DateTimeFormat/forPattern fmt)
        :else (fmt iso-formatters)))

(defn formatter
  "Constructs a DateTimeFormatter out of a number of

  * format strings - \"YYYY/mm/DD\", \"YYY HH:MM\", etc.
  * ISODateTimeFormat formatter names - :date, :time-no-millis, etc.
  * other DateTimeFormatter instances

  The resulting formatter will parse all of the requested formats and print
  using the first one."
  [fmt & more]
  (if (empty? more)
    (as-formatter fmt)
    (let [all (map as-formatter (cons fmt more))
          printer (.getPrinter ^DateTimeFormatter (first all))
          parsers (map #(.getParser ^DateTimeFormatter %) all)]
      (-> (DateTimeFormatterBuilder.) ^DateTimeFormatterBuilder
          (.append ^DateTimePrinter printer
                   ^"[Lorg.joda.time.format.DateTimeParser;"
                   (into-array DateTimeParser parsers))
          .toFormatter))))

(extend-type DateTimeFormatter
  c/HasZone
  (with-zone [dtf zone]
    (.withZone dtf (c/timezone zone))))
