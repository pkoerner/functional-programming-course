(ns unit01.core-test
  (:require
   [clojure.test :refer :all]
   [unit01.core :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 1.1
(defn- decrementing? [[a b & _ :as coll]]
  (if b
    (and (= 1 (- a b)) (recur (rest coll)))
    true))

(deftest seq-a-test
  (testing "Sequence of all integers from 100 to -100 in descending order."
    (is (and (= [100 -100] [(first seq-a) (last seq-a)])
             (decrementing? seq-a)))))

(deftest seq-b-test
  (testing "Sequence of all square numbers from 0 to 1000."
    (is (= (range 32)
           (map #(int (Math/sqrt %)) seq-b)))))

(deftest seq-c-test
  (testing "Sequence of all numbers from 0 to 1000 that are not evenly divisible by 3."
    (is (and (= 667 (count (distinct seq-c)))
             (every? pos? (map #(mod % 3) seq-c))))))

(defn- square-tuple? [[n m]]
  (let [n-squared (* n n)]
    (and (< 0 n 1000)
         (> m n-squared)
         (<= (dec m) n-squared))))

(deftest seq-d-test
  (testing "Sequence of tuples [n, m] such that 0 < n < 1000 and n^2 < m and m minimal."
    (is (and (= 999 (count (distinct seq-d)))
             (every? square-tuple? seq-d)))))

(defn- ascending-palindrome? [[a b c d e]]
  (and (= a e)
       (= b d)
       (< a b c)))

(deftest seq-e-test
  (testing "Sequence of 5-tuples of numbers that are palindromes and ascending."
    (is (and (= 120 (count (distinct seq-e)))
             (every? #(and (= 5 (count %))
                           (ascending-palindrome? %))
                     seq-e)))))

;; 1.2

(deftest fibonacci-0-test
  (testing "(fibonacci 0) should yield []."
    (is (= [] (fibonacci 0)))))

(deftest fibonacci-1-test
  (testing "(fibonacci 1) should yield [0]."
    (is (= [0] (fibonacci 1)))))

(deftest fibonacci-2-test
  (testing "(fibonacci 2) should yield [0 1]."
    (is (= [0 1] (fibonacci 2)))))

(deftest fibonacci-6-test
  (testing "(fibonacci 6) should yield [0 1 1 2 3 5]."
    (is (= [0 1 1 2 3 5] (fibonacci 6)))))

(deftest fibonacci-100-test
  (testing "(last (fibonacci 100)) should yield 218922995834555169026N."
    (is (= 218922995834555169026N (last (fibonacci 100))))))

(defn- fib-sums [[a b & _ :as fib-seq] sum]
  (if (< 2 (count fib-seq))
    (recur (rest fib-seq) (conj sum (+' a b)))
    sum))

(defn- fib-seq? [fib-seq]
  (cond
    (< (count fib-seq) 1)  (empty? fib-seq)
    (= 1 (count fib-seq))  (= [0] fib-seq)
    :else (and (= [0 1] (take 2 fib-seq)) ; The first two numbers in the fib-seq are correct
               (= (fib-sums fib-seq []) (nthrest fib-seq 2))))) ; The rest add up

;; Generates natural numbers up to 1000 and checks that the sequence
;; returned by fibonacci follows the fibonacci formula
;;
;; The output of a failed test in the form of
;; {... :smallest [z] ... }
;; means that `(fibonacci z)` did not yield a correct sequence
(defspec fibonacci-sequence-property-test 50
  (prop/for-all [z (gen/fmap (partial min 1000) gen/nat)]
                (let [fib-seq (fibonacci z)]
                  (and (= z (count fib-seq))
                       (fib-seq? fib-seq)))))

;; 1.4

(deftest remove-duplicates-empty-test
  (testing "remove-duplicates on an empty seq should yield an empty seq."
    (is (= [] (remove-duplicates [])))))

(deftest remove-no-duplicates-test
  (testing "remove-duplicates on a duplicate-less seq should yield the seq itself."
    (is (= [0 1 2 3 4 5 6 7 8 9] (remove-duplicates [0 1 2 3 4 5 6 7 8 9])))))

(deftest remove-only-duplicates-test
  (testing "(remove-duplicates [\"a\" \"a\" \"a\" \"a\"]) should yield [\"a\"]."
    (is (= ["a"] (remove-duplicates ["a" "a" "a" "a"])))))

(deftest remove-duplicates-heterogeneous-test
  (testing "(remove-duplicates [\"a\" 1 \"1\" -103 1 2 \"b\" \"ab\" \"a\" :b]) should yield [\"a\" 1 \"1\" -103 2 \"b\" \"ab\" :b]."
    (is (= ["a" 1 "1" -103 2 "b" "ab" :b]
           (remove-duplicates ["a" 1 "1" -103 1 2 "b" "ab" "a" :b])))))

;; Generates non-empty sequences of equatables and ensures that all
;; unique elements were kept in the same order they appeared in the
;; original sequence
;;
;; The output of a failed test in the form of
;; {... :smallest [v] ... }
;; means that `(remove-duplicates v)` either removed a unique element,
;; left a duplicate or changed the order of the elements from v
(defspec remove-duplicates-property-test 25
  (prop/for-all [v (gen/not-empty (gen/vector gen/simple-type-equatable))]
                (= (distinct v)
                   (remove-duplicates v))))