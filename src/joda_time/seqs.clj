(ns joda-time.seqs
  (:refer-clojure :exclude [iterate]))

(defn- partialr [f & args]
  (fn [a & as]
    (apply f a (concat as args))))

(defn iterate
  "Returns a lazy sequence of `initial` , `(apply f initial v vs)`, etc.

  Useful when you want to produce a sequence of dates/periods/intervals, for
  example:

    (iterate plus (millis 0) 1)
    => (#<Period PT0S> #<Period PT0.001S> #<Period PT0.002S> ...)

    (iterate plus (local-date \"2010-01-01\") (years 1))
    => (#<LocalDate 2010-01-01> #<LocalDate 2011-01-01> ...)

    (iterate move-end-by (interval 0 1000) (seconds 1))
    => (#<Interval 00.000/01.000> #<Interval 00.000/02.000> ...)"
  [f initial v & vs]
  (clojure.core/iterate
    (apply partialr f v vs) initial))

