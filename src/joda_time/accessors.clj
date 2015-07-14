(ns joda-time.accessors
  (:require [joda-time.property :as prop]
            [joda-time.impl :as impl]
            [joda-time.core :as c]))

(doseq [date-time-field-type-name impl/date-time-field-type-names]
  (let [value-fn (symbol (impl/dashize (str date-time-field-type-name)))
        prop-fn (symbol (str value-fn "-" 'prop))
        max-fn (symbol (str 'max "-" value-fn))
        min-fn (symbol (str 'min "-" value-fn))
        with-max-fn (symbol (str 'with-max "-" value-fn))
        with-min-fn (symbol (str 'with-min "-" value-fn))]
    (eval
      `(do
         (defn ~prop-fn
           ~(str "The property referring to the " value-fn " of the provided date/partial.\n"
                 "Equivalent to `(property date :" date-time-field-type-name ")`")
           [o#] (c/property o# ~(str date-time-field-type-name)))
         (defn ~value-fn
           ~(str "The " value-fn " value of the provided date/partial.\n"
                 "Equivalent to `(value (property date :" date-time-field-type-name "))`")
           [o#] (prop/value (~prop-fn o#)))
         (defn ~max-fn
           ~(str "The maximum " value-fn " value of the provided date/partial.\n"
                 "Equivalent to `(max-value (property date :" date-time-field-type-name "))`")
           [o#] (prop/max-value (~prop-fn o#)))
         (defn ~min-fn
           ~(str "The minimum " value-fn " value of the provided date/partial.\n"
                 "Equivalent to `(min-value (property date :" date-time-field-type-name "))`")
           [o#] (prop/min-value (~prop-fn o#)))
         (defn ~with-max-fn
           ~(str "The same date/partial with its " value-fn " set to the maximum value.\n"
                 "Equivalent to `(with-max-value (property date :" date-time-field-type-name "))`")
           [o#] (prop/with-max-value (~prop-fn o#)))
         (defn ~with-min-fn
           ~(str "The same date/partial with its " value-fn " set to the maximum value.\n"
                 "Equivalent to `(with-min-value (property date :" date-time-field-type-name "))`")
           [o#] (prop/with-min-value (~prop-fn o#)))))))
