(ns joda-time.sugar-test
  (:require [clojure.test :refer :all]
            [joda-time :as j]))

(deftest calculates-number-of-duration-units
  (doseq [[duration-type handles-duration?]
          [['years false] ['months false] ['weeks false] ['days false]
           ; Duration is only handled by periods with duration types < days
           ['hours true] ['minutes true] ['seconds true] ['millis true]]]
    (let [ctor (symbol "joda-time.sugar" (str duration-type '-in))
          period-ctor (symbol "j" (str duration-type))]
      (eval `(testing ~(str "in " duration-type)
               (let [now# (j/date-time)
                     now-local# (j/local-date-time)]
                 (is (= (~ctor 1)
                        (~ctor (~period-ctor 1))
                        (~ctor (j/interval now#
                                           (j/plus now# (~period-ctor 1))))
                        (~ctor now# (j/plus now# (~period-ctor 1)))
                        (~ctor (j/partial-interval
                                 now-local#
                                 (j/plus now-local# (~period-ctor 1))))
                        (~ctor now-local# (j/plus now-local# (~period-ctor 1)))
                        ~(if handles-duration?
                           `(~ctor (j/duration {:start 0, :period (~period-ctor 1)}))
                           1)
                        1))))))))

(deftest number-of-duration-units-handles-partial-intervals
  (is (= 3 (j/years-in (j/partial-interval (j/local-date "2010") (j/local-date "2013")))))

  (is (= 3 (j/years-in (j/local-date "2010") (j/local-date "2013"))))
  (is (= 36 (j/months-in (j/local-date "2010") (j/local-date "2013"))))
  (is (= 36 (j/months-in (j/date-time "2010") (j/instant "2013")))))

(deftest recognizes-days-of-week
  (doseq [ctor [j/local-date j/local-date-time j/date-time j/instant]]
    (is (not (j/monday? (ctor "2010-12-21"))))
    (is (j/monday? (ctor "2010-12-20")))
    (is (j/tuesday? (ctor "2010-12-21")))
    (is (j/wednesday? (ctor "2010-12-22")))
    (is (j/thursday? (ctor "2010-12-23")))
    (is (j/friday? (ctor "2010-12-24")))
    (is (j/saturday? (ctor "2010-12-25")))
    (is (j/sunday? (ctor "2010-12-26")))

    (is (not (j/weekend? (ctor "2010-12-24"))))
    (is (j/weekend? (ctor "2010-12-25")))
    (is (j/weekend? (ctor "2010-12-26")))
    (is (not (j/weekday? (ctor "2010-12-26"))))
    (is (j/weekday? (ctor "2010-12-27"))))

  (is (thrown? IllegalArgumentException (j/monday? (j/local-time "10:30")))))
