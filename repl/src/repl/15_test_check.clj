(ns repl.15-test-check
  (:use [repl.15-test-check-b])
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test :as t]))

 
  ;; first of all: ebt and transients


  ;; If you were to implement transients in Clojure
  ;; How would you ensure that your implementation is correct?

  ;; What kind of tests would you write?


  ;; 2-3 minutes
  ;; You don't need to write Clojure code, test scenarios are enough



  ;; short interlude: types of functions (notation)

  ;; many functions take arguments of certain types
  ;; and map to specific types

  ;; Type of (the two-ary) +: Number -> Number -> Number
  ;; this as a curried function.
  ;; + takes an argument of the Number type
  ;; and returns a function that takes a second Number.
  ;; the result of the function is again of type Number.


  ;; there are also type variables (usually a, b, c, ...)
  ;; Type of str: a -> String
  ;; str takes something and makes a string out of it



  ;; Type of identity: a -> a


  ;; For example, lists of one type are written as [string] or [a].
  ;; we assume here that the empty list is also of type [a] - 
  ;; your type system may consider that a different type ;-)


  ;; Type of reverse: [a] -> [a]
  ;; Type of rest: [a] -> [a]
  ;; Type of clojure.string/join: [String] -> String


  ;; if an argument is a function you have to parenthesize:

  ;; Type of map: (a -> b) -> [a] -> [b]
  ;; map receives an arbitrary function that maps type a to type b;
  ;; a and b can also be the same type!
  ;; The second argument is a list of type a
  ;; the result is the list of type b.




  ;; Question: What functions could these be?

  ;; Int -> Int -> Int
  ;; [a] -> Int
  ;; (a -> b) -> ([a] -> [b])
  ;; (a -> Bool) -> [a] -> [a]


  ;; What is the type of reduce?




  ;; back to the topic at hand.

  ;; what if we could describe the input of a function
  ;; and characterize the output accordingly?


  ;; Welcome to test.check!


