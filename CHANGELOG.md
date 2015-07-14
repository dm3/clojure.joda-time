0.6.0

* Add `joda-time.accessors` namespace for accessors on property values, e.g.
  `year`, `min-year`, `max-year`, `with-min-year`, `with-max-year`.
* Remove property accessors from `joda-time.purgatory`. Their uses will have to
  be replaced with `joda-time.accessors/${property}-prop` functions.
* Add `weekend?`/`weekday?` predicates

0.5.0

* Upgrade Joda-time to 2.8.1
* Add multi-arity `date-time` and partial date constructor functions
* Add `in-zone` which calls `.withZoneRetainFields` on a `DateTime` putting the
  date-time in the specified zone without moving the clock

0.4.0

* Upgrade Joda-Time to 2.7

0.3.0

* Upgrade Joda-Time to 2.6

0.2.0

* Remove support for Clojure 1.2/1.3
* Update test dependencies
