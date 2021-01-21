(ns unit02.core-test
  (:require
   [clojure.test :refer :all]
   [unit02.core :refer :all]
   [clojure.string :as s]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 2.2

(deftest max-test
  (testing "(max-value 3 42 1336 12.5) should yield 1336"
    (is (= 1336 (max-value 3 42 1336 12.5)))))

(deftest max-value-positive-test
  (testing "(max-value 1 3 2) should yield 3."
    (is (= 3 (max-value 1 3 2)))))

(deftest max-value-negative-test
  (testing "(max-value -100 -3 -5 -12 -1) should yield -1."
    (is (= -1 (max-value -100 -3 -5 -12 -1)))))

(deftest max-value-mixed-sign-test
  (testing "(max-value 99 -12 -100 -1 0 1234 5) should yield 1234."
    (is (= 1234 (max-value 99 -12 -100 -1 0 1234 5)))))

(deftest max-value-non-distinct-test
  (testing "(max-value 0 0 0 0 0) should yield 0."
    (is (zero? (max-value 0 0 0 0 0)))))

(def ^:private max-coll-gen
  (gen/bind gen/large-integer
            (fn [max]
              (gen/tuple
               (gen/return max)
               (gen/fmap
                #(conj % max)
                (gen/vector (gen/large-integer* {:max max})))))))

(def ^:private max-value-input-gen
  (gen/bind max-coll-gen
            (fn [[max coll]]
              (gen/tuple
               (gen/return max)
               (gen/shuffle coll)))))

;; Generates random numbers and checks that the return value of max-value
;; is the actual max
;;
;; The output of a failed test in the form of
;; {... :smallest [[max input]] ... } where input is a sequence
;; means that `(apply max-value input)` did not yield maxima `max` as expected
(defspec max-value-prop-test 30
  (prop/for-all [[max input] max-value-input-gen]
                (= max (apply max-value input))))

(deftest longest-empty-test
  (testing "(longest []) should yield []."
    (is (= [] (longest [])))))

(deftest longest-multiple-empty-test
  (testing "(longest [] [] []) should yield []."
    (is (= [] (longest [] [] [])))))

(deftest longest-multiple-same-length-test
  (testing "(longest [1 2 3] [\"1\" \"2\" \"3\"] [:a :b :c]) should yield [1 2 3]."
    (is (= [1 2 3] (longest [1 2 3] ["1" "2" "3"] [:a :b :c])))))

(deftest longest-nested-test
  (testing "(longest [] [:a :b 12] [[1 2 3 4 5 6]])) should yield [:a :b 12]."
    (is (= [:a :b 12] (longest [] [:a :b 12] [[1 2 3 4 5 6]])))))

(def ^:private vector-tuple-gen
  (gen/bind (gen/fmap inc gen/nat)
            (fn [n]
              (gen/tuple
               ; Generate some smaller colls before the longest
               (gen/vector (gen/vector gen/any-printable 0 (dec n)))
               ; The first longest
               (gen/vector gen/any-printable n)
               ; The rest
               (gen/vector (gen/vector gen/any-printable 0 n))))))

(def ^:private longest-input-gen
  (gen/bind vector-tuple-gen
            (fn [[bef max aft]]
              (gen/tuple
               (gen/return max)
               (gen/return (concat bef [max] aft))))))

;; Generates random-length sequences in a sequence, calls longest and
;; checks whether or not the longest sequence out of the generated
;; sequences was returned
;;
;; The output of a failed test in the form of
;; {... :smallest [[expected input]] ... }
;; means that `(apply longest input)` did not yield `expected` as the longest
;; sequence
(defspec longest-prop-test 10
  (prop/for-all [[expected input] longest-input-gen]
                (= expected (apply longest input))))

(deftest max-length-empty-test
  (testing "(max-length []) should yield 0."
    (is (zero? (max-length [])))))

(deftest max-length-multiple-empty-test
  (testing "(max-length [] [] []) should yield 0."
    (is (zero? (max-length [] [] [])))))

(deftest max-length-multiple-same-length-test
  (testing "(max-length [1 2 3] [\"1\" \"2\" \"3\"] [:a :b :c]) should yield 3."
    (is (= 3 (max-length [1 2 3] ["1" "2" "3"] [:a :b :c])))))

(deftest max-length-nested-test
  (testing "(max-length [[[] [] [] []]] [:a :b 12] [[1 2 3 4 5 6]])) should yield 3."
    (is (= 3 (max-length [[[] [] [] []]] [:a :b 12] [[1 2 3 4 5 6]])))))

(def ^:private max-length-input-gen
  (gen/bind vector-tuple-gen
            (fn [[bef max aft]]
              (gen/tuple
               (gen/return (count max))
               (gen/return (concat bef [max] aft))))))

;; Generates random-length sequences in a sequence, calls max-length and
;; checks whether or not the size of longest sequence out of the generated
;; sequences was returned
;;
;; The output of a failed test in the form of
;; {... :smallest [[expected input]] ... }
;; means that `(apply max-length input)` did not yield `expected` as the size
;; of the longest sequence
(defspec max-length-prop-test 10
  (prop/for-all [[expected input] max-length-input-gen]
                (= expected (apply max-length input))))

;; 2.3


(deftest p-identity-matrix-test
  (testing "Testing p! with the identity matrix."
    (let [m [[1 0 0] [0 1 0] [0 0 1]]
          res (s/replace (with-out-str (p! m)) #"\r\n" "\n")]
      (is (or (= "1 0 0\n0 1 0\n0 0 1\n" res)
              (= "1 0 0 \n0 1 0 \n0 0 1 \n" res))))))

(deftest p-matrix2-test
  (testing "Testing p! with matrix2."
    (let [m [[1 0 0 1] [0 1 0 1] [0 0 1 1]]
          res (s/replace (with-out-str (p! m)) #"\r\n" "\n")]
      (is (or (= "1 0 0 1\n0 1 0 1\n0 0 1 1\n" res)
              (= "1 0 0 1 \n0 1 0 1 \n0 0 1 1 \n" res))))))

(def ^:private matrix-gen (gen/let [[n m] (gen/tuple
                                           (gen/fmap inc gen/nat)
                                           (gen/fmap inc gen/nat))]
                            (gen/vector (gen/vector gen/nat m) n)))

(defn- parse-ints [strings]
  (map #(Integer/parseInt ^String %) strings))

(defn- str->matrix [s]
  (as-> s m
    (clojure.string/split m #"\n")
    (map #(clojure.string/split % #"\s+") m)
    (map parse-ints m)))

(defspec p-prop-test 30
  (prop/for-all [m matrix-gen]
                (let [n (str->matrix (with-out-str (p! m)))]
                  (= n m))))

(deftest trans-identity-matrix-test
  (testing "Transposing has no effect on the identity matrix."
    (let [m [[1 0 0] [0 1 0] [0 0 1]]]
      (is (= m (trans m))))))

(deftest trans-matrix2-test
  (testing "Transposing matrix2."
    (let [m [[1 0 0 1] [0 1 0 1] [0 0 1 1]]]
      (is (= [[1 0 0]
              [0 1 0]
              [0 0 1]
              [1 1 1]]
             (trans m))))))

(deftest trans-non-quadratic-test
  (testing "Transposing a 3x2 matrix should yield a 2x3 matrix."
    (is (= [[1 3 5]
            [2 4 6]]
           (trans [[1 2] [3 4] [5 6]])))))

(defn- trans? [original transposed]
  (if (first transposed)
    (if (= (map first original) (first transposed))
      (recur (map rest original) (rest transposed))
      false)
    (empty? (first original)))) ; Was the original exhausted?

;; Generates matrices, calls `trans` and checks if the resulting matrix is
;; the transposed of the original
;;
;; The output of a failed test in the form of
;; {... :smallest [m] ... }
;; means that `(trans m)` did not transpose the matrix correctly
(defspec trans-prop-test 25
  (prop/for-all [m matrix-gen]
                (trans? m (trans m))))

;; 2.4

(deftest data-type-test-1
  (testing "empty data structures are identified"
    (are [input output] (= (data-type input) output)
      #{} :set
      {} :map
      [] :vector
      () :list)))

(deftest data-type-test-2
  (testing "non-empty data structures are identified"
    (are [input output] (= (data-type input) output)
      #{:a :b :c} :set
      {:a 1, :b 2} :map
      [:a :b] :vector
      '(:a :b) :list)))

;; Generates maps and calls `data-type` on them to verify if they are correctly
;; identified
;;
;; The output of a failed test in the form of
;; {... :smallest [m] ... }
;; means that `(data-type m)` did not detect map as the correct data-type
(defspec generative-data-type-map-prop-test 100
  (prop/for-all [m (gen/map gen/simple-type-printable
                            gen/simple-type-printable)]
                (= :map (data-type m))))

;; Generates lists and calls `data-type` on them to verify if they are correctly
;; identified
;;
;; The output of a failed test in the form of
;; {... :smallest [l] ... }
;; means that `(data-type l)` did not detect list as the correct data-type
(defspec generative-data-type-list-prop-test 100
  (prop/for-all [l (gen/list gen/simple-type-printable)]
                (= :list (data-type l))))

;; Generates vectors and calls `data-type` on them to verify if they are correctly
;; identified
;;
;; The output of a failed test in the form of
;; {... :smallest [v] ... }
;; means that `(data-type v)` did not detect vector as the correct data-type
(defspec generative-data-type-vector-prop-test 100
  (prop/for-all [v (gen/vector gen/simple-type-printable)]
                (= :vector (data-type v))))

;; Generates sets and calls `data-type` on them to verify if they are correctly
;; identified
;;
;; The output of a failed test in the form of
;; {... :smallest [s] ... }
;; means that `(data-type s)` did not detect set as the correct data-type
(defspec generative-data-type-set-prop-test 100
  (prop/for-all [s (gen/set gen/simple-type-printable)]
                (= :set (data-type s))))