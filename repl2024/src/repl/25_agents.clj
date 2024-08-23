(ns repl.25-agents)

;; 1 Introduction
;; 2 Case Study: Atoms for Logging
;; 3 Introducing Agents
;;   - agent
;;   - deref
;;   - await
;;   - send vs. send-off
;; 4 When Things go Wrong
;;   - restart-agent


;; 1 Introduction
;; --------------

  ;; agents

  ;; agents are uncoordinated and asynchronous.
  ;; - asynchronous: calls return immediately; action may still have to take place.
  ;; - uncoordinated: you cannot read or write two agents consistently. So do not even try.
  ;;   (we hopefully learned it the hard way in the atom unit)


;; 2 Case Study: Atoms for Logging
;; -------------------------------

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


;; 3 Introducing Agents
;; --------------------

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



;; 4 When Things go Wrong
;; ----------------------

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
