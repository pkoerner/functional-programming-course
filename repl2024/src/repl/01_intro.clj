(ns repl.01-intro)

;; this REPL session covers:
;; - how and when to use Clojure's built-in syntactical elements (numbers, strings, symbols, keywords, regex, maps, ...)
;; - basic core functions and their semantics
;; - fundamental special forms (def, let, fn, do, if)
;; - some first function definitions
;; - beta reduction
;; - a glance at macros


;; 1 Accessing Documentation etc.
;; 2 A Closer Look at Names: Symbols vs Keywords
;;   - When to use symbols
;;   - When to use keywords
;;   - Naming Conventions
;; 3 Strings and Regex Functions
;;   - Basic String operations
;;   - print functions
;;   - regex syntax and operations
;; 4 Basic Arithmic Functions
;; 5 Basic Functions on Collections
;;   - Creation of lists, vectors, maps, sets
;;   - Accessing functions
;;   - Inserting elements
;;   - Removing elements
;;   5.1 Idiom: maps are callable, keywords are callable
;; 6 Variable Number of Arguments
;;   6.1 Unpacking the last list: apply
;; 7 Basic Language Building Blocks: Special Forms
;;   7.1 def and let
;;   7.2 Interlude: Immutability
;;   7.3 Functions and the Evaluation Mechanism
;;   7.4 Control Structures: do, if 
;;   7.5 Exceptions (try, catch, finally)
;;   7.6 Putting it all together: Our first factorial functions
;; 8 An Overview of Useful Functions on lists, vector, sets and maps

;; Note: this session is rather long, but most parts are easy to process and give a shallow overview.
;; Worth spending additional time on to understand all the details are sections 6 and 7.
;; It might be worth taking notes during sections 5 and 8 to build a vocabulary of functions (or, alternatively, you can work with the cheatsheet).






;; Note: the content of REPL sessions is wrapped in a (comment ,,,) block,
;; so the code does not executed on load (as some calls may raise errors)

