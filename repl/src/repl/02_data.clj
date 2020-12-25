(ns repl.02-data)

(comment
;; Especially important if we work with infinite data structures

(set! *print-length* 20)

;; Data structures in Clojure


;; ---------------------------------------------------------
;; Functions / Higher Order Functions
;; ---------------------------------------------------------


;; Revision: apply

;; does not work: Type error (List instead of Integer)
(+ 4 [1 2 3])

;; apply unpacks the collection
(apply + 4 [1 2 3])


;; apply can take 'ordinary' arguments for the function (in this case +) ahead of the collection to unpack
(apply + 4 1 2 3 [42])

;; the last one has to be a collection however
(apply + 4 1 2 3)


;; Def: Higher Order Function - A function that takes a function as a parameter and/or returns one

;; Standard examples (already seen): map filter reduce

(map inc (range 2 7))
(map + [1 2 3] [3 4 5] [4 6])

(filter (fn [x] (< 2 x 6)) [1 2 3 4 5 6])

(reduce * 1 (range 2 7))
(reduce * (range 2 7))


;; reduce is described as 'the mother of all HOF'
;; and can be used to define a version of map and filter

(defn mymap [f c]
  (reduce (fn [a e] ;; a stands for accumulator, e for element
            (conj a (f e)))
          []
          c))
(mymap inc [1 2 3])


;; returns a vector instead of a list, but decent enough
(type (mymap inc [1 2 3]))
;; the real map is additionally lazy, while ours is not
(type (map inc [1 2 3]))


;; Filter is possible, too
(defn myfilter [pred c]
  (reduce (fn [a e]
            (if (pred e)
              (conj a e) ;; keep the element
              a)) ;; drop the element
          []
          c))
(myfilter even? [1 2 3 4])

;; mapcat is map + concat on the resulting elements
(map (fn [e] (range 1 e)) [2 3 4])
(mapcat (fn [e] (range 1 e)) [2 3 4])

;; so the same as
(apply concat
       (map (fn [e] (range 1 e)) [2 3 4]))

;; Exercise: Define mapcat using reduce




;; ---------------------------------------------------------
;; Homemade Laziness
;; ---------------------------------------------------------

;; Range with data structures

;; note the following API, which differs from 'range'
 ; (ranje 0) ; (0 1 2 3 4 ...)
 ; (ranje 10) ; (10 11 12 ...)
;; ranje returns something which contains all integers starting from an initial value

;; So something closer to this
  (drop 10 (range))

;; We define this as such:
;; Only the first element is evaluated
;; we delay the rest of the elements with a function
(defn ranje [n]
  {:first n
   :rest (fn [] (ranje (inc n)))})
;; either we take the first element
(defn head [r]
  (:first r))
;; or call the function that generates the rest
(defn tail [r]
  ((:rest r)))

(head (tail (tail (ranje 0))))

;; We are using a data structure here (the map)
;; We will get rid of that in the next step

;; Range a la Houdini - now with 20 % more magic

;; We still delay the evaluation of the tail through the use of a function
(defn ranje2 [head]
  (fn [head?] (if head? 
                head 
                (ranje2 (inc head)))))

(defn head2 [ranje-fn]
  (ranje-fn true))

(defn tail2 [ranje-fn]
  (ranje-fn false))

(head2 (tail2 (tail2 (ranje2 0))))

;; General idea:
;; A lazy data structure keeps a pointer to the rest of the sequence and knows
;; which functions have to be applied to those elements



;; What to keep in mind:
;;   1. No side effects

;; laziness works as expected
(def lz (map (fn [e] (println :doh) (inc e))
             (take 100 (range))))
(nth lz 4)
(nth lz 4)
(nth lz 31)
(nth lz 32)

;; Let us define the same sequence differently
(def lz (map (fn [e] (println :dah) (inc e))
             (range 0 100)))


(nth lz 4)
(nth lz 4)
;; huh?
(nth lz 31)
(nth lz 32)


;; Processing 32 elements at once is usually more efficient on current processors
;; but it is not obvious when this chunking happens
;; It depends on the data structure!
;; Which also means that sometimes a lot of side effects trigger at once...
;; So avoid this completely!


;;   2. Avoid certain operations for infinite sequences
;;      - What is the last element? (last (range)) takes a while...
;;      - How long is the sequence? (count (range)) also takes a while...



  )

