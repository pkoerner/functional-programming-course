(ns repl.15-test-check
  (:use [repl.15-test-check-b])
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test :as t]))

 
  ;; zuerst: ebt und transients


  ;; Angenommen ihr hättet transients in Clojure eingebaut.
  ;; Wie würdet ihr sicherstellen, dass eure Implementierung
  ;; korrekt ist?

  ;; Welche Tests würdet ihr schreiben?


  ;; 2-3 Minuten
  ;; Ihr braucht keinen Clojure Code schreiben,
  ;; Testszenarios reichen aus



  ;; kurzer Einschub: Typen von Funktionen (Notation)

  ;; viele Funktionen nehmen Argumente von bestimmten Typen
  ;; und bilden auf bestimmte Typen ab

  ;; Typ von (dem zweistelligen) +: Number -> Number -> Number
  ;; wir verstehen das als curried function.
  ;; + nimmt ein Argument vom Typ Number,
  ;; und gibt eine Funktion zurück, die eine zweite Number nimmt.
  ;; das Ergebnis der Funktion ist wieder vom Typ Number.


  ;; es gibt auch Typvariablen (i.d.R. a, b, c, ...)
  ;; Typ von str: a -> String
  ;; str nimmt irgendetwas und macht einen String raus



  ;; Typ von identity: a -> a


  ;; Listen von einem Typen werden beispielsweise als [String]
  ;; oder [a] geschrieben.
  ;; wir nehmen hier an, dass die leere Liste auch vom Typ [a] ist - 
  ;; euer Typsystem sieht das ggf. anders ;-)


  ;; Typ von reverse: [a] -> [a]
  ;; Typ von rest: [a] -> [a]
  ;; Typ von clojure.string/join: [String] -> String


  ;; wenn ein Argument eine Funktion ist,
  ;; muss man klammern:

  ;; Typ von map: (a -> b) -> [a] -> [b]
  ;; map kriegt eine beliebige Funktion, die Typ a nach Typ b abbildet;
  ;; das können auch dieselben Typen sein!
  ;; Das zweite Argument ist eine Liste vom Typ a,
  ;; das Ergebnis ist die Liste von Typ b.




  ;; Frage: Welche Funktionen könnten das sein?

  ;; Int -> Int -> Int
  ;; [a] -> Int
  ;; (a -> b) -> ([a] -> [b])
  ;; (a -> Bool) -> [a] -> [a]


  ;; Was ist der Typ von reduce?




  ;; zurück zum eigentlichen Thema.

  ;; was wäre wenn wir von einer Funktion den Input beschreiben könnten
  ;; und den Output entsprechend charakterisieren könnten?


  ;; Willkommen zu test.check!