(comment
  ;; Step 1: Describe the input

  ;; there are a bunch of generators for that:
  gen/nat
  ;; is a generator object

  ;; sample provides a few sample values from the generator
  (gen/sample gen/nat)

  ;; or a little bit more on demand (here 100 pieces)
  (gen/sample gen/nat 100)

  ;; there are not only natural numbers, but many, many more
  (gen/sample gen/boolean)
  (gen/sample gen/char-ascii)
  (gen/sample gen/keyword)
  (gen/sample gen/int)
  (gen/sample gen/any)
  (gen/sample gen/any-printable) ;; so that it does not ring the bell in the terminal, and other control characters do no appear in the input
  (gen/sample gen/string 20)
  (gen/sample gen/string-ascii 20)
  (gen/sample gen/string-alphanumeric 20)

  ;; not so obvious
  ;; Choose for intervals (both sides included)
  (gen/sample (gen/choose 100 250))
  ;; return takes a value and returns a generator that generates only that value
  ;; which means return has the type a -> gen a
  (gen/sample (gen/return 3))

  ;; Composed
  ;; homogenous collections of values
  (gen/sample (gen/vector gen/boolean))
  ;; Vector of fixed length
  (gen/sample (gen/vector gen/boolean 3))
  ;; Tuple with generators per index
  (gen/sample (gen/tuple gen/int gen/boolean))
  ;; Maps of keywords which map to maps of natural numbers mapping to integers
  (gen/sample (gen/map gen/keyword (gen/map gen/nat gen/int)))

  ;; Filter
  ;; non-empty vectors
  (gen/sample (gen/not-empty (gen/vector gen/boolean)))
  ;; specific predicate
  (gen/sample (gen/such-that even? gen/nat) 30)
  ;; if you try this often enough (or are unlucky) you will get an error message:
  ;; Couldn't satisfy such-that predicate after 10 tries. 
  ;; such-that should be used if it is a rather small limitation for the generator.
  ;; If you want to exclude more you should consider another construction,
  ;; e.g. as follows:


  ;; Higher order generators


  ;; Transformation of generated values
  ;; fmap: (a -> b) -> gen a -> gen b
  (gen/sample (gen/fmap str gen/int))

  ;; this is very powerful!
  ;; Here there are three booleans.
  ;; The last element in the vector is a count of how many of them are true.
  (gen/sample 
   (gen/fmap (fn [[x y z :as c]]
               [x y z (count (filter identity c))])
             (gen/vector gen/boolean 3)))

  ;; This is significantly more than a classic type system can usually express!
  ;; This is called a dependent type because it depends on the value
  ;; whether it is in the type or not.




  ;; with 'frequency' you can also give values a certain weight
  (let [x (gen/sample
           (gen/frequency [[70 (gen/return :heads)]
                           [30 (gen/return :tails)]]) 1000)]
    (frequencies x))

  ;; the values do not have to add up to 100
  (let [x (gen/sample
           (gen/frequency [[2000 (gen/return :heads)]
                           [2000 (gen/return :tails)]]) 1000)]
    (frequencies x))

  ;; Selection from a fixed collection of values
  ;; elements: [a] -> gen a
  (gen/sample (gen/elements [1 2 3]))
  ;; an "or" of generators:
  ;; one-of: [gen a] -> gen a
  (gen/sample (gen/one-of [gen/int gen/boolean]))


  ;; 1000 values from the range 0 to 10 that are not 5
  (let [g (gen/such-that
           (fn [e] (not= e 5))
           (gen/choose 0 10))
        sample (gen/sample g 1000)]
    (frequencies sample))


  ;; Complex Generators
  ;; A tuple consisting of a vector and an element from this vector

  ;; first we need a non-empty vector
  (def vector-gen (gen/not-empty (gen/vector gen/int)))
  (gen/sample vector-gen)

  ;; given a vector, return the vector and select an element from it
  (defn tuple-from-vector-gen [v] 
    (gen/tuple (gen/return v)
               (gen/elements v)))
  (gen/sample (tuple-from-vector-gen [1 2 3]))


  ;; and the final step:
  ;; the results from one generator must be passed into the next generator.
  ;; This is what the 'bind' does.
  ;; bind: gen a -> (a -> gen b) -> gen b
  (def complex-gen (gen/bind vector-gen tuple-from-vector-gen))
  (gen/sample complex-gen 20)
)






  ;; bind and return?
  ;; keep this in mind (for a while)






  ;; So what does this have to do with testing?







  ;; The idea of Property-Based Testing:
  ;; Specify the relation between input and output as a predicate.
  ;; Throw random inputs into the function and check the Output.


  ;; Generally there are three patterns:
  ;; 1. Test against an inverse function
  ;;    (e.g. Parsing + Pretty Printing, Serialization + Deserialization, ...)
  ;; 2. Test against an existing implementation (an oracle)
  ;; 3. Test by characterization of certain properties