(comment


;; 1 Accessing Documentation etc.
;; ------------------------------

  (use 'clojure.repl) ;; for apropos, doc and source
  (use 'clojure.walk) ;; for macroexpand-all

;; Print the documentation for a function
  (doc doc)
  (doc source)
  (doc apropos)

;; search defined functions
;; useful for when you approximately know how something would be named 
  (apropos "map")

;; Print source code (Caution, long!)
  (source map)
;; The Clojure library functions are optimized for multiple arities

;; Online Material:
;; http://clojure.org/cheatsheet links to
;; https://clojuredocs.org/ which has many great example calls.


;; Reminder: (Session 0, Section 1.3) 
;; nil is the exact same as null in Java!
  (- nil)
;; NullPointerExceptions are very rare in pure Clojure code.
;; (Almost) all Clojure library functions can deal with nil (See: nil punning)
;; You have to try hard and call into Java code to cause one


;; 2 A Closer Look at Names: Symbols vs Keywords
;; ---------------------------------------------


;; Two kinds: symbols and keywords

;; Symbols are identifiers like in any other language. They represent something
  inc
  +'
  blah!
  *clojure-version*
  (type *clojure-version*)

  (quote *clojure-version*)

;; Quote prevents the evaluation of symbols
;; cmp.: Düsseldorf contains a ü - The letter is physically somewhere in the city
;; "Düsseldorf" contains a ü - The letter is somewhere in the name of the city

  (type (quote *clojure-version*))

  foo
  (quote foo)
;; Convention: Symbols with ! (Bang) caution you that a side-effect is associated with them
  (quote foo!)
;; Convention: Symbols with ? (Qmark) represent predicates, so something that returns either true or false
;; Example: 'even?' as in (even? 2)
  (quote foo?)
;; Symbols with dots before or after involve host interop (addressed later in the course)
  (quote Foo.)
  (quote .foo)

  ;; Convention: In Clojure, we prefer the lisp-case (or kebab-case)
  ;; fooBar in Java or foo_bar in C would be
  (def foo-bar 3)
  foo-bar
  ;; typically, languages do not allow - in identifiers (as it is reserved for subtraction)
  ;; thus, using - in identifiers gives Clojure developers a sense of superiority ;-)

  ;; identifiers can handle a bunch of weird characters (even emojis) - use at your own risk
  (def ffds4¼½³’ 42)
  ffds4¼½³’

;; ' is reader syntax für quote - we will talk about it in more detail when we talk about macros
;; these two expressions are the same after the read in read-eval-print-loop (REPL):
  'blah!
  (quote blah!)


;; Keywords begin with a ':'. Keywords represent themselves
  :foo
  (type :foo)
;; Versatile  usage:
;; - Keys in maps 
;; - Keywords in APIs
;; - Wherever you would use string constants in Java


;; 3 Strings and Regex Functions
;; -----------------------------


;; str returns the concatenation of all of its arguments' .toString() return value
  (str "foo" \f)
  (str "hello "
       "world")
;; This is a little unusual, especially since '\n' has the conventional meaning within strings
  (str \n)

  (str \newline)
  (str "Hello" "world")

  (str "Hello" \space "world")

  (str "Hello" \newline "world")

;; println prints things + a newline character
  (println \u03bb "Calculus" "ftw!")
;; Only print a newline character
  (newline)
;; No newline character
  (print "plonk")
;; Re-readable (e.g. strings get quotation marks) (or pr without newline character) 
  (prn "test")

;; ---------- Regex

  #"([0-9]{4})/([0-9]{2})/([0-9]{2})"
  (type #"([0-9]{4})/([0-9]{2})/([0-9]{2})")

;; Literal syntax for Java's patterns
  (re-seq #"A(.*?)B" "ACBAAABBBBBSSSSYYYAJHDHGHJ")


;; 4 Basic Arithmic Functions
;; --------------------------

;; inc adds +1
  (inc 4)
;; what happens when you add 1 to MAX_LONG?
  (inc 9223372036854775807)
  (+ 9223372036854775807 1)

;; Sensible semantics: No under-/overflows, instead exceptions
;; unless you use unsafe operations, e.g. unsafe-add

;; In case the result is a larger value than fits in a long, there exist operations with followed by '
;; aka auto-promotion
  (inc' 9223372036854775807)
  (*' 9223372036854775807 9223372036854775807)

  (*' 9223372036854775807 9223372036854775807)
;; *1 is the last result in the REPL
  (type *1)
  (type (*' 9223372036854775807 9223372036854775807))


;; Division returns a fraction
  (/ (inc 32) 7)
; Reminder: (inc 32)/7 ;; is not valid!


;; 5 Basic Functions on Collections
;; --------------------------------

;; Defining lists
  (list 1 :two 3)
  '(1 2 3)
  (type '(1 2 3))

;; Vectors
  [1 2 ,,3,,,4]
  (vector 1 2 3 4)
  (type [str, 2, :foo, \space])

;; 0-indexed access
  (nth [1 2 3] 2)
;; conj: insert the element somewhere in the data structure
  (conj [1 2 3] 1)
  (conj '(1 2 3) 1)
;; the position is depended on the data structure
;; in Clojure's standard data structures elements are inserted at the position where it is most efficient

;; typical access pattern: handle the first element, then deal with the rest (recursion!) 
(first [1 2 3])  
(rest [1 2 3])  

;; note: Clojure has the concept of *structural equality*, meaning lists and vector are equal
;; as long as their elements are equal and in the same order
(= (list 1 2 3) [1 2 3])

;; thus, we usually do not care too much about the actual type
;; more details in the unit on data structures
;; For now (and the exam): it is okay if you return a vector if a list is returned in the example calls (and vice versa),
;; unless states otherwise.

;; Maps
  {:name "Bendisposto", :first-name "Jens" :age :uHu}
  (hash-map :name "Körner" :first-name "Philipp" :age :biVi)
  (type {:key1 "foo", 2 9})

;; Every value can be a key
  {[1] \n, [] \l, :foo 1}

;; Key-Lookup happens via =
  (get {[1] \n, [] \l} '())
  (= '() [])


;; Different data structures can be equal, but not identical
  (= '(:a :b) [:a :b])
  (identical? '(:a :b) [:a :b])


;; Inserting key-value pairs: assoc
  (assoc {:foo 1} :bar 2)
  (assoc {:foo 1} :bar 2 :baz 3 :boing 4)
;; Removing a key: dissoc
  (dissoc {:foo 1, :bar 2, :baz 3, :boing 4} :foo)

;{:kaboom 1 :kaboom 2}
;{:kaboom 1 :kaboom 1}

;; If the key is already present, assoc overwrites the mapping
  (assoc {:kaboom 1} :kaboom 2)


;; Sets
  #{2 3 5 7 11}
  (type #{str, 2, :foo, \space})
;; inserting: conj
  (conj #{1 2 3} 4)
;; removing: disj
  (disj #{1 2 3} 2)


;; arbitrary nesting
  (def n [{:name "Bendisposto", :first-name "Jens" :age :uHu :lotto-numbers [1 3 5 15 21 44]}
          {:name "Witulski", :first-name "John" :age :bivi}])

  (get n 1)
  (get (get n 1) :name)
;; or: using 'get-in' and a 'path' in the data structure
  (get-in n [1 :name])


;;   5.1 Idiom: maps are callable, keywords are callable
;;   ---------------------------------------------------


;; Maps and vectors are functions, which take a key to look up in themselves
;; In the case of vectors this key is a position
  (n 1)
  ((n 1) :name)

;; Keywords are functions, which take a map to look themselves up within
  (:name (n 0))






;; 6 Variable Number of Arguments
;; ------------------------------

  (+ 1 2 3 4 5 6 7)
  (+ (* 3 4) 11 112)

;; Question: What is a 'reasonable' value for (*) and (+)?
  (*)
  (+)

;; the neutral elements of multiplication / addition

;;   6.1 Unpacking the last list: apply
;; ------------------------------------

;; ---------- apply
;; + is a function which takes multiple arguments. You however cannot pass a collection to it.
;; apply 'unpacks' its last argument
;; (apply + [1 3]) -> (+ 1 3)
;; (apply + 1 2 [3 4 5]) -> (+ 1 2 3 4 5)
  (+ [1 2 3])
  (apply + 5 6 7 [1 2 3])

  (apply + [1 [2 2]])
;; -> (+ 1 [2 2]), therefore throws an exception

;; apply is an important function and it is useful more often than you might expect now.
;; Keep it in mind.


;; 7 Basic Language Building Blocks: Special Forms
;; -----------------------------------------------

;; The apply function is a good transition to special forms:
;; While it is a "regular" function, we cannot implement it ourselves
;; (fun fact: it is featured on the cover of the book "Structure and Interpretation of Computer Programs") 
;; Most functions, we could implement ourselves (if we knew how).
;; Yet, there are some fundamental basic blocks that are required to exist in the language.
;; They are so-called special forms (as the compiler treats them different. We come back to this at the end of the course.)


;;   7.1 def and let
;; -----------------


;; ---------- Definition (special form: def)

;; pi does not exist yet
  pi
  (def pi 3) ; Pi is exactly 3
  pi

;; okay, a little more precisely
  (def π 3.141592653589793238M)
  (* 2 π)


;; Redefining things is frowned upon

;; Why?

;; in the REPL you do it for experiments
;; in production code you will experience a horrible day with state...
;; On this occasion: That same code should now run concurrently. Have fun!


  (def fancy-calculation identity)


;; WARNING
;; The following code has been written by a group of experts for demonstration purposes.
;;     Please
;;     DO NOT
;; Try this at home

  (defn my-terrible-idea [x]
  ;; do not try this at home
    (def y (fancy-calculation x))
  ;; more code that uses y (or not)
    (println y))

;; A def within another def is the equivalent of a goto.
;; You *could* do it, but you risk getting eaten by a raptor (https://xkcd.com/292/)
;; or getting zero points in the exam.

;; If you need local variables, use let
;; If you need global state, you're doing it wrong


;; ---------- local bindings (special form: let)

;; this is not defined
  whargbl

;; let binds symbols to a value - but only inside the block!
  (let [whargbl 42]
    whargbl)

;; still does not exist
  whargbl

;; let supports any number of pairs of symbols and values
  (let [a 3
        b 42]
    (* a b))

;; the most inner binding catches first
  (let [a 3
        a (+ a 1)] ;; The old a is not overwritten! But you cannot access it anymore...
    (println :location1 a)
    (let [a 0]
      (println :location2 a)))

;; the last computed value is returned
  (let [x :whatever]
    1
    2
    3)

;; this enables side effects, e.g. for logging, debugging etc.
  (let [x (+ 1 1)] ;; complicated calculation
    (println :debug x)
    x)

;; also possible
  (let [x (+ 1 1)
        _ (println x)]
    x)
;; _ does not have an exceptional semantic (like in Prolog) and is an ordinary symbol!
;; It is just convention to name symbol '_' you do not use 



;;   7.2 Interlude: Immutability
;; -----------------------------


  (def v [1 2 3])
  v
;; All functions that 'modify' a collection
;; return a new collection, the original collection
;; remains unchanged
  (concat v [4 5 6])
  v


;;   7.3 Functions and the Evaluation Mechanism
;; --------------------------------------------

;; λx.x+3
;; (fn [argument-vector] body)
;; the return value is the last form in the body to be evaluated 
  (fn [x] (+ 3 x))

  (def square (fn [x] (* x x)))

  (def sum-square (fn [x y] (+ (square x) (square y))))

;; defn is syntactic sugar for (def (fn ...))
  (defn sq2 [x] (* x x))
  (sq2 15)

;; the proof
  (macroexpand-1 '(defn sq2 [x] (* x x)))

;; multiple-arity definition:
(defn sum-3
  "nobody needs to add more than three numbers" ;; docstring
  ([] 0)
  ([n] n)
  ([n m] (+ n m))
  ([n m o] (+ (+ n m) o)))

(sum-3)
(sum-3 1 2)
(doc sum-3) ;; you can find the docstring again, hooray!

;; If you know that you are guaranteed to receive at least one argument and possibly some more
  (defn variadic-args-function [x & args]
    (println :x x :args args))

  (variadic-args-function 1 2 3 4)

;; Evaluation of functions
;; Mechanism: beta reduction

;; To evaluate an application
;; 1) Resolve the first element to get the function
;; 2) Evaluate the rest of the elements to get the arguments for the function
;; 3) Apply the function to the arguments
;;     - copy the function body and substitute the formal
;;       parameters with the operands
;;     - evaluate the resulting body

;; Example: Sum of squares

  (sum-square 3 4)

  (+ (square 3) (square 4))
  (clojure.core/+ (square 3) (square 4))
  (+ (* 3 3) (square 4))
  (+ 9 (square 4))
  (+ 9 (* 4 4))
  (+ 9 16)
  25

;;   7.4 Control Structures: do, if 
;; ---------------------------------

;; do combines multiple expressions into one and returns the last value 
  (do (+ 1 2) :a "yo")

;; Forms that contain a 'body', e.g. fn, let, etc. implicitly wrap a do-block around it
  (fn [x] (println x) x)
;; is the same as
  (fn [x] (do (println x) x))


;; (if condition then-branch else-branch)
  (if true 1 2)
  (if false 1 2)

;; The values triggering the then-branch are called "truthy",
;; the values triggering the else-branch "falsey".
;; nil and false are falsey, everything else truthy
  (if 1 2 3)
  (if :doh 2 3)
  (if nil 1 2)

;; 'if' is something _special_ and not a function!
  (if true (println 1) (println 2))
;; A function would evaluate both branches!

;; What if I want to do two or more things in a branch?
;; e.g. print something to the terminal and return a value
;; The answer is a 'do'
  (if (= 2 (+ 1 1)) ;; condition
    (do (println :hello) :the-math-checks-out)       ;; then-branch
    :broken-math)  ;; else-branch


;; cond-macro: nested ifs
  (cond
    (= 1 2) 1   ;; is (= 1 2) true? return 1
    (< 1 2) 2   ;; is (< 1 2) true? return 2
    :else 3)    ;; :else is always truthy, i.e. if no condition was true return 3

;; the "default" case is not set! :else was used as an arbitrary value, that is always truthy
  (cond
    (= 1 2) 1
    (> 1 2) 2
    :otherwise 3)

;; or
  (cond
    (= 1 2) 1
    (> 1 2) 2
    :true 3)
;; are equally valid

;; 'if-not' and 'cond' are syntactic sugar

;; 'if' is something fundamental (a special form) 
  (macroexpand-all '(if 1 2 3))

;; 'cond' are just many 'if's in a trench coat
  (macroexpand-all '(cond (= 1 2) 2
                          (= 2 2) 4))


;; 'if-not' is just (if (not ...) ... ...)
  (macroexpand-all  '(if-not false 1 2))



;;   7.5 Exceptions (try, catch, finally)
;; ------------------------------------------

;; Exceptions (special form: try)
;; try with catch and finally, same as in Java
  (defn reciprocal [x]
    (try (/ 1 x)
         (catch Exception e :inf)
         (catch IllegalAccessError e :oh-no)
         (finally (println "done."))))

  (reciprocal 2)
  (reciprocal 0)

;; the sound an uncaught exception makes, referencing a tool developed at ETH Zurich
  (defn rodäng!!! [] (throw (RuntimeException. "RodängDBException")))
  (rodäng!!!)

;; This actually calls a Java-constructor. More on that later. 
  (def e (RuntimeException. "RodängDBException"))

;; Superclasses of the exception
  (supers (type e))




;;   7.6 Putting it all together: Our first factorial functions
;; -------------------------------------------------------------

  (defn ! [n]
    (if (= 1 n)
      1
      (* n (! (dec n)))))

  (! 10)

  (! 30)

;; * causes an overflow if the result does not fit in a long

  (defn ! [n]
    (if (= 1 n)
      n
      (*' n (! (dec n))))) ;; Solution: use *' for auto promotion

  (! 30)

  (! 10000)

;; we will come back to this issue in the next session (23-hof)

;; time returns the value produced by its argument and prints the elapsed time to the terminal
;; we return :ok so we do not measure the overhead of how long it takes to print a large number
  (time  (let [x (! 3000)] :ok)) ;; 3000 should work - check your JVM stack size otherwise



;; 8 An Overview of Useful Functions on lists, vector, sets and maps
;; -----------------------------------------------------------------

;; Lists

;; Only the first 40 elements and values 5 nesting-levels deep are printed
;; This is used to prevent the REPL from spamming the terminal
;; Printing of arbitrary nested collections can be re-enabled by passing the value nil
;; Additionally this prevents some following calls from being non-terminating...
  (set! *print-length* 40)
  (set! *print-level* 5)

;; The elements from 0 (inclusive) to 10 (exclusive)
  (range 10)
;; from 10 (inclusive) to 20 (exclusive)
  (range 10 20)
;; from 1 to 100 (exclusive) in steps of 10
  (range 1 100 10)

;; Woah! infinite lists
  (range) ;; all natural numbers (including 0)
  (nth (range) 4756) ;; 0-indexed






;; seq
;; a small collection of elementary functions to handle sequences
  (first [1 2 3])
  (first (list 1 2 3))
  (first #{2 1 3}) ;; depending in which order the elements were inserted into the set another element is returned
  (rand-nth (seq #{2 3 4 5 6})) ;; random

;; maps can be converted to sequences of key-value tuples
  (first {:b 2 :c 3 :a 1})
  (first [])
  (first nil)

  (rest [1 2 3])
  (rest (list 1 2 3))
  (rest #{4 3 2 1})
;; rest (often) changes the type of the data structure
  (type  (rest [1 2 3]))
  (type  (rest (list 1 2 3)))
  (type  (rest #{4 3 2 1}))
  (rest {:a 1 :b 2 :c 3})

;; what once was a map does not always stay one...
  (conj (rest {:a 2}) [:1 1])

  (rest [])
  (rest nil)

;; cons adds an element to the front of the sequence
  (cons 1 [2])
  (cons 2 3)

;; conj inserts wherever is most efficient
  (conj [2 3] 1)
  (conj (list 2 3) 1)

;; empty returns an empty collection of the corresponding type
  (empty [1 2])

;; into inserts a sequence into an existing collection
  (into [1 2 3] (list 4 5 6))
  (into #{} [4 5 6])


  (mm inc [1 2 3])
  (mm inc '(1 2 3))
  (mm inc #{1 2 4})

;; If the type of a data structure is important you can explicitly create it
  (vec '(1 2 3))
  (set '(1 2 3))
  (list* [1 2 3])

;; Vectors are not lazy by the way!
  (def x (time (into [:a :b] (range 30000000))))
  (type x)
;; last has linear runtime
  (time (last x))
  (time (last x))

  (count x)


;; Why is that? historical reasons...
  (doc last)
  (source last)

;; cumbersome, but this is how to access the last element of a vector efficiently (or use 'peek')
  (time (nth x (dec (count x))))

;; This may change in a future clojure version...
;; usually you explicitly access the last element
;; In most use cases you access the head and go into a recursion for the rest
;; or directly use a higher-order function


;; Adding a million elements from the above vector into a list takes a while
  (def y (into '() x))

;; seq instead transforms it in constant time!
  (def y (time (seq x)))
  (type y)
;; last is nevertheless slow :-(
  (time (last y))

(distinct [])


;; Maps
;; A mapping of keys unto values
;; for values use: vals
  (keys {:a 1 :b 2 "foo" 3 nil 4})

;; Two different objects with the same information are the same!
  (= {:a 1 :b 2 :c 3} (assoc {:a 1 :b 2} :c 3))

;; lookup via get (optionally takes a default value)
  (get {:a 1, :b 2} :b)
;; or via map or keyword as function
  ({:a 1, :b 2} :b)
  (:b {:a 1 :b 2})
;; with default value as well
  (:c {:c 1} "default")
  (:c {:a 123} "default")

;; for
;; for is lazy und generates a sequence of values
  (for [user ["Itchy" "Scratchy"]]
    (str user " for president!"))

;; different collections are nested
  (let [xs [1 2 3]
        ys [4 5 6]]
    (for [x xs
          y ys]
      [x y]))
;; can be read as
;; result = ()
;; for x in xs:
;;    for y in ys:
;;     ... ;; if additional collections follow
;;        result.addLast((x, y))
;; return result

;; all combinations of x and y
  (for [x [1 2 3 4] y [1 2 3 4]] [:tuple x y])
;; here :when is actual syntax
;; elements are added when they fullfil a condition
  (for [x [1 2 3 4] y [1 2 3 4] :when (<= y x)] [:tuple x y])



;; Following are some very useful functions in the standard library

;; (!!) If you catch yourself thinking 'this function on a collection would be useful', it often already exists!
;; https://clojure.org/api/cheatsheet
;; links to https://clojuredocs.org/ (with example calls)
;; the sections on sequences and collection are always worth a look while programming

  (reverse [1 2 3])

  (concat [1 2 3] [4 5 6])

  (count [1 1 1 1])

  (interleave [1 1 1 1] [2 2 2 2] [3 3 3 3])
  (interleave [1 1 1 1] [2 2])
  (interpose "1" [2 2 2 2 2 2])

;; especially useful when
  (println (apply str (interpose \newline [1 2 3])))

  (repeat 5 :a)

  (take 4 (range))
  (take 5 (repeat "a"))
  (take 10 (repeat "a"))

  (drop 5 (range 14))

  (last [1 2 3])
  (butlast [1 2 3])

  (first [1 2 3])
  (second [1 2 3])
  (nth [1 2 3] 2)
  (nth (range 1000) 33)

  (range 13)
;; Creating tuples of three values from a sequence
  (partition 3 (range 13))
;; with overlap
  (partition 3 2 (range 13))
;; Changing behavior when a sequence runs out of values
  (partition 3 3 (repeat :a) (range 13))
  (partition 3 3 [] (range 13))

  (take 20 (cycle [1 2 3]))

;; Set operations
  (use 'clojure.set)
  (clojure.set/union #{1 2 3} #{2 3 4 5 6})
  (clojure.set/difference #{1 2 3} #{2 3 4 5 6})
  (clojure.set/intersection #{1 2 3} #{2 3 4 5 6})

;; Caution: both arguments MUST be sets
;; otherwise strange things happen
  (clojure.set/intersection #{1 2 3} [3 4 5]))

