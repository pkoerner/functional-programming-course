(ns repl.09-evaluation-order)


  ;; Evaluation von Symbolen
  ;; ------------------------------------------------------------------------------------
(comment
  ;; Literale werden direkt ausgewertet
  1
  :a
  ;; in Datenstrukturliteralen werden die Elemente von links nach rechts ausgewertet
  [:a 1]
  {:a 1}
  [(println 1) (println 2)]
  ;; Funktionsaufrufe: erst die Expression, die die Funktion zurückgibt, dann die des ersten Arguments (und rekursiv dasselbe Schema), das zweite Argument, usw.
  (+ 1 2)

  ;; manche Sachen können nicht ausgewertet werden
  ding ;; error

  ;; es sei denn, es ist definiert
  (def ding 1)
  ding

  ;; definieren wir mal eine Funktion.
  ;; die Rückgabe ist ein komisches #'repl.09-evaluation-order/foo Ding
  (defn foo [] 42)
  ;; das ergibt ein Funktionsobjekt
  foo

  ;; steht das Symbol foo also für eine Funktion? anscheinend...
  (supers (type foo))

  ;; wir können die problemlos aufrufen
  (foo)

  ;; so bekommt man wieder das komische #'-Ding
  (var foo)

  ;; oder wir schreiben es direkt hin
  #'foo
  ;; dieses #'-Ding ist Callable (deshalb funktioniert der Funktionsaufruf), aber ist hauptsächlich eine Referenz
  (supers (type #'foo))

  ;; das #'-Ding ist eine Var.
  ;; Das kann man sich als Box vorstellen, die auf den Wert, der dahinter steht, zeigt.

  ;; Das Symbol foo wird im Namespace auf die Var abgebildet, das auf die definierte Funktion zeigt.


  ;; wenn wir die Var aufrufen, wird die Funktion dahinter ausgewertet
  ((var foo))

  ;; die beiden folgenden Formen sind äquivalent
  ;; @ verhält sich zu deref wie ' zu quote
  @#'foo
  (deref (var foo))
  ;; Wenn wir also die Var derefenzieren, bekommen wir das Ding, wo es hinzeigt.

  ;; das Funktionsobjekt kann man auch direkt aufrufen
  (@#'foo)

  ;; äquivalentes Ding ohne syntaktischen Zucker
  ((deref (var foo)))


  ;; also nochmal:

  ;; Namespace:
  ;; Symbol foo -> ein var

  ;; Var:
  ;; Var -> Das Funktionsobjekt


  ;; was passiert mit mehreren Vars (Aliasing)?
  (def bar1 foo)

  ;; bar1 steht auch für die Funktion - weil foo direkt aufgelöst wird und durch den Wert ersetzt wird
  bar1

  ;; das kann man aufrufen
  (bar1)

  (var bar1)  ;; #'bar1
  (var foo)   ;; #'foo
  ;; es sind verschiedene Vars...

  ;; die für dasselbe Ding stehen
  (identical? foo bar1)
  ;; aber nicht dasselbe Objekt sind!
  (identical? (var foo) (var bar1))

  ;; was passiert, wenn meine Var auf eine andere Var zeigt?
  (def bar2 #'foo)
  bar2

  (def bar3 #'bar2)
  bar3
  ;; wir haben also:
  ;; bar3 ---> bar2 ---> foo --> fn

  ;; die ausgewertete Expression bar3 ergibt die Var bar2
  (eval bar3)
  (type (eval bar3))

  ;; wenn wir das noch einmal dereferenzieren (dem Pointer folgen), kommen wir raus bei...
  @bar3
  ;; und zweimal dereferenziert...
  @@bar3
  bar3

  ;; Preisfrage: was passiert, wenn wir die Vars, die auf eine Var zeigt, die auf noch eine Var zeigt, die auf eine Funktion zeigt, aufrufen?
  (bar3)
  ;; ...die Varkette wird verfolgt und die Funktion aufgerufen

  ;; und was, wenn wir die Kette jetzt umbiegen?
  (def foo (var bar3))
  (bar3)
  ;; Willkommen in der Endlosschleife der Dereferenzierung!
  ;; wir haben wirklich den Kreis
  ;; bar3 --> bar2 --> foo
  ;;  ^                 v
  ;;   <----------------
  (-> bar3 deref deref)

  ;; Lessons learned: Vars verwendet man nicht wirklich.
  ;; Die einzige sinnvollen use cases sind konstante Definitionen!
  ;; def, defn verwendet man nur auf der obersten Ebene des Programms.

  ;; Wer vars anders benutzt wird mit Fragen in der Prüfung dazu nicht unter 5 Stück bestraft!





  ;; Evaluationsreihenfolge bei Symbolen
  ;; ------------------------------------------------------------------------------------

  ;; 1) vollständig qualifizierte Symbole, also mit Namespace, schlagen alles
  ;; java.io.Writer ist eine Javaklasse
  (def java.io.Writer 101)
  ;; vollständig qualifiziertes Symbol ergibt den definierten Wert
  repl.09-evaluation-order/java.io.Writer
  ;; ohne Qualifizierung gewinnt die Javaklasse
  java.io.Writer

  ;; 2) Javaklassen können nicht in Bindings verwendet werden
  ;; und haben Priorität gegenüber normalen Symbolen
  (let [java.io.Writer 12] java.io.Writer) ;; error


  ;; 3) lokale Variablen haben Vorrang vor globalen Variablen
  ;; je weiter innen sie stehen, desto stärker sind sie
  (def foo 10)
  foo
  (let [foo 11] foo)
  ;; und in Kombination mit dem vollständig qualifizierten Symbol
  (let [foo 11] (+ foo repl.09-evaluation-order/foo))


  ;; 4) kommt das globale Symbol dran
  foo

  ;; 5) wenn dann nichts gefunden wird, gibt es einen Error
  sdfjks



  ;; besondere Ausnahme: special forms sind... speziell
  ;; special forms sind die Grundlagen von Clojure. Davon gibt es nur sehr wenige
  ;; (je nach dem, wie man zählt, gibt es 13 Stück. Dann kommen noch welche für Java Interop hinzu.
  ;; Beispiele sind def, fn, if, do, let und loop/recur. var und quote sind auch welche.
  (def if +)
  if
  ;; ist jetzt mein if weg?

  ;; if ist jetzt irgendwie +
  (apply if [1 2 3])

  ;; und irgendwie nicht
  (if 1 2 3)

  ;; special forms gewinnen vor Namespace Symbolen,
  ;; FALLS sie dasjenige sind, was aufgerufen wird
  ;; das macht der Compiler für uns...

  ;; anderes Quote: der Syntaxquote `
  ;; der wird uns später noch einmal begegnen.
  ;; für jetzt müssen wir wissen: er quotet nicht nur das Symbol, sondern fügt den Namespace mit dran
  `foo

  ;; special forms werden vom Syntaxquote nicht mit einem Namespace versehen!
  `do
  `if


  ;; lessons learned für heute:
  ;; - def(n) ist für Konstanten und Funktionen.
  ;; - special Forms sollte man nicht versuchen zu überschreiben.
  ;; - wann wird ein Symbol wie aufgelöst?

  ;; - wer wider aller Warnungen doch Quatsch macht, darf in der Klausur so etwas ausrechnen:

  (def if false)
  (def do +)
  (def quote 3)
  (def throw 4)
  (def try 5)
  (def catch 6)
  (def var 7)

  (let [if true
        do -
        quote 9
        throw 10
        try 11
        catch 12]
    (if
      `if
      ((do (deref (var do))) ((do do) (try try catch throw)) (eval (quote quote)) quote)
      ((do do) ((do do) catch var (deref (var var)) (eval (quote quote))) quote)))


)
