(ns unit07.core-test
  (:require
   [clojure.test :refer :all]
   [unit07.core :refer :all]
   [clojure.test.check :as tc]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 7.1

(def ^:private test-environment
  [["x" "x" "x" "x"]
   ["x" " " " " "x"]
   ["x" " " " " "x"]
   ["x" "x" "x" "x"]])

(deftest ^:eftest/synchronized execute-forward-north-test
  (testing "Moving the rover forwards on position (2, 2) pointing north, should move it to (2, 1)."
    (init! 2 2 :north test-environment)
    (execute! "f")
    (is (= [2 1 :north] (rover-status)))))

(deftest ^:eftest/synchronized execute-forward-east-test
  (testing "Moving the rover forwards on position (1, 2) pointing east, should move it to (2, 2)."
    (init! 1 2 :east test-environment)
    (execute! "f")
    (is (= [2 2 :east] (rover-status)))))

(deftest ^:eftest/synchronized execute-forward-south-test
  (testing "Moving the rover forwards on position (1, 1) pointing south, should move it to (1, 2)."
    (init! 1 1 :south test-environment)
    (execute! "f")
    (is (= [1 2 :south] (rover-status)))))

(deftest ^:eftest/synchronized execute-forward-west-test
  (testing "Moving the rover forwards on position (2, 1) pointing west, should move it to (1, 1)."
    (init! 2 1 :west test-environment)
    (execute! "f")
    (is (= [1 1 :west] (rover-status)))))

(deftest ^:eftest/synchronized execute-backward-north-test
  (testing "Moving the rover backwards on position (2, 1) pointing north, should move it to (2, 2)."
    (init! 2 1 :north test-environment)
    (execute! "b")
    (is (= [2 2 :north] (rover-status)))))

(deftest ^:eftest/synchronized execute-backward-east-test
  (testing "Moving the rover backwards on position (2, 1) pointing east, should move it to (1, 1)."
    (init! 2 1 :east test-environment)
    (execute! "b")
    (is (= [1 1 :east] (rover-status)))))

(deftest ^:eftest/synchronized execute-backward-south-test
  (testing "Moving the rover backwards on position (2, 2) pointing south, should move it to (2, 1)."
    (init! 2 2 :south test-environment)
    (execute! "b")
    (is (= [2 1 :south] (rover-status)))))

(deftest ^:eftest/synchronized execute-backward-west-test
  (testing "Moving the rover backwards on position (1, 1) pointing south, should move it to (2, 1)."
    (init! 1 1 :west test-environment)
    (execute! "b")
    (is (= [2 1 :west] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-left-north-test
  (testing "Turning the rover left when pointing north, should turn it to face west."
    (init! 2 1 :north test-environment)
    (execute! "l")
    (is (= [2 1 :west] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-left-west-test
  (testing "Turning the rover left when pointing west, should turn it to face south."
    (init! 2 1 :west test-environment)
    (execute! "l")
    (is (= [2 1 :south] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-left-south-test
  (testing "Turning the rover left when pointing south, should turn it to face east."
    (init! 2 1 :south test-environment)
    (execute! "l")
    (is (= [2 1 :east] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-left-east-test
  (testing "Turning the rover left when pointing east, should turn it to face north."
    (init! 2 1 :east test-environment)
    (execute! "l")
    (is (= [2 1 :north] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-right-north-test
  (testing "Turning the rover right when pointing north, should turn it to face east."
    (init! 2 1 :north test-environment)
    (execute! "r")
    (is (= [2 1 :east] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-right-east-test
  (testing "Turning the rover right when pointing east, should turn it to face south."
    (init! 2 1 :east test-environment)
    (execute! "r")
    (is (= [2 1 :south] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-right-south-test
  (testing "Turning the rover right when pointing south, should turn it to face west."
    (init! 2 1 :south test-environment)
    (execute! "r")
    (is (= [2 1 :west] (rover-status)))))

(deftest ^:eftest/synchronized execute-turn-right-west-test
  (testing "Turning the rover right when pointing west, should turn it to face north."
    (init! 2 1 :west test-environment)
    (execute! "r")
    (is (= [2 1 :north] (rover-status)))))

(deftest ^:eftest/synchronized execute-forward-obstacle-test
  (testing (str "Attempting to move the rover forwards into an obstacle"
                " should not move the rover and print a warning.")
    (init! 1 1 :north test-environment)
    (let [warning (with-out-str (execute! "f"))]
      (is (= [1 1 :north] (rover-status)))
      (is (not-empty warning)))))

(deftest ^:eftest/synchronized execute-backward-obstacle-test
  (testing (str "Attempting to move the rover backwards into an obstacle"
                " should not move the rover and print  a warning.")
    (init! 2 2 :north test-environment)
    (let [warning (with-out-str (execute! "b"))]
      (is (= [2 2 :north] (rover-status)))
      (is (not-empty warning)))))

(deftest ^:eftest/synchronized execute-multiple-instructions-test
  (testing "Executing multiple instructions should move the rover accordingly."
    (init! 1 1 :north test-environment)
    (execute! "rfrfrfb")
    (is (= [2 2 :west] (rover-status)))))

(deftest ^:eftest/synchronized execute-multiple-instructions-obstacle-test
  (testing (str
            "Executing multiple instructions, which attempt to move the rover into"
            " an obstacle, should only execute the ones up to the last legal"
            " instruction and print a warning.")
    (init! 1 1 :north test-environment)
    (let [warning (with-out-str (execute! "rfrffrr"))]
      (is (= [2 2 :south] (rover-status)))
      (is (not-empty warning)))))


;; 7.2


(deftest common-min-search-1-test
  (testing "(common-min-search [3 4 5]) should yield 3."
    (is (= 3 (common-min-search [3 4 5])))))

(deftest common-min-search-2-test
  (testing "(common-min-search [1 2 3 4 5 6 7] [0.5 3/2 4 19]) should yield 4."
    (is (= 4 (common-min-search [1 2 3 4 5 6 7] [0.5 3/2 4 19])))))

;; Be careful when executing this test, as it can lead to an endless execution
;; of incorrect implementations of common-min-search
(deftest common-min-search-3-test
  (testing "(common-min-search (range) (range) (iterate inc 20))) should yield 20."
    (is (= 20 (common-min-search (range)
                                 (range)
                                 (iterate inc 20))))))

;; Be careful when executing this test, as it can lead to an endless execution
;; of incorrect implementations of common-min-search
(deftest common-min-search-4-test
  (testing "common-min-search of the fourth given example should yield 64."
    (is (= 64 (common-min-search (map  (fn [x] (* x x x)) (range))
                                 (filter (fn [x] (zero? (bit-and x (dec x))))
                                         (range))
                                 (iterate inc 20))))))

(def common-min-search-input-gen
  (gen/bind gen/large-integer
            (fn [common-min]
              (gen/tuple
               ; The elements used to make up the lists, they are
               ; distinct as not to create a common element between the lists
               (gen/vector-distinct gen/large-integer {:min-elements 25})
               ; The number of elements per list, e.g. a vector [3 10] will
               ; distribute the above elements in a list of 3,
               ; a list of 10 elements and the rest in one additional list
               (gen/vector (gen/fmap inc gen/nat) 1 10)
               (gen/return common-min)))))

(defn- split-at-idxs [coll idxs]
  (apply conj
         (reduce
          (fn [[splits elements] idx]
            (let [[bef aft] (split-at idx elements)]
              [(conj splits bef) aft]))
          [[] coll]
          idxs)))

(defn- split-insert-sort [[elements elements-per-split common-min]]
  (let [splits (split-at-idxs elements elements-per-split)]
    [(map (fn [spl] (sort (conj spl common-min))) splits)
     common-min]))

;; Generates finite sequences with at least one common number in all sequences
;; calls common-min-search with all sequences
;; finally checks if the common minimal number of those sequences was found
;;
;; The output of a failed test in the form of
;; {... :smallest [[seqs common-min]] ... }
;; means that `(apply common-min seqs)` did not yield `common-min` as expected
(defspec common-min-search-match-prop-test 20
  (prop/for-all [[seqs common-min] (gen/fmap split-insert-sort
                                             common-min-search-input-gen)]
                (= common-min (apply common-min-search seqs))))