(ns unit08.core-test
  (:require
   [clojure.test :refer :all]
   [unit08.core :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 8.1

(deftest calc-plus-test
  (testing "(calc [] 1 + 1) should yield 2."
    (is (= 2 (calc [] 1 + 1)))))

(deftest calc-minus-test
  (testing "(calc [] 1563 - 63) should yield 1500."
    (is (= 1500 (calc [] 1563 - 63)))))

(deftest calc-mul-test
  (testing "(calc [] 13 * 11) should yield 143."
    (is (= 143 (calc [] 13 * 11)))))

(deftest calc-div-test
  (testing "(calc [] 90 / 10) should yield 9."
    (is (= 9 (calc [] 90 / 10)))))

(deftest calc-simple-binding-test
  (testing "(calc [a 3] a) should yield 3."
    (is (= 3 (calc [a 3] a)))))

(deftest calc-complex-bindings-test
  (testing "(calc [a 2 b 5 c 10] a * b / c) should yield 1"
    (is (= 1 (calc [a 2 b 5 c 10] a * b / c)))))

(deftest calc-example-1-test
  (testing "(calc [a 3] 3 + 5 * a) should yield 24"
    (is (= 24 (calc [a 3] 3 + 5 * a)))))

(deftest calc-example-2-test
  (testing "(calc [b 5 a 3] a * b + 3) should yield 18"
    (is (= 18 (calc [b 5 a 3] a * b + 3)))))

;; 8.2

(deftest dfa-match-empty-test
  (testing "\"\" should be accepted by the dfa."
    (is (= :accept (dfa-match z0 "")))))

(deftest dfa-match-1-test
  (testing "\"010100010\" (162) should be rejected by the dfa."
    (is (= :reject (dfa-match z0 "010100010")))))

(deftest dfa-match-2-test
  (testing "\"0101000101\" (325) should be accepted by the dfa."
    (is (= :accept (dfa-match z0 "0101000101")))))

(deftest dfa-match-long-test
  (testing (str "\"10110101111001100010000011110100100000000000101\""
                "(100000000000005) should be accepted by the dfa.")
    (is (= :accept
           (dfa-match z0 "10110101111001100010000011110100100000000000101")))))

(defn- nat->bits [n]
  (if (< n 2)
    [n]
    (reduce
     (fn [bits q] (conj bits (mod q 2)))
     '()
     (take-while pos? (iterate (fn [x] (quot x 2)) n)))))

;; Generates tuples of natural numbers and their
;; bit-representation as string [n bits].
;; Checks if (dfa-match `bits`) accepts bits if n % 5 == 0
;; or rejects it if n % 5 != 0
;;
;; The output of a failed test in the form of
;; {... :smallest [[n input]] ... }
;; means that (dfa-match `input`) did not yield the expected result
(defspec generative-dfa-match-prop-test 200
  (prop/for-all [[n input] (gen/fmap
                            (fn [n] [n (apply str (nat->bits n))])
                            gen/nat)]
                (let [expected (if (zero? (mod n 5)) :accept :reject)]
                  (= expected (dfa-match z0 input)))))