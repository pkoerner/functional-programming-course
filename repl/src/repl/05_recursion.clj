(ns repl.05-recursion
  (:use [clojure.tools.trace]))

;; requires tools.trace in project.clj
;; new today:
;; debugging library: clojure.tools.trace: deftrace
;; clojure.core: recur, loop

(comment

  ;; deftrace is a defn, which outputs debug-informationen for calls
  (deftrace ! [n]
    (if (= 1 n)
      n
      (*' n (! (dec n)))))

  ;; you can see the stack frames build up
  (! 8)

  ;; from Prolog (and some other languages) we know:
  ;; If the last call in a function is the same function again,
  ;; then no new stack frame must be allocated, instead the old
  ;; one can be reused.
  ;; This is called tail-call optimization (TCO)

  ;; A version with an accumulator, which saves the product so far.
  ;; Now this function is tail recursive:
  (deftrace !
    ([n] (! n 1))
    ([n a] (if (= 1 n)
             a
             (! (dec n) (*' a n)))))

  ;; stack frames are still created
  (! 8)

  ;; Clojure has no automatic TCO.
  ;; The JVM does not provide a reasonable way to do this.

  ;; Rich Hickey decided against optimizations that sometimes
  ;; magically take effect and sometimes do not
  ;; This is why you have to explicitly write 'recur'.

  (deftrace !
    ([n] (! n 1))
    ([n a] (if (= 0 n)
             a
             (recur (dec n) (*' a n)))))

  (! 8)

  ;; A version without trace!
  ;; fn / defn create a recursion point:
  ;; if the number of arguments passed to recur matches the ones
  ;; of the recursion point, the function is called again
  (defn !
    ([n] (! n 1))
    ([n a] (if (= 0 n)
             a
             (recur (dec n) (*' a n)))))

  (! 10000)

  ;; recur cannot be used everywhere:
  ;; it HAS to be the last call made, otherwise the compiler gets sad
  (defn ! [n]
    (if (= 1 n)
      n
      (*' n (recur (dec n)))))

  ;; loop marks another recursion point for recur,
  ;; which is used instead of the one provided by the function itself:
  ;; recur then sets the bindings of the symbols defined by loop
  (defn ! [n]
    (loop [n n
           a 1]
      (if (= 0 n)
        a
        (recur (dec n) (*' a n)))))






  ;; mutual recursion


  ;; all symbols have to be defined before their use à la C
  ;; use 'declare' for forward declarations
  (declare my-odd? my-even?)
  (macroexpand-1 '(declare my-odd? my-even?))

  ;; this way you can define functions that depend on each other
  ;; 0 is even.
  ;; 1 is not even.
  ;; A number n is even, if n-1 is not even.
  (defn my-even? [n]
    (cond (= 0 n)  true
          (= 1 n)  false
          :otherwise (my-odd? (dec n))))

  ;; analogous definition of odd.
  (defn my-odd? [n]
    (cond (= 0 n) false
          (= 1 n) true
          :otherwise (my-even? (dec n))))

  ;; this works...
  (my-even? 6)
  (my-even? 5)
  (my-odd? 5)
  (my-odd? 6)
  
  ;; ...unless the input is too large
  (my-even? 100000)


  ;; We cannot use recur since we call another function!
 
  ;; the solution is 'trampoline':
  ;; trampoline takes a call, which it evaluates.
  ;; if the return value is not a function it is returned,
  ;; otherwise if it is a function it is called without parameters (and so on, and so on)

  ;; so we return a function in place of triggering the mutual recursion
  (defn my-even? [n]
    (cond (= 0 n)  true
          (= 1 n)  false
          :otherwise (fn [] (my-odd? (dec n)))))

  ;; ditto...
  (defn my-odd? [n]
    (cond (= 1 n)  true
          (= 0 n)  false
          :otherwise (fn [] (my-even? (dec n)))))

  
  ;; and the call is wrapped by trampoline
  (trampoline (my-even? 5))
  (trampoline (my-even? 100000))
  (trampoline (my-even? 100000111)) ;; takes a while


  ;; Beware! What if the ultimate return value of the mutual recursion is itself
  ;; a function?

  ;; Then you have to wrap it in a data structure (e.g. list, vector)
  ;; to avoid the function call by trampoline and unwrap it afterwards

)
