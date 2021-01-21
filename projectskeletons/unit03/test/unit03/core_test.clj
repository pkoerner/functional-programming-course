(ns unit03.core-test
  (:require
   [clojure.test :refer :all]
   [unit03.core :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(def ^:private epsilon 1e-5)

(defn abs-diff [x y]
  (let [z (- x y)]
    (max z (- z))))

(defn- approximately? [a b]
  (> epsilon (abs-diff a b)))

;; 3.1

(deftest newton-linear-test
  (testing "newton: f=x, f'=1 should yield approximately 0."
    (let [newt (newton
                (fn [x] x)
                (fn [x] 1))]
      (is (approximately? 0 (newt 10 epsilon))))))

(deftest newton-quadratic-test
  (testing "newton: f=x^2-9, f'=2x should yield approximately 3."
    (let [newt (newton
                (fn [x] (- (* x x) 9))
                (fn [x] (* 2 x)))]
      (is (approximately? 3 (newt 1 epsilon))))))

(deftest newton-cubic-test
  (testing "newton: f=x^3+8, f'=3x^2 should yield approximately -2."
    (let [newt (newton
                (fn [x] (+ (* x x x) 8))
                (fn [x] (* 3 x x)))]
      (is (approximately? -2 (newt -7 epsilon))))))

(deftest sqrt-0-test
  (testing "(sqrt 0) should yield approximately  0."
    (is (approximately? 0 (sqrt 0)))))

(deftest sqrt-1-test
  (testing "(sqrt 1) should yield approximately 1."
    (is (approximately? 1 (sqrt 1)))))

(deftest sqrt-4-test
  (testing "(sqrt 4) should yield approximately 2."
    (is (approximately? 2 (sqrt 4)))))

(deftest sqrt-9-test
  (testing "(sqrt 9) should yield approximately 3."
    (is (approximately? 3 (sqrt 9)))))

(deftest sqrt-100-test
  (testing "(sqrt 100) should yield approximately 10."
    (is (approximately? 10 (sqrt 100)))))

;; Generates natural numbers and checks if the square root of the square
;; of the number yields the original number (approximately) 
;;
;; The output of a failed test in the form of
;; {... :smallest [n] ... }
;; means that `(sqrt (* n n))` did not yield `n` as expected
(defspec sqrt-prop-test 100
  (prop/for-all [n gen/nat]
                (approximately? n (sqrt (* n n)))))

;; 3.2

(deftest flip-nth-test
  (testing "((flip nth) 2 [3 4 5 6]) should yield 5."
    (is (= 5 ((flip nth) 2 [3 4 5 6])))))

(deftest flip-sub-test
  (testing "((flip -) 1 2 3) should yield 0."
    (is (= 0 ((flip -) 1 2 3)))))

(deftest flip-drop-test
  (testing "((flip drop) [4 5 6 7] 2) should yield [6 7]."
    (is (= [6 7] ((flip drop) [4 5 6 7] 2)))))

(deftest flip-str-test
  (testing "((flip str) \"!\" \"World\" \", \" \"Hello\") should yield \"Hello, World!\"."
    (is (= "Hello, World!" ((flip str) "!" "World" ", " "Hello")))))

(deftest flip-conj-no-args-test
  (testing "((flip conj)) should yield []."
    (is (= [] ((flip conj))))))

(deftest comp-1-test
  (testing "((mycomp inc) 4) should yield 5."
    (is (= 1 ((mycomp inc) 0)))))

(deftest comp-2-test
  (testing "((mycomp inc (fn [x] (* x x))) 4) should yield 17."
    (is (= 17 ((mycomp inc (fn [x] (* x x))) 4)))))

(deftest comp-3-test
  (testing "((mycomp (fn [x] (* x x)) inc) 4) should yield 25."
    (is (= 25 ((mycomp (fn [x] (* x x)) inc) 4)))))

(deftest comp-4-test
  (testing "((mycomp inc dec) 4) should yield 4."
    (is (= 4 ((mycomp inc dec) 4)))))

(deftest comp-5-test
  (testing "((mycomp (partial * 2) (fn [c] (reduce * c)) (fn [c] (map inc c)))) [0 1 2 3 4]) should yield 150."
    (is (= 150 ((mycomp (partial + 30)
                        (fn [c] (reduce * c))
                        (fn [c] (map inc c)))
                [0 1 2 3 4])))))

(deftest myjuxt-1-test
  (testing "((myjuxt inc) 99) should yield [100]"
    (is (= [100] ((myjuxt inc) 99)))))

(deftest myjuxt-2-test
  (testing "((myjuxt inc dec (fn [x] (* x x))) 3) should yield [4 2 9]"
    (is (= [4 2 9] ((myjuxt inc dec (fn [x] (* x x))) 3)))))

(deftest myjuxt-3-test
  (testing "((myjuxt vec count identity) \"clojure\") should yield [[\\c \\l \\o \\j \\u \\r \\e] 7 \"clojure\"]."
    (is (= [[\c \l \o \j \u \r \e] 7 "clojure"]
           ((myjuxt vec count identity) "clojure")))))

(deftest myjuxt-multiple-arguments-test
  (testing "Calling the function produced by myjuxt with multiple arguments should pass all of them to the given functions."
    (is (= ["(), [], {}, #{}, false, , true" false true]
           ((myjuxt (fn [& args] (clojure.string/join ", " args))
                    (fn [& args] (every? true? args))
                    (fn [& args] (some true? args)))
            '() [] {} #{} false nil true)))))

;; 3.3

(deftest myevery-1-test
  (testing "(myevery even? [1 2 3 4]) should yield false"
    (is (not (myevery? even? [1 2 3 4])))))

(deftest myevery-2-test
  (testing "(myevery even? [2 4]) should yield true"
    (is (myevery? even? [2 4]))))

(deftest myevery-fn-test
  (testing "(myevery? fn? [inc + - (fn []) even?]) should yield true"
    (is (myevery? fn? [inc + - (fn []) even?]))))

(deftest myevery-empty-test
  (testing "A myevery?-call with an empty collection should trivially yield true"
    (is (myevery? :key []))))

(deftest myevery-set-test
  (testing "(myevery? #{1 2 3} [1 2 :a]) should yield false"
    (is (not (myevery? #{1 2 3} [1 2 :a])))))