(ns joda-time.convert
  (:import [org.joda.time LocalDate LocalDateTime DateTime
            ReadableInstant]
           [org.joda.time.base AbstractInstant]))

(defn ^java.util.Date to-java-date
  "Converts a date entity to a `java.util.Date`."
  [o]
  (cond (instance? java.util.Date o)
        o

        (instance? LocalDate o)
        (.toDate ^LocalDate o)

        (instance? LocalDateTime o)
        (.toDate ^LocalDateTime o)

        (instance? AbstractInstant o)
        (.toDate ^DateTime o)))

(defn ^java.sql.Date to-sql-date
  "Converts a date entity to a `java.sql.Date`."
  [o]
  (if (nil? o) nil
    (java.sql.Date. (.getTime (to-java-date o)))))

(defn ^java.sql.Timestamp to-sql-timestamp [o]
  "Converts a date entity to a `java.sql.Timestamp`."
  (if (nil? o) nil
    (java.sql.Timestamp. (.getTime (to-java-date o)))))

(defn to-millis-from-epoch [o]
  "Converts a date entity to a `long` representing the number of milliseconds
  from epoch."
  (cond (nil? o) nil

        (instance? ReadableInstant o)
        (.getMillis ^ReadableInstant o)

        :else (.getTime (to-java-date o))))
