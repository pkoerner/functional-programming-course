(ns repl.03-concurrency)

(comment


  ;; Concurrency


  ;; Atoms

  ;; ein Atom bekommt man mit atom. Da kann man direkt den initialen Wert angeben.
  (def foo (atom {}))

  ;; wir sehen: es gibt ein Atom-Objekt
  foo

  ;; Ein Atom ist nicht der gespeicherte Wert sondern
  ;; eine Verpackung für den Wert!

  ;; die beiden Aufrufe sind buchstäblich das gleiche
  (deref foo)
  @foo
  ;; der Beweis
  (read-string "@foo")

  ;; Atome ändern:
  ;; (swap! atom funktion arg1 arg2 ...)
  ;; (swap! a f x y z) führt die Funktion (f a x y z)
  ;; aus und speichert das Ergebnis im Atom
  
  (defn store [k v] (swap! foo assoc ,,,, k v))

  (store :x 10)
  (store :y 100)

  @foo

  (store :x 7)

  @foo

  ;; nicht derefenziert!
  (:x foo)
  ;; richtig:
  (:x @foo)


  ;; das dereferenzieren macht eine Beobachtung vom Atom.
  ;; beobachtete Werte sind eben Werte - und damit immutable!
  (def blah @foo)
  blah
  (store :x 1000)
  @foo
  blah


  ;; Atome müssen dereferenziert werden um auf den
  ;; aktuellen Wert zuzugreifen.
  ;; Dabei wird eine neue Beobachtung gemacht!

  (:x @foo)


  ;; reset!
  ;; was ist, wenn wir alles auf Anfang bringen wollen?
  ;; wir könnten das Atom neu definieren, aber def wurde uns vom fiesen Philipp verboten :-(
  ;; also müssen wir uns mit einer Funktion behelfen, die einen konstanten Wert zurückgibt...

  (swap! foo (fn [_] {}))
  @foo

  ;; Apropos ...
  ;; constantly nimmt einen Wert und generiert eine
  ;; Funktion, die beliebig viele Argumente akzeptiert
  ;; und immer den Wert zurückgibt
  (constantly :my-constant)
  ((constantly :my-constant) 12 42) ;; egal wie viele Argumente

  (swap! foo (constantly {:x 23}))


  ;; Übung: wie implementiert man constantly?
  (defn my-constantly [v] ,,,) ; TODO: do it LIVE!
  ((my-constantly 42) :lol :trololo 1 4 2)


  ;; Es ist ein häufiges Muster, dass man sich nicht
  ;; für den aktuellen Wert des Atoms interessiert
  ;; und einen konstanten Wert setzen will.
  ;; Daher gibt es reset!

  (swap! foo (constantly (+ 899 1)))
  ;; prinzipiell das gleiche
  (reset! foo (+ 899 1))

  @foo


  ;; was passiert nun, wenn ein Atom nebenläufig verwendet wird?

  ;; compare-and-swap (CAS) Semantik:
  ;; 1. der alte Wert wird gelesen
  ;; 2. der neue Wert wird berechnet
  ;; 3. check: ist der aktuelle Wert gleich dem alten?
  ;;    a) falls ja: speichere den neuen Wert im Atom (atomare Operation, thread-safe)
  ;;    b) falls nein: gehe zurück zu 1.

  (def counter (atom 0))

  (defn incer [] (swap! counter inc))

  ;; future wirft einen neuen Thread los und die Kontrolle kommt zurück
  ;; der Thread hier schläft erstmal 10 Sekunden und rechnet dann 3 und 3 zusammen
  (def x (future (Thread/sleep 10000) (+ 3 3)))
  ;; dereferenzieren vom Future wartet, bis es fertig wird
  @x

  ;; hier wird ein Thread losgeworfen, der 100 Millionen mal +1 rechnet.
  ;; die Beobachtung @counter passiert irgendwann. Nicht unbedingt, wenn alle fertig sind...
  (do
    (future (dotimes [_ 100000000] (incer)))
    @counter)

  @counter

  ;; Wenn man schnell genug immer wieder nachschaut, sieht man, wie sich
  ;; der Wert im Atom ändert.

  ;; andere Funktion:
  ;; sleepy-inc schläft ein paar Sekunden, wacht dann auf und rechnet +1
  (defn sleepy-inc [n]
    (println "Zzz")
    (Thread/sleep 4000)
    (println "Whut?")
    (inc n))

  (sleepy-inc 6)

  (defn v-incer [] (swap! counter sleepy-inc))

  ;; v-incer ist so langsam (4 Sekunden plus ein bisschen), dass man von Hand race
  ;; conditions erzeugen kann

  (v-incer)

  ;; tease setzt das counter-Atom sofort auf einen zufälligen Wert
  (defn tease []
    (let [c (rand-int 1000)]
      (reset! counter c)))



  ;; am besten auf einer echten Clojure REPL direkt nacheinander ausführen
  (future  (v-incer)) ; console
  (tease)             ; repl

  ;; Man beobachtet, dass sleepy-inc immer wieder neu gestartet wird,
  ;; wenn man mit tease das Atom in der Zwischenzeit ändert.
  ;; Das ist die Bedeutung von compare-and-swap.

  @counter

  ;; watcher
  ;; (add-watch reference name vierstelligeFkt)
  ;; Funktion: name reference alter-wert neuer-wert

  ;; watcher funktionieren auf allen Referenzen, die Zustand managen
  ;; für uns sind das atoms, refs und agents (siehe unten)

  ;; wenn sich der Wert der Referenz ändert, wird die entsprechende Watcher Funktion aufgerufen, die registriert wurde

  (def agent-x
    (add-watch counter
               :my-awesome-watcher
               (fn [k r old new]
                 (println (str k ": " old " -> " new)))))

  (tease)


  ;; Dinge, bei denen man aufpassen muss!

  (def mouse-position (atom {:x 12 :y 100}))


  ;; Falsch:
  (println
   (:x @mouse-position)
   (:y @mouse-position))




  ;; @mouse-position dereferenziert das Atom, wenn man das
  ;; mehrfach macht, kann man das Atom zu verschiedenen
  ;; (ggf. inkonsistenten) Zeiten sehen


  ;; Richtig:
  (let [pos @mouse-position]
    (println
     (:x pos)
     (:y pos)))




  ;; Wenn man schon nicht ein einziges Atom konsistent
  ;; mehrfach dereferenzieren kann, geht das erst recht
  ;; nicht mit mehreren!


  ;; ganz Falsch:
  (def x-pos (atom 12))
  (def y-pos (atom 212))

  (println @x-pos @y-pos)


  ;; Richtig: Entweder wie oben in einem Atom oder mit refs (s.u.)
  ;; in der Regel verwendet man aus diesem Grund höchstens ein Atom.
  ;; Ausnahme: man /beweist/ (und damit meine ich einen formalen Beweis),
  ;; dass sich zwei Zustände nicht beeinflussen können.
  ;; Aber wer will das schon machen?


  ;; Falsch:
  (def screenwidth 1024) ; px

  (defn move-mouse [x]
    (if (< (+ (:x @mouse-position) x)
           screenwidth)
      (swap! mouse-position update :x + x)
      @mouse-position))

  (move-mouse 300)


  ;; das Atom wird zweimal dereferenziert!
  ;; einmal in der if-Bedingung und einmal im swap! (bzw. noch einmal im else-branch)
  ;; zwischendurch kann sich der Wert aber geändert haben...

  ;; Richtig: den Check in die Funktion ziehen, mit der geswappt wird,
  ;; oder refs (korrekt) verwenden


  ;; Atome werden übrigens nicht rekursiv dereferenziert
  (deref (atom [(atom :a) (atom :b)]))

  ;; SLIDES






  ;; agents

  ;; erstmal ein Atom

  (def log-file (atom []))

  ;; die debug-Funktion ist heute ein wenig langsam
  (defn debug [& words]
    (swap! log-file
           (fn [x]
             (Thread/sleep 1000)
             (conj x (apply str (interpose " " words))))))

  ;; dauert also einen Moment
  (debug "Eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
  (debug "Noch" "eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")

  (def log-file (atom []))

  ;; ziemlich genau zwei Sekunden, wie wir es erwarten
  (time
   (do
     (debug "Eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
     (debug "Noch" "eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
     ))

  log-file


  ;; Bei Atomen wird die Funktion im Aufrufer-Thread ausgeführt

  ;; jetzt wirklich Agenten

  ;; Das Interace von Agenten ist etwas anders als das von Atomen

  ;; agent erzeugt einen neuen Agenten
  (def log-file (agent []))

  ;; send ist so etwas ähnliches wie swap!
  (defn debug [& words]
    (send log-file
          (fn [x]
            (Thread/sleep 4000)
            (conj x (apply str (interpose " " words))))))

  (time
   (do
     (debug "Eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
     (debug "Noch" "eine" "Ausgabe")))

  ;; Agents arbeiten asynchron, daher bekommen wir sofort die Kontrolle
  ;; wenn wir schnell genug sind, können wir im log-file nachgucken, wie es wächst

  @log-file

  ;; nochmal neu
  (def log-file (agent []))

  (time
   (do
     (debug "Eine" "Ausgabe," "die" "an" "einem" "Stück" "erfolgen" "soll")
     (debug "Noch" "eine" "Ausgabe")))

  ;; Mit await kann man blockieren, bis alle bisher submitteten Tasks
  ;; erledigt sind. await-for macht das Gleiche mit Time-out
  (do  (await log-file) @log-file)



  ;; send wird auf einem Threadpool ausgeführt (normalerweise #Cores + 2)
  (def ag
    (for [x (range 21)] (agent nil)))
  ag

  ;; bei mir dauert das 3 Sekunden (Threadpool mit 10 Threads)
  (time
   (do
     (doseq [a ag] (send a (fn [_] (Thread/sleep 1000))))
     (doseq [a ag] (await a))))



  ;; hier verwenden wir send-off statt send:
  ;; send-off erzeugt einen neuen Thread für jeden Task

  ;; das hier dauert also nur eine Sekunde
  (time
   (do
     (doseq [a ag] (send-off a (fn [_] (Thread/sleep 1000))))
     (doseq [a ag] (await a))))


  ;; wenn in Agenten Exceptions fliegen, dann tauchen sie unter und nehmen nichts mehr an
  (def agent-x (agent 0))
  (send agent-x (fn [state] (/ 0 0)))

  ;; beobachte der :status ändert sich auf :failed
  agent-x
  (send agent-x inc)

  ;; man muss dann den Agenten wieder neu aufsetzen
  (restart-agent agent-x 0)
  (send agent-x inc)

  ;; Bei Atomen kriegt der Aufrufer die Exception.
  ;; Bei Agenten ist der Aufrufer schon weg.




  ;; SLIDES





  ;; Refs

  ;; Was ist das Problem mit mehreren Atomen?

  ;; Fallbeispiel: Banksystem mit Konten, die kein Überziehen erlauben.
  (def konto1 (atom 100))
  (def konto2 (atom 0))

  (defn transfer-money [amount]
    (when (<= amount (deref konto1)) ;; Konto gedeckt
      (do (swap! konto2 #(+ % amount)) ;; Empfänger kriegt Geld
          (Thread/sleep 5000) ;; viel los im System
          (swap! konto1 #(- % amount))))) ;; Auftraggeber sendet Geld

  (defn pay-bill [amount]
    (when (<= amount (deref konto1))  ;; Konto gedeckt
      (println "Payed" amount)
      (swap! konto1 #(- % amount)))) ;; Auftraggeber sendet Geld

  (future (transfer-money 10))

  ;; dauert ein wenig, bis die Änderung sichtbar wird
  [@konto1 @konto2]
  (pay-bill 10)

  ;; wenn wir die beiden jetzt schnell genug ausführen...
  (future (transfer-money 60))
  (pay-bill 60)

  ;; geht Konto1 ins Negative. Das entspricht nicht unserer Invariante!
  [@konto1 @konto2]

  ;; Können wir das reparieren?
  ;; die Antwort ist: nicht, solange mehrere Atome mit von der Partie sind!
  ;; auch wenn wir die Reihenfolge der beiden swap! in transfer-money umdrehen,
  ;; kann die andere Aktion immer noch zwischen dem Check im when und dem swap! ausgeführt werden.
  ;; Das Beispiel oben erlaubt nur ein größeres Timing.


  ;; nun den gleichen Spaß mit refs
  (def konto1 (ref 100))
  (def konto2 (ref 0))

  ;; dereferenzieren geht wie vorher:
  konto1
  @konto1

  ;; swap! für refs heißt alter
  ;; außen rum kommt ein dosync 
  ;; Ein dosync-Block beschreibt exakt, wann eine Transaktion anfängt und endet.
  ;; alter außerhalb eier Transaktion geht nicht.
  (defn transfer-money [amount]
    (dosync (when (<= amount (deref konto1))
              (do (alter konto2 #(+ % amount))
                  (Thread/sleep 5000)
                  (alter konto1 #(- % amount))))))

  (defn pay-bill [amount]
    (dosync (when (<= amount (deref konto1))
              (println "Payed" amount)
              (alter konto1 #(- % amount)))))

  ;; das funktioniert wie gehabt
  (future (transfer-money 10))
  [@konto1 @konto2]

  ;; und auch dieses
  (pay-bill 10)

  ;; wenn man jetzt aber diese beiden hier ausführt
  (future (transfer-money 60))
  (pay-bill 60)

  [@konto1 @konto2]
  ;; geht die erste Transaktion nicht mehr durch!
  ;; Sie wird neu gestartet, weil eine beteiligte Ref sich geändert hat
  ;; (und das passiert auch sofort, sobald eine Ref sich ändert).
  ;; Im zweiten Versuch ist die when-Bedingung nicht mehr wahr und der Geldtransfer wird abgelehnt.


  ;; korrekte Lösung mit einem (!) Atom wäre eine Map à la:
  (def bank (atom {:konto1 100 :konto2 0}))

  (defn transfer-money2 [konten from-name to-name amount]
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

  (defn pay-bill2 [konten konto-name amount]
    (swap! bank
           (fn [k]
             (let [konto-stand (get k konto-name)]
               (if (<= amount konto-stand)
                 (assoc k konto-name (- konto-stand amount))
                 k)))))

  (future (transfer-money2 bank :konto1 :konto2 70))
  (pay-bill2 bank :konto1 70)
  @bank

  ;; kann problematisch (z.B. Performance) sein,
  ;; wenn Millionen Konten verwaltet werden,
  ;; da es zu sehr vielen Konflikten und retries kommt,
  ;; auch wenn verschiedene Konten nichts miteinander zu tun haben


  ;; commute
  ;; nochmal neu
  (def konto1 (ref 10000))
  (def konto2 (ref 10000))

  ;; diesmal möchte ich mitzählen, wie viele Transaktionen so gemacht werden:
  (def x-counter (ref 0))

  ;; dazu wird zusätzlich der Counter um eins erhöht (und auch hier erzeugen wir Last im System)
  (defn pay-bill [kto amount transaction-name]
    (dosync  (when (<= amount (deref kto))
               (println "Payed" amount "in transaction" transaction-name)
               (Thread/sleep 2000)
               (alter kto #(- % amount))
               (alter x-counter inc))))


  (pay-bill konto1 10 :transactionsMcTransactionsFace)
  [@konto1 @konto2 @x-counter]

  ;; am besten in echter REPL
  (do (future (pay-bill konto1 10 :transaction-a))
      (future (pay-bill konto2 10 :transaction-b)))

  ;; ein pay-bill wird neu gestartet, da der x-counter im Konflikt steht...

  ;; eigentlich ist mir der genaue Wert vom Counter egal...
  ;; das sollte keinen Konflikt erzeugen,
  ;; sondern einfach inkrementieren, was immer da steht!
  ;; commute ist dafür da, um Konflikte bei kommutativen
  ;; Änderungen zu vermeiden

  (defn pay-bill [kto amount transaction-name]
    (dosync  (when (<= amount (deref kto))
               (println "Payed" amount "in transaction" transaction-name)
               (Thread/sleep 2000)
               (alter kto #(- % amount))
               (commute x-counter inc))))

  ;; triggert keinen Neustart mehr
  (do (future (pay-bill konto1 10 :transaction-a))
      (future (pay-bill konto2 10 :transaction-b)))



  ;; reset! heißt bei refs ref-set
  (dosync (ref-set konto1 100))

  ;; Das hier ist korrekt bei refs (bei Atomen war das falsch!)

  (dosync
   (println @konto1 @konto2))

  ;; Lesezugriffe auf **refs** in einem dosync Block sind konsistent.
  ;; Man kann innerhalb eines dosync auch Atome auslesen;
  ;; das ändert aber absolut gar nichts an der Inkonsistenz!




  ;; Anmerkung: wir wissen nun, was eine Transaktion ist.
  ;; Das Ausrufezeichen an einer Funktion wie swap! bedeutet,
  ;; dass man sie nicht sicher in einer Transaktion verwenden kann.
  ;; Seiteneffekte werden durch Konflikte (vielleicht) noch einmal ausgeführt -
  ;; je nach dem, wie weit die Transaktion vor dem Konflikt kam.
  ;; Das ist eine ganz unangenehme Kombination.
  ;; Daher bei so etwas möglichst nur pure Funktionen verwenden!





  ;; Problem: Write Skew

  ;; besorgen wir uns noch einmal zwei refs
  (def r1 (ref 0))
  (def r2 (ref 0))

  ;; wenn beide refs 0 sind, dann soll die erste 1 werden
  (defn f1 []
    (dosync
     (when (= 0 @r1 @r2)
       (Thread/sleep 200)
       (alter r1 inc))))

  ;; wenn beide refs 0 sind, dann soll die zweite 1 werden
  (defn f2 []
    (dosync
     (when (= 0 @r1 @r2)
       (Thread/sleep 200)
       (alter r2 inc))))

  ;; Offensichtlich kann nur eine der beiden Transaktionen ohne Konflikt durchgehen.
  ;; Also kann auch nur exakt ein Wert 1 werden.

  (do
    (future (f1))
    (future (f2)))

  ;; ups
  (dosync [@r1 @r2])


  ;; Es gibt keine Sequenz f1, f2 in der das passieren sollte.
  ;; Das Problem ist, dass die beiden Transaktionen nicht im Konflikt sind!
  ;; rein lesende Zugriffe werden nicht in die Menge der Konflikte aufgenommen...

  ;; Man könnte manuell den Konflikt provozieren, indem wir den gelesenen Wert mit sich selbst ersetzen:

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



  ;; Effizienter und schöner nimmt ensure eine Ref mit ins Konfliktset auf

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



  ;; Agents & Refs passen gut zusammen

  ;; erst eine Version ohne Agent
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

  ;; siehe REPL
  (do
    (future (transfer k1 k2 10))
    (future (transfer k1 k2 10)))
  (dosync [@k1 @k2])

  ;; warum so viele transferring-Prints?
  ;; refs wissen, ob sie in einer Transaktion
  ;; bereits geändert wurden.
  ;; Wenn es eine laufende Transaktion gibt,
  ;; die die Ref geändert hat,
  ;; so schlägt das deref in einer
  ;; anderen Transaktion direkt fehl.


  ;; und jetzt besorgen wir uns einen Agenten für die Seiteneffekte
  (def spy (agent nil))

  (defn transfer' [f t a]
    (dosync
     (when (< a @f)
       (send spy (fn [_] (println "transferring (I'm printed by an agent!)")))
       (alter f #(- % a))
       (Thread/sleep 2000)
       (alter t #(+ % a)))))

  ;; siehe REPL
  (transfer' k1 k2 10)

  (dosync [@k1 @k2])

  (do
    (future (transfer' k1 k2 10))
    (future (transfer' k1 k2 10)))

  (dosync [@k1 @k2])

  ;; send hat kein !, also ist es sicher in einer Transaktion.
  ;; tatsächlich werden die Aufträge an die Agenten erst verbindlich, wenn die Transaktion durchging.


  ;; nochmal in direkter Kombination:
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


  ;; Future: Container für den Wert einer Berechnung,
  ;; die nebenläufig erfolgt.
  ;; Dereferenzieren einer noch nicht beendeten
  ;; Berechung blockiert den Aufrufer

  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     @result))

  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     (Thread/sleep 1500)
     @result))


  ;; Mit realized? kann man herausfinden, ob die Berechnung gelaufen ist
  (time
   (let [result (future (do (Thread/sleep 1000) 23))]
     (println "Check1:" (realized? result))
     (Thread/sleep 1500)
     (println "Check2:" (realized? result))))


  ;; nag fragt regelmäßig, ob wir schon da sind, aber höchstens n-Mal
  ;; (damit wir uns nicht versehentlich für immer die Konsole vollspammen)
  ;; default ist 200-Mal.
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


  ;; das zeigt noch einmal den Unterschied, was passiert, je nach dem wann man dereferenziert:
  (let [x (future (do (Thread/sleep 2000) 42))]
    (println "nagging")
    (println :first-try x)
    (nag x))

  (let [x (future (do (Thread/sleep 2000) 42))]
    (println "nagging")
    (println :first-try @x) ;; deref!
    (nag x))



  ;; futures gehen in einen Threadpool.
  ;; bei mir arbeiten 32 futures auf einmal:
  (time
   (let [comp-array
         (for [_ (range 32)] (future (do (Thread/sleep 2000) 1)))]
     (doseq [c comp-array] @c)))

  ;; Auch mal mit mehr Futures ausprobieren




  ;; Promise ist ein Container, der irgendwann einmal von irgendwem befüllt wird.
  ;; Bei Dereferenzierung blockiert der Aufrufer bis der Wert geliefert wurde.

  (def x (promise))

  (future (nag x))
  (deliver x 10)
  @x



  )
