(ns repl.24-atoms)

  ;; Atoms

  ;; atoms are synchronous and uncoordinated.
  ;; What does this mean?
  ;; - synchronous: calls return to us, once the operation was completed.
  ;; - uncoordinated: we cannot inspect or write to two (or more) atoms consistently.
  ;; Direct consequence: DO NOT EVEN THINK ABOUT USING MORE THAN ONE ATOM IN YOUR PROGRAM*.
  ;; *(terms and conditions may apply)



  ;; You get an atom by calling the function 'atom'. An initial value has to be specified.
  (def foo (atom {}))

  ;; We observe: There is an atom-object
  foo

  ;; An atom is not the stored value, but
  ;; a wrapper for the value

  ;; these two calls are literally the same
  (deref foo)
  @foo
  ;; the proof
  (read-string "@foo")

  ;; Manipulating atoms:
  ;; (swap! atom function arg1 arg2 ...)
  ;; (swap! a f x y z) executes the function (f a x y z)
  ;; and saves the result in the atom

  (defn store [k v] (swap! foo assoc ,,,, k v))

  (store :x 10)
  (store :y 100)

  @foo

  (store :x 7)

  @foo

  ;; returns nil, as foo is not dereferenced
  (:x foo)
  ;; correct:
  (:x @foo)


  ;; dereferencing observes the value of an atom
  ;; observed values are just values - and therefore immutable!
  (def blah @foo)
  blah
  (store :x 1000)
  @foo
  blah


  ;; Atoms have to be dereferenced to access the current value.
  ;; A new observation is made in that process!

  (:x @foo)


  ;; reset!
  ;; What if we want to reinitialize the atom?
  ;; We could redefine the atom, but Philipp forbade us using def in that way. That meany!
  ;; So we have to make do with a function that returns a constant value...

  (swap! foo (fn [_] {}))
  @foo

  ;; Apropos ...
  ;; constantly takes a value x and generates a function,
  ;; which accepts any number of arguments and
  ;; always returns that value x
  (constantly :my-constant)
  ((constantly :my-constant) 12 42) ;; no matter how many arguments

  (swap! foo (constantly {:x 23}))


  ;; Exercise: How would you implement constantly?
  (defn my-constantly [v]) ; TODO: do it LIVE!
  ((my-constantly 42) :lol :trololo 1 4 2)


  ;; It is often the case that we do not care
  ;; for the current value of the atom
  ;; and just want to set a constant value
  ;; For those cases there is 'reset!'

  (swap! foo (constantly (+ 899 1)))
  ;; the same, in principle
  (reset! foo (+ 899 1))

  @foo


  ;; What happens now, when the atom is used concurrently?

  ;; compare-and-swap (CAS) semantics:
  ;; 1. The old value is read
  ;; 2. The new value is calculated
  ;; 3. Check: is the current value still the same as the old?
  ;;    a) if so: store the new value in the atom (atomic operation, thread-safe)
  ;;    b) otherwise: return to step 1.

  (def counter (atom 0))

  (defn incer [] (swap! counter inc))

  ;; 'future' starts up a new thread and returns the control flow back to the caller
  ;; this thread here sleeps 10 seconds and then adds 3 and 3 together
  (def x (future (Thread/sleep 10000) (+ 3 3)))
  ;; dereferencing a future blocks until it is finished
  @x

  ;; Here a thread is started which increments the counter 100 million times
  ;; The observation from '@counter' happens sometime, not necessarily when the future finishes
  (do
    (future (dotimes [_ 100000000] (incer)))
    @counter)

  @counter

  ;; If you dereference the counter fast enough consecutively you can witness
  ;; different values in the atom

  ;; Other functions:
  ;; sleepy-inc sleeps a couple of second, awakes and increments n
  (defn sleepy-inc [n]
    (println "Zzz")
    (Thread/sleep 4000)
    (println "Whut?")
    (inc n))

  (sleepy-inc 6)

  (defn v-incer [] (swap! counter sleepy-inc))

  ;; v-incer is so slow (4 seconds and change), that you can create
  ;; race conditions by hand

  (v-incer)

  ;; tease sets the content of the counter-atom to a random value
  (defn tease []
    (let [c (rand-int 1000)]
      (reset! counter c)))



  ;; best to execute on a real Clojure REPL directly one after the other
  (future  (v-incer)) ; console
  (tease)             ; repl

  ;; Note that 'sleepy-inc' is restarted over and over again,
  ;; if you use 'tease' to change the content of the atom in the meantime.
  ;; This is the meaning of compare-and-swap.

  @counter

  ;; watcher
  ;; (add-watch reference name fn-of-4-args)
  ;; fn arguments: name reference old-value new-value

  ;; watchers work on every reference that manages state
  ;; for us these are: atoms, refs and agents (see below)

  ;; the corresponding, registered watcher function is called, if the value of the reference changes

  (def agent-x
    (add-watch counter
               :my-awesome-watcher
               (fn [k r old new]
                 (println (str k ": " old " -> " new)))))

  (tease)


  ;; Things to watch out for!

  (def mouse-position (atom {:x 12 :y 100}))


  ;; Incorrect:
  (println
   (:x @mouse-position)
   (:y @mouse-position))




  ;; @mouse-position dereferences the atom. If you do this
  ;; multiple times, you may read (possibly inconsistent)
  ;; values from different points in time


  ;; Correct:
  (let [pos @mouse-position]
    (println
     (:x pos)
     (:y pos)))




  ;; If you cannot read a single atom multiple times consistently,
  ;; than you cannot dereference multiple atoms consistently!


  ;; completely wrong:
  (def x-pos (atom 12))
  (def y-pos (atom 212))

  (println @x-pos @y-pos)


  ;; Correct: Either use a single atom as above or use refs (see below)
  ;; This is why you generally only use a single atom at most.
  ;; Exception: If you can /prove/ (meaning a formal proof)
  ;; that two states cannot influence each other.
  ;; But who wants to bother with that?


  ;; Incorrect:
  (def screen-width 1024) ; px

  (defn move-mouse [x]
    (if (< (+ (:x @mouse-position) x)
           screen-width)
      (swap! mouse-position update :x + x)
      @mouse-position))

  (move-mouse 300)


  ;; The atom is dereferenced twice in the above code snippet!
  ;; once in the check of the if-condition and a second time in the swap!
  ;; (or in the else-branch)
  ;; in the meantime the value could have changed...

  ;; Correct: Extracting the check into a function which swap! calls
  ;; or using refs (correctly) (next week)


  ;; Atoms are not recursively dereferenced by the way
  (deref (atom [(atom :a) (atom :b)]))

