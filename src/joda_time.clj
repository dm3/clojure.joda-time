(ns joda-time
  (:refer-clojure :exclude [merge partial iterate format print contains? max min])
  (:require [joda-time.potemkin.namespaces :as pns]
            [joda-time core seqs interval duration period instant
             partial property sugar format convert]))

(pns/import-vars
  [joda-time.core
   property properties
   merge
   plus minus
   negate abs
   max min
   before? after?

   with-zone

   duration?
   interval?
   period?
   instant?
   partial?

   timezone
   chronology]

  [joda-time.property
   value max-value min-value
   with-max-value with-min-value
   with-value]

  [joda-time.seqs
   iterate]

  [joda-time.convert
   to-java-date to-sql-date to-sql-timestamp to-millis-from-epoch]

  [joda-time.period period
   years months weeks days hours minutes seconds millis

   standard-period-type
   period-type
   period-type->seq]

  [joda-time.interval interval partial-interval
   partial-interval?
   start end contains? overlaps? abuts?
   overlap gap
   move-start-by
   move-end-by
   move-start-to
   move-end-to]

  [joda-time.duration duration]

  [joda-time.instant date-time instant in-zone]

  [joda-time.partial partial
   local-date
   local-time
   local-date-time
   year-month
   month-day]

  [joda-time.sugar as-map
   millis-in seconds-in minutes-in hours-in days-in weeks-in months-in years-in
   monday? tuesday? wednesday? thursday? friday? saturday? sunday? weekend? weekday?]

  [joda-time.format print formatter
   parse-local-date parse-local-date-time parse-local-time parse-date-time
   parse-mutable-date-time])
