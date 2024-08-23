(ns repl.23-hof)

;; 1 Reminder: factorial function
;; 2 Reminder: stack overflow
;; 3 Concept: Higher-Order Function (HOF)
;; 4 Important Built-in HOFs
;;   4.1 map
;;   4.2 filter (and remove)
;;   4.3 reduce, the mother of all HOFs
;;   4.4 Revision: apply
;;   4.5 partial
;;   4.6 iterate
;;   4.7 more examples



(comment 

;; 1 Reminder: factorial function
;; ------------------------------

;; where we last left off in 01_intro Section 7.6

  (defn ! [n]
    (if (= 1 n)
      n
      (*' n (! (dec n))))) ;; Solution: use *' for auto promotion

  (! 10000)


;; 2 Reminder: stack overflow
;; --------------------------

;; Recursive calls produce frames on the stack and can cause a stack overflow
;; A stack frame contains local variables and is needed
;; in case a function with its own bindings returns again

;; There are two solutions to fix this:
;; 1. use explicit recursion (later, 05-recursion)
;;    - this is however requires that we re-state basic logic each time we want a similar operation
;; 2. usage of higher-order function
;;    - this abstraction hides the logic from the caller and focuses on the transformation instead


;; We will cover the basics of higher-order functions before we present a solution.

;; 3 Concept: Higher-Order Function (HOF)
;; --------------------------------------

;; Viewing functions as a normal parameter/return value is the key to functional programming;
;; It takes some exercise to train the brain to think accordingly!
;; Such functions are refered to as higher-order function (HOF).
;; HOF are not special, since functions are not special (first-class citizens)

;; fn as parameter
  (defn evaluate1 [f v] (f v))
  (evaluate1 inc 12)

;; fn as value
  (defn mk-adder [n] (fn [x] (+ x n)))

;; effectively: (def foo (fn [x] (+ x 17))) -- beta reduction!
  (def foo (mk-adder 17))
  (foo 4)

;; Constructs, that occasionally happen:
((mk-adder 17) 3)
;; two opening parentheses at the beginning, because the function returned by mk-adder is called
;; alternatively, give the function a name in a local binding:
(let [adds-17-fn (mk-adder 17)]
  (adds-17-fn 3))



;; 4 Important Built-in HOFs
;; -------------------------

;; there are some basic higher-order functions that you should know by heart:

;;   4.1 map
;; ---------


;; map accepts a function and a sequence of values x1, ..., xn and returns the sequence f(x1), ..., f(xn)
  (map inc [2 3 4 5 6])
;; If multiple lists are provided, then the n-ary function is called with one element from each list until (at least) one input-list is exhausted
  (map + [1 2 3] [3 4 5] [2 3])


;; You can use any function
;; This produces all even numbers for example
  (map (fn [x] (* 2 x)) (range))



;; 'map' is lazy

;; the side effect (the print) is used for didactic reasons
;; do not mix side effects with map (!!)
  (def squares
    (map (fn [x] (println :berechnet x) (* x x)) (range)))

;; the side effect only occurs once during the first evaluation
  (first squares)
  (nth squares 33)
;; already occurred side effects do not happen again
  (nth squares 39)

;; (!!) It is an extraordinarily stupid idea to combine side effects with laziness
;; It is often difficult to predict when and if a side effect occurs

;; Example:
  (defn my-debug-print [input]
    (map println input)
    nil) ;; no return value that is reasonable

;; Whoops!
  (my-debug-print [1 2 3])

;; there are even more tricky cases, where calling a function in the REPL causes the side effect to trigger (during the print phase),
;; but during the execution of a real program  it is not...

;; lessons learned the hard way: no 'map' with side effects!
;; Other constructs exist for use with side effects, e.g. 'doseq'
;; it guarantees the evaluation of all elements and associated side effects:

  (do
    (doseq [x [1 2 3]]
      (println x))
    nil)

;; map always returns a LazySeq.
;; A type-preserving map could be defined as
  (defn mm [f v] (into (empty v) (map f v)))

;; there already exists mapv which always returns a vector


;;   4.2 filter (and remove)
;; --------------------------

;; 'filter' expects a function p? and a sequence x1, ..., xn.
;; a sequence of all xi, for which (p? xi) returns a truthy value, is produced
  (filter (fn [x] (< 2 x 6)) [1 2 3 4 5 6])

;; What does 'remove' do?
  (remove (fn [x] (< 2 x 6)) [1 2 3 4 5 6])



;;   4.3 reduce, the mother of all HOFs
;;   -----------------------------------

;; reduce expects a function f of 2 arguments (fn [accumulator element] ...),
;; an initial value a as accumulator and a sequence x1, ..., xn.
;; Calculates: (f ... (f (f a x1) x2) ... xn)

;; a unfolding of a specific call:
;; the call (reduce f a [x y z]) calculates
;; (f (f (f (f a) x) y) z)

(reduce + 1 [2 3 4 5 6]) ;; (+  (+ (+ 1 2) 3) 4 ...)

;; Exercise: expand the following calls by hand, on paper

;; (reduce reduce-function init-value sequence)

  (reduce * (range 2 7))

  (reduce + 0 (range 2 7))
  (reduce + (range 2 7))

  (reduce conj [] [1 2 3])
  (reduce conj '() [1 2 3])


;; reduce handles some special cases differently
;; Exercise: What happens if ...
;; - the initial value is left out?
;; - the collection is empty?
;; - both of the above cases apply at the same time?

;; Hint: read the docstring and experiment


;; now that we know reduce, we can fix our factorial function:

  (defn ! [n]
    (reduce *' 0 (range 1 (inc n))))


;;   4.4 Revision: apply
;; ----------------------
(apply str (interpose \newline [1 2 3]))


;;   4.5 partial
;; -------------

;; partial is function that takes another function and "hard-wires" its first argument(s).
;; it returns a function that may take more arguments:

(partial + 1 2) ;; a function, that always adds 3
((partial + 1 2) 3 4) ;; 1 + 2 (hard-wired) + 3 + 4


;;   4.6 iterate
;; --------------

;; iterate takes a function f and a value v
;; and returns an infinite sequence [v, f(v), f(f(v)), f(f(f((v)))), ...]

(take 5 (iterate inc 42))


;; In functional programming, higher-order functions are used heavily 
;; and are the prefered solution over explicit recursion.
;; If we want to filter values, we do not care *how* they are filtered;
;; element-wise? ordering? laziness? 
;; while programming, we should not be concerned with the details...


;;   4.7 more examples
;; -------------------

;; some more examples of HOFs:
;; note: keywords can act as functions; 
;; we do not consider functions that may return a keyword (e.g., first) as higher-order function per se
  (group-by :lecturer
            [{:course :fp, :year 2013, :lecturer :bendisposto}
             {:course :fp, :year 2021, :lecturer :koerner}
             {:course :propra, :year 2020, :lecturer :bendisposto}])


  (merge-with + {:a 1, :b 2} {:a 3})

  (take-while neg? (range -10 10000))
  (drop-while neg? (range -10 10))

;; once you know what you are doing,
;; nesting of HOFs may get crazy (from one of my projects):
   ; (reduce (partial merge-with concat) (map (partial analyze-block property-map list) data))  


;; Remark.
;; reduce is called the "mother of all HOFs" for a reason:
;; you can implement pretty much every core (higher-order) function using reduce.
;; It uses a pattern that happens more often than you think.
;; As an exercise, try implementing some list operations by reducing a function!
;; for example: first, map, filter, concat, interpose, take, ...


;; also: try to implement naive versions of basic higher-order functions,
;; e.g., map, filter, reduce, apply, partial



)
