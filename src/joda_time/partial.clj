(ns joda-time.partial
  (:refer-clojure :exclude [partial])
  (:require [joda-time.core :as c]
            [joda-time.impl :as impl])
  (:import [org.joda.time DateTimeFieldType DateTimeField Chronology
            Partial ReadablePartial YearMonth MonthDay LocalDate
            LocalDateTime LocalTime ReadablePeriod]
           [org.joda.time.base AbstractPartial]))

(defn- select-values [m]
  (reduce (fn [res n]
            (if-let [value ((keyword n) m)]
              (conj res [(impl/date-time-field-types (name n)) value])
              res)) [] impl/date-time-field-type-names))

(defn- dissoc-if [m k kd]
  (if (k m) (dissoc m kd) m))

; TODO: should this be somehow configurable? dynamic vars maybe?
(defn- remove-duplicates [m]
  (-> m
      (dissoc-if :hourOfDay :clockhourOfDay)
      (dissoc-if :hourOfHalfday :clockhourOfHalfday)))

(defn- partial-from-map [{:keys [chronology] :as types}]
  (let [date-time-field-vals (select-values (remove-duplicates types))]
    (Partial. ^"[Lorg.joda.time.DateTimeFieldType;"
              (into-array DateTimeFieldType (map first date-time-field-vals))
              ^ints (int-array (map second date-time-field-vals))
              ^Chronology chronology)))

(defn ^Partial partial
  "Constructs a Partial out of another partial or a map with keys corresponding
  to the names of date-time field types and an (optional) chronology.

    (partial {})
    ; => #<Partial []>

    (partial {:year 5})
    ; => #<Partial 0005>

    (partial {:year 5, :chronology (ISOChronology/getInstanceUTC)})
    ; => #<Partial 0005>

    (partial {:era 1, :centuryOfEra 2, :yearOfEra 3, :dayOfYear 5})
    ; => #<Partial [era=1, centuryOfEra=2, yearOfEra=3, dayOfYear=5]>"
  ([] (Partial.))
  ([o] (cond (nil? o) nil
             (map? o) (partial-from-map o)
             :else (Partial. ^ReadablePartial o))))

(doseq [[fn-name partial-type] [['local-date 'LocalDate]
                                ['local-date-time 'LocalDateTime]
                                ['local-time 'LocalTime]
                                ['year-month 'YearMonth]
                                ['month-day 'MonthDay]
                                ['partial 'Partial]]]
  (when (not= 'partial fn-name) ; Partial construction fn is specified explicitly
    (let [partial-ctor (symbol (str partial-type '.))]
      (eval `(defn ~(with-meta fn-name {:tag partial-type})
               ~(str "Constructs a " partial-type " out of \n\n"
                     "  * another partial (which must have all of the needed fields)\n"
                     "  * an instant or a number of milliseconds\n"
                     "  * a java (util/sql) Date/Timestamp or a Calendar\n"
                     "  * an ISO formatted string\n"
                     "  * a map with keys corresponding to the names of date-time field types\n"
                     "    and an (optional) chronology.\n\n"
                     "  When called with no arguments produces a value of `" partial-type "/now`.")
               ([] (. ~partial-type now))
               ([o#] (cond (nil? o#) nil
                           (map? o#) (~partial-ctor (partial-from-map o#))
                           (number? o#) (~partial-ctor (long o#))
                           :else (~partial-ctor o#)))))))

  (let [result (with-meta (gensym) {:tag partial-type})]
    (eval `(extend-type ~partial-type
             c/HasProperties
             (properties [p#]
               (reduce
                 (fn [result# ^DateTimeFieldType field-type#]
                   (assoc result#
                          (keyword (.getName field-type#))
                          (.property p# field-type#)))
                 {} (.getFieldTypes p#)))
             (property [p# k#]
               (.property p# (impl/date-time-field-types (name k#))))

             c/Plusable
             (seq-plus [part# periods#]
               (reduce
                 (fn [~result p#]
                   (.plus ~result ^ReadablePeriod p#)) part# periods#))

             c/Minusable
             (seq-minus [part# periods#]
               (reduce
                 (fn [~result p#]
                   (.minus ~result ^ReadablePeriod p#)) part# periods#))))))

(extend-type ReadablePartial
  c/Mergeable
  (seq-merge [p partials]
    (reduce (fn [result ^ReadablePartial el]
              (reduce (fn [^Partial part idx]
                        (let [^DateTimeField f (.getField el idx)]
                          (.with part (.getType f) (.getValue el idx))))
                      result (range (.size el))))
            (Partial. (.getChronology p)) (conj partials p))))

(extend-type AbstractPartial
  c/Ordered
  (single-before? [i ^ReadablePartial o]
    (.isBefore i o))
  (single-after? [i ^ReadablePartial o]
    (.isAfter i o)))