(comment

  ;; sometimes it is difficult to find the right property

  ;; back to example-based-testing
  ;; my-sort was defined somewhere else, don't cheat by looking it up
  ;; here are a few test cases for the sort function

  (t/are [x y] (= x y) 
    (my-sort [1]) [1]
    (my-sort [1 2]) [1 2]
    (my-sort [5 1 3 2 4]) [1 2 3 4 5]
    (my-sort [1 2]) (my-sort [2 1])
    (my-sort [1 3 2 4 5]) (my-sort [5 1 3 2 4]))


  ;; incorrect test cases really do fail
  (t/are [x y] (= x y) 
    (my-sort [1]) [3])


  ;; all tests passed!
  ;; the implementation must CERTAINLY be correct!



  ;; Is that enough test cases?

  ;; of course not!
  ;; we now generate test cases instead of writing them ourselves
  (def vectors-of-numbers (gen/vector gen/int))

  (gen/sample vectors-of-numbers)

  ;; We use an oracle, which is an implementation that is correct.
  ;; This is quite useful when you want to replace an old
  ;; (correct but possibly not optimal) implementation

  ;; sort should do the trick
  (defn sortiert? [v]
    (= (sort v) v))

  (sortiert? [])
  (sortiert? [1 2])
  (sortiert? [2 1])


  ;; we check the following property with generated input now:
  ;; The return value of a sort function should be sorted:
  ;; for all data generated by the generator, the property should hold.
  (def sortiert-prop (prop/for-all [data vectors-of-numbers] 
                                   (sortiert? (my-sort data))))


  ;; 100 test cases, go!
  (tc/quick-check 100 sortiert-prop) 
  ;; oops, forgot about the empty collection


  ;; A macro exists to convert test.check into a clojure.test test case
  ;; (defspec qs-sorted 100 sortiert-prop)


  (t/is (= (my-sort []) []))
  ;; returns nil instead of []


  ;; Attempt two, we fix the empty collection case
  (def sortiert-prop (prop/for-all [data vectors-of-numbers] 
                                   (sortiert? (my-sort2 data))))


  (my-sort2 [])
  (my-sort2 [3 4 1])
  ;; 100 Test cases, go!
  (tc/quick-check 100 sortiert-prop) 
  ;; 1000 Tests to make sure?
  (tc/quick-check 1000 sortiert-prop) 




  ;; all tests passed!
  ;; the implementation must CERTAINLY be correct!





  ;; Another characteristic that we missed in the heat of the moment: 
  ;; All original values should be preserved.
  ;; A sort function that always returns the empty vector is not useful.

  (defn permutation? [v1 v2]
    (= (frequencies v1) (frequencies v2)))


  (def permutation-prop
    (prop/for-all [data vectors-of-numbers]
                  (permutation? data (my-sort2 data))))


  ;; with this specific seed it is guaranteed to break
  (def r (tc/quick-check 10 permutation-prop :seed 1422980091656)) 

  (:fail r)  ;; this is the failing test that was found

  r  ;; this is the entire result

  ;; and now for the amazing part:
  ;; test.check minimizes the input for the function
  ;; and can give a minimal example that fails
  (-> r :shrunk :smallest)


  (t/is (= (my-sort2 [0 0]) [0 0]))
  ;; Duplicates are discarded...



  ;; Implementation 1
  (defn my-sort [coll]
    (into (sorted-set) coll))



  ;; Implementation 2
  (defn my-sort2 [coll]
    (into [] (into (sorted-set) coll)))






  )




