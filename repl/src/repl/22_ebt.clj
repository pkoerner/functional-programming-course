(ns repl.22-ebt
  (:require [clojure.test :as t]))

;; The solution of a recent programming exercise:

(defn levenshtein [[h1 & t1 :as s1] [h2 & t2 :as s2]]
  (cond (empty? s1) (count s2)
        (empty? s2) (count s1)
        :otherwise (min
                     (inc (levenshtein t1 s2))
                     (inc (levenshtein s1 t2))
                     (if (= h1 h2)
                       (levenshtein t1 t2)
                       (inc (levenshtein t1 t2))))))


;; Following, a solution of an anonymous student:
;; Suppose that this implementation is a super-efficient solution.
(defn mapHelper "Which letter should be keept" [hold keep]
  (if (= hold keep)
    hold
    false))

(defn levenshtein-Helper "compares two strings, keeps equal letters and adds all not equal up" [stringSeq1 stringSeq2]
  (count (filter #(= false %) (map mapHelper stringSeq1 stringSeq2))))

(defn prepareString "makes the strings equaly long" [string lengthOfTheOther]
  (let [dif (- (count string) lengthOfTheOther)]
    (if (< dif 0 )
      (concat (seq string) (repeat (- dif) " "))
      (seq string))))

(defn levenschtein "prepare the strings and compares them in two different kinds, return result" [string1 string2]
  (let [prepString11 (prepareString string1 (count string2))
        prepString12 (prepareString string2 (count string1))
        prepString21 (prepareString (reverse string1) (count string2))
        prepString22 (prepareString (reverse string2) (count string1))
        x (levenshtein-Helper prepString11 prepString12)
        y (levenshtein-Helper prepString21 prepString22)]
        (if (< x y)
          x
          y)))

;; Is that second implementation correct?
;; The former one is understandable and I can (informally) reason about it...
;; The latter solution is somewhat more involved and its correctness is not clear.

;; Let's use the opportunity for tests!

(comment 

;; usually, this is defined in a file in the test/ folder rather than src/
;; 'deftest' defines a test, 'is' is an assertion in the test
(t/deftest empty-test 
  (t/is (= (levenschtein "" "") 0)))

;; Usually, lein test executes this:
(t/run-tests) ; =>  {:type :summary, :pass 1, :test 1, :error 0, :fail 0}

;; One can write many (similar) assertions more concise with 'are':
(t/deftest examples 
  (t/are [x y] (= (levenshtein x y) (levenschtein x y))
         "" "hallo"
         "Brett" "nett"
         "foo" "ffoo"
         "ffoo" "foo"
         "foon" "fax"
         "simple" "easy"))

(t/run-tests) ; =>  {:type :summary, :pass 7, :test 2, :error 0, :fail 0}

;; Can we be *now* be sure that levenschtein is a correct implementation?


;; The "ebt" of this file name is for "example-based testing".
;; Such tests are important!
;; However, they are just as good as the examples that I choose.
;; Of the entire input space of the function, we covered almost nothing...

;; We will come back to this in the exercises!

)



