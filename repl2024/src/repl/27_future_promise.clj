(ns repl.27-future-promise)

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
  @x 
