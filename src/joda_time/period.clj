(ns joda-time.period
  (:require [clojure.string :as string]
            [joda-time.core :as c]
            [joda-time.impl :as impl]
            [joda-time.property :as property])
  (:import [org.joda.time PeriodType DurationFieldType
            ReadablePeriod ReadableDuration ReadableInstant
            ReadablePartial MutablePeriod Period Duration
            Years Months Weeks Days Hours Minutes Seconds]))

(defn- select-duration-fields [field-names]
  ; Doesn't check for the validity of the provided field-names. This is
  ; intentional as a check should happen somewhere higher up the call stack.
  (select-keys impl/duration-field-types
               (vec (map name field-names))))

(def ^PeriodType standard-period-type
  "Standard period type. Alias to `PeriodType/standard`."
  (PeriodType/standard))

(defn ^PeriodType period-type
  "Either gets the period type of the given `ReadablePeriod` or constructs a
  `PeriodType` out of the provided duration types.

    (period-type :years :months :weeks :days :hours :minutes :seconds :millis)
    => #<PeriodType PeriodType[Standard]>

    (period-type (years 1))
    => #<PeriodType PeriodType[Years]>" [t & types]
  (cond (c/period? t) (.getPeriodType ^ReadablePeriod t)

        :else
        (PeriodType/forFields
          (into-array DurationFieldType
                      (vals (select-duration-fields (conj types t)))))))

