(ns unit06.core-test
  (:require
   [clojure.test :refer :all]
   [unit06.core :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 6.1

(deftest rev-interleave-1-test
  (testing "(rev-interleave [1 2 3 4 5 6] 2) should yield [[1 3 5] [2 4 6]]."
    (is (= [[1 3 5] [2 4 6]] (rev-interleave [1 2 3 4 5 6] 2)))))

(deftest rev-interleave-2-test
  (testing "(rev-interleave [1 2 3 4 5 6] 3) should yield [[1 4] [2 5] [3 6]]."
    (is (= [[1 4] [2 5] [3 6]] (rev-interleave [1 2 3 4 5 6] 3)))))

(deftest rev-interleave-3-test
  (testing "(rev-interleave [1 2 3 4 5 6] 6) should yield [[1] [2] [3] [4] [5] [6]]."
    (is (= [[1] [2] [3] [4] [5] [6]] (rev-interleave [1 2 3 4 5 6] 6)))))

(defn- factors [n]
  (filter (fn [i] (zero? (mod n i)))
          (range 1 (inc n))))

(def ^:private rev-interleave-input-gen
  (gen/bind (gen/not-empty (gen/vector-distinct gen/nat))
            (fn [vec]
              (gen/tuple
               (gen/return vec)
               (gen/fmap first (gen/shuffle (factors (count vec))))))))

;; Generates finite sequences and chooses a factor of its length
;; Calls interleave with both of these and checks that the seqeuence was split
;; into the correct number of sequences and applying interleave returns the
;; original sequence
;;
;; The output of a failed test in the form of
;; {... :smallest [[s n]] ... }
;; means that `(rev-interleave s n)` did not split the sequence correctly
(defspec generative-rev-interleave-prop-test 30
  (prop/for-all [[s n] rev-interleave-input-gen]
                (let [output (rev-interleave s n)]
                  (and
                   ;; The output was split into the correct amount of seqs
                   (= n (count output))
                   ;; The output was split in the correct way
                   (= s (apply interleave output))))))

(deftest my-flatten-empty-test
  (testing "(my-flatten []) should have no effect."
    (is (= [] (my-flatten [])))))

(deftest my-flatten-single-element-test
  (testing "(my-flatten [1]) should have no effect."
    (is (= [1] (my-flatten [1])))))

(deftest my-flatten-single-nested-test
  (testing "(my-flatten [1 2 3 [4]]) should yield [1 2 3 4]."
    (is (= [1 2 3 4] (my-flatten [1 2 3 [4]])))))

(deftest my-flatten-multiple-nested-test
  (testing "(my-flatten [1 2 3 [4] [5 6] [7]]) should yield [1 2 3 4 5 6 7]."
    (is (= [1 2 3 4 5 6 7] (my-flatten [1 2 3 [4] [5 6] [7]])))))

(deftest my-flatten-multiple-nest-levels-test
  (testing "(my-flatten [1 2 3 [4 [5 6] [7 [8]]] 9]) should yield [1 2 3 4 5 6 7 8 9]."
    (is (= [1 2 3 4 5 6 7 8 9] (my-flatten [1 2 3 [4 [5 6] [7 [8]]] 9])))))

(deftest my-flatten-1-test
  (testing "(my-flatten '[[1 2] 3 (4 [5 6])) should yield [1 2 3 4 5 6]."
    (is (= [1 2 3 4 5 6] (my-flatten '[[1 2] 3 (4 [5 6])])))))

(deftest my-flatten-2-test
  (testing "(my-flatten [\"a\" [\"b\"] \"c\"])) should yield [\"a\" \"b\" \"c\"]."
    (is (= ["a" "b" "c"] (my-flatten ["a" ["b"] "c"])))))

(deftest my-flatten-3-test
  (testing "(my-flatten [1 [[[[[[[[[[[[2]]]]]]]]]]]]])) should yield [1 2]."
    (is (= [1 2] (my-flatten [1 [[[[[[[[[[[[2]]]]]]]]]]]]])))))

(deftest my-flatten-4-test
  (testing "(my-flatten [:a #{:b}]) should yield [:a :b]."
    (is (= [:a :b] (my-flatten [:a #{:b}])))))

(defn- seq-container-gen-fn [gen]
  (gen/one-of [(gen/not-empty (gen/vector-distinct gen))
               (gen/not-empty (gen/list-distinct gen))]))

;; Generates possibly nested sequences (note: no sets or maps)
;; and compares the custom implementation to flatten in the standard library
;;
;; The output of a failed test in the form of
;; {... :smallest [input] ... }
;; means that the behaviour of `(my-flatten input)` differs from flatten
(defspec generative-flatten-prop-test 20
  (prop/for-all [input (seq-container-gen-fn
                        (gen/recursive-gen seq-container-gen-fn gen/nat))]
                (= (flatten input) (my-flatten input))))

;; 6.2

(deftest collatz-1-test
  (testing "(collatz 1) should yield [1]."
    (is (= [1] (collatz 1)))))

(deftest collatz-2-test
  (testing "(collatz 2) should yield [2 1]."
    (is (= [2 1] (collatz 2)))))

(deftest collatz-11-test
  (testing "(collatz 11) should yield [11 34 17 52 26 13 40 20 10 5 16 8 4 2 1]."
    (is (= [11 34 17 52 26 13 40 20 10 5 16 8 4 2 1] (collatz 11)))))

(deftest collatz-12-test
  (testing "(collatz 12) should yield [12 6 3 10 5 16 8 4 2 1]."
    (is (= [12 6 3 10 5 16 8 4 2 1] (collatz 12)))))

(deftest collatz-27-test
  (testing "(collatz 27) should yield a sequence of 112 numbers."
    (is (= [27 82 41 124 62 31 94 47 142 71 214 107 322
            161 484 242 121 364 182 91 274 137 412 206 103
            310 155 466 233 700 350 175 526 263 790 395 1186
            593 1780 890 445 1336 668 334 167 502 251 754 377
            1132 566 283 850 425 1276 638 319 958 479 1438 719
            2158 1079 3238 1619 4858 2429 7288 3644 1822 911 2734
            1367 4102 2051 6154 3077 9232 4616 2308 1154 577 1732
            866 433 1300 650 325 976 488 244 122 61 184 92 46 23
            70 35 106 53 160 80 40 20 10 5 16 8 4 2 1]
           (collatz 27)))))

(defn- collatz? [n [c & cs]]
  (cond
    (and (= n c 1) (empty? cs)) true
    (and (even? n) (= n c)) (recur (/ n 2) cs)
    (and (odd? n) (= n c)) (recur (inc (* 3 n)) cs)))

;; Generates a natural number, calls collatz and verifies that the sequence
;; follows the collatz-formula
;;
;; The output of a failed test in the form of
;; {... :smallest [n] ... }
;; means that the sequence returned by `(collatz n)` is incorrect
(defspec generative-collatz-prop-test 50
  (prop/for-all [n (gen/fmap inc gen/nat)]
                (collatz? n (collatz n))))

;; 6.3

(deftest combinations-1-2-test
  (testing "(combinations [1 2]) should yield #{[1 2]}."
    (is (= #{[1 2]} (combinations [1 2])))))

(deftest combinations-1-2-3-test
  (testing "(combinations [1 2 3]) should yield #{[1 2] [1 3] [2 3]}."
    (is (= #{[1 2] [1 3] [2 3]} (combinations [1 2 3])))))

(deftest combinations-1-2-3-4-test
  (testing "(combinations [1 2 3 4]) should yield #{[1 2] [1 3] [1 4] [2 3] [2 4] [3 4]}."
    (is (= #{[1 2] [1 3] [1 4] [2 3] [2 4] [3 4]} (combinations [1 2 3 4])))))

(defn- factorial
  "Calculates the factorial of `n`."
  [n]
  (reduce *' (range 1 (inc' n))))

(defn- binomial-coefficient-2
  "Calculates the binomial coefficient `n` choose 2."
  [n]
  (quot (factorial n)
        (*' 2 (factorial (-' n 2)))))

(defn- all-pairs?
  "Returns `true` if `pairs` is a collection of all possible pairs in `coll`."
  [coll pairs]
  (let [nr-of-expected-combinations (binomial-coefficient-2 (count coll))
        freq (frequencies (flatten (vec pairs)))]
    (and
     (= nr-of-expected-combinations (count pairs))
     ;; All elements appear in (n-1) tuples
     (apply = (dec (count coll)) (vals freq))
     ;; All (and only) elements from the original coll appear
     (= (set (keys freq)) (set coll)))))

;; Generates a range of at least 3 elements, calls combinations and
;; verifies that the number of tuples is correct, as well as
;; the number of times individual elements appear is correct
;;
;; The output of a failed test in the form of
;; {... :smallest [v] ... }
;; means that `(combinations v)` returned too many or too few tuples
(defspec generative-combinations-prop-test 35
  (prop/for-all [v (gen/fmap #(range (+ 2 %)) gen/nat)]
                (all-pairs? v (combinations v))))