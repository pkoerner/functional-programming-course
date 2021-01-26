(ns repl.16-transducer
  (:require [clojure.core.reducers :as r])
  (:use clojure.repl))


(comment

  ;; a look at map
  ;; map takes something sequential and returns a LazySeq
  (map inc [1 2 3])
  ;; Same but different: returns the content in a vector
  (mapv inc [1 2 3])
  ;; what about sets?
  (map inc #{1 2 3})
  ;; you have to create your own version that returns sets
  (into #{} (map inc #{1 2 3}))
  ;; there is also a parallel version of map
  (pmap inc (range 100))
  ;; in core.async you can apply a function to all values a channel receives
  ;; (core.async/map< inc channel)




  ;; How to implement a simple map:
  ;; For simplicity's sake:
  ;;  - unary functions
  ;;  - no laziness nor chunking

  (defn map2 [f c]
    (when (seq c)
      (cons (f (first c))
            (map2 f (rest c)))))
  (map2 inc (range 19))
  ;; the stack overflows with sequences that are too long
  (map2 inc (range 1900000))

  (map2 inc [1 2 3])

  ;; better alternative: Reconstruct map with reduce
  (defn map3 [f c]
    (reduce (fn [a e]
              (conj a (f e))) [] c))

  ;; technically this is mapv,
  ;; but we are not concerned about it for the time being
  (map3 inc [1 2 3])


  ;; filter could be recreated like this
  (defn filter3 [p c]
    (reduce (fn [a e]
              (if (p e)
                (conj a e)
                a))
            [] c))

  (filter3 even? (range 10))

  ;; mapcat via concat
  (defn mapcat2 [f c]
    (reduce (fn [a e]
              (concat a (f e))) [] c))

  (mapcat2 (fn [e] [e e e]) [1 2 3])


  ;; or using reduce twice
  (defn mapcat3 [f c]
    (reduce (fn [a e]
              (reduce conj a (f e))) [] c))

  (mapcat3 (fn [e] [(inc e) e (dec e)]) [1 2 3])


  ;; These all look similar ...


  ;; There are two aspects that are complected here!
  ;;   - the data structure itself (by using concat / conj)
  ;;   - the essence of map / filter / ...
  ;;     (Transformation of values with function f,
  ;;      check if element is to be included)

  ;; What do we do if we have a new data structure without conj
  ;; (Java streams, core.async channel, GUI elements) ?


  ;; We want to try to get rid of the conj.
  ;; We do that by extracting it as a parameter.
  (defn mape [f]
    (fn [step]
      (fn [a e] (step a (f e)))))

  ;; our concrete map function (from collection and into a vector)
  ;; then looks like this
  (defn map4 [f c]
    (reduce ((mape f) conj) [] c))

  ;; reduce handles the input type (collection)
  ;; conj the output type (vector in this case)

  (map4 inc [5 2 4])

  ;; the same with filter
  (defn filtere [p]
    (fn [step]
      (fn [a e] (if (p e)
                  (step a e)
                  a))))

  ;; compare to map4!
  (defn filter4 [p c]
    (reduce ((filtere p) conj) [] c))

  (filter4 even? (range 10))


  ;; the result:
  ;; mape and filtere do not combine results anymore
  ;;
  ;; The essence of map: "Give me a step function and I will give you
  ;; a modified version of step, which will apply f to all values before
  ;; passing it on"
  ;; this (approximately) is called a transducer


  ;; Composing of transducers is done via 'comp'


  (reduce
   ((comp
     (filtere even?)
     (mape inc)
     (filtere (fn [e] (<= e 10))))
    conj)
   []
   (range 20))

  ;; the order is a little unusual:
  ;; first all even elements are filtered,
  ;; then incremented and then filtered by <=
  ;; this is the different than without transducer:
  ((comp (partial filter even?)
         (partial map inc)
         (partial filter (fn [e] (<= e 10)))) (range 20))

  ;; this is because the step-functions are first passed to each other
  ;; (from right to left),
  ;; which are then processed like a stack


  ;; Clojure has built-in transducers
  ;; (map f) returns a transducer

  ;; (fn [step]
  ;;   (fn [a e] (step a (f e))))

  ;; Transducers are decoupled, they have no idea what step will do, when called,
  ;; The only decision a transducer is able to make is how many times
  ;; to call step

  ;; Transducers return a reduce function.
  ;; The reduce functions receive an accumulator and a value,
  ;; and return a new accumulator value.

  ;; Important: The new accumulator value must be generated with the step function
  ;; (or stay the old accumulator value)
  ;; The element e may be modified by the reduce function




  ;; Is that all?
  ;; Unfortunately it is not that easy :-(

  (take-while (fn [e] (<=  (Math/pow 2 e) (* e e e))) (range 2 91))

  ;; take-while must be able to abort the reduction early.
  ;; Can this be written as a transducer?


  ;; If a value is wrapped with 'reduced', reduce aborts.
  ;; This is essentially a box containing the value
  ;; and which acts as a termination signal.
  ;; But before that, reduce unpacks the value.
  (reduce (fn [a e] (println e) (if (> a 100) (reduced a) (+ a e)))
          (range 10000))


  ;; this allows a transducer to terminate early
  (defn take-whilee [p]
    (fn [step]
      (fn [a e]
        (if (p e)
          (step a e)
          (reduced a)))))

  (defn take-while4 [p c]
    (reduce ((take-whilee p) conj) [] c))

  (last (take-while4 (fn [e] (<= e 99)) (range 1000)))


  ;; And now for our favorite problem: state!
  ;; Some transducers have local state
  ;; e.g. drop-while or partition-by

  ;; Ugly, gigantic transducer alarm!
  ;; ok, here is a stateful reducer in all its mutable glory:

  (partition-by even? [2 4 3 5 7 8])

  (defn partition-bye [f]
    (fn [step]
      (let [temparray (new java.util.ArrayList)
            pv (volatile! ::none)]
        (fn [a e]
          (let [pval @pv
                val (f e)]
            (vreset! pv val)
            (if (or (identical? pval ::none)
                    (= val pval))
              (do
                (.add temparray e)
                a)
              (let [v (vec (.toArray temparray))]
                (.clear temparray)
                (.add temparray e)
                (step a v))))))))

  (defn partition-by4 [f c]
    (reduce ((partition-bye f) conj) [] c))

  ;; looking good, *but*: it is missing the 14
  (partition-by4 (fn [e] (< 3 (mod e 5))) (range 15))


  ;; even worse:
  ;; What happens if you pair a transducer that terminates early
  ;; with a stateful transducer?

  (->> (range 20)
       (take-while (fn [e] (< e 8)))
       (partition-by (fn [e] (< 3 (mod e 5)))))

  (reduce
   ((comp
     (take-whilee (fn [e] (< e 8)))
     (partition-bye (fn [e] (< 3 (mod e 5)))))
    conj)
   []
   (range 20))


  ;; The stateful transducer must be given the opportunity to flush its state.
  ;; -> All transducers (even stateless ones) must be able
  ;;    to handle early termination!

  ;; Flushing is handled by calling a unary function.
  ;; Transducers are therefore multiary.
  ;; Stateless transducers just pass on whatever they are given.

  (defn mape [f]
    (fn [step]
      (fn
        ([a] (step a)) ;; <--
        ([a e]
         (step a (f e))))))

  ;; but with some transducers this can become complex (this is only a sketch)
  (defn partition-bye-bye
    [f]
    (fn [rf]
      (let [a (java.util.ArrayList.)
            pv (volatile! ::none)]
        (fn
          ([result input] #_[... almost the same as before ...])
          ([result] ;; flush
           (let [result (if (.isEmpty a)
                          result
                          ;; flush
                          (let [v (vec (.toArray a))]
                            (.clear a)
                            (unreduced (rf result v))))]
             (rf result)))))))

  ;; There is also (optionally) a nullary function that generates an initial value.
  ;; Usually by calling step without arguments.
  ;; Otherwise you don't know where to get that value,
  ;; because you don't know the underlying data structure!

  (defn mape [f]
    (fn [step]
      (fn
        ([] (step)) ;; <--
        ([a] (step a))
        ([a e]
         (step a (f e))))))

  (conj) ;; oh, so that's why conj returns an empty vector without arguments!


  ;; a call with transduce
  (transduce
   (comp (map inc)
         (map #(* % %))
         (filter even?))
   conj
   (range 10))

  (as-> (range 10) c
    (map inc c)
    (map #(* % %) c)
    (filter even? c))


  ;; we have had transduce all along, this is roughly the implementation
  (defn my-transduce [transducer f init coll]
    (reduce (transducer f) init coll))

  ;; There are other functions that work with transducers

  ;; into (uses transduce)
  (into #{}
        (comp (map inc)
              (map #(* % %))
              (filter even?))
        (range 20))

  ;; other stateful transducers: dedupe removes consecutive duplicates

  (dedupe [1 2 3 3 3 4 5])
  (transduce (comp (map inc) (dedupe)) conj [1 2 3 3 3 4 5])

  ;; sequence reintroduces laziness
  ;; dumb idea: (take 10 (transduce (map inc) conj (range)))

  (take 10 (sequence (map inc) (range)))



  ;; Parallel processing is faster with transducers:
  (let [v (vec (range 10000000))]
    (System/gc)
    (time (reduce + (map inc v)))
    (System/gc)
    (time (r/fold + ((map inc) +) v)) ;; <-- reducers (essentially: parallel transduce via fork/join)
    )
  (let [v (vec (range 1000000))]
    (time (reduce + (map inc v)))
    (System/gc)
    (time (reduce + (pmap inc v))) ;; <-- pmap has too much overhead to speed up +.
    (System/gc)
    (time (r/fold + ((map inc) +) v)))


  ;; Great, we did some decomplecting, but map is more complicated now :-/

  ;; The main advantage: you get A LOT of functionality for free
  ;; if you write new data sources/sinks
  ;; Pre-transducers version of core.async had practically re-implemented
  ;; every HOF on channels (map, filter, ...)

  ;; Now you only need to implement transduce (or sequence, fold)
  ;; and you pretty much get access to all sequence functions.
  )