(comment
  ;; Schritt 1: Input beschreiben

  ;; dafür gibt es einen Haufen Generatoren:
  gen/nat
  ;; ist ein Generatorobjekt

  ;; sample gibt mal ein paar Beispielwerte von dem Generator
  (gen/sample gen/nat)

  ;; oder auch auf Bestellung etwas mehr (hier 100 Stück)
  (gen/sample gen/nat 100)

  ;; es gibt nicht nur natürliche Zahlen, sondern viel, viel mehr
  (gen/sample gen/boolean)
  (gen/sample gen/char-ascii)
  (gen/sample gen/keyword)
  (gen/sample gen/int)
  (gen/sample gen/any)
  (gen/sample gen/any-printable) ;; damit es nicht auf dem Terminal so klingelt, und andere Steuerzeichen wegbleiben
  (gen/sample gen/string 20)
  (gen/sample gen/string-ascii 20)
  (gen/sample gen/string-alphanumeric 20)

  ;; nicht so offensichtlich
  ;; Choose für Intervalle (beide Seiten inklusive)
  (gen/sample (gen/choose 100 250))
  ;; return nimmt einen Wert und gibt einen Generator zurück, der nur diesen Wert generiert
  ;; return hat also den Typen a -> gen a
  (gen/sample (gen/return 3))

  ;; Composed
  ;; homogene Collections von Werten
  (gen/sample (gen/vector gen/boolean))
  ;; Vektor mit fester Länge
  (gen/sample (gen/vector gen/boolean 3))
  ;; Tupel mit Generator pro Index
  (gen/sample (gen/tuple gen/int gen/boolean))
  ;; Maps von Keywords auf Maps von natürlichen Zahlen auf Integer
  (gen/sample (gen/map gen/keyword (gen/map gen/nat gen/int)))

  ;; Filter
  ;; nicht-leere Vektoren
  (gen/sample (gen/not-empty (gen/vector gen/boolean)))
  ;; bestimmtes Prädikat
  (gen/sample (gen/such-that even? gen/nat) 30)
  ;; wenn man das oft genug probiert (oder Pech hat) bekommt man eine Fehlermeldung:
  ;; Couldn't satisfy such-that predicate after 10 tries. 
  ;; such-that sollte man nutzen, wenn es eine eher kleine Einschränkung des Generators ist.
  ;; Will man mehr ausschließen sollte man über eine andere Konstruktion nachdenken, z.B. wie folgt:


  ;; Higher order Generatoren


  ;; Transformation von generierten Werten
  ;; fmap: (a -> b) -> gen a -> gen b
  (gen/sample (gen/fmap str gen/int))

  ;; das ist sehr mächtig!
  ;; Hier gibt es drei Booleans, und als letztes Element wird gezählt, wie viele davon wahr sind.
  (gen/sample 
   (gen/fmap (fn [[x y z :as c]]
               [x y z (count (filter identity c))])
             (gen/vector gen/boolean 3)))

  ;; Das ist deutlich mehr als ein Typsystem in der Regel ausdrücken kann!
  ;; Man nennt dies dependent type, weil es auf den Wert drauf ankommt, ob er im Typ drin ist oder nicht.
  ;; das ist sehr mächtig!
  ;; Hier gibt es drei Booleans, und als letztes Element wird gezählt, wie viele davon wahr sind.
  ;; Das ist deutlich mehr als ein Typsystem in der Regel ausdrücken kann!
  ;; Man bezeichnet dies als dependent type, weil es auf den Wert drauf ankommt, ob er im Typ drin ist oder nicht.




  ;; mit frequency man kann auch mit einer bestimmten Gewichtung Werte bekommen
  (let [x (gen/sample
           (gen/frequency [[70 (gen/return :kopf)]
                           [30 (gen/return :zahl)]]) 1000)]
    (frequencies x))

  ;; die Werte müssen nicht 100 ergeben
  (let [x (gen/sample
           (gen/frequency [[2000 (gen/return :kopf)]
                           [2000 (gen/return :zahl)]]) 1000)]
    (frequencies x))

  ;; Auswahl aus fester Collection an Werten
  ;; elements: [a] -> gen a
  (gen/sample (gen/elements [1 2 3]))
  ;; ein "oder" auf Generatoren:
  ;; one-of: [gen a] -> gen a
  (gen/sample (gen/one-of [gen/int gen/boolean]))


  ;; 1000 Werte aus dem Bereich 0 bis 10, die nicht 5 sind
  (let [g (gen/such-that
           (fn [e] (not= e 5))
           (gen/choose 0 10))
        sample (gen/sample g 1000)]
    (frequencies sample))


  ;; Komplexere Generatoren
  ;; Ein Tupel bestehend aus einem Vektor
  ;; und einem Element aus diesem Vektor

  ;; zuerst brauchen wir einen nicht-leeren Vektor
  (def vector-gen (gen/not-empty (gen/vector gen/int)))
  (gen/sample vector-gen)

  ;; gegeben ein Vektor, gebe den Vektor zurück und wähle ein Element aus
  (defn tuple-from-vector-gen [v] 
    (gen/tuple (gen/return v)
               (gen/elements v)))
  (gen/sample (tuple-from-vector-gen [1 2 3]))


  ;; und der letzte Schritt:
  ;; die Ergebnisse aus einem Generator müssen in den nächsten Generator gepresst werden.
  ;; Das macht der Pömpel bind.
  ;; bind: gen a -> (a -> gen b) -> gen b
  (def complex-gen (gen/bind vector-gen tuple-from-vector-gen))
  (gen/sample complex-gen 20)
)






  ;; bind und return?
  ;; behaltet das mal im Kopf (für eine Weile)






  ;; Was hat das nun mit Testing zu tun?







  ;; Idee von Property-Based Testing:
  ;; Spezifiziere die Relation zwischen Input und Output als Prädikat.
  ;; Wirf zufällige Eingaben in die Funktion und
  ;; checke den Output.


  ;; in der Regel gibt es drei Muster:
  ;; 1. Test gegen eine Umkehrfunktion
  ;;    (Parsing + Pretty Printing, Serialisierung + Deserialisierung, ...)
  ;; 2. Test gegen bestehende Implementierung (ein Orakel)
  ;; 3. Test durch Charakterisierung bestimmter Eigenschaften

