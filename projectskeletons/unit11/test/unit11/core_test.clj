(ns unit11.core-test
  (:require
   [clojure.test :refer :all]
   [unit11.core :refer :all]
   [clojure.data :as d]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(deftest transplace-1-test
  (testing "(transduce (transplace {:y :a}) conj [:x :y :z]) should yield [:x :a :z]"
    (is (= [:x :a :z]
           (transduce (transplace {:y :a}) conj [:x :y :z])))))

(deftest transplace-2-test
  (testing "(transduce (comp (transplace {nil 0}) (map inc)) conj [42 nil 3]) shoudl yield [43 1 4]"
    (is (= [43 1 4]
           (transduce (comp (transplace {nil 0}) (map inc))
                      conj
                      [42 nil 3])))))

(deftest transplace-3-test
  (testing "(transduce (comp (transplace {nil -1}) (partition-by pos?)) conj [1 2 3 0 5 6]) should result in [[1 2 3] [0] [5 6]]"
    (is (= [[1 2 3] [0] [5 6]]
           (transduce (comp (transplace {nil -1}) (partition-by pos?))
                      conj
                      [1 2 3 0 5 6])))))

(deftest transplace-empty-test
  (testing "Transplacing on an empty collection should yield the default value"
    (is (= 0 (transduce (transplace {nil :nil}) + [])))))

(deftest transplace-one-element-test
  (testing "Transplacing on a collection of size 1 should yield that value"
    (is (= 1 (transduce (transplace {:one :two}) + [1])))))

(deftest transplace-add-test
  (testing "Transplacing negative with positive numbers and adding them should yield a positive number"
    (is (= 6 (transduce (transplace {-1 1 -2 2 -3 3}) + [-1 -2 -3])))))

(deftest transplace-mult-test
  (testing "Transplacing 0 with 1 in a string of multiplications should yield a number larger than 0"
    (is (= 500 (transduce (transplace {0 1}) * [100 0 5])))))

(def transplace-generator
  (gen/bind (gen/vector gen/simple-type-printable-equatable 10 500)
            (fn [k]
              (gen/tuple
               (gen/not-empty
                ;; Generate a map to replace some elements within k
                (gen/map (gen/elements k)
                         ;; Ensure elements are replaced with ones distinct
                         ;; from the orignal collection
                         (gen/such-that
                          (complement (partial contains? (set k)))
                          gen/simple-type-printable-equatable)))
               (gen/shuffle k)))))


;; Generates tuples a sequence and a map of elements of that sequence and
;; elements distinct from the ones in the sequence
;; Calls (transduce (transplace m) conj c) and verifies that
;; the replacement of the keys and values was correct
;;
;; The output of a failed test in the form of
;; {... :smallest [[m c]] ... }
;; means that (transduce (transplace m) conj c) did not replace 
;; the keys of m correctly


(defspec generative-prop-test 10
  (prop/for-all [[m c] transplace-generator]
                (let [t (transduce (transplace m) conj c)
                      [in-c in-t in-both] (map (partial remove nil?)
                                               (d/diff c t))
                      old (set (keys m))
                      new (set (vals m))]
                  (is (and
                       ;; Where no additional values added or lost?
                       (= (count c) (count t))
                       ;; Where the old values replaced?
                       (every? (partial contains? old) in-c)
                       ;; Where the new values swapped in?
                       (every? (partial contains? new) in-t)
                       ;; Where the other values left untouched?
                       (= (remove (partial contains? old) c) in-both))))))

;: 11.2

(deftest to-bits-0-test
  (testing "(to-bits 0) should yield [0]."
    (is (= [0] (to-bits 0)))))

(deftest to-bits-1-test
  (testing "(to-bits 1) should yield [1]."
    (is (= [1] (to-bits 1)))))

(deftest to-bits-4-test
  (testing "(to-bits 4) should yield [1 0 0]."
    (is (= [1 0 0] (to-bits 4)))))

(deftest to-bits-6-test
  (testing "(to-bits 6) should yield [1 1 0]."
    (is (= [1 1 0] (to-bits 6)))))

(deftest to-bits-15-test
  (testing "(to-bits 15) should yield [1 1 1 1]."
    (is (= [1 1 1 1] (to-bits 15)))))

(defn- bits->int [bits]
  (first
   (reduce (fn [[sum exp] base] [(+' sum (*' exp base)) (+' exp exp)])
           [0 1]
           (reverse bits))))

;; Generates a natural number `n` calls to-bits with it
;; and converts the result back to a decimal representation
;; finally checks if the initial number and re-converted number are the same
;;
;; The output of a failed test in the form of
;; {... :smallest [n1] ... :fail [n2] ...}
;; means that the input n1 and n2 did not yield the expected result
(defspec generative-to-bits-prop-test 100
  (prop/for-all [n gen/nat]
                (let [o (bits->int (to-bits n))]
                  (= n o))))

(deftest squares-1-0-test
  (testing "(squares 1 0) should yield []."
    (is (= [] (squares 1 0)))))

(deftest squares-0-1-test
  (testing "(squares 0 1) should yield [0]."
    (is (= [0] (squares 0 1)))))

(deftest squares-1-2-test
  (testing "(squares 1 2) should yield [1 1]."
    (is (= [1 1] (squares 1 2)))))

(deftest squares-2-8-test
  (testing "(squares 2 8) should yield [2 4 16 256]."
    (is (= [2 4 16 256] (squares 2 8)))))

(deftest squares-4-4-test
  (testing "(squares 4 4) should yield [4 16 256]."
    (is (= [4 16 256] (squares 4 4)))))

(deftest squares-5-4-test
  (testing "(squares 5 4) should yield [5 25 625]."
    (is (= [5 25 625] (squares 5 4)))))

(defn- exp->zero-vector-size
  ([exp] (exp->zero-vector-size exp 1 1))
  ([exp vs size]
   (if (< exp (+ vs vs))
     size
     (recur exp (+ vs vs) (inc size)))))

(defn- squares? [[a b & _ :as sqs] base exp]
  (cond
    (= base 0) (= (repeat (exp->zero-vector-size exp) 0) sqs)
    (= exp 1) (= [base] sqs)
    (and a b) (when (and (not (zero? a))
                         (= a (/ b a)))
                (recur (rest sqs) base exp))
    :else true))

;; Generates a natural number `b` and a natural number greater than one `e`
;; calls (squares b e)
;; and finally checks if they are a sequence of squares via the squares?-fn
;;
;; The output of a failed test in the form of
;; {... :smallest [[b1 e1]] ... :fail [[b2 e2]] ...}
;; means that the calls (squares b1 e1) and (squares b2 e2)
;; did not yield the expected results
(defspec generative-squares-prop-test 100
  (prop/for-all [[b e] (gen/tuple gen/nat
                                  (gen/fmap inc gen/nat))]
                (squares? (squares b e) b e)))

(deftest square-and-multiply-0-0-test
  (testing "(square-and-multiply 0 0) should yield 1."
    (is (= 1 (square-and-multiply 0 0)))))

(deftest square-and-multiply-10-3-test
  (testing "(square-and-multiply 10 3) should yield 1000."
    (is (= 1000 (square-and-multiply 10 3)))))

(deftest square-and-multiply-4-2-test
  (testing "(square-and-multiply 4 2) should yield 16."
    (is (= 16 (square-and-multiply 4 2)))))

(deftest square-and-multiply-large-test
  (testing "(square-and-multiply 10 20) should yield 100000000000000000000N."
    (is (= 100000000000000000000N (square-and-multiply 10 20)))))

;; Generates a natural number `b` and a natural number greater than zero `e`
;; calls (square-and-multiply b e)
;; and finally compares the result to a slower method of exponentiation
;;
;; The output of a failed test in the form of
;; {... :smallest [[b e]] ...}
;; means that the calls (square-and-multiply b e) and
;; did not yield the expected results
(defspec generative-square-and-multiply-prop-test 100
  (prop/for-all [[b e] (gen/tuple gen/nat
                                  (gen/fmap inc gen/nat))]
                (= (reduce *' (repeat e b))
                   (square-and-multiply b e))))

;; 11.3

(deftest hamilton-c3-test
  (testing "A graph that is a cycle contains a hamiltonian-path."
    (is (hamilton [[:a :b] [:b :c] [:c :d]]
                      #{:a :b :c}
                      :a))))

(deftest hamilton-c3-w-loop-test
  (testing (str "A graph that is a cycle with an additional loop "
                "contains a hamiltonian-path.")
    (is (hamilton [[:a :a] [:a :b] [:b :c] [:c :d]]
                      #{:a :b :c}
                      :a))))

(deftest hamilton-c4-test
  (testing "A graph that is a cycle of 4 verteces contains a hamiltonian-path."
    (is (hamilton [[:a :a] [:c :d] [:a :b] [:a :d] [:b :c]]
                      #{:a :b :c :d}
                      :a))))

(deftest hamilton-no-hamilton-test
  (testing (str "hamilton should return nil on a graph that contains "
                "no hamiltonian-path.")
    (is (not (hamilton [[:a :a] [:d :e] [:a :d] [:b :c] [:a :b] [:c :d]]
                       #{:a :b :c :d}
                       :b)))))

(deftest hamilton-edgless-test
  (testing "hamilton should return nil on a graph that contains no edges but multiple verteces."
    (is (not (hamilton []
                       #{:a :c :b :d :e}
                       :a)))))

(deftest hamilton-s5-test
  (testing "hamilton should return nil on the s5."
    (is (not (hamilton [[:a :b] [:a :c] [:a :d] [:a :e]]
                       #{:a :b :c :d :e}
                       :a)))))