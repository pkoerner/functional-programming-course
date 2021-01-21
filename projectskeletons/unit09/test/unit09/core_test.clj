(ns unit09.core-test
  (:require
   [clojure.test :refer :all]
   [unit09.core :refer :all]
   [clojure.test.check :as tc]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 9.2

(def ^:private one [[1]])

(def ^:private a
  [[1]
   [2 4]
   [5 1 4]
   [2 3 4 5]])

(def ^:private b
  [[3]
   [2 4]
   [1 9 3]
   [9 9 2 4]
   [4 6 6 7 8]
   [5 7 3 5 1 4]])

(def ^:private zero
  [[0]
   [9 0]
   [9 0 9]
   [9 0 5 1]
   [6 0 6 7 1]])

(def ^:private negative
  [[1]
   [0 -1]
   [0 9 -1]
   [0 1 0 -1]
   [0 6 0 0 -1]
   [0 0 4 0 0 -1]
   [0 0 0 0 5 0 -1]])

(def ^:private greedy
  [[1]
   [1 1]
   [9 9 1]
   [-9 2 2 1]
   [-9 3 3 3 1]
   [0 4 4 4 4 4]])

(deftest path-one-test
  (testing "(path one) should yield 1."
    (is (= 1 (path one)))))

(deftest path-1-test
  (testing "(path a) should yield 7."
    (is (= 7 (path a)))))

(deftest path-2-test
  (testing "(path b) should yield 20."
    (is (= 20 (path b)))))

(deftest path-zero-test
  (testing "(path zero) should yield 0."
    (is (= 0 (path zero)))))

(deftest path-negative-test
  (testing "(path negative) should yield -5."
    (is (= -5 (path negative)))))

(deftest path-greedy-test
  (testing "(path greedy) should yield -7."
    (is (= -7 (path greedy)))))

(def path-gen (gen/vector gen/nat 1 10))

(def triangle-values-gen
  (gen/bind
   path-gen
   (fn [path]
     (gen/tuple
      (gen/return path)
      (gen/vector (gen/large-integer* {:min (apply max path)})
                  (apply + (range (count path))))
      (gen/vector (gen/one-of [(gen/return :left)
                               (gen/return :right)])
                  (dec (count path)))))))

(def triangle-gen
  (gen/bind
   triangle-values-gen
   (fn [[path values left-or-right]]
     (loop [[a & ps]   (rest path) ; The path to weave into the triangle
            values     values      ; The values to fill the triangle with
            [lr & lrs] left-or-right ; Whether to move the path further right or left
            tri        [[(first path)]] ; The triangle
            left       0 ; The amount of values to the left of the path on the previous level
            right      0] ; The amount of values to the right of the path on the previous level
       (cond
         ;; Move the path to the right, by adding more values to the left
         (= lr :right)
         (let [new-left (inc left)
               row      (vec
                         (concat (take new-left values)
                                 [a]
                                 (take right values)))]
           (recur ps
                  (drop (+ new-left right) values)
                  lrs
                  (conj tri row)
                  new-left
                  right))
         ;; Move the path to the left, by adding more values to the right
         (= lr :left)
         (let [new-right (inc right)
               row       (vec
                          (concat (take left values)
                                  [a]
                                  (take new-right values)))]
           (recur ps
                  (drop (+ left new-right) values)
                  lrs
                  (conj tri row)
                  left
                  new-right))
         ;; The triangle is complete
         :else (gen/return [path tri]))))))


;; Generates a path `p` and corresponding triangle `t`
;; calls (path t)
;; finally checks if the sum of p is the same as the returned value
;;
;; The output of a failed test in the form of
;; {... :smallest [[p t]]  ...}
;; means that the value of (path `t`) was not equal to the sum of path `p`


(defspec generative-path-prop-test 15
  (prop/for-all [[p t] triangle-gen]
                (let [sum (path t)]
                  (= (apply + p) sum))))

;; 9.3

(deftest my-trampoline-simple-test
  (testing "A recursive function that returns its next recursion should not lead to a stack overflow."
    (letfn [(foo [x] (if (neg? x)
                       :done
                       (fn [] (foo (dec x)))))]
      (is (= :done (my-trampoline foo 500))))))

(deftest my-trampoline-1-test
  (testing "my-trampoline of example call 1."
    (letfn [(triple [x] (fn [] (sub-two (* 3 x))))
            (sub-two [x] (fn [] (stop? (- x 2))))
            (stop? [x] (if (> x 50)
                         x
                         (fn [] (triple x))))]
      (is (= 82 (my-trampoline triple 2))))))

(deftest my-trampoline-2-test
  (testing "my-trampoline of example call 2."
    (letfn [(my-even? [x] (if (zero? x)
                            true
                            (fn [] (my-odd? (dec x)))))
            (my-odd? [x] (if (zero? x)
                           false
                           (fn [] (my-even? (dec x)))))]
      (is (= [true false true false true false]
             (map (partial my-trampoline my-even?) (range 6)))))))