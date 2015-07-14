# Clojure.Joda-Time

[![Build Status](https://travis-ci.org/dm3/clojure.joda-time.png?branch=master)](https://travis-ci.org/dm3/clojure.joda-time)

An idiomatic Clojure wrapper for Joda-Time.

Main goals:

* Provide a consistent API for common operations with
  instants, date-times, periods, partials and intervals.
* Provide an escape hatch from Joda types to clojure datastructures
  and back where possible.
* Avoid reflective calls (this is a problem, because many types in Joda-Time
  have similar functionality hidden under similarly named and overloaded
  methods with no common interfaces).
* Provide an entry point into Joda-Time by freeing the user from importing most
  of the Joda-Time classes.

Compared to [clj-time](https://github.com/clj-time/clj-time), this library is
not `DateTime`-centric. If you tend to use **local** dates in most of your
projects, meaning you don't care about the time zones, there's no purpose in
using `DateTime` at all. You should be using various `Partials` provided by
Joda-Time, most common being `LocalDate` and `LocalDateTime`. This also means
that date-times created through Clojure.Joda-Time are not converted to the UTC
timezone by default, as they are in **clj-time**.

## Usage

Add the following dependency to your `project.clj`:

```clj
[clojure.joda-time "0.6.0"]
```

[API](http://dm3.github.io/clojure.joda-time/) of the Clojure.Joda-Time
consists of one namespace, namely: `joda-time`.  For the purposes of this
guide, we will `use` the main namespace:

```clj
(refer-clojure :exclude [merge partial iterate format print contains? max min])
(use 'joda-time)
```

### An appetizer

First, a quick run through common use cases.

What is the current date and time in our time zone?

```clj
(def now (date-time))
=> #<DateTime 2013-12-10T13:07:16.000+02:00>
```

In UTC?

```clj
(with-zone now (timezone :UTC))
=> #<DateTime 2013-12-10T11:07:16.000Z>
```

In UTC but with the current timezone's time?

```clj
(in-zone now (timezone :UTC))
=> #<DateTime 2013-12-10T13:07:16.000Z>
```

Without the time zone?

```clj
(def now-local (local-date-time))
=> #<LocalDateTime 2013-12-10T13:07:16.000>
```

Now, how would we go about a date five years and six months from now?  First,
we would need to represent this period:

```clj
(period {:years 5, :months 6})
=> #<Period P5Y6M>
```

or as a sum:

```clj
(def five-years-and-some (plus (years 5) (months 6)))
=> #<Period P5Y6M>
```

Now for the date:

```clj
(def in-five-years (plus now five-years-and-some))
=> #<DateTime 2019-06-10T13:07:16.000+03:00>

(def in-five-years-local (plus now-local five-years-and-some))
=> #<LocalDateTime 2019-06-10T13:07:16.000>
```

How many hours to the point five years and six months from now?

```clj
(hours-in now in-five-years)
=> 48191

(hours-in now-local in-five-years-local)
=> 48191
```

What if we want a specific date?

```clj
(def in-two-years (date-time "2015-12-10"))
=> #<DateTime 2015-12-10T00:00:00.000+02:00>

(def in-two-years-local (local-date-time "2015-12-10"))
=> #<LocalDateTime 2015-12-10T00:00:00.000>
```

Same with positional arguments:

```clj
(def in-two-years-positional (date-time 2015 12 10))
=> #<DateTime 2015-12-10T00:00:00.000+02:00>

(def in-two-years-local-positional (local-date-time 2015 12 10))
=> #<LocalDateTime 2015-12-10T00:00:00.000>
```

Does the interval from `now` to `in-five-years` contain this date?

```clj
(after? in-five-years in-two-years now)
=> true

(after? in-five-years-local in-two-years-local now-local)
=> true
```

Another way, actually using the interval type:

```clj
(contains? (interval now in-five-years) in-two-years)
=> true

(contains? (partial-interval now-local in-five-years-local) in-two-years-local)
=> true
```

What about the current day of month?

```clj
(-> now (property :dayOfMonth) value)
=> 10

(-> now-local (property :dayOfMonth) value)
=> 10
```

The date at the last day of month?

```clj
(-> now (property :dayOfMonth) with-max-value)
=> #<DateTime 2013-12-31T13:07:16.000+02:00>

(def new-years-eve (-> now-local (property :dayOfMonth) with-max-value)
=> #<LocalDateTime 2013-12-31T13:07:16.000>
```

We can also do this using the `accessors` namespace:

```clj
(require '[joda-time.accessors :as ja])

(value (ja/day-of-month-prop now))
=> 10

(ja/day-of-month now)
=> 10

(ja/min-day-of-month now)
=> 1

(ja/max-day-of-month now)
=> 31

(ja/with-max-day-of-month now)
=> #<DateTime 2013-12-31T13:07:16.000+02:00>
```

Every date at the last day of month from now?

```clj
(iterate plus new-years-eve (months 1))
=> (#<LocalDateTime 2013-12-31T13:07:16.000>
    #<LocalDateTime 2014-01-31T13:07:16.000> ...)
```

In case we want to print the dates, we'll need a formatter:

```clj
(def our-formatter (formatter "yyyy/MM/dd"))
=> #<DateTimeFormatter ...>

(print our-formatter now)
=> "2013/12/10"

(print our-formatter now-local)
=> "2013/12/10"
```

And what about parsing?

```clj
(parse-date-time our-formatter "2013/12/10")
=> #<DateTime 2013-12-10T00:00:00.000+02:00>

(parse-local-date our-formatter "2013/12/10")
=> #<LocalDate 2013-12-10>
```

How should we convert between Joda dates and java.util/sql Dates?

```clj
(local-date now)
=> #<LocalDate 2013-12-10>

(date-time now-local)
=> #<DateTime 2013-12-10T13:07:16.000+02:00>

(local-time now)
=> #<LocalTime 13:07:16.000>

(date-time (local-time now))
=> #<DateTime 1970-01-01T13:07:16.000+03:00>

(to-java-date now)
=> #inst "2013-12-10T11:07:16.000-00:00"

(to-java-date local-now)
=> #inst "2013-12-10T11:07:16.000-00:00"

(to-millis-from-epoch now)
=> 1386673636000
```

I hope you're interested. However, we've barely scratched the surface of the
API. Please, continue reading for a deeper look.

### Joda-Time entities

Clojure.Joda-Time provides a way to construct most of the time entities
provided by the Joda-Time. For example, given a `LocalDate` type in Joda-Time,
the corresponding construction function (I'll hijack the name "constructor" to
define construction functions) in Clojure.Joda-Time will be called
`local-date`.

A call to a constructor with a single argument goes through the following
pattern:

* given a `nil`, return a `nil` (**important**: this is different from the
  default Joda-Time behaviour which usually has a default value for `nil`)
* given a number, convert it to `Long` and invoke the next rule,
* given a map, try to reconstruct a time entity from its map representation
  (see **Properties** section) or invoke one of the constructors on the
  corresponding Java class.
* given any object, pass it to the Joda-Time `ConverterManager`,

Mostly single-argument constructors are supported (except for a several cases
which we will look at later on) to avoid confusion with overloading.

By convention, a call to a constructor without arguments will return a time
entity constructed at the current date and time.

#### Instants

In Joda-Time [instants](http://www.joda.org/joda-time/key_instant.html) are
represented by `DateTime` and `Instant` types.

```clj
(date-time)
=> #<DateTime 2013-12-10T10:20:13.133+02:00>

(instant)
=> #<Instant 2013-12-10T10:20:16.233+02:00>
```

You might have noticed that `DateMidnight` is not supported. This is because
the type is deprecated in the recent versions of Joda-Time. If you need a
midnight date, you should be use:

```clj
(.withTimeAtStartOfDay (date-time))
=> #<DateTime 2013-12-10T00:00:00.000+02:00>
```

#### Partials

[Partials](http://www.joda.org/joda-time/key_partial.html) are represented by
`Partial`, `LocalDate`, `LocalDateTime`, `LocalTime`, `YearMonth` and `MonthDay`.

```clj
(partial)
=> #<Partial []>

(partial {:year 2013, :monthOfYear 12})
=> #<Partial 2013-12>

(local-date)
=> #<LocalDate 2013-12-10>

(local-date-time)
=> #<LocalDateTime 2013-12-10T10:15:13.553>

(local-time)
=> #<LocalTime 10:15:15.234>

(year-month)
=> #<YearMonth 2013-12>

(month-day)
=> #<MonthDay --12-10>
```

Multi-arity versions of the constructors are also supported. The fields not
provided will default to minimum values (0 for hours, minutes, seconds, millis;
1 for days).

#### Periods

Joda types for [periods](http://www.joda.org/joda-time/key_period.html) are
`Period`, `MutablePeriod`, `Years`, `Months`, `Weeks`, `Days`, `Hours`,
`Minutes` and `Seconds`.

Multi-field period accepts a map of several possible shapes. The first shape is
a map representation of a period, e.g.:

```clj
(period {:years 10, :months 10})
=> #<Period P10Y10M>
```

the second shape delegates to an appropriate Joda `Period` constructor, such
as:

```clj
(period {:start 0, :end 1000})
=> #<Period PT1S>
```

Period constructor can be called with two arguments, where the second argument
is the type of the period - either a `PeriodType` or a vector of duration field
name keywords:

```clj
(period 1000 [:millis])
=> #<Period PT1S>

(period {:start 0, :end 1000} (period-type :millis))
=> #<Period PT1S>
```

All of the single-field periods are constructed the same way:

```clj
(years 10)
=> #<Years P10Y>

(months 5)
=> #<Months P5M>
```

Single-field period constructors can also be used to extract duration component
out of the multi-field period:

```clj
(years (period {:years 20, :months 10}))
=> #<Years P20Y>
```

When called on an interval, single-field period constructor will calculate the
duration (same as `Years.yearsIn`, `Months.monthsIn`, etc. in Joda-Time):

```clj
(minutes (interval (date-time "2008") (date-time "2010")))
=> #<Minutes PT1052640M>
```

You can get the value of the single-field period using the helper functions:

```clj
(minutes-in (period {:years 20, :minutes 10}))
=> 10

(minutes-in (interval (date-time "2008") (date-time "2010")))
=> 1052640

(minutes-in (duration (* 1000 1000))
=> 16
```

You can also query the type of the period:

```clj
(period-type (period))
=> #<PeriodType PeriodType[Standard]>

(period-type (years 5))
=> #<PeriodType PeriodType[Years]>
```

`period-type` can also construct a `PeriodType` out of the duration field type
names:

```clj
(period-type :years)
=> #<PeriodType PeriodType[Years]>

(period-type :years :months :weeks :days)
=> #<PeriodType PeriodType[StandardNoHoursNoMinutesNoSecondsNoMillis]>
```

You can also convert the `PeriodType` back to a seq of keywords:

```clj
(period-type->seq (period-type (years 10)))
=> [:years]
```

#### Durations

[Duration](http://www.joda.org/joda-time/key_duration.html) is the most
boring time entity. It can be constructed in the following way:

```clj
(duration 1000)
=> #<Duration PT1S>
```

Duration constructor also accepts a map (you can find the whole set of options
in the docstring):

```clj
(duration {:start (date-time), :period (years 5)})
=> #<Duration PT157766400S>
```

#### Intervals

[Intervals](http://www.joda.org/joda-time/key_interval.html) consist of a
single type inherited from Joda Time:

```clj
(interval 0 1000)
=> #<Interval 1970-01-01T00:00:00.000Z/1970-01-01T00:00:01.000Z>
```

and one additional type defined in this library:

```clj
(partial-interval (partial {:year 0}) (partial {:year 2010}))
=> #joda_time.interval.PartialInterval{:start #<Partial 0000>, :end #<Partial 2010>}

(partial-interval (local-date "2010") (local-date "2013"))
=> #joda_time.interval.PartialInterval{:start #<LocalDate 2010-01-01>,
                                       :end #<LocalDate 2013-01-01>}
```

record representation of the partial interval is an implementation detail and
should not be relied upon.

As you can see, the interval constructor accepts start and end arguments -
either milliseconds from epoch, instants or date-times. Constructor also
accepts a map with different combinations of `start`, `end`, `duration` and
`period` parameters, same as Joda-Time `Interval` constructors:

```clj
(interval {:start 0, :end 1000})
=> #<Interval 1970-01-01T00:00:00.000Z/1970-01-01T00:00:01.000Z>

(interval {:start 0, :duration 1000})
=> #<Interval 1970-01-01T00:00:00.000Z/1970-01-01T00:00:01.000Z>

(interval {:start 0, :period (seconds 1)})
=> #<Interval 1970-01-01T00:00:00.000Z/1970-01-01T00:00:01.000Z>
```

Both instant and partial intervals support a common set of operations on their
start/end (string representation of the interval is shortened for readability):

```clj
(def i (interval 0 10000))
=> #<Interval 00.000/10.000>

(move-start-to i (instant 5000))
=> #<Interval 05.000/10.000>

(move-end-to i (instant 5000))
=> #<Interval 00.000/05.000>

(move-start-by i (seconds 5))
=> #<Interval 05.000/10.000>

(move-end-by i (seconds 5))
=> #<Interval 05.000/15.000>

(move-end-by i (seconds 5))
=> #<Interval 05.000/15.000>
```

intervals can also be queried for several properties:

```clj
(start i)
=> #<DateTime 1970-01-01T00:00:00.000Z>

(end i)
=> #<DateTime 1970-01-01T00:00:10.000Z>

(contains? i (interval 2000 5000))
=> true

(contains? i (interval 0 15000))
=> false

(contains? (interval (date-time "2010") (date-time "2012"))
           (date-time "2011"))
=> true

(overlaps? (interval (date-time "2010") (date-time "2012"))
           (interval (date-time "2011") (date-time "2013")))
=> true

(abuts? (interval (date-time "2010") (date-time "2012"))
        (interval (date-time "2012") (date-time "2013")))
=> true
```

we can also calculate interval operations present in the Joda-Time:

```clj
(overlap (interval (date-time "2010") (date-time "2012"))
         (interval (date-time "2011") (date-time "2013")))
=> #<Interval 2011/2012>

(gap (interval (date-time "2010") (date-time "2012"))
     (interval (date-time "2013") (date-time "2015")))
=> #<Interval 2012/2013>
```

All of the above functions work with partial intervals the same way.

#### Timezones and Chronologies

Timezones can be constructed through the `timezone` function given the
(case-sensitive) timezone ID:

```clj
(timezone)
=> #<CachedDateTimeZone Europe/Vilnius>

(timezone "Europe/Vilnius")
=> #<CachedDateTimeZone Europe/Vilnius>

(timezone :UTC)
=> #<FixedDateTimeZone UTC>
```

Chronologies are constructed using `chronology` with a lower-case chronology
type and an optional timezone argument:

```clj
(chronology :coptic)
=> #<CopticChronology CopticChronology [Europe/Vilnius]>

(chronology :coptic :UTC)
=> #<CopticChronology CopticChronology [UTC]>

(chronology :iso (timezone :UTC))
=> #<ISOChronology ISOChronology [UTC]>
```

#### Formatters

Formatters (printers and parsers) are defined through the `formatter` function:

```clj
(formatter "yyyy-MM-dd")
=> #<DateTimeFormatter ...>
```

All of the ISO formatter defined by Joda-Time in the `ISODateTimeFormat` class
can be referenced by the appropriate keywords:

```clj
(formatter :date-time)
=> #<DateTimeFormatter ...>
```

Formatters may also be composed out of multiple patterns and other formatters:

```clj
(def fmt (formatter "yyyy/MM/dd" :date-time (formatter :date)))
=> #<DateTimeFormatter ...>
```

the resulting formatter will print according to the first pattern:

```clj
(print fmt (date-time "2010"))
=> "2010/01/01"
```

and parse all of the provided formats. Dates can be parsed from strings using
a family of `parse` functions:

```clj
(parse-date-time fmt "2010/01/01")
=> #<DateTime 2010-01-01T00:00:00.000+02:00>

(parse-mutable-date-time fmt "2010/01/01")
=> #<MutableDateTime 2010-01-01T00:00:00.000+02:00>

(parse-local-date fmt "2010/01/01")
=> #<LocalDate 2010-01-01>

(parse-local-date-time fmt "2010/01/01")
=> #<LocalDateTime 2010-01-01T00:00:00.000>

(parse-local-time fmt "2010/01/01")
=> #<LocalTime 00:00:00.000>
```

### Conversions

Joda-Time partials, instants and date-times can be converted back and forth
using the corresponding constructors:

```clj
(def now (date-time))
=> #<DateTime 2013-12-10T13:07:16.000+02:00>

(local-date now)
=> #<LocalDate 2013-12-10>

(local-date-time now)
=> #<LocalDateTime 2013-12-10T13:07:16.000>

(date-time (local-date now))
=> #<DateTime 2013-12-10T00:00:00.000+02:00>

(instant (local-date now))
=> #<Instant 2013-12-10T00:00:00.000Z>

(date-time (partial {:hourOfDay 12}))
=> #<DateTime 1970-01-01T12:00:00.000+03:00>
```

As you can see, conversions to date-time do not force the UTC timezone and set
the missing fields to the unix epoch. If we want to construct a date-time out
of a partial and fill the missing fields in another way, we could use the map
constructor:

```clj
(date-time {:partial (partial {:millisOfDay 1000}), :base now})
=> #<DateTime 2013-12-10T00:00:01.000+02:00>
```

You can customize date-time construction from partials by registering a custom
`InstantConverter` in the Joda `ConverterManager`.

We can also convert Joda date entities to native Java types:

```clj
(to-java-date now)
=> #inst "2013-12-10T11:07:16.000-00:00"

(type (to-sql-date now))
=> java.sql.Date

(to-sql-timestamp now)
=> #inst "2013-12-10T11:07:16.000000000-00:00"

(to-millis-from-epoch now)
=> 1386673636000
```

Of course, native Java types can be converted between themselves:

```clj
(to-java-date 1386673636000)
=> #inst "2013-12-10T11:07:16.000-00:00"

(to-java-date "2013-12-10")
=> #inst "2013-12-09T22:00:00.000-00:00"
```

Don't worry about the seemingly incorrect java date in the last example. We get
an `2013-12-09` *inst* out of a `2013-12-10` string because *inst* is printed
in the UTC timezone. We can check that everything is OK by converting back to
the Joda date-time:

```clj
(= (date-time 2013 12 10) (date-time (to-java-date "2013-12-10")))
=> true
```

Same with local dates:

```clj
(= (local-date-time 2013 12 10) (local-date-time (to-java-date "2013-12-10")))
=> true
```

Even more conversions:

```clj
(= now (date-time (local-date-time (to-java-date now))))
=> true
```

### Properties

Properties allow us to query and act on separate fields of date-times,
instants, partials and periods.

We can query single properties by using the `property` function:

```clj
(value (property (date-time "2010") :monthOfYear))
=> 1

(max-value (property (instant "2010") :monthOfYear))
=> 12

(min-value (property (partial {:monthOfYear 10}) :monthOfYear))
=> 1

(with-value (property (period {:years 10, :months 5}) :years) 15)
=> #<Period P15Y5M>
```

Property expressions read better when chained with threading macros:

```clj
(-> (date-time "2010") (property :monthOfYear) value)
=> 1
```

Clojure loves maps, so I've tried to produce a map interface to the most
commonly used Joda-time entities. Date-times, instants, partials and periods
can be converted into maps using the `properties` function which uses
`property` under the hood.  For example, a `DateTime` contains a whole bunch of
properties - one for every `DateTimeFieldType`:

```clj
(def props (properties (date-time)))
=> {:centuryOfEra #<Property Property[centuryOfEra]>, ...}

(keys props)
=> (:centuryOfEra :clockhourOfDay :clockhourOfHalfday
    :dayOfMonth :dayOfWeek :dayOfYear
    :era :halfdayOfDay :hourOfDay :hourOfHalfday
    :millisOfDay :millisOfSecond :minuteOfDay
    :minuteOfHour :monthOfYear :secondOfDay :secondOfMinute
    :weekOfWeekyear :weekyear :weekyearOfCentury
    :year :yearOfCentury :yearOfEra)
```

Now we can get the values for all of the fields (we'll cheat and use
`flatland.useful.map/map-vals`):

```clj
(useful/map-vals props value)
=> {:year 2013, :monthOfYear 12, :dayOfMonth 8,
    :yearOfCentury 13, :hourOfHalfday 9, :minuteOfDay 1305, ...}
```

although this is better achieved by calling `as-map` convenience function.

As you can see, map representations allow us to plug into the rich set of
operations on maps provided by Clojure and free us from using
`DateTimeFieldType` or `DurationFieldType` classes directly.

Partials contain a smaller set of properties, for example:

```clj
(-> (partial {:year 2013, :monthOfYear 12})
    properties
    (useful/map-vals value))
=> {:year 2013, :monthOfYear 12}
```

Properties allow us to perform a bunch of useful calculations, such as getting
the date for the last day of the current month:

```clj
(-> (date-time) (property :dayOfMonth) with-max-value)
```

or get the date for the first day:

```clj
(-> (date-time) (properties :dayOfMonth) with-min-value)
```

The above can also be done using the `joda-time.accessors` namespace which
defines a function for every possible date-time field supported by Joda-Time.

We can also solve a common problem of getting a sequence of dates for the last
day of month:

```clj
(iterate plus
         (-> (local-date) (property :dayOfMonth) with-max-value)
         (months 1))
=> (#<LocalDate 2013-12-31> #<LocalDate 2014-01-31> ...)
```

Note that `iterate` is defined in the `joda-time` namespace.

### Operations

One of the most useful parts of the Joda-Time library is it's rich set of
arithmetic operations allowed on the various time entities. You can sum periods
and durations together or add them to date-times, instants or partials.  You
can compute the difference of durations and periods or subtract them from
dates. You can also negate and compute absolute values of durations and
periods.

Here's an example of using a `plus` operation on a date-time:

```clj
(def now (date-time "2010-01-01"))
=> #<DateTime 2010-01-01T00:00:00.000+02:00>

(plus now (years 11))
=> #<DateTime 2021-01-01T00:00:00.000+02:00>

(def millis-10sec (* 10 1000))
=> 10000

(def duration-10sec (duration millis-10sec)
=> #<Duration PT10S>

(plus now (years 11) (months 10) (days 20) duration-10sec millis-10sec)
=> #<DateTime 2021-11-21T00:00:20.000+02:00>
```

same with instants:

```clj
(plus (instant "2010-01-01") (years 11))
=> #<Instant 2021-01-01T00:00:00.000Z>
```

with partials:

```clj
(def now (local-date 2010 1 1))
=> #<LocalDate 2010-01-01>

(plus now (years 11) (months 10) (days 20))
=> #<LocalDate 2021-11-21>
```

or with periods:

```clj
(def p (plus (years 10) (years 10) (months 10)))
=> #<Period P20Y10M>

(period-type p)
=> #<PeriodType PeriodType[StandardNoWeeksNoDaysNoHoursNoMinutesNoSecondsNoMillis]>
```

or with durations:

```clj
(plus (duration 1000) (duration 1000) 1000)
=> #<Duration PT3S>
```

Obviously, you can `minus` all the same things you can `plus`:

```clj
(minus now (years 11) (months 10))
=> #<LocalDate 1998-03-01>

(minus (duration 1000) (duration 1000) 1000)
=> #<Duration PT-1S>

(minus (years 10) (years 10) (months 10))
=> #<Period P-10M>
```

As you can see, durations and periods can become negative. Actually, we can
turn a positive period into a negative one by using `negate`:

```clj
(negate (years 10))
=> #<Years P-10Y>

(negate (duration 1000))
=> #<Duration PT-1S>
```

and we can take an absolute value of a period or a duration:

```clj
(abs (days -20))
=> #<Days P20D>

(abs (days 20))
=> #<Days P20D>

(abs (duration -1000))
=> #<Duration PT1S>
```

There is also a `merge` operation which is supported by periods and partials.
In case of a period, `merge` works like `plus`, only the values get overwritten
like when merging maps with `clojure.core/merge`:

```clj
(merge (period {:years 10, :months 6}) (years 20) (days 10))
=> #<Period P20Y6M10D>

(merge (local-date) (local-time))
=> #<Partial 2013-12-09T11:10:00.350>

(merge (local-date) (local-time) (partial {:era 0})
=> #<Partial [era=0, year=2013, monthOfYear=12, dayOfMonth=9,
              hourOfDay=11, minuteOfHour=10, secondOfMinute=5,
              millisOfSecond=429]>
```

Essentially, merging several partials or periods together is the same as
converting them to their map representations with `as-map`, merging maps and
converting the result back into a period/partial, only in a more efficient way.

It's important to note that operations on mutable Joda-Time entities aren't
supported. You are expected to chain methods through java interop.

## License

Copyright © 2013 Vadim Platonov

Distributed under the MIT License.
