(ns joda-time.convert
  (:import [org.joda.time LocalDate LocalDateTime DateTime
            ReadableInstant]
           [org.joda.time.base AbstractInstant AbstractPartial]))

(defprotocol ^:private JavaDateable
  (^java.util.Date to-java-date [o]
     "Converts (almost) anything to a `java.util.Date`. By
     default conversion will happen in the default time zone,
     i.e.:

       ; In +02:00 zone
       (to-java-date \"2013-12-10\")
       => #inst \"2013-12-09T22:00:00.000-00:00\"

       ; In UTC
       (to-java-date \"2013-12-10\")
       => #inst \"2013-12-10T00:00:00.000-00:00\""))

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

(extend-protocol JavaDateable
  nil
  (to-java-date [o] nil)

  String
  (to-java-date [o] (to-java-date (DateTime. o)))

  Number
  (to-java-date [o] (java.util.Date. (long o)))

  java.sql.Date
  (to-java-date [o] (java.util.Date. (.getTime o)))

  java.sql.Timestamp
  (to-java-date [o] (java.util.Date. (.getTime o)))

  java.util.Date
  (to-java-date [o] o)

  AbstractInstant
  (to-java-date [o] (.toDate o))

  LocalDate
  (to-java-date [o] (.toDate o))

  LocalDateTime
  (to-java-date [o] (.toDate o))

  AbstractPartial
  (to-java-date [o] (to-java-date (DateTime. o))))
