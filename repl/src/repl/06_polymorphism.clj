(ns repl.06-polymorphism
  (:use [clojure.repl])
  (:require [instaparse.core :refer [parser]])) ;; für ein Beispiel
;; benötigt instaparse in der project.clj


;; heute neu:
;; Multimethoden: defmulti, defmethod
;; Bibliothek: instaparse: parser
;; clojure.core: defrecord, ->Typkonstruktor,
;;               defmulti, defmethod, prefer-method
;;               defprotocol, extend-type, extend-protocol, extend
;;               isa?


(comment

  ;; zuerst: Das Expression Problem


  ;; nun: zwei Lösungen für das Expression Problem

  ;; mit defrecord besorgen wir uns eine neue Java Klasse
  ;; eine Instanz des Records hat garantiert die angegebenen Felder
  (defrecord KlausurAufgabe [aufgabe punkte])

  ;; dazu geschenkt bekommt man den Konstruktor mit ->..., 
  ;; der die Werte in Reihenfolge in die Felder schreibt
  (def aufgabe1 (->KlausurAufgabe 1 10))
  ;; Instanzen sind immutable
  ;; und verhalten sich wie Maps

  ;; ist aber eine echte neue Klasse
  (class aufgabe1)

  (:punkte aufgabe1)
  ;; sieht auch fast wie eine Map aus
  aufgabe1
  (assoc aufgabe1 :foo 12)
  (dissoc aufgabe1 :punkte) ;; keine KlausurAufgabe mehr!






  ;; multimethod = multi + method
  (defmulti get-columns class) ;; "Interface"-ish
  (defmulti get-columns ;; Funktionsname (beliebig wählbar)
            class)      ;; Dispatchfunktion

  (defmethod get-columns KlausurAufgabe [_] ["Aufgabe","Punkte"]) ;; konkrete Implementierung
  (defmethod get-columns ;; Funktionsname (muss zum defmulti passen)
             KlausurAufgabe ;; Ergebnis der Dispatchfunktion
             [this] ;; Argumentvektor
             ["Aufgabe","Punkte"]) ;; Body
  (get-columns aufgabe1)


  ;; auf Strings ist die Multimethode (noch) nicht definiert
  (get-columns "sss")

  ;; also machen wir das einfach - was passiert, ist uns grad egal
  (defmethod get-columns String [s] (println :juhu s))

  (get-columns "sss")

  ;; genauso definieren kann man die value-Funktion
  (defmulti get-values class)
  (defmethod get-values KlausurAufgabe [k] [(:aufgabe k) (:punkte k)])


  (get-values aufgabe1)

  ;; geht natürlich nicht auf Vektoren...
  (def b ["1","2"])
  (get-values b)

  ;; aber das können wir implementieren!
  (class b)
  (defmethod get-columns clojure.lang.PersistentVector [k] k)
  (defmethod get-values clojure.lang.PersistentVector [k] k)

  (get-values b)

  ;; nil ist nichts Besonderes!
  (class nil)
  (defmethod get-values nil [_] ["empty"])
  (get-values nil)


  ;; Ganzzahlen genauso...
  (get-values -3)
  (defmethod get-values Long [_] ["foo"])


  (get-values -3.14)
  ;; fangen wir mal alles ab:
  ;; :default ist hier tatsächlich mal wieder ein Keyword!
  ;; alternative Werte kann man aber im defmulti setzen
  (defmethod get-values :default [x]
    (println "get-values not implemented for" x "oop! oop! oop!"))
  (get-values -3.14)




  ;; die Dispatchfunktion kann komplizierter sein, als nach der Klasse zu fragen:
  (defmulti cred (fn [c] (> 5 (count c))))

  ;; man kann JEDE beliebige Funktion fürs Dispatching verwenden

  (defmethod cred true [c] (println "Kurze Liste"))
  (defmethod cred false [c] (println "Lange Liste!!!"))


  (cred [1 2 3 4 5 6])
  (cred [1])


  ;; Dispatching on multiple inputs

  (def simba {:species :lion})
  (def clarence {:species :lion})

  (def bugs {:species :bunny})
  (def donnie {:species :bunny})

  ;; dann wird die Dispatchfunktion halt länger mit mehr Argumenten
  (defmulti encounter (fn [a b] [(:species a) (:species b)]))

  ;; und das Verhalten dazu
  (defmethod encounter [:bunny :lion] [x y] :run-away)
  (defmethod encounter [:lion :lion] [x y] :fight )
  (defmethod encounter [:bunny :bunny] [x y] :mate)
  (defmethod encounter [:lion :bunny] [x y] :omnomnom)

  (encounter simba bugs)
  (encounter clarence simba)
  (encounter bugs bugs)





  ;; Reversible auf Strings lösen
  (defn reverse-a-string-java-style [s]
    (clojure.string/join (reverse s)))

  (reverse-a-string-java-style "foo")
  (reverse-a-string-java-style -3)


  ;; Das ist das Pendant zu new Dispatcher(new IFunction() {...})
  (defmulti reversr (fn [object] (class object)))


  ;; Das Pendant zu reverse.register(String.class, new IFunction() {...})
  (defmethod reversr String [s] (reverse-a-string-java-style s))


  (reversr "foo")

  ;; und auf Long, und was auch immer wir wollen!
  (reversr 15)

  (defmethod reversr Long [l] (- l))

  (reversr 15)

  ;; Vorteil: Datenrepräsentation (Typ) und dispatching (defmulti) sind nicht mehr complected
  ;; Vorteil: defmethods können in anderen Namespaces beliebig erweitert werden (insbesondere, wenn das defmulti in einer Bilbiothek steht)




  ;; Performance

  (defmulti m1 class)
  (defmethod m1 clojure.lang.PersistentVector [k] (count k))
  (defn m2 [k] (count k))

  (time (dotimes [i 10000000] (m1 [1])))
  (time (dotimes [i 10000000] (m2 [1])))

  ;; Zwischen Funktionsaufruf und Multimethods gibt es eine Faktor von ca. 8
  ;; Funktionen sind auch noch etwas langsamer als direkte Methodenaufrufe in Java








  ;; Fallbeispiel
  ;; Wir implementieren einene Interpreter für Squarejure
  ;; related: Clochure (http://clochure.org/)

  ;; die Sprache sieht so aus:
  [:add [:int 4] [:int 9]]
  ;; Funktionsaufrufe gehen mit eckigen Klammern.
  ;; wir müssen dann noch built-ins für :add, :int, etc. implementieren

  ;; wir dispatchen also auf dem ersten Element eines Vektors...
  (defmulti squarejure (fn [[e & _]] e))

  ;; für Integer nehmen wir Clojure-Longs...
  (defmethod squarejure :int [[_ v]] v)

  (squarejure [:int 6])

  ;; addieren ist dann auch einfach
  (defmethod squarejure :add [[_ a b]]
    (+ (squarejure a) (squarejure b)))

  (squarejure [:add [:int 4] [:int 9]])


  ;; und wir können den Interpreter (und damt die Sprache) später beliebig erweitern
  (defmethod squarejure :sum [[_ & args]]
    (apply + (map squarejure args)))

  ;; Was mit Funktionen geht, geht auch mit Multimethods

  (squarejure [:sum [:add [:int 3] [:int 7]]
                    [:int 6]
                    [:int 77]
                    [:int -4]])


  (def ebnf "
    S = add
    add = int '+' S | int '+' int
    int = #'[0-9]+'
  ")


  ;; parser stammt aus der Instaparse-Bibliothek
  ;; Die Bibliothek ist ziemlich cool - sie generiert Parser für ziemlich alle Typen an Grammatiken,
  ;; insbesondere nichtdeterministische Grammatiken (dann kriegt man alle Parsebäume)
  (def parse (parser ebnf))



  ;; der Syntaxbaum sieht so aus
  (parse "3+5")
  (parse "3+9+12")
  ;; warum die Liste außen rum?
  ;; es ist die Sequenz aller Parsebäume!


  ;; wir machen den Syntaxbaum abstrakt und verstecken mit <...> ein paar Symbole, die uns nicht interessieren
  (def ebnf2 "
    <S> = add
    add = int <'+'> S | int <'+'> int
    int = #'[0-9]+'
  ")

  (def parse (parser ebnf2))

  (parse "3+5")
  (parse "3+9+12")


  ;; der erste Syntaxbaum ist gut genug.
  (defn sqeval [prog] (squarejure (first (parse prog))))

  ;; read-string transformiert Strings in Clojure-Datenstrukturen
  (read-string "8")
  (read-string "[:a :b]")

  ;; falls wir ein Literal haben, ist es ein String und muss noch in einen Clojure Long transformiert werden
  (defmethod squarejure :int [[_ e]] (if (string? e) (read-string e) e))

  (sqeval "8+9")
  (sqeval "2+2+2")

  ;; das war ziemlich wenig Code für einen erweiterbaren Interpreter!


  ;; Multimethoden können auch mit Hierarchien umgehen.
  ;; Eine Standardhierarchie ist die Superklassen-Beziehung:

  (defmulti hierarchy-taist class)
  (defmethod hierarchy-taist java.util.Collection [x] (count x))

  ;; die Klasse Vektor ist nicht das Interface Collection

  (class [1 2 3])
  (= (class [1 2 3]) java.util.Collection)
  ;; aber es implementiert das Interface und klappt damit trotzdem!
  (hierarchy-taist [1 2 3])


  ;; multimethods verwenden isa? für den Dispatch
  (isa? (class [1 2 3]) java.util.Collection)
  (isa? [1 2 3] java.util.Collection)
  (isa? [1 2 3] [1 2 3])

  (doc isa?)
  ;; wahr, bei
  ;; - Gleichheit
  ;; - Typvererbung (Java)
  ;; - abgeleitete Werte (Stichworte: die Funktionen derive, make-hierarchy)
  ;; das letzte (drive/make-hierarchy) lassen wir aus - es ist unschön, unhandlich und brauchen wir hier nicht.














  ;; ----------------------------------------------------------------------------------------------
  ;; defprotocol - Expression Problem, Haskell Style


  ;; Definition eines Interfaces. Das Protocol beschreibt einen Satz von Funktionen
  (defprotocol TheCount ;; ahh, ahh, ahh!
    (cnt [v]))



  ;; error
  (cnt "foo")

  ;; String implementiert das Protocol nicht - dann sorgen wir dafür, dass es das tut!
  (extend-type String
    TheCount
    (cnt [s] (.length s))) ;; ruft die Java-Methode s.length() auf

  (cnt "foo")

  ;; nil ist wieder nichts besonderes
  (cnt nil)

  ;; andere Geschmacksrichtung: extend-protocol
  ;; Typ und Interface sind vertauscht
  (extend-protocol TheCount
    nil
    (cnt [_] 0))

  (cnt nil)

  ;; und nun Vektoren...
  (cnt [1 3])

  ;; noch eine Version: extend
  (extend java.util.Collection
    TheCount
    {:cnt (fn [k] (count k))})


  (cnt [1 2])
  (cnt '(1 2 3))
  (cnt #{1})
  (cnt {1 2, 3 4})

  ;; Maps sind keine Collections, aber zumindest Counted


  (extend clojure.lang.Counted
    TheCount
    {:cnt (fn [k] (count k))})

  (cnt {1 2, 3 4})



  ;; extend-protocol ist eigentlich extend-type
  (macroexpand-1
   '(extend-protocol TheCount
      java.io.File
      (cnt [f] (if (.exists f) (.length f) 0))))


  ;; extend-type ist eigentlich extend
  (macroexpand-1
   '(extend-type java.io.File
      TheCount
      (cnt [f] (if (.exists f) (.length f) 0))))




  ;; die Dokumentation erklärt genau, was man wofür nimmt
  (doc extend-type)
  ;; mit extend-type man kann einen Typen auch um mehrere Protokolle erweitern

  (doc extend-protocol)
  ;; mit extend-protocol kann man ein Protokoll zu mehreren Typen hinzufügen

  (doc extend)
  ;; extend ist die Basis mit der hässlichen Syntax ;-)





  ;; Performance: Multimethoden vs. normalen Funktionen vs. Protokolle

  (defmulti m1 class)
  (defmethod m1 clojure.lang.PersistentVector [k] (count k))

  (defn m2 [k] (count k))

  (defprotocol MyProt
    (m3 [this]))

  (extend-protocol MyProt
    clojure.lang.PersistentVector
    (m3 [this] (count this)))

  (m3 [1 2 4 6 7])

  (time (dotimes [i 10000000] (m1 [1])))
  (time (dotimes [i 10000000] (m2 [1])))
  (time (dotimes [i 10000000] (m3 [1])))

  ;; Protocols vs. Multimethods

  ;; 1) Multimethods sind viel flexibler!
  ;;   - Dispatch-Funktion ist praktisch frei wählbar.
  ;;   - Bei Protocol: type auf dem ersten Argument

  ;; 2) Protocol ist deutlich schneller!
  ;;   - Protocol ist fast auf Augenhöhe mit direkten Methodenaufrufen
  ;;   - Multimethods sind ~ Faktor 8 (?) langsamer
  ;;   - Es gibt keinen Grund Multimethods zu verwenden, wenn die Dispatchfunktion type/class ist





  ;; a propos Typen und Performance:
  ;; type hints

  ;; wenn man diese Flag hier auf wahr setzt, meckert der Compiler, wenn Reflection verwendet wird,
  ;; um eine Java Methode oder ein Attribut zu finden.
  ;; Das ist nämlich voll langsam!
  (set! *warn-on-reflection* true)

  ;; REPL
  (defn len [x]
    (.length x)) ;; length kann nicht gefunden werden, also werden wir nun gewarnt

  ;; funktioniert aber
  (len "trololo")


  ;; und dauert ein Weilchen
  (time (reduce + (map len (repeat 1000000 "foo"))))


  ;; hier versprechen wir dem Compiler, dass x ein String ist
  (defn lenny [x]
    (.length ^String x)) ;; keine Reflection-Warnung!

  ;; viel, viel schneller!
  (time (reduce + (map lenny (repeat 1000000 "foo")))) 


  )
