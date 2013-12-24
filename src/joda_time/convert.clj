(ns joda-time.convert
  (:import [org.joda.time LocalDate LocalDateTime DateTime
            ReadableInstant]
           [org.joda.time.base AbstractInstant]))

(defn ^java.util.Date to-java-date [o]
  (cond (instance? java.util.Date o)
        o

        (instance? LocalDate o)
        (.toDate ^LocalDate o)

        (instance? LocalDateTime o)
        (.toDate ^LocalDateTime o)

        (instance? AbstractInstant o)
        (.toDate ^DateTime o)))

(defn to-sql-date [o]
  (if (nil? o) nil
    (java.sql.Date. (.getTime (to-java-date o)))))

(defn to-sql-timestamp [o]
  (if (nil? o) nil
    (java.sql.Timestamp. (.getTime (to-java-date o)))))

(defn to-millis-from-epoch [o]
  (cond (nil? o) nil

        (instance? ReadableInstant o)
        (.getMillis ^ReadableInstant o)

        :else (.getTime (to-java-date o))))
