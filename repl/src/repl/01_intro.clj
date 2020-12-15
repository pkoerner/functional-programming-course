(ns repl.01-intro)

(comment
(use 'clojure.repl) ;; für apropos, doc und source
(use 'clojure.walk) ;; für macroexpand-all

;; Dokumentation von Funktionen ausgeben
(doc doc)
(doc source)
(doc apropos)

;; suche definierte Symbole 
;; nützlich, wenn man ungefähr weiß, wie es heißen könnte
(apropos "map")

;; Sourcecode lesen (Vorsicht, lang!)
(source map)
;; die Clojure Bibliotheksfunktionen sind auf mehrere Aritäten optimiert


;; nil ist exakt das Selbe wie null in Java!
(- nil)
;; NullPointerException sieht man sehr selten in reinem Clojure code.
;; die Clojure Bibliotheksfunktionen können (fast) alle mit nil umgehen
;; man muss sich schon etwas Mühe geben und Dinge aufrufen, die auf Java Code zurückgreifen.



;; ---------- Umgang mit Strings und Characters


;; str nimmt alles, was es bekommt, ruft .toString() auf und klebt es zusammen
(str "foo" \f)
(str "hallo "
     "welt")
;; Das ist etwas ungewohnt, insbesondere, weil \n in Strings die "normale" Bedeutung hat
(str \n)

(str \newline)
(str "Hallo" "Welt")

(str "Hallo" \space "Welt")

(str "Hallo" \newline "Welt")

;; println druckt Dinge aus + newline
(println \u03bb "Kalkül" "ftw!")
;; nur Zeilenumbruch printen
(newline)
;; kein Zeilenumbruch
(print "plonk")
;; wieder einlesbar (z.B. Strings kriegen Anführungszeichen) (oder pr ohne newline)
(prn "testi")



;; Arithmetische Grundlagen

;; inc rechnet +1
(inc 4)
;; was passiert, wenn man auf MAX_LONG eins addiert?
(inc 9223372036854775807)
(+ 9223372036854775807 1)

;; vernünftige Semantik: keine Under-/Overflows, sondern exceptions
;; außer man nutzt unsafe Operationen, z.B. unsafe-add 

;; falls es mal etwas größer wird, gibt es arithmetische Operationen mit ' dahinter
;; aka Auto-Promotion
(inc' 9223372036854775807)
(*' 9223372036854775807 9223372036854775807)

(*' 9223372036854775807 9223372036854775807)
;; *1 ist das letzte Ergebnis in der Repl
(type *1)
(type (*' 9223372036854775807 9223372036854775807))

(def foo-bar 3)
foo-bar
(def ffds4¼½³’ 42)
ffds4¼½³’


;; Division gibt Brüche zurück
(/ (inc 32) 7)
; Erinnerung: (inc 32)/7 ;; geht nicht!

;; ---------- Names

;; Zwei Arten: Symbols und Keywords

;; Symbols sind wie Bezeichner in anderen Sprachen, sie stehen für etwas
inc
+'
blah!
(quote *clojure-version*)
*clojure-version*
(type *clojure-version*)


;; Quote verhindert die Aufloesung des Symbols
;; vgl.: In Düsseldorf ist ein ü - irgendwo in der Stadt steht der Buchstabe rum
;; In "Düsseldorf" ist ein ü - der Name der Stadt enthält den Buchstaben

(type (quote *clojure-version*))

foo
(quote foo)
;; Konvention: Symbole mit ! stehen für etwa "Vorsicht, Vorsicht, hier ist ein Seiteneffekt!")
(quote foo!)
;; Konvention: Symbole mit ? stehen für ein Prädikat, also etwas was wahr oder falsch zurückgibt
;; Beispiel: even? wie in (even? 2)
(quote foo?)
;; Symbole mit Punkten vorne und hinten stehen für Host-Interop (später in der Veranstaltung)
(quote Foo.)
(quote .foo)

;; ' ist Reader-Syntax für quote
'blah!
(quote blah!)



;; Keywords fangen mit einem : an, sie stehen für sich selber
:foo
(type :foo)
;; Vielseitige Verwendung:
;; - Schlüssel in maps
;; - Keywords in APIs
;; - Wo man in Java Stringkonstanten benutzen würde

;; ---------- Regex

#"([0-9]{4})/([0-9]{2})/([0-9]{2})"
(type #"([0-9]{4})/([0-9]{2})/([0-9]{2})")

;; Literalsyntax für Javas Pattern
(re-seq #"A(.*?)B" "ACBAAABBBBBSSSSYYYAJHDHGHJ")

;; ---------- Kollektionen

;; Erzeugen von Listen
(list 1 :zwei 3)
'(1 2 3)
(type '(1 2 3))

;; Vektoren
[1 2 ,,3,,,4,,,]
(vector 1 2 3 4)
(type [str, 2, :foo, \space])

;; 0-indizierter Zugriff
(nth [1 2 3] 2)
;; conj: füge ein Element irgendwo in die Datenstruktur ein
(conj [1 2 3] 1)
(conj '(1 2 3) 1)
;; die Position ist abhängig von der Datenstruktur
;; bei Clojure Standard-Datenstrukturen fügt es dort ein, wo es effizient ist


;; Maps
{:name "Bendisposto", :vorname "Jens" :alter :uHu}
(hash-map :name "Körner" :vorname "Philipp" :alter :biVi)
(type {:key1 "foo", 2 9})

;; jeder Wert kann ein Key sein
{[1] \n, [] \l, :foo 1}

;; Key-Lookup funktioniert nach =
(get {[1] \n, [] \l} '())
(= '() [])


;; Gleiche Strukturen sind gleich, aber nicht unbedingt identisch
(= '(:a :b) [:a :b])
(identical? '(:a :b) [:a :b])


;; Key-Value Paare hinzufügen: assoc
(assoc {:foo 1} :bar 2)
(assoc {:foo 1} :bar 2 :baz 3 :boing 4)
;; Key entfernen: dissoc
(dissoc {:foo 1, :bar 2, :baz 3, :boing 4} :foo)

;{:kaboom 1 :kaboom 2}
;{:kaboom 1 :kaboom 1}

;; falls Key vorhanden, überschreibt assoc das Mapping
(assoc {:kaboom 1} :kaboom 2)


;; Sets
#{2 3 5 7 11}
(type #{str, 2, :foo, \space})
;; rein: conj
(conj #{1 2 3} 4)
;; raus: disj
(disj #{1 2 3} 2)


;; beliebiges Nesting
(def n [{:name "Bendisposto", :vorname "Jens" :alter :uHu :lottozahlen [1 3 5 15 21 44]}
        {:name "Witulski", :vorname "John" :alter :bivi}])

(get n 1)
(get (get n 1) :name)
;; oder auch: get-in und "Pfad" in der Datenstruktur
(get-in n [1 :name])

;; Maps und Vektoren sind Funktionen, die einen Key bekommen und den Key in sich selber nachschlagen
;; Bei Vektoren sind die Keys die Positionen
(n 1)
((n 1) :name)

;; Keywords sind Funktionen, die eine Map bekommen und sich selbst darin nachschlagen
(:name (n 0))


;; ---------- Call

(+ 1 2 3 4 5 6 7)
(+ (* 3 4) 11 112)

;; Frage: Was ist ein "vernünftiger" Wert für (*) und (+)
(*)
(+)

;; neutrale Elemente der Multiplikation / Addition

;; ---------- apply
;; + ist eine Funktion, die mehreren Argumente nimmt. Man kann aber keine Collection übergeben.
;; apply "entpackt" das letzte Argument
;; (apply + [1 3]) -> (+ 1 3)
;; (apply + 1 2 [3 4 5]) -> (+ 1 2 3 4 5)
(+ [1 2 3])
(apply + 5 6 7 [1 2 3])

(apply + [1 [2 2]])
;; -> (+ 1 [2 2]), explodiert also





;; ---------- Definition

;; pi gibt es noch nicht
pi
(def pi 3) ; Pi ist genau 3
pi

;; okay, etwas genauer
(def π 3.141592653589793238M)
(* 2 π)


;; Dinge neu zu definieren ist nicht gerne gesehen.

;; Warum?

;; in der REPL macht man das für Experimente
;; in Produktionscode hat man dann eine grauenhafte Episode Zustand...
;; Bei der Gelegenheit: Ihr Code soll nun nebenläufig arbeiten können. Viel Spaß!


(def fancy-calculation identity)


;; WARNUNG
;; Folgender Code wurde zu Demonstrationszwecken von Experten geschrieben.
;; Versuchen Sie das nicht zu Hause!

(defn my-terrible-idea [x]
  ;; do not try this at home
  (def y (fancy-calculation x))
  ;; mehr Code, der y verwendet (oder auch nicht)
  (println y)
  )

;; Ein def in einem anderen def ist das Äquivalent von goto.
;; Man *könnte* es machen, aber dann frisst einen ein Raptor (https://xkcd.com/292/)

;; wenn man lokale Variablen benötigt, verwendet man let 


;; ---------- lokale Bindings

;; das hier ist nicht definiert
whargbl

;; let bindet Symbole an Werte - aber nur innerhalb des Blocks!
(let [whargbl 42]
  whargbl)

;; gibt es immer noch nicht
whargbl

;; es gehen beliebig viele Paare an Symbolen und Werten
(let [a 3
      b 42]
  (* a b))

;; das innerste Binding zählt
(let [a 3
      a (+ a 1)] ;; das alte a wird nicht überschrieben! man kommt nur nicht mehr dran...
  (println :location1 a)
  (let [a 0]
    (println :location2 a)))

;; der letzte berechnete Wert wird zurückgegeben
(let [x :egal]
  1
  2
  3)

;; das ermöglicht Seiteneffekte, z.B. für Logging, Debugging, etc.
(let [x (+ 1 1)] ;; schwierige Berechnung
  (println :debug x)
  x)

;; auch möglich
(let [x (+ 1 1)
      _ (println x)]
  x)
;; _ hat keine besondere Semantik (wie in Prolog) und ist ein ganz normales Symbol!
;; Es ist nur Konvention, Symbole, die man nicht verwendet, _ zu nennen.



;; ---------- Immutability

(def v [1 2 3])
v
;; Alle Funktionen, die Kollektionen "modifizieren",
;; geben eine neue Kollektion zurück, die ursprüngliche
;; Version bleibt erhalten.
(concat v [4 5 6])
v


;; ---------- Funktionen

;; λx.x+3
;; (fn [argumentvektor] body)
;; letzte Form im Body, die ausgewertet wird, ist der Rückgabewert
(fn [x] (+ 3 x))

(def square (fn [x] (* x x)))

(def sum-square (fn [x y] (+ (square x) (square y))))

;; defn ist syntaktischer Zucker für (def (fn ...))
(defn sq2 [x] (* x x))
(sq2 15)

;; der Beweis
(macroexpand-1 '(defn sq2 [x] (* x x)))

;; falls man weiß, dass man ein Element garantiert bekommt, und vielleicht noch ein paar mehr:
(defn variadic-args-function [x & args]
  (println :x x :args args))

(variadic-args-function 1 2 3 4)

;; Funktionsauswertung
;; Mechanismus: beta reduction

;; Um eine Anwendung auszuwerten
;; 1) werte das erste Element aus um die Funktion zu erhalten
;; 2) werte die restlichen Elemente aus um die Argumente zu erhalten
;; 3) wende die Funktion auf die Argumente an
;;     - kopiere den Funktionskörper substituiere dabei die formalen
;;       parameter durch die Operanden
;;     - werte den resultierenden neuen Körper aus

;; Beispiel Summe der Quadratzahlen

(sum-square 3 4)

(+ (square 3) (square 4))
(clojure.core/+ (square 3) (square 4))
(+ (* 3 3) (square 4))
(+ 9 (square 4))
(+ 9 (* 4 4))
(+ 9 16)
25

;; ---------- Control structures

;; do fasst mehrere Sachen zu einem Ausdruck zusammen und gibt den letzten Wert zurück
(do (+ 1 2) :a "yo")

;; Formen, die einen "body" erwarten, z.B. fn, let, etc. erzeugen impizit einen do-Block
(fn [x] (println x) x)
;; ist das gleiche wie
(fn [x] (do (println x) x))


;; (if condition then-branch else-branch)
(if true 1 2)
(if false 1 2)

;; was den then-branch triggered wird als "truthy" bezeichnet,
;; was den else-branch triggered als "falsey".
;; falsey sind nil und false, alles andere ist truthy
(if 1 2 3)
(if :doh 2 3)
(if nil 1 2)

;; if ist etwas besonderes und keine Funktion!
(if true (println 1) (println 2))
;; eine Funktion würde beide Branches auswerten!

;; was ist, wenn ich bei erfolgreichem Test zwei Dinge machen möchte?
;; z.B. etwas auf die Konsole printen und einen Wert zurückgeben?
;; Antwort ist: do
(if (= 2 (+ 1 1)) ;; Bedingung
  (do (println :hallo) :mathe-ok)       ;; then-Branch
  :mathe-kaputt)  ;; else-Branch


;; cond-Macro: viele verschachtelte ifs
(cond
  (= 1 2) 1    ;; (= 1 2) erfolgreich? mit 1 aussteigen
  ( < 1 2) 2   ;; (< 1 2) erfolgreich? mit 2 aussteigen
  :else 3)     ;; :else ist immer truthy, d.h. wenn keine Bedingung erfüllt wird, geben wir 3 zurück

;; der "default" Test ist nicht festgelegt! :else ist hier als willkürlicher Wert, der immer truthy ist, verwendet worden
(cond
  (= 1 2) 1
  ( > 1 2) 2
  :otherwise 3)

;; oder
(cond
  (= 1 2) 1
  ( > 1 2) 2
  :true 3)
;; tun genau so!

;; if-not und cond sind syntaktischer Zucker

;; if ist etwas grundlegendes (eine special form)
(macroexpand-all '(if 1 2 3))

;; cond sind nur viele if in einem Trenchcoat
(macroexpand-all '(cond (= 1 2) 2
                        (= 2 2) 4))


;; if-not ist nur (if (not ...) ... ...)
(macroexpand-all  '(if-not false 1 2))



;; Exceptions
;; try mit catch und finally, Bedeutung wie in Java
(defn kehrwert [x]
  (try (/ 1 x)
       (catch Exception e :inf)
       (catch IllegalAccessError e :oh-no)
       (finally (println "done."))))

(kehrwert 2)
(kehrwert 0)

;; das Geräusch, was eine ungefangene Exception macht, angelehnt an ein Werkzeug der ETH Zürich
(defn rodäng!!! [] (throw (RuntimeException. "RodängDBException")))
(rodäng!!!)

;; das ist eigentlich ein Java-Konstruktor, der aufgerufen wird. Dazu später mehr.
(def e (RuntimeException. "RodängDBException"))

;; Superklassen der Exception
(supers (type e))




;; ------ Fakultätsfunktion
(defn ! [n]
  (if (= 1 n)
    1
    (* n (! (dec n)))))

(! 10)

(! 30)

;; * gibt einen Overflow, wenn das Ergebnis nicht mehr in den Long Typ passt.

(defn ! [n]
  (if (= 1 n)
    n
    (*' n (! (dec n))))) ;; Reperatur: verwende *' für auto promotion

(! 30)

(! 10000)

;; Rekursive Aufrufe produzieren Frames auf dem Stack und irgendwann sind es zu viele.
;; Ein Stackframe enthält lokale Variablen und wird gebraucht,
;; falls eine Funktion mit eigenen Bindings wieder zurückspringt.

;; range gibt einfach die Zahlen von einem Wert bis einem Wert (exklusive) zurück
(range 1 10)



;; eine mögliche Lösung verwendet eine higher-order Funktion, nämlich reduce:
;; reduce nimmt eine zweistellige Funktion, einen Startwert und eine Liste an Elementen
;; der Aufruf (reduce f a [x y z]) berechnet dann
;; (f (f (f (f a) x) y) z)
(defn ! [n]
  (reduce *' 0 (range 1 (inc n))))

;; reduce handelt einige Sonderfälle anders.
;; Übung: Was passiert bei einem Aufruf, wenn...
;; - der Startwert weggelassen wird?
;; - die Liste leer ist?
;; - beides der oberen eintritt?

;; time gibt den letzten berechneten Wert zurück und gibt die verstrichene Zeit stattdessen aus
;; wir geben :ok zurück, damit wir nicht messen wie lange es dauert, eine sehr große Zahl auszugeben...
(time  (let [x (! 10000)] :ok))



;; Funktionen als normale Parameter/Rückgabewerte zu sehen ist der Schlüssel zu funktionaler Programmierung
;; Es erfordert etwas Übung das Gehirn entsprechend zu trainieren!
;; Solche Funktionen bezeichnet man als Higher-Order Function (Funktion höherer Ordnung, HOF)

;; fn als param
(defn evaluate1 [f v] (f v))
(evaluate1 inc 12)

;; fn als wert
(defn mk-adder [n] (fn [x] (+ x n)))

;; quasi: (def foo (fn [x] (+ x 17)))
(def foo (mk-adder 17))
(foo 4)

;; Konstrukt, das manchmal halt passiert:
;; ((mk-adder 17) 3)
;; zwei Klammern am Anfang, weil eine Funktion zurückgegeben wird, die man aufruft




;; Listen

;; Nur die ersten 40 Elemente und 5 Verschachtelungsebenen von Datenstrukturen werden ausgegeben.
;; Das benutzt man in der REPL um nicht von der Ausgabe zugemüllt zu werden.
;; Auf beliebige Längen/Tiefen kommt man mit dem Wert nil.
;; Außerdem verhindert es, dass die Aufrufe nicht terminieren...
(set! *print-length* 40)
(set! *print-level* 5)

;; die Elemente von 0 (inklusive) bis 10 (exklusive)
(range 10)
;; von 10 (inklusive) bis 20 (exklusive)
(range 10 20)
;; von 1 bis 100 (exklusive) in 10er-Schritten
(range 1 100 10)

;; woah! unendliche Listen
(range) ;; alle natürlichen Zahlen
(nth (range) 4756) ;; 0-indiziert


;; Higher order Functions: map, filter, reduce
;; HOF sind aber nichts besonderes, da Funktionen nichts besonderes sind

;; map bekommt eine Funktion f und eine Sequenz von Werten x1 ... xn und berechnet die Sequenz f(x1) ... f(xn)
(map inc [2 3 4 5 6])
;; werden n Listen gegeben, werden Elemente von jeder Liste in die n-stellige Funktion gesteckt, bis (mindestens) eine Eingabeliste erschöpft ist
(map + [1 2 3] [3 4 5] [2 3])


;; man kann beliebige Funktionen verwenden
;; so produziert man beispielsweise alle geraden Zahlen
(map (fn [x] (* 2 x)) (range))



;; map ist lazy

;; der Seiteneffekt (der Print) wird aus didaktischen Gründen verwendet
(def squares
  (map (fn [x] (println :berechnet x) (* x x)) (range)))

;; nur bei der ersten Ausführung wird der Seiteneffekt gefeuert!
(first squares)
(nth squares 33)
;; alte Seiteneffekte tauchen nicht mehr auf
(nth squares 39)

;; (!!) es ist eine ganz dumme Idee, Seiteneffekte mit Laziness zu verbinden.
;; Es ist oft schwierig nachzuvollziehen, ob und wann Seiteneffekte gefeuert werden.

;; Beispiel:
(defn my-debug-print [input]
  (map println input)
  nil) ;; keine Rückgabe, die sinnvoll ist

;; hoppla!
(my-debug-print [1 2 3])

;; es gibt noch fiesere Fälle, wo Aufrufe auf der REPL die Seiteneffekte triggern,
;; aber im echten Programm dann nichts passiert...

;; lessons learned the hard way: kein map mit Seiteneffekt!
;; um Seiteneffekte unterzubringen, gibt es andere Konstrukte, z.B. doseq
;; damit werden garantiert alle Elemente und Seiteneffekte ausgewertet:

(do 
  (doseq [x [1 2 3]]
    (println x))
  nil)



;; filter bekommt eine Funktion p? und eine Sequenz x1..xn.
;; es wird eine Sequenz aller xi produziert, für die (p? xi) eine Wert liefert, der truthy ist
(filter (fn [x] (< 2 x 6)) [1 2 3 4 5 6])

;; Was macht remove?
(remove (fn [x] (< 2 x 6)) [1 2 3 4 5 6])


;; reduce, die Mutter aller HOFs (vgl. oben)

;; reduce bekommt eine Funktion f der Struktur (fn [akkumulator element] ...),
;; einen Start-wert a für den Akkumulator und eine Sequenz x1..xn.
;; Berechnet wird: (f ... (f (f a x1) x2) ... xn)

(reduce + 1 [2 3 4 5 6]) ;; (+  (+ (+ 1 2) 3) 4 ...)

;; (reduce reduce-funktion start-wert sequenz)

(reduce * (range 2 7))

(reduce + 0 (range 2 7))
(reduce + (range 2 7))

(reduce conj [] [1 2 3])
(reduce conj '() [1 2 3])




;; seq
;; eine kleine Sammlung an elementaren Funktionen, um mit Sequenzen umgehen zu können
(first [1 2 3])
(first (list 1 2 3))
(first #{2 1 3}) ;; je nach Konstruktion vom Set kommt ein anderes erstes Element raus
(rand-nth (seq #{2 3 4 5 6})) ;; Zufall

;; maps kriegt man in eine Sequenz gepresst - dann werden es Key-Value Tupel
(first {:b 2 :c 3 :a 1})
(first [])
(first nil)

(rest [1 2 3])
(rest (list 1 2 3))
(rest #{4 3 2 1})
;; rest verändert (oft) den Typen
(type  (rest [1 2 3]))
(type  (rest (list 1 2 3)))
(type  (rest #{4 3 2 1}))
(rest {:a 1 :b 2 :c 3})

;; was mal eine Map war muss nicht unbedingt wieder eine werden...
(conj (rest {:a 2}) [:1 1])

(rest [])
(rest nil)

;; cons baut ein Element vorne dran
(cons 1 [2])
(cons 2 3)

;; conj fügt da ein, wo es schnell geht
(conj [2 3] 1)
(conj (list 2 3) 1)

;; empty gibt die passende leere Collection je nach Typ zurück
(empty [1 2])

;; into fügt eine Sequenz in eine bestehende Collection ein
(into [1 2 3] (list 4 5 6))
(into #{} [4 5 6])

;; ein typerhaltendes Map kann also so aussehen:
(defn mm [ f v] (into (empty v) (map f v)))

(mm inc [1 2 3])
(mm inc '(1 2 3))
(mm inc #{1 2 4})

;; wenn der Typ wichtig ist, kann man auch explizit die passende Datenstruktur erzeugen
(vec '(1 2 3))
(set '(1 2 3))
(list* [1 2 3])

;; Vektoren sind übrigens nicht lazy!
(def x (time (into [:a :b] (range 30000000))))
(type x)
;; last hat linearen Zugriff
(time (last x))
(time (last x))

(count x)


;; warum ist das so? historische Gründe...
(doc last)
(source last)

;; unhandlich, aber so geht es bei Vektoren schnell:
(time (nth x (dec (count x))))

;; vielleicht wird das in einer späteren Clojure Version geändert...
;; man greift aber in der Regel selten explizit auf das letzte Element zu.
;; Die meisten Anwendungen verwenden den Head (das erste Element) und Rekursion
;; oder direkt higher-order Funktionen.


;; ein paar Millionen Elemente aus dem Vektor oben in eine Liste zu bauen dauert etwas
(def y (into '() x))

;; seq hingegen transformiert in konstanter Zeit!
(def y (time (seq x)))
(type y)
;; last ist aber dennoch langsam :-(
(time (last y))



;; maps
;; Schlüsselmenge
;; für Werte: vals
(keys {:a 1 :b 2 "foo" 3 nil 4})

;; zwei verschiedene Objekte mit denselben Informationen sind gleich!
(= {:a 1 :b 2 :c 3} (assoc {:a 1 :b 2} :c 3))

;; nachschlagen mit get (kann auch optional einen default-Wert nehmen)
(get {:a 1, :b 2} :b)
;; oder via Map oder Keyword als Funktion
({:a 1, :b 2} :b)
(:b {:a 1 :b 2})
;; auch mit default-Wert
(:c {:c 1} "default")
(:c {:a 123} "default")

;; for
;; for ist lazy und generiert eine Sequenz an Werten
(for [user ["Itchy" "Scratchy"]]
  (str user " for president!"))

;; verschiedene Collections werden verschachtelt
(let [xs [1 2 3]
	  ys [4 5 6]]
  (for [x xs
		y ys
		,,,]
	[x y]))
;; kann gelesen werden als
;; result = ()
;; for x in xs:
;;    for y in ys:
;;     ... ;; falls noch weitere Collections dazukommen
;;        result.addLast((x, y))
;; return result
 
;; alle Kombinationen von x und y
(for [x [1 2 3 4] y [1 2 3 4]] [:tuple x y])
;; hier ist :when echte Syntax:
;; Elemente werden nur aufgenommen, wenn sie eine Filterbedingung wahr machen
(for [x [1 2 3 4] y [1 2 3 4] :when (<= y x)] [:tuple x y])



;; Es folgen eine paar sehr nützliche Funktionen aus der Standardbibliothek.

;; (!!) wenn man sich denkt "diese Funktion auf einer Collection wäre voll nützlich", dann gibt es sie meist bereits!
;; https://clojure.org/api/cheatsheet
;; verlinkt auf: https://clojuredocs.org/ (mit Beispielaufrufen)
;; die Sektionen Sequences und Collections sind immer einen Blick wert beim Programmieren...

(reverse [1 2 3])

(concat [1 2 3] [4 5 6])

(count [1 1 1 1])

(interleave [1 1 1 1] [2 2 2 2] [3 3 3 3])
(interleave [1 1 1 1] [2 2])
(interpose "1" [2 2 2 2 2 2])

;; besonders nützlich als
(println (apply str (interpose \newline [1 2 3])))

(repeat 5 :a)

(take 4 (range))
(take 5 (repeat "a"))
(take 10 (repeat "a"))

(drop 5 (range 14))
(take-while neg? (range -10 10000))
(drop-while neg? (range -10 10))


(last [1 2 3])
(butlast [1 2 3])

(first [1 2 3])
(second [1 2 3])
(nth [1 2 3] 2)
(nth (range 1000) 33)

(range 13)
;; 3-er Päckchen der Sequenz erzeugen
(partition 3 (range 13))
;; mit Überlappung
(partition 3 2 (range 13))
;; Verhalten beeinflussen was passiert, wenn einem die Sequenz ausgeht
(partition 3 3 (repeat :a) (range 13))
(partition 3 3 [] (range 13))

(take 20 (cycle [1 2 3]))

;; Mengenoperationen
(use 'clojure.set)
(clojure.set/union #{1 2 3} #{2 3 4 5 6})
(clojure.set/difference #{1 2 3} #{2 3 4 5 6})
(clojure.set/intersection #{1 2 3} #{2 3 4 5 6})

;; Vorsicht: Argumente MUESSEN beides Mengen sein
;; sonst passieren ganz komische Dinge!
(clojure.set/intersection #{1 2 3} [3 4 5])






)
