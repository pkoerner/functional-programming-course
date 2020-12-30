(ns repl.03-concurrency)

(comment


  ;; Concurrency


  ;; Atoms

  ;; You get an atom with 'atom'. An initial value has to be specified.
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
  ;; or using refs (correctly)


  ;; Atoms are not recursively dereferenced by the way
  (deref (atom [(atom :a) (atom :b)]))

  ;; SLIDES






  ;; agents

  ;; First an atom

  (def log-file (atom []))

  ;; The debug-function is a little slow today
  (defn debug [& words]
    (swap! log-file
           (fn [x]
             (Thread/sleep 1000)
             (conj x (apply str (interpose " " words))))))

  ;; So calling it takes a while
  (debug "An" "output" "to" "be" "logged" "in" "one" "piece")
  (debug "Another" "output" "to" "be" "logged" "in" "one" "piece")

  (def log-file (atom []))

  ;; pretty much two seconds as expected
  (time
   (do
     (debug "An" "output" "to" "be" "logged" "in" "one" "piece")
     (debug "Another" "output" "to" "be" "logged" "in" "one" "piece")))

  log-file


  ;; For atoms, Functions are executed in the caller's thread

  ;; now onto agents

  ;; The interface of agents is a little different than that of atoms

  ;; 'agent' creates a new agent
  (def log-file (agent []))

  ;; 'send' is something similar to 'swap!'
  (defn debug [& words]
    (send log-file
          (fn [x]
            (Thread/sleep 4000)
            (conj x (apply str (interpose " " words))))))

  (time
   (do
     (debug "An" "output" "to" "be" "logged" "in" "one" "piece")
     (debug "Another" "output")))

  ;; Agents work asynchronously, so we get control back immediately
  ;; if we are quick enough, we can watch the log file grow in size

  @log-file

  ;; Let's start anew
  (def log-file (agent []))

  (time
   (do
     (debug "Eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
     (debug "Noch" "eine" "Ausgabe")))

  ;; 'await' blocks until all submitted tasks up to that point are done. 
  ;; 'await-for' does the same with a time-out
  (do  (await log-file) @log-file)



  ;; The function passed to 'send' is executed in a thread pool
  ;; (typically of size #Cores + 2)
  (def ag
    (for [x (range 21)] (agent nil)))
  ag

  ;; this takes 3 seconds on my machine (thread pool with 10 threads)
  (time
   (do
     (doseq [a ag] (send a (fn [_] (Thread/sleep 1000))))
     (doseq [a ag] (await a))))



  ;; here we use 'send-off' instead of send:
  ;; 'send-off' creates a new thread for every task

  ;; so this will only take a second
  (time
   (do
     (doseq [a ag] (send-off a (fn [_] (Thread/sleep 1000))))
     (doseq [a ag] (await a))))


  ;; When exceptions are thrown in functions executed by agents, they go into hiding
  ;; and do accept any more tasks
  (def agent-x (agent 0))
  (send agent-x (fn [state] (/ 0 0)))

  ;; Note: the :status has changed to :failed
  agent-x
  (send agent-x inc)

  ;; You have to restart the agent
  (restart-agent agent-x 0)
  (send agent-x inc)

  ;; With atoms the caller gets the exception.
  ;; With agents the caller has already moved on.




  ;; SLIDES





  ;; Refs

  ;; What is the problem with multiple atoms?

  ;; Case study: A banking system with accounts that does not allow overdrafts.
  (def account1 (atom 100))
  (def account2 (atom 0))

  (defn transfer-money [amount]
    (when (<= amount (deref account1)) ;; credited account has sufficient funds
      (do (swap! account2 #(+ % amount)) ;; debit is received
          (Thread/sleep 5000) ;; system is busy
          (swap! account1 #(- % amount))))) ;; credit is sent

  (defn pay-bill [amount]
    (when (<= amount (deref account1))  ;; credited account has sufficient funds
      (println "Payed" amount)
      (swap! account1 #(- % amount)))) ;; credit is sent

  (future (transfer-money 10))

  ;; It takes a little while for the changes to become visible
  [@account1 @account2]
  (pay-bill 10)

  ;; If we execute the following expressions quick enough...
  (future (transfer-money 60))
  (pay-bill 60)

  ;; account1 is overdrawn. This violates our invariant!
  [@account1 @account2]

  ;; Can we fix this?
  ;; The answer: Not as long as several atoms are involved!
  ;; Even if we switch the order of the two 'swap!'-expressions in transfer-money
  ;; the other action can always fire in between the check in the 'when' and the 'swap!'
  ;; The example above simply increases the window for this to occur


  ;; now the same shtick with refs
  (def account1 (ref 100))
  (def account2 (ref 0))

  ;; dereferencing works as before:
  account1
  @account1


  ;; 'alter' is the 'swap!' for refs
  ;; The whole transaction is wrapped in a 'dosync'
  ;; A dosync-block describes exactly when a transaction starts and ends
  ;; using 'alter' outside of a transaction causes an execution error
  (defn transfer-money [amount]
    (dosync (when (<= amount (deref account1))
              (do (alter account2 #(+ % amount))
                  (Thread/sleep 5000)
                  (alter account1 #(- % amount))))))

  (defn pay-bill [amount]
    (dosync (when (<= amount (deref account1))
              (println "Payed" amount)
              (alter account1 #(- % amount)))))

  ;; This works as before
  (future (transfer-money 10))
  [@account1 @account2]

  ;; as does this
  (pay-bill 10)

  ;; But if we know execute both both of these in short time
  (future (transfer-money 60))
  (pay-bill 60)

  [@account1 @account2]
  ;; The first transaction is no longer carried out!
  ;; It is restarted, since an involved ref's state has changed
  ;; (The restart happens as soon as the state changes).
  ;; In the second attempt, the when condition no longer holds true
  ;; and the money transfer is rejected.


  ;; a correct solution with a single (!) atom would be a map à la:
  (def bank (atom {:account1 100 :account2 0}))

  (defn transfer-money2 [from-name to-name amount]
    (swap! bank
           (fn [k]
             (let [from-wert (get k from-name)
                   to-wert (get k to-name)]
               (if (<= amount from-wert)
                 (let [k' (assoc k to-name (+ to-wert amount))]
                   (Thread/sleep 5000)
                   (assoc k' from-name (- from-wert amount)))
                 (do (println "Rejected!")
                     k))))))

  (defn pay-bill2 [account-name amount]
    (swap! bank
           (fn [k]
             (let [account-balance (get k account-name)]
               (if (<= amount account-balance)
                 (assoc k account-name (- account-balance amount))
                 k)))))

  (future (transfer-money2 :account1 :account2 70))
  (pay-bill2 :account1 70)
  @bank

  ;; this can be problematic (e.g. performance),
  ;; if a million accounts have to be managed
  ;; and a lot of conflicts (and consequently retries)
  ;; occur even if different transactions do not influence each other


  ;; commute
  ;; once again
  (def account1 (ref 10000))
  (def account2 (ref 10000))

  ;; This time we want to count how many transactions are made
  (def x-counter (ref 0))

  ;; For this we increment the times each time (we also once again simulate a heavy load in the system)
  (defn pay-bill [acc amount transaction-name]
    (dosync  (when (<= amount (deref acc))
               (println "Payed" amount "in transaction" transaction-name)
               (Thread/sleep 2000)
               (alter acc #(- % amount))
               (alter x-counter inc))))


  (pay-bill account1 10 :transactionsMcTransactionsFace)
  [@account1 @account2 @x-counter]

  ;; best evaluated in a real REPL
  (do (future (pay-bill account1 10 :transaction-a))
      (future (pay-bill account2 10 :transaction-b)))

  ;; a pay-bill call is restarted, since the x-counter causes a in conflict...

  ;; We don't actually care for the exact value of the counter...
  ;; It should not cause a conflict,
  ;; but instead simply increment the value!
  ;; 'commute' exists to prevent conflicts on commutative changes

  (defn pay-bill [acc amount transaction-name]
    (dosync  (when (<= amount (deref acc))
               (println "Payed" amount "in transaction" transaction-name)
               (Thread/sleep 2000)
               (alter acc #(- % amount))
               (commute x-counter inc))))

  ;; Does not trigger a restart anymore
  (do (future (pay-bill account1 10 :transaction-a))
      (future (pay-bill account2 10 :transaction-b)))



  ;; 'reset!' is called 'ref-set' for refs
  (dosync (ref-set account1 100))

  ;; The following is correct for refs (but would be wrong for atoms!)

  (dosync
   (println @account1 @account2))

  ;; Derefs on **refs** inside a dosync-block are consistent.
  ;; You can also deref atoms in a dosync-block;
  ;; but this changes absolutely nothing about the
  ;; problem with inconsistency




  ;; Note: We now know what a transaction is.
  ;; The 
  ;; The exclamation mark (or bang) on a function like swap! means
  ;; that you cannot use it safely inside a transaction.
  ;; Side effects (may) get triggered multiple times because of conflicts
  ;; depending on how far a transaction progressed before the conflict occurred.
  ;; This is quite the unpleasant combination.
  ;; Therefore we should only use pure functions in transactions if possible!


  ;; Problem: Write Skew

  ;; Lets define two more refs
  (def r1 (ref 0))
  (def r2 (ref 0))

  ;; If both refs are 0 than the r1 should be incremented
  (defn f1 []
    (dosync
     (when (= 0 @r1 @r2)
       (Thread/sleep 200)
       (alter r1 inc))))

  ;; If both refs are 0 than the r2 should be incremented
  (defn f2 []
    (dosync
     (when (= 0 @r1 @r2)
       (Thread/sleep 200)
       (alter r2 inc))))

  ;; Obviously only one of the two transactions should complete without conflict.
  ;; So only one of the refs should become 1

  (do
    (future (f1))
    (future (f2)))

  ;; oops
  (dosync [@r1 @r2])


  ;; There is no sequence f1, f2 in which this is possible.
  ;; The problem is that the two transaction are not in conflict with each other!
  ;; read-only accesses are not included in the set of conflicts...

  ;; You could manually provoke a conflict, by replacing the read value with itself:

  (defn f1' []
    (dosync
     (when (= 0 @r1 @r2)
       (Thread/sleep 200)
       (alter r1 inc)
       (alter r2 identity))))

  (def r1 (ref 0))
  (def r2 (ref 0))

  (dosync [@r1 @r2])

  (do
    (future (f1'))
    (future (f2)))

  (dosync [@r1 @r2])



  ;; More efficient and elegant 'ensure' includes a ref in the conflicts-set

  (defn f2' []
    (dosync
     (when (= 0 (+ @r1 @r2))
       (Thread/sleep 200)
       (alter r2 inc)
       (ensure r1))))


  (def r1 (ref 0))
  (def r2 (ref 0))

  (do
    (future (f1))
    (future (f2')))

  (dosync [@r1 @r2])



  ;; Agents & Refs work well together

  ;; first a version without agent
  (def k1 (ref 100000))
  (def k2 (ref 0))

  (defn transfer [f t a]
    (dosync
     (when (< a @f)
       (println "transferring")
       (alter f #(- % a))
       (Thread/sleep 2000)
       (alter t #(+ % a)))))

  (dosync [@k1 @k2])

  ;; see REPL
  (do
    (future (transfer k1 k2 10))
    (future (transfer k1 k2 10)))
  (dosync [@k1 @k2])

  ;; Why so many 'transferring'-prints?
  ;; refs know if they have changed during a transaction.
  ;; If there is an ongoing transaction that changes a ref
  ;; the deref in other transactions fails immediately.


  ;; and now we send an agent to handle our side effects
  (def spy (agent nil))

  (defn transfer' [f t a]
    (dosync
     (when (< a @f)
       (send spy (fn [_] (println "transferring (I'm printed by an agent!)")))
       (alter f #(- % a))
       (Thread/sleep 2000)
       (alter t #(+ % a)))))

  ;; see REPL
  (transfer' k1 k2 10)

  (dosync [@k1 @k2])

  (do
    (future (transfer' k1 k2 10))
    (future (transfer' k1 k2 10)))

  (dosync [@k1 @k2])

  ;; send does not end in a !, so it is safe to use in transactions
  ;; In fact, the tasks are only submitted to the agent,
  ;; once the transaction completes and discarded otherwise.


  ;; once again in direct combination:
  (defn transfer'' [f t a]
    (dosync
     (when (< a @f)
       (println :retry)
       (send spy (fn [_] (println "transferring (spy)")))
       (alter f #(- % a))
       (Thread/sleep 2000)
       (alter t #(+ % a)))))

  (dosync [@k1 @k2])

  (do
    (future (transfer'' k1 k2 10))
    (future (transfer'' k1 k2 10)))

  (dosync [@k1 @k2])














  ;; future & promise


  ;; Future: Container for the value of a calculation
  ;; that is performed concurrently
  ;; Dereferencing of a future that is not yet finished
  ;; blocks the caller

  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     @result))

  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     (Thread/sleep 1500)
     @result))


  ;; You can find out if a future has finished with 'realized?' 
  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     (println "Check1:" (realized? result))
     (Thread/sleep 1500)
     (println "Check2:" (realized? result))))


  ;; 'nag' asks periodically if we are there yet, but at most n times.
  ;; (so we do not accidentally spam the console forever)
  ;; The default is 200 times.
  (defn nag
    ([f] (nag f 200))
    ([f n]
     (if (> n 0)
       (if (realized? f)
         @f
         (do (Thread/sleep 100)
             (println :nag f)
             (recur f (dec n))))
       (println :stopped))))


  ;; This again shows the difference in what happens depending on when
  ;; you dereference a future
  (let [x (future (do (Thread/sleep 2000) 42))]
    (println "nagging")
    (println :first-try x)
    (nag x))

  (let [x (future (do (Thread/sleep 2000) 42))]
    (println "nagging")
    (println :first-try @x) ;; deref!
    (nag x))



  ;; futures go into a thread pool
  ;; on my machine 32 futures are executed at once:
  (time
   (let [comp-array
         (for [_ (range 32)] (future (do (Thread/sleep 2000) 1)))]
     (doseq [c comp-array] @c)))

  ;; Try this with some more futures



  ;; Promise is a container that is going to be filled by someone at some point.
  ;; The caller is blocked on dereferencing until a value is delivered.

  (def x (promise))

  (future (nag x))
  (deliver x 10)
  @x)
