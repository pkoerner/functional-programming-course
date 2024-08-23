(ns repl.26-refs)

;; Refs


;; 1 Introduction
;; 2 What is the problem with multiple atoms?
;; 3 Vocabulary of Refs
;;   - ref
;;   - dosync
;;   - deref
;;   - alter
;; 4 Interlude: Many Atoms to One
;; 5 Commuting Through Heavy Traffic
;; 6 Ref-Set, Dosync and Derefs ;; TODO: merge with Sec 3
;; 7 The Write Skew Problem
;;   - ensure
;; 8 Composing Agents and Refs


;; 1 Introduction
;; --------------

  ;; refs are synchronous and coordinated.
  ;; - synchronous: calls return to us, once the operation was completed.
  ;; - coordinated: we may finally use (read from / write to) several refs at the same time!

  ;; Note: depending on your setup, you might not see prints that happen in a thread that is not the main thread.
  ;; You can always execute the expressions directly on the leiningen REPL to get the full picture.


;; 2 What is the problem with multiple atoms?
;; ------------------------------------------

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


;; 3 Vocabulary of Refs

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



;; 4 Interlude: Many atoms to one
;; ------------------------------

  ;; a correct solution with a single (!) atom would be a map (or vector, or ...) à la:
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


;; 5 Commuting Through Heavy Traffic
;; ---------------------------------

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



;; 6 Ref-Set, Dosync and Derefs
;; ----------------------------

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


;; 7 The Write Skew Problem
;; ------------------------

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



;; 8 Composing Agents and Refs
;; ---------------------------

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