(comment

  ;; manchmal ist es schwierig, die richtige Property zu finden

  ;; zurück zum example-based-testing
  ;; my-sort wurde woanders definiert, nicht spicken!
  ;; hier ein paar Testcases für die Sortierfunktion

  (t/are [x y] (= x y) 
    (my-sort [1]) [1]
    (my-sort [1 2]) [1 2]
    (my-sort [5 1 3 2 4]) [1 2 3 4 5]
    (my-sort [1 2]) (my-sort [2 1])
    (my-sort [1 3 2 4 5]) (my-sort [5 1 3 2 4]))


  ;; falsche Testcases explodieren wirklich
  (t/are [x y] (= x y) 
    (my-sort [1]) [3])


  ;; alle Tests laufen durch!
  ;; die Implementierung ist BESTIMMT korrekt!



  ;; Reicht das an test-cases?

  ;; natürlich nicht!
  ;; wir generieren jetzt Testcases, anstatt sie selbst zu schreiben
  (def vectors-of-numbers (gen/vector gen/int)) 

  (gen/sample vectors-of-numbers) 

  ;; Wir benutzen ein Orakel, also eine Implementierung die korrekt
  ;; ist. Das ist ziemlich nützlich, wenn man eine alte (korrekte aber
  ;; möglicherweise nicht optimale) Implementierung ersetzen will

  ;; sort funktioniert bestimmt
  (defn sortiert? [v]
    (= (sort v) v)) 

  (sortiert? []) 
  (sortiert? [1 2]) 
  (sortiert? [2 1]) 


  ;; wir testen bei generiertem Input nun die Eigenschaft:
  ;; Das Ergebnis einer sort Funktion sollte sortiert sein:
  ;; für alle Daten, die aus dem Generator rausfallen, soll die Eigenschaft gelten.
  (def sortiert-prop (prop/for-all [data vectors-of-numbers] 
                                   (sortiert? (my-sort data))))


  ;; dann mal 100 Testcases, go!
  (tc/quick-check 100 sortiert-prop) 
  ;; ups, leere Collection vergessen


  ;; Es gibt ein Macro um test.check in einen clojure.test Testcase umzuwandeln
  ;; (defspec qs-sorted 100 sortiert-prop)


  (t/is (= (my-sort []) [])) 
  ;; da kommt nill statt = raus...



  ;; Versuch zwei, wir fixen die leere Collection
  (def sortiert-prop (prop/for-all [data vectors-of-numbers] 
                                   (sortiert? (my-sort2 data))))


  (my-sort2 [])
  (my-sort2 [3 4 1])
  ;; dann mal 100 Testcases, go!
  (tc/quick-check 100 sortiert-prop) 
  ;; mal 1000 Tests zur Sicherheit?
  (tc/quick-check 1000 sortiert-prop) 




  ;; alle Tests laufen durch!
  ;; die Implementierung ist BESTIMMT korrekt!





  ;; Noch eine Eigenschaft, die im Eifer des Gefechts fehlte: 
  ;; Alle ursprünglichen Werte sollten erhalten bleiben.
  ;; Eine Sortierfunktion, die immer den leeren Vektor zurückgibt, ist nicht gut.

  (defn permutation? [v1 v2]
    (= (frequencies v1) (frequencies v2))) 


  (def permutation-prop
    (prop/for-all [data vectors-of-numbers]
                  (permutation? data (my-sort2 data)))) 


  ;; mit dem Seed geht es garantiert kaputt
  (def r (tc/quick-check 10 permutation-prop :seed 1422980091656)) 

  (:fail r)  ;; das ist der kaputte Test, der gefunden wird

  r  ;; das ist das gesamte Ergebnis

  ;; und jetzt der coole Trick:
  ;; test.check minimiert die Eingabe an die Funktion und kann ein Minimalbeispiel geben, bei dem es fehlschlägt
  (-> r :shrunk :smallest) 


  (t/is (= (my-sort2 [0 0]) [0 0])) 
  ;; Duplikate werden weggeworfen...



  ;; Implementierung 1
  (defn my-sort [coll]
    (into (sorted-set) coll))



  ;; Implementierung 2
  (defn my-sort2 [coll]
    (into [] (into (sorted-set) coll)))






  )




