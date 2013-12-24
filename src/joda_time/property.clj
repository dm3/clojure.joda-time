(ns joda-time.property
  (:import [org.joda.time LocalDate$Property LocalDateTime$Property
            LocalTime$Property MonthDay$Property YearMonth$Property
            Partial$Property DateTime$Property]
           [org.joda.time.field AbstractPartialFieldProperty
            AbstractReadableInstantFieldProperty]))

(defprotocol Property
  "Allows for compile-time access to various property fields, as most of the
  usefule methods on Joda-Time Properties do not implement a common interface.

  Only properties of the immutable dates/instants/partials/periods are
  supported."

  (^int value [p]
    "Value of this property.")
  (^int max-value [p]
    "The maximum value of a property.")
  (^int min-value [p]
    "The minimum value of a property.")
  (with-max-value [p]
    "Instant, partial or a period with the value of the property set to the
    `max-value`.")
  (with-min-value [p]
    "Instant, partial or a period with the value of the property set to the
    `min-value`.")
  (with-value [p v]
    "Instant, partial or a period with the property set to the provided
    value."))

(doseq [t ['LocalDate$Property
           'LocalDateTime$Property
           'LocalTime$Property
           'MonthDay$Property
           'YearMonth$Property
           'Partial$Property
           'DateTime$Property]]
  (eval
    `(extend-type ~t
       Property
       (value [p#] (.get p#))
       (min-value [p#] (.getMinimumValue p#))
       (with-min-value [p#] (.setCopy p# (min-value p#)))

       (max-value [p#] (.getMaximumValue p#))
       (with-max-value [p#] (.setCopy p# (max-value p#)))

       (with-value [p# v#] (.setCopy p# (int v#))))))