(defn period-type->seq
  "Constructs a sequence of duration type names out of a PeriodType.

    (period-type->seq standard-period-type)
    => [:years :months :weeks :days :hours :minutes :seconds :millis]

    (period-type->seq (period-type (years 1))
    => [:years]"
  [^PeriodType period-type]
  (reduce #(conj %1 (keyword (.getName (.getFieldType period-type %2))))
          [] (range (.size period-type))))

(defn- coerce-period-type [t]
  (cond (nil? t) t
        (instance? PeriodType t) t
        :else (apply period-type t)))

(defn- to-duration-if-number [d]
  (if (number? d)
    (Duration. (long d)) d))

(defn- period-constructor-params [{:keys [duration start end
                                          years months weeks days hours
                                          minutes seconds millis] :as m
                                   :or {years 0 months 0 weeks 0 days 0
                                        hours 0 minutes 0 seconds 0 millis 0}}
                                  ptype]
  (let [ptype (coerce-period-type ptype)]
    (cond (and (and start end) (c/partial? start))
          [start end ptype]

          (and start end)
          [(impl/to-instant-if-number start)
           (impl/to-instant-if-number end) ptype]

          (and duration end)
          [(to-duration-if-number duration)
           (impl/to-instant-if-number end) ptype]

          (and duration start)
          [(impl/to-instant-if-number start)
           (to-duration-if-number duration) ptype]

          :else
          [years months weeks days hours minutes seconds millis
           (or ptype (coerce-period-type (keys m)))])))

(defn- mk-period
  ([p] (Period. ^Period p))
  ([x y z]
   (cond (c/partial? x)
         (Period. ^ReadablePartial x ^ReadablePartial y ^PeriodType z)

         (c/duration? x)
         (Period. ^ReadableDuration x ^ReadableInstant y ^PeriodType z)

         (c/duration? y)
         (Period. ^ReadableInstant x ^ReadableDuration y ^PeriodType z)

         :else
         (Period. ^ReadableInstant x ^ReadableInstant y ^PeriodType z)))
  ([y m w d h mm s mmm tt]
   (Period. y m w d h mm s mmm tt)))

(defn ^Period period
  "Constructs a Period. Takes a number, duration, string,
  interval, another period or a map.

    (period {:years 2, :months 3})
    => #<Period P1Y3M>

    (period {:start 0, :end 1000})
    => #<Period PT1S>

    (period {:start 0, :duration 1000})
    => #<Period PT1S>

    (period {:duration 1000, :end 0})
    => #<Period PT1S>

    (period 1000)
    => #<Period PT1S>

    (period \"PT1S\")
    => #<Period PT1S>

    (period (duration 1000))
    => #<Period PT1S>

    (period (interval 0 1000))
    => #<Period PT1S>

  Accepts two arguments where the second one is the desired type of the period
  (either an instance of `PeriodType` or a vector of duration type keywords,
  e.g. `[:seconds, :millis]`):

    (period {:start 0, :duration (* 1000 1000 1000)} [:days])
    => #<Period P11D>

    (period {:start 0, :duration (* 1000 1000 1000)} (period-type :weeks))
    => #<Period P1W>)"
  ([] (Period.))
  ([o] (period o nil))
  ([o t] (if (nil? o) nil
           (cond
             (number? o) (Period. (long o) ^PeriodType (coerce-period-type t))
             (map? o) (apply mk-period (period-constructor-params o t))
             :else (Period. ^Object o ^PeriodType (coerce-period-type t))))))

(def ^:private statically-typed-periods
  ['years 'months 'weeks 'days 'hours 'minutes 'seconds])

(doseq [period-type statically-typed-periods]
  (let [capitalized-type (string/capitalize (str period-type))]
    (eval `(defn ~(with-meta period-type {:tag (symbol capitalized-type)})
             ~(str "Constructs a " capitalized-type
                   " period representing the given number of " (str period-type) ". "
                   "Given a time entity tries to extract the number of " (str period-type) ". "
                   "Given another period, extracts its " (str period-type) " part.")
             [o#] (if (instance? ~(symbol capitalized-type) o#) o#
                    (. ~(symbol capitalized-type) ~period-type
                       (if (number? o#) (int o#)
                         (let [^Period p#
                               (if (c/period? o#) o#
                                 (period o# (~(symbol (str "PeriodType/" period-type)))))]
                           (. p# ~(symbol (str "get" capitalized-type)))))))))
    (eval `(extend-type ~(symbol capitalized-type)
             c/HasSign
             (negate [p#] (.negated p#))
             (abs [p#] (if (pos? (.getValue p# 0))
                         p# (c/negate p#)))))))

; There's no separate Joda type for Millis
(defn ^Period millis
  "Constructs a Period representing the given number of milliseconds.  Given a
  time entity tries to extract the number of millis. Given another period,
  extracts it's millis part."
  [o] (let [millis (cond (number? o) (int o)
                         (c/period? o) (.getMillis ^Period o)
                         :else (.getMillis ^Period (period o (PeriodType/millis))))]
        (period {:millis millis, :type (PeriodType/millis)})))

(defn- merge-type [ptype ^ReadablePeriod p]
  (set (concat ptype (period-type->seq (.getPeriodType p)))))

(defn- do-with-mutable [effect-fn objs]
  (let [mperiod (MutablePeriod.)]
    [mperiod
     (reduce (fn [result o]
               (effect-fn mperiod o)
               (if (c/period? o)
                 (merge-type result o)
                 result)) [] objs)]))

(defn- reduce-add-into-result [f ^MutablePeriod result ptype o]
  (if (c/period? o)
    (do (.add result ^ReadablePeriod (f o))
        (merge-type ptype o))
    ; Do not support durations as summing them into period only makes sense with
    ; periods less than one day.
    ; Do not support intervals as they cannot be negated, so the contract for
    ; plus/minus breaks.
    (do
      (cond
        (and (number? o) (= (count ptype) 1))
        (.add result
              ^DurationFieldType (impl/duration-field-types (name (first ptype)))
              ^int (f o))

        :else
        (throw (ex-info
            (str "Cannot plus/minus " o " into a period with type " ptype ".")
            {:object o, :type ptype})))
      ptype)))

(defn- add-into-period [f ^ReadablePeriod p objects]
  (let [result (MutablePeriod.)]
    (.add result p)
    (let [ptype (reduce
                  (clojure.core/partial reduce-add-into-result f result)
                  (merge-type #{} p) objects)]
      (Period. ^Object result ^PeriodType (apply period-type ptype)))))

; Don't want to define defrecord as we don't need autogenerated functions and
; map semantics. However writing a custom equals/hashCode sucks.
(defrecord ^:private PeriodProperty [^ReadablePeriod p
                                     ^DurationFieldType duration-field-type]
  property/Property
  (value [_] (.get p duration-field-type))
  (max-value [_] Integer/MAX_VALUE)
  (min-value [_] Integer/MIN_VALUE)
  (with-max-value [self]
    (.withField (period p) duration-field-type (property/max-value self)))
  (with-min-value [self]
    (.withField (period p) duration-field-type (property/min-value self)))
  (with-value [_ value]
    (.withField (period p) duration-field-type (int value))))

(ns-unmap *ns* '->PeriodProperty)
(ns-unmap *ns* 'map->PeriodProperty)

(extend-type ReadablePeriod
  c/HasProperties
  (properties [^ReadablePeriod period]
    (reduce (fn [result [n t]]
              (if (.isSupported period t)
                (assoc result (keyword n) (PeriodProperty. period t))
                result))
            {} impl/duration-field-types))
  (property [^ReadablePeriod period n]
    (let [t (impl/duration-field-types (name n))]
      (when (.isSupported period t) (PeriodProperty. period t))))

  ; Currently merge and sum are lenient, i.e. (c/merge (c/years 1) (c/days 2))
  ; will produce a period of type [:years :days]. Another option is to make merge
  ; strict, which will make the above invocation fail, as the type of the period
  ; will be determined by the first period in the merge. This can be done either
  ; by creating another merge function (e.g. `merge-strict`), by creating
  ; type-classes for different merge semigroups (strict and lenient, e.g. by
  ; extending the Mergeable protocol from a PeriodType instead of ReadablePeriod).
  ;
  ; There's also a question of the default. Should the default merge type be
  ; lenient or strict? I'm presupposed towards lenient as it seems the most
  ; useful. You can always get strict by chaining `.mergePeriod` method calls
  ; via java interop.

  c/Mergeable
  (seq-merge [p periods]
    (let [[result ptype]
          (do-with-mutable #(.mergePeriod ^MutablePeriod %1 %2) (conj periods p))]
      (Period. ^Object result ^PeriodType (apply period-type ptype))))

  c/Plusable
  (seq-plus [p objects]
    (add-into-period identity p objects))

  c/Minusable
  (seq-minus [p objects]
    (add-into-period c/negate p objects))

  c/HasSign
  (negate [p]
    (.negated (Period. p (period-type p))))
  (abs [p]
    (let [result (MutablePeriod. p)]
      (doseq [i (range (.size p))]
        (let [v (.getValue p i)]
          (when (neg? v)
            (.set result (.getFieldType p i) (Math/abs v)))))
      (Period. result))))