(comment
  ;; Zurück zum Transient Beispiel

  ;; Wenn wir eine Sequenz haben (-> #{} (conj 1) (conj 2) (disj 0))
  ;; Dann können wir die Performance erhöhen, indem wir
  ;; 1) Als Erstes transient aufrufen
  ;; 2) conj durch conj! und disj durch disj! ersetzen
  ;; 3) Am Ende persistent! aufrufen


  ;; Beobachtung:
  ;; Es gibt eine API mit Pre- und Postconditions für die API Calls.

  ;; Z.B.: Wir können transient und persistent! öfter aufrufen, aber nur
  ;; paarweise. transient ... persistent! ... transient ... persistent! ...
  ;; Aber nicht transient ... transient ... persistent! ... persistent!


  ;; Idee: 
  ;; Generiere viele Sequenzen von API Calls, die erlaubt sind
  ;; und verifiziere die Postconditions.
  
  ;; Tatsächlich (korrekten) Code zu generieren ist etwas aufwändiger.
  ;; Alternativ können wir aber Instruktionen generieren und durch einen kleinen Interpreter ausführen lassen.
  ;; Falls die Precondition nicht erfüllt ist, wird die Instruktion ignoriert.


  (defn transient? [x]
    (instance? clojure.lang.ITransientCollection x)) 


  ;; Unsere Instruktionen sollen die Form [:conj Zahl], [:disj Zahl], [:trans] oder [:pers] haben.


  ;; Was machen wir?
  ;; 1) Generiere alle Aktionen

  ;; wir können transient und persistent! aufrufen...
  (gen/sample (gen/elements [[:trans] [:pers]]))
  ;; oder conj + Zahl und disj + Zahl
  (gen/sample (gen/tuple (gen/elements [:conj :disj]) gen/int))

  ;; die verodern wir mit one-of und wollen eine (nicht-leere) Reihe davon (Vektor)
  (def gen-mods
    (gen/not-empty (gen/vector (gen/one-of
                                 [(gen/elements [[:trans] [:pers]])
                                  (gen/tuple (gen/elements [:conj :disj]) gen/int)])))) 


  (gen/sample gen-mods) 
  ;; jede Aufrufsequenz ist ein Testcase!

  ;; 2) Lasse die Actionens auf dem leeren Set laufen.
  ;;    Falsche Sequenzen werden vom Interpreter automatisch repariert

  (defn run-action [c [f & [arg]]] 
    (condp = [(transient? c) f]
      [true   :conj]          (conj! c arg) ;; wenn es transient ist, nehmen wir das transient-conj!
      [false  :conj]          (conj c arg)  ;; wenn es persistent ist, dann das normale conj
      [true   :disj]          (disj! c arg)
      [false  :disj]          (disj c arg)
      [true   :trans]         c             ;; ein transient machen wir nicht noch einmal transient
      [false  :trans]         (transient c) ;; ein persistent machen wir transient
      [true   :pers]          (persistent! c) ;; transient machen wir persistent
      [false  :pers]          c))             ;; persistents machen wir nicht persistent

  ;; damit können wir eine beliebige Instruktion abarbeiten!

  (run-action #{} [:conj 2]) 
  (run-action #{} [:trans]) 
  (persistent! (run-action (transient #{1}) [:disj 1])) 

  ;; damit wir viele Instruktionen abarbeiten, reducen wir die Funktion einfach darüber, mit dem Set als Akkumulator
  (defn reduce-actions [coll actions]
    (reduce run-action coll actions)) 

  (reduce-actions #{} [[:conj 3]])
  (reduce-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:pers]]) 
  (reduce-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]])

  ;; und eine Hilfsfunktion noch, damit es nicht doof aussieht:
  ;; wenn ein Transient am Ende rausfällt, machen wir den noch einmal persistent
  (defn apply-actions [coll actions] 
    (let [applied (reduce-actions coll actions)]
      (if (transient? applied)
        (persistent! applied)
        applied)))

  ;; fertig!
  (apply-actions #{} [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]]) 

  ;; 3) Wir werfen alle Calls, die transient und persistent machen weg.
  ;; Das wird unsere Referenz, die nur conj und disj auf den persistenten Datenstrukturen aufruft.
  ;; apply-actions macht immer wieder transients raus und die wieder persistent.

  (defn filter-actions [actions] 
    (filter (fn [[a & args]]
              (#{:conj :disj} a))
            actions))

  (filter-actions [[:conj 2] [:trans] [:conj 4] [:trans] [:trans]]) 


  ;; 4) Wenn transients korrekt funktionieren, muss das Ergebnis das gleiche sein,
  ;; als ob ich nie ins transient gegangen wäre.

  (def transient-property 
    (prop/for-all
     [a gen-mods]
     (= (apply-actions #{} a) ;; benutzt persistente Sets und transients gemischt
        (apply-actions #{} (filter-actions a))))) ;; benutzt nur persistente Sets


  ;; das sieht gut aus (je nach Glück)
  (tc/quick-check 100 transient-property) 
  (tc/quick-check 1000 transient-property) 

  ;; aber...
  (def r (tc/quick-check 400 transient-property :seed 1422983037254)) 

  ;; es gibt diese komische Sequenz hier
  (:fail r) 
  ;; 119 Schritte macht die
  (count (first (:fail r)))

  ;; wo liegt nun der Fehler...? Uff...

  ;; Shrinking to the rescue!
  (def full-seq (-> r :shrunk :smallest first)) 

  full-seq 
  ;; Keine Aktion kann man weglassen ohne, das der Fehler verschwindet.

  (let [le-seq [[:conj -49] [:conj 48] [:trans] [:disj -49] [:pers]]] ;; ohne [:conj -49]
    (= (apply-actions #{} le-seq)
       (apply-actions #{} (filter-actions le-seq)))) 

  (= (apply-actions #{} full-seq) 
     (apply-actions #{} (filter-actions full-seq)))

  ;; eigentlich ist es noch schlimmer:

  (= (apply-actions #{} full-seq) 
     (apply-actions #{} full-seq))

  ;; Was ist denn genau nicht gleich?

  (t/is (= (apply-actions #{} full-seq) 
           (apply-actions #{} (filter-actions full-seq))))
  ;; WTF - {48 -49} ist nicht gleich {48 -49}?

  ;; was das Problem ist:
  (hash -49) 
  (hash 48) 

  ;; Problem: HashCollisionNode
  ;; Alle Werte, die Hashkollisionen erzeugen, werden in einem Array gespeichert.
  ;; Bei persistenten Datenstrukturen wird bei dissoc / disj dieses Array verkleinert.
  ;; Bei transients werden die Werte einfach mit null überschrieben.
  ;; Allerdings stimmt dann nicht mehr die Arraylänge mit dem Elementcount überein!
  ;; Wer von euch hätte diesen Testcase gehabt?


  ;; wir brauchten dafür ein altes Clojure.
  *clojure-version* ; =>  {:major 1, :minor 5, :incremental 1, :qualifier nil}


  ;; in Clojure 1.6 gefixt



  ;; Bug: http://dev.clojure.org/jira/browse/CLJ-1285
  ;; Wurde tatsächlich mit dieser Methode gefunden!
  ;; https://groups.google.com/forum/#!msg/clojure-dev/HvppNjEH5Qc/1wZ-6qE7nWgJ


  ;; Es dadurch gibt eine Spezialbibliothek für das Testen von Datenstruktur-Implementierungen
  ;; https://github.com/ztellman/collection-check


)