(comment
  ;; Back to the transient example

  ;; When we have a sequence (-> #{} (conj 1) (conj 2) (disj 0))
  ;; Then we can increase performance by
  ;; 1) First calling 'transient'
  ;; 2) Replace conj with conj! and disj with disj!
  ;; 3) Finally calling 'persistent!'


  ;; Note:
  ;; There is an API with pre- and postconditions for the API calls.

  ;; eg..: We can call transient and persistent! more than once, but only
  ;; pairwise transient ... persistent! ... transient ... persistent! ...
  ;; but not transient ... transient ... persistent! ... persistent!


  ;; Idea: 
  ;; Generate many sequences of API calls that are allowed
  ;; and verify the postconditions.
  
  ;; Actually generating (correct) code is a bit more involved.
  ;; Alternatively, however, we can generate instructions
  ;; and have them executed by a small interpreter.
  ;; If the precondition is not met, the instruction is simply ignored.


  (defn transient? [x]
    (instance? clojure.lang.ITransientCollection x)) 


  ;; Our instructions should be in the form
  ;; [:conj number], [:disj number], [:trans], or [:pers].


  ;; How do we proceed?
  ;; 1) Generate all actions

  ;; we can call transient and persistent!...
  (gen/sample (gen/elements [[:trans] [:pers]]))
  ;; or conj + Zahl and disj + Zahl
  (gen/sample (gen/tuple (gen/elements [:conj :disj]) gen/int))

  ;; we combine them with one-of and want a (non-empty) series of them (vector)
  (def gen-mods
    (gen/not-empty (gen/vector (gen/one-of
                                 [(gen/elements [[:trans] [:pers]])
                                  (gen/tuple (gen/elements [:conj :disj]) gen/int)])))) 


  (gen/sample gen-mods)
  ;; each sequence of calls is a test case!

  ;; 2) Run the actions on the empty set.
  ;;    Invalid sequences are automatically repaired by the interpreter

  (defn run-action [c [f & [arg]]] 
    (condp = [(transient? c) f]
      [true   :conj]          (conj! c arg) ;; if it is a transient, we us transient-conj!
      [false  :conj]          (conj c arg)  ;; if it is persistent, the ordinary conj
      [true   :disj]          (disj! c arg)
      [false  :disj]          (disj c arg)
      [true   :trans]         c             ;; We do not call transient on a transient
      [false  :trans]         (transient c) ;; We call transient on persistent collections
      [true   :pers]          (persistent! c) ;; We call persistent! on transient
      [false  :pers]          c))             ;; We do not call persistent! on persistent data structures

  ;; with this we can process any series of instructions!

  (run-action #{} [:conj 2])
  (run-action #{} [:trans])
  (persistent! (run-action (transient #{1}) [:disj 1]))

  ;; so that we process many instructions, we simply reduce the actions
  ;; providing the function, with the set as accumulator
  (defn reduce-actions [coll actions]
    (reduce run-action coll actions))

  (reduce-actions #{} [[:conj 3]])
  (reduce-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:pers]])
  (reduce-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]])

  ;; and a helper function, to avoid some goofy stuff:
  ;; if a transient is returned, we make it persistent again
  (defn apply-actions [coll actions] 
    (let [applied (reduce-actions coll actions)]
      (if (transient? applied)
        (persistent! applied)
        applied)))

  ;; done!
  (apply-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]])

  ;; 3) We discard all transient- and persistent!-calls.
  ;; This is our reference, calling only conj and disj on the persistent data structures.
  ;; apply-actions always converts the collection into a transient
  ;; and back into a persistent data structure.

  (defn filter-actions [actions] 
    (filter (fn [[a & args]]
              (#{:conj :disj} a))
            actions))

  (filter-actions [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]])


  ;; 4) If transients work correctly, the result must be the same
  ;; as if we had never used transients.

  (def transient-property 
    (prop/for-all
     [a gen-mods]
     (= (apply-actions #{} a) ;; uses persistent sets and transients
        (apply-actions #{} (filter-actions a))))) ;; only uses persistent sets


  ;; this looks fine (depending on luck)
  (tc/quick-check 100 transient-property)
  (tc/quick-check 1000 transient-property)

  ;; but...
  (def r (tc/quick-check 400 transient-property :seed 1422983037254))

  ;; there is this weird sequence here
  (:fail r)
  ;; it took 119 steps to create it
  (count (first (:fail r)))

  ;; where is the error...? Whew...

  ;; Shrinking to the rescue!
  (def full-seq (-> r :shrunk :smallest first))

  full-seq
  ;; No more actions can be omitted without the test failing.

  (let [le-seq [[:conj -49] [:conj 48] [:trans] [:disj -49] [:pers]]] ;; without [:conj -49]
    (= (apply-actions #{} le-seq)
       (apply-actions #{} (filter-actions le-seq))))

  (= (apply-actions #{} full-seq) 
     (apply-actions #{} (filter-actions full-seq)))

  ;; actually, it's even worse:

  (= (apply-actions #{} full-seq)
     (apply-actions #{} full-seq))

  ;; What exactly is not the same?

  (t/is (= (apply-actions #{} full-seq) 
           (apply-actions #{} (filter-actions full-seq))))
  ;; WTF - {48 -49} does not equal {48 -49}?

  ;; the problem:
  (hash -49)
  (hash 48)

  ;; Problem: HashCollisionNode
  ;; All values that generate hash collisions are stored in an array.
  ;; With persistent data structures, dissoc / disj shrinks this array.
  ;; With transients the values are simply overwritten with null.
  ;; However, the array length then no longer matches the element count!
  ;; How many of you would have thought of this test case?


  ;; We needed an old clojure for this.
  *clojure-version* ; =>  {:major 1, :minor 5, :incremental 1, :qualifier nil}


  ;; fixed in Clojure 1.6



  ;; Bug: http://dev.clojure.org/jira/browse/CLJ-1285
  ;; The bug was actually found using this method!
  ;; https://groups.google.com/forum/#!msg/clojure-dev/HvppNjEH5Qc/1wZ-6qE7nWgJ


  ;; As a result, there is a special library
  ;; for testing data structure implementations
  ;; https://github.com/ztellman/collection-check


)





