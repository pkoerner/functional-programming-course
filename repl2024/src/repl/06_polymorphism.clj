(ns repl.06-polymorphism
  (:use [clojure.repl])
  (:require [instaparse.core :refer [parser]])) ;; used for an example
;; requires the instaparse library (cf. project.clj)



;; 1 Revision: defrecord
;; I. Multimethods
;;   2 multimethod = multi + method
;;   3 Dispatching on multiple inputs 
;;   4 Supporting Existing Classes
;;   5 Performance Considerations
;;   6 Case Study: Interpreter
;;   7 Multimethods and Hierarchies (optional) 
;; II. Protocols
;;   8 Defining and Extending Protocols
;;   9 Performance Comparison: Multimethods vs. regular Functions vs. Protocols
;;   10 Concluding Thoughts
;; 11 Type Hints (optional)


;; new content in this session
;; Multimethods defmulti, defmethod
;; Libraries: instaparse (generates parsers)
;; clojure.core: defrecord, ->TypeConstructors,
;;               defmulti, defmethod, prefer-method
;;               defprotocol, extend-type, extend-protocol, extend
;;               isa?


(comment

;; 1 Revision: defrecord
;; ---------------------

  ;; we will see two solutions for the expression problem

  ;; defrecord will generate a new Java class
  ;; an instance of that record guarantees that the specified fields will be present
  (defrecord ExamQuestion [question score])

  ;; we get a new constructor-macro ->... for free
  ;; it writes the provided values in the given order to the fields
  (def question1 (->ExamQuestion 1 10))
  ;; the instances are immutable and behave like Clojure maps

  ;; but it really is a new class
  (class question1)

  (:score question1)
  ;; it even looks like a map
  question1
  (assoc question1 :foo 12)
  (dissoc question1 :score) ;; missing a required key does *not* yield an instance of ExamQuestion




;; I. Multimethods

;; 2 multimethod = multi + method
;; ------------------------------

  (defmulti get-columns class) ;; "Interface"-ish
  (defmulti get-columns ;; function name (arbitrary)
            class)      ;; dispatch function

  (defmethod get-columns ExamQuestion [_] ["Question","Score"]) ;; concrete implementation
  ;; or more detailed:
  (defmethod get-columns ;; function name (matches the defmulti)
             ExamQuestion ;; result of the dispatch function
             [this] ;; argument vector
             ["Question","Score"]) ;; body
  (get-columns question1)


  ;; the multimethod does not support Strings (yet)
  (get-columns "sss")

  ;; we can extend the multimethod for Strings though - the implementation does not matter for now
  (defmethod get-columns String [s] (println :yay s))

  (get-columns "sss")

  ;; analogously to get-columns, one can define a function for values
  (defmulti get-values class)
  (defmethod get-values ExamQuestion [k] [(:exercise k) (:score k)])


  (get-values question1)

  ;; naturally, it does not work with vectors...
  (def b ["1","2"])
  (get-values b)

  ;; again, we can just extend the implementation
  (class b)
  (defmethod get-columns clojure.lang.PersistentVector [k] k)
  (defmethod get-values clojure.lang.PersistentVector [k] k)

  (get-values b)

  ;; nil is not special here
  (class nil)
  (defmethod get-values nil [_] ["empty"])
  (get-values nil)


  ;; neither are integer values
  (get-values -3)
  (defmethod get-values Long [_] ["foo"])


  (get-values -3.14)
  ;; implementing a catch-all:
  ;; :default is a "real" keyword here that is part of the syntax!
  ;; a different value can be specified in the corresponding defmulti
  (defmethod get-values :default [x]
    (println "get-values not implemented for" x "oop! oop! oop!"))
  (get-values -3.14)




  ;; the dispatch function can be more sophisticated than getting the class:
  (defmulti cred (fn [c] (> 5 (count c))))

  ;; actually, ANY function can be used:

  (defmethod cred true [c] (println "short list"))
  (defmethod cred false [c] (println "long list!!!"))


  (cred [1 2 3 4 5 6])
  (cred [1])


;; 3 Dispatching on multiple inputs 
;; --------------------------------

;; (see also https://clojure.org/about/runtime_polymorphism)

  (def simba {:species :lion})
  (def clarence {:species :lion})

  (def bugs {:species :bunny})
  (def donnie {:species :bunny})

  ;; the dispatch function simply takes more arguments
  (defmulti encounter (fn [a b] [(:species a) (:species b)]))

  ;; the matching behaviour:
  (defmethod encounter [:bunny :lion] [x y] :run-away)
  (defmethod encounter [:lion :lion] [x y] :fight )
  (defmethod encounter [:bunny :bunny] [x y] :mate)
  (defmethod encounter [:lion :bunny] [x y] :omnomnom)

  (encounter simba bugs)
  (encounter clarence simba)
  (encounter bugs bugs)





;; 4 Supporting Existing Classes
;; ------------------------------

  ;; adding a "Reversible" interface to Strings:
  (defn reverse-a-string-java-style [s]
    (clojure.string/join (reverse s)))

  (reverse-a-string-java-style "foo")
  (reverse-a-string-java-style -3)


  ;; in Java, a solution would look like new Dispatcher(new IFunction() {...})
  (defmulti reversr (fn [object] (class object)))


  ;; and this is the same as reverse.register(String.class, new IFunction() {...})
  (defmethod reversr String [s] (reverse-a-string-java-style s))


  (reversr "foo")

  ;; we can add our reverse-function to anything we want, e.g., Long values
  (reversr 15)

  (defmethod reversr Long [l] (- l))

  (reversr 15)

  ;; pro: data representation (data types) and dispatching (defmulti) are not complected any more
  ;; pro: defmethods can be extended in any arbitrary namespace (especially useful if the defmulti is shipped as part of a library)



;; 5 Performance Considerations
;; ----------------------------

  (defmulti m1 class)
  (defmethod m1 clojure.lang.PersistentVector [k] (count k))
  (defn m2 [k] (count k))

  (time (dotimes [i 10000000] (m1 [1])))
  (time (dotimes [i 10000000] (m2 [1])))

  ;; a Clojure function-call is about eight times faster than calling a multimethod
  ;; Clojure functions are also a tiny bit slower than direct Java method-calls








;; 6 Case Study: Interpreter
;; -------------------------

  ;; Let's write an interpreter for Squarejure

  ;; the language should look like this:
  [:add [:int 4] [:int 9]]
  ;; function calls are square brackets
  ;; we need to implement built-ins :add, :int, etc.

  ;; we will dispatch on the first element of a vector...
  (defmulti squarejure (fn [[e & _]] e))

  ;; Integer values will be repesented as Clojure-Longs...
  (defmethod squarejure :int [[_ v]] v)

  (squarejure [:int 6])

  ;; a simple addition
  (defmethod squarejure :add [[_ a b]]
    (+ (squarejure a) (squarejure b)))

  (squarejure [:add [:int 4] [:int 9]])


  ;; and we can extend the interpeter (and, thus, the language) later on
  (defmethod squarejure :sum [[_ & args]]
    (apply + (map squarejure args)))

  ;; we can map multimethods the same way as regular functions

  (squarejure [:sum [:add [:int 3] [:int 7]]
                    [:int 6]
                    [:int 77]
                    [:int -4]])

  ;; Welcome to theoretical computer science. 
  ;; Let's write a grammar:

  (def ebnf "
    S = add
    add = int '+' S | int '+' int
    int = #'[0-9]+'
  ")


  ;; parser is part of the Instaparse-library
  ;; This library is pretty awesome - it generates parsers for pretty much any type of grammars,
  ;; in particular non-deterministic ones (it generates all parse trees)
  (def parse (parser ebnf))



  ;; a syntax tree looks like that:
  (parse "3+5")
  (parse "3+9+12")
  ;; why is it wrapped in a list?
  ;; it's the sequence of *all* parse trees!


  ;; with <...>, we can hide symbols we do not care for and generate an abstract syntax tree instead
  (def ebnf2 "
    <S> = add
    add = int <'+'> S | int <'+'> int
    int = #'[0-9]+'
  ")

  (def parse (parser ebnf2))

  (parse "3+5")
  (parse "3+9+12")


  ;; the first syntax tree is good enough
  (defn sqeval [prog] (squarejure (first (parse prog))))

  ;; read-string transforms Strings into Clojure data structures
  (read-string "8")
  (read-string "[:a :b]")

  ;; if we encounter a literal, it will be a String and has to be translated into a Clojure Long
  (defmethod squarejure :int [[_ e]] (if (string? e) (read-string e) e))

  (sqeval "8+9")
  (sqeval "2+2+2")

  ;; that's very little code for an entire, extensible interpreter!



;; 7 Multimethods and Hierarchies (optional) 
;; -----------------------------------------

  ;; Multimethods can handle hierarchies.
  ;; A default hierarchy is the superclass-relationship:

  (defmulti hierarchy-taist class)
  (defmethod hierarchy-taist java.util.Collection [x] (count x))

  ;; the vector class is not the collection interface

  (class [1 2 3])
  (= (class [1 2 3]) java.util.Collection)
  ;; but it implements the interface, and thus it works!
  (hierarchy-taist [1 2 3])


  ;; multimethods use isa? for the dispatch
  (isa? (class [1 2 3]) java.util.Collection)
  (isa? [1 2 3] java.util.Collection)
  (isa? [1 2 3] [1 2 3])

  (doc isa?)
  ;; true, on
  ;; - equality
  ;; - inheritance (Java)
  ;; - derived values (see: functions derive, make-hierarchy)
  ;; we'll skip (derive/make-hierarchy) - it's not really pretty, a bit awkward and we won't need it here.












;; -------------
;; II. Protocols
;; -------------


;; 8 Defining and Extending Protocols
;; ----------------------------------

  ;; defprotocol - Expression Problem, Haskell Style


  ;; Definition of an interface. A protocol describes a set of functions:
  (defprotocol TheCount ;; ahh, ahh, ahh! 
    (cnt [v]))



  ;; error
  (cnt "foo")

  ;; again, String does not implement our new protocol - let's make it!
  (extend-type String
    TheCount
    (cnt [s] (.length s))) ;; simply call the Java method s.length()

  (cnt "foo")

  ;; again, nil is not special
  (cnt nil)

  ;; a different flavour:  extend-protocol
  ;; it simply swaps type and interface:
  (extend-protocol TheCount
    nil
    (cnt [_] 0))

  (cnt nil)

  ;; now for vectors
  (cnt [1 3])

  ;; yet another version: extend
  (extend java.util.Collection
    TheCount
    {:cnt (fn [k] (count k))})


  (cnt [1 2])
  (cnt '(1 2 3))
  (cnt #{1})
  (cnt {1 2, 3 4})

  ;; Maps are not Collections, but they are Counted


  (extend clojure.lang.Counted
    TheCount
    {:cnt (fn [k] (count k))})

  (cnt {1 2, 3 4})



  ;; extend-protocol is extend-type
  (macroexpand-1
   '(extend-protocol TheCount
      java.io.File
      (cnt [f] (if (.exists f) (.length f) 0))))


  ;; extend-type is extend
  (macroexpand-1
   '(extend-type java.io.File
      TheCount
      (cnt [f] (if (.exists f) (.length f) 0))))




  ;; the documentation explains exactly when to use which function:
  (doc extend-type)
  ;; extend-type allows extension of a single type with several protocols

  (doc extend-protocol)
  ;; extend-protocol allows us to extend many types by the same protocol

  (doc extend)
  ;; extend is the foundation with an ugly syntax ;-)





;; 9 Performance Comparison: Multimethods vs. regular Functions vs. Protocols
;; --------------------------------------------------------------------------
  

  (defmulti m1 class)
  (defmethod m1 clojure.lang.PersistentVector [k] (count k))

  (defn m2 [k] (count k))

  (defprotocol MyProt
    (m3 [this]))

  (extend-protocol MyProt
    clojure.lang.PersistentVector
    (m3 [this] (count this)))

  (m3 [1 2 4 6 7])

  (time (dotimes [i 10000000] (m1 [1])))
  (time (dotimes [i 10000000] (m2 [1])))
  (time (dotimes [i 10000000] (m3 [1])))


;; 10 Concluding Thoughts
;; ---------------------


  ;; Protocols vs. Multimethods

  ;; 1) Multimethods are way more flexible!
  ;;   - Dispatch-Function can be chosen arbitrarily.
  ;;   - Protocols are a special case: type of the first argument

  ;; 2) Protocols are way faster!
  ;;   - Protocols are almost as fast as regular function calls
  ;;   - Multimethods are around eight times slower
  ;;   - There is no reason to use a multimethod if the distach function is type/class





;; 11 Type Hints (optional)
;; ------------------------

  ;; This is not relevant for the exam but for real life applications.

  ;; a propos types and performance:

  ;; if we set this flag to true, the compiler will warn us
  ;; if reflection is used to resolve Java methods oder fields.
  ;; That's very slow!
  (set! *warn-on-reflection* true)

  ;; REPL
  (defn len [x]
    (.length x)) ;; length cannot be resolved, so now we get a warning

  ;; but it still works....
  (len "trololo")


  ;; it takes quite a while
  (time (reduce + (map len (repeat 1000000 "foo"))))


  ;; we can promise the compiler that x will be a String
  (defn lenny [x]
    (.length ^String x)) ;; no Reflection-Warning!

  ;; much faster!
  (time (reduce + (map lenny (repeat 1000000 "foo")))) 


  )
