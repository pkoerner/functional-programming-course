(ns repl.21-transients)

(comment 
  ;; Transients: Wir vermeiden mutable Objects, aber ...

  ;; Wenn das innerhalb einer Funktion gekapselt ist,
  ;; ist es kein Problem.
  ;; Konstruktion von grossen Vektoren, Sets, etc. (z.B. Datenbank vom Festplatte einlesen)
  ;; geht so einiges schneller.

  (-> #{} (conj 1) (conj 2) (conj 3) (conj 4) (disj 3)) ; =>  #{1 2 4}

  ;; man kann eine Datenstruktur in ein Transient umwandeln
  ;; dann ist es ein komisches Ding, wo man nicht reingucken kann
  (transient #{1 2})

  ;; die API bleibt gleich, außer dass man ein ! dranschreibt (nicht in Transaktionen verwenden!)
  ;; am Ende macht man das Ding wieder persistent

  (-> #{} transient (conj! 1) (conj! 2) (conj! 3) 
                    (conj! 4) (disj! 3) persistent!); =>  #{1 2 4}

  ;; danach ist es wieder ein normales Set, das Original hat sich nicht verändert
  (type (persistent! (transient #{}))) ; =>  clojure.lang.PersistentHashSet

  (-> #{} transient) ; =>  #<TransientHashSet clojure.lang.PersistentHashSet$TransientHashSet@147eec90>


  ;; genauere Untersuchung
  (def a #{1 3})
  a

  ;; nicht machen!
  ;; Transients hält man nur lokal in einem kleinen Block.
  (def b (transient a)) ; =>  #'repl.21-transients/b

  a ; =>  #{1 3} (unverändert)

  (conj! b 6)
  ;; gar nicht gut, siehe unten

  a ; =>  #{1 3}

  ;; b ist immer noch ein Transient
  (def c  (persistent! b))
  [a b c] ; =>  [#{1 3} #<TransientHashSet clojure.lang.PersistentHashSet$TransientHashSet@1adb83c8> #{1 3 6}]
  ;; c ist nur zufällig das, was wir "erwarten"!


  ;; nur damit der Vergleich unten gleich fairer ist - einmal den Garbage Collector explizit anwerfen
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

  ;; Transients sind etwa Faktor 2 schneller als die immutable Versionen und sind auf Augenhöhe mit
  ;; regulären mutable Datenstrukturen.



  ;; conj!, disj!, etc. haben dieselbe API
  ;; wie ihre persistenten Gegenstücke!(!)
  ;; Sie modifizieren nicht UNBEDINGT das Objekt
  ;; und dürfen auch ein neues zurückgeben!

  ;; ganz fies falsch:
  (let [x (transient {})] 
    (doseq [e (range 100)]
      (assoc! x e e))      ;; Rückgabe ignoriert
    (persistent! x)); =>  {0 0, 1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7}
  ;; Maps sind sehr besonders - nach 8 Elementen wird die Darstellung gewechselt.
  ;; Kleine Maps tauchen relativ häufig auf und besonders optimiert.
  ;; Dadurch, dass manchmal neue Objekte zurückgegeben werden, reicht es nicht aus,
  ;; die Referenz auf das originale Transient zu halten.


  ;; Also ist die richtige Anwendung von transients:
  ;; - Immer innerhalb einer Funktion generieren
  ;; und am Ende persistent! aufrufen.
  ;; - Immer den Rückgabewert von conj!, assoc!, etc. weiterverwenden.

  ;; richtige Lösung von dem oben
  (defn foo [n] 
    (loop [i 0 v (transient #{})]
      (if (< i n)
        (recur (inc i) (conj! v i))
        (persistent! v))))

  (foo 100)

  ;; die mutable Fünf sind
  conj!
  disj!
  assoc!
  dissoc!
  pop!
  

  ;; Noch richtiger: gar nicht benutzen, wenn einen keiner dazu zwingt :-)

)
