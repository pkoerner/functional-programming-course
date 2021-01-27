(ns repl.21-transients)

(comment 
  ;; Transients: we avoid mutable objects, but...

  ;; If it is encapsulated in a function (so that it does not leak outside), there is no issue.
  ;; Construction of large vectors, sets, etc. (e.g. creating a data base from a hard drive)
  ;; is much faster this way.

  (-> #{} (conj 1) (conj 2) (conj 3) (conj 4) (disj 3)) ; =>  #{1 2 4}

  ;; One can transform a data structure into a transient -
  ;; a weird container that we cannot observe easily.
  (transient #{1 2})

  ;; The API is the same, except that we need to add a bang (!, do not use in transactions).
  ;; Finally, just make it persistent again.

  (-> #{} transient (conj! 1) (conj! 2) (conj! 3) 
                    (conj! 4) (disj! 3) persistent!); =>  #{1 2 4}

  ;; Making a transient persistent results in a regular set, the original value does not change.
  (type (persistent! (transient #{}))) ; =>  clojure.lang.PersistentHashSet

  (-> #{} transient) ; =>  #<TransientHashSet clojure.lang.PersistentHashSet$TransientHashSet@147eec90>


  ;; closer examination:
  (def a #{1 3})
  a

  ;; Never store a transient anywhere it can be accessed from the outside!
  ;; Only keep transients locally in a small block.
  (def b (transient a)) ; =>  #'repl.21-transients/b

  a ; =>  #{1 3} (unchanged!)

  ;; not good at all, see below
  (conj! b 6)

  a ; =>  #{1 3}

  ;; b is still a transient
  (def c  (persistent! b))
  [a b c] ; =>  [#{1 3} #<TransientHashSet clojure.lang.PersistentHashSet$TransientHashSet@1adb83c8> #{1 3 6}]
  ;; c only is an "expected" value on accident!


  ;; just to make the comparison below fair, let's give the garbage collector an opportunity to run
  (System/gc) 

  (let [x (doall (range 1e7))] 
    (do (print "Persistent: ")
        (time (loop [c #{}, x x]
                (if (seq x)
                  (recur (conj c (first x)) (rest x))
                  c)))
		(System/gc)
        (print "Transient: ")
		(time (loop [c (transient #{}), x x]
                (if (seq x)
                  (recur (conj! c (first x)) (rest x))
                  (persistent! c))))
              nil))

  ;; Transients are about twice as fast compared to the immutable versions.
  ;; The perform similarly to regular mutable data structures.



  ;; conj!, disj!, etc. use the same API
  ;; as their persistent counterparts!(!)
  ;; They do not need to modify the object
  ;; and may return something else entirely!

  ;; evil:
  (let [x (transient {})] 
    (doseq [e (range 100)]
      (assoc! x e e))      ;; ignoring return value
    (persistent! x)); =>  {0 0, 1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7}
  ;; Maps are special: after 8 elements, the internal representation is changed.
  ;; Small maps are used rather often and are optimised.
  ;; Because sometimes new objects may be returned, it is not sufficient
  ;; to keep a reference to the original transient object.


  ;; Summarizing the correct usage of transients:
  ;; - Always use it locally in a function, always call persistent! before returning it.
  ;; - Always use the return value of conj!, associ!, etc.

  ;; a correct implementation would be:
  (defn foo [n] 
    (loop [i 0 v (transient #{})]
      (if (< i n)
        (recur (inc i) (conj! v i))
        (persistent! v))))

  (foo 100)

  ;; the mutable five are:
  conj!
  disj!
  assoc!
  dissoc!
  pop!
  

  ;; even more correct usage: not at all, unless someone makes you :-)

)
