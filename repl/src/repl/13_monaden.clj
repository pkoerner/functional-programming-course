(ns repl.13-monaden
  (:use [clojure.algo.monads]
        [repl.19-wiederholung :refer [unscharf half]]
        [clojure.walk :refer [macroexpand-all]]))

(comment


  ;; Streng genommen besteht eine Monade aus einen Typkonstruktor, der
  ;; return Funktion und einer Funktion zur Komposition (bind ist nur
  ;; eine Möglichkeit).

  ;; Der Typkonstruktor spielt in Clojure als dynamischer Sprache quasi
  ;; keine Rolle.


  ;; Typen (nur angelehnt an Haskell, kein echtes Haskell):

  ;; inc           : Long -> Long
  ;; half          : Long -> Long | nil
  ;; unscharf      : Long -> [Long]

  ;; Die Funktionen nehmen einen Wert und liefern einen
  ;; Wert mit Kontext. Ein Wert mit Kontext heisst monadischer Wert.
  ;; Funktionen, die aus einem normalen Wert einen monadischen Wert
  ;; machen heissen monadische Funktionen.

  ;; mf : a -> m b

  ;; Kontext kann zum Beispiel sein, das die Berechnung fehlgeschlagen
  ;; ist oder nicht-deterministisch war.


  ;; return: Funktion, die einen normalen Wert als
  ;; Parameter bekommt und einen monadischen Wert zurückgibt
  ;; return : a -> m b
  ;; -> return ist eine monadische Funktion

  ;; bind: Funktion die einen monadischen Wert und eine monadische
  ;; Funktion als Parameter bekommt und einen monadischen Wert zurückgibt.
  ;; bind : m a -> (a -> m b) -> m b



  ;; Es gibt verschiedene valide Kombinationen von bind und return,
  ;; die unterschiedliches Verhalten erzeugen.

  ;; bind und return müssen folgende Regeln erfüllen,
  ;; um mit uberlet zu funktionieren:

  ;; Rechts/Links Identität
  ;; (>>= (return a) f) =  (f a)
  ;; (>>= m return) =  m

  ;; Assoziativität
  ;; (>>= (>>= m f) g) = (>>= m (fn [x] (>>= (f x) g)))


  ;; Ab hier verwenden wir die Bibliothek algo.monades

  ;; uberlet heisst domonad
  ;; bind heisst m-bind
  ;; return heisst m-result

  ;; triviale Monade / Identitätsmonade (let)
  (domonad identity-m [a 3
                       b (+ a 4)
                       c (* a b)] c)

  ;; Sequence Monad/ Listmonad (for)
  (domonad sequence-m [a (unscharf 10)
                       b (unscharf a)
                       c (unscharf b)] c)





  ;; Maybe Monad
  (domonad maybe-m [a 20
                    b (half a)
                    c (half b)] c)

  (domonad maybe-m [a 10
                    b (half a)
                    c (half b)] c)


  ;; Wenn es identity-m nicht gäbe:

  (defmonad trivial-m
    [m-result identity
     m-bind (fn [mv f] (f mv))])

  (domonad trivial-m [a 3
                      b (+ a 4)
                      c (* a b)] c)


  ;; Der erste Parameter ist eine Map mit bind und return
  trivial-m

  (macroexpand-all '(defmonad trivial-m
                      [m-result identity
                       m-bind (fn [mv f] (f mv))]))





  ;; do you even lift?

  (+ 5 nil)
  (def +m (with-monad maybe-m (m-lift 2 +)))
  (+m 2 4)
  (+m 5 nil)

  (+ [1 5] [2 4 5])
  (def +s (with-monad sequence-m (m-lift 2 +)))
  (+s [1 2] [1 2 3])

  (def +s2 (with-monad sequence-m (m-lift 1 inc)))
  (+s2 [1 2 3])



  ;; also: Monaden sind überall


  ;; let kriegt man als Monade hin
  ;; for kriegt man als Monade hin


  ;; Generatoren (aus test.check) sind nur Monaden

  ;; bind und return hießen da sogar so!

  ;; bind: gen a -> (a -> gen b) -> gen b
  ;; return: a -> gen a



  ;; unter den Codeanalysen in core.async für go-Blöcke
  ;; liegt die State-Monade


  ;; Ziel: stateful calculations
  ;;       in einem puren Kontext
  ;;       ausführen


  ;; State Monade im Beispiel:

  ;; Es soll ein Interpreter für eine (sehr einfache) Sprache
  ;; geschrieben werden:

  ;; x = 4
  ;; y = x++ + x

  ;; Ein Parser soll dafür schon existieren und folgende Datenstruktur
  ;; generieren

  (def input '($do ($assign :x ($int 4))
                   ($assign :y
                            ($add ($postinc :x)
                                  ($id :x)))))



  ;; Aufgabe: Schreibe einen Interpreter für die Sprache
  ;; Das Ergebnis des Programmaufrufs soll 9 sein!
  ;; Der Typ jeder Variablen ist Long. Der Default-Wert einer nicht
  ;; gesetzten Variable ist 0.


  ;; Zeit: 10 Minuten














  (def state (ref {}))
  (def $int identity)
  (defn $id [n] (dosync (get @state n 0)))
  (def $add +)
  (defn $postinc [n]
    (dosync (let [v (get @state n 0)]
              (alter state (fn [s] (assoc s n (inc v))))
              v)))
  (defn $assign [n v] (dosync (alter state (fn [s] (assoc s n v))) nil))
  (defn $do [a b] b)

  (eval input)
  @state



  ;; Wie kann man das testen?

  ;; meh!

  (defn test1 []
    (dosync (let [s @state]
              (alter state (constantly {:x 1}))
              ($postinc :x)
              (assert (= 2 (get @state :x)))
              (alter state (constantly s)))))

  (do (test1) @state)

  ;; Es wäre besser, wenn die Funktionen den State explizit
  ;; als Eingabe bekommen würden.
  ;; Wenn der State verändert wird, muss der neue State auch
  ;; explizit mit zurückgegeben werden.
  ;; Die Rückgabe wird dann [<Wert> <neuer State>]

  (defn $postinc' [env n] (let [v (get env n)] [v (assoc env n (inc v))]))

  (defn test2 [x]
    (= ($postinc' {:x x} :x) [x {:x (inc x)}]))

  (test2 1)
  (test2 2)


  ;; Von Hand wird das ziemlich aufwändig! Insbesondere müssen wir den
  ;; Input ändern

  ;; state-m to the rescue


  ;; Umbenennung nur zur Klarheit hier. Es wäre auch mit dem Original gegangen!
  (def m-input '(m-assign :y
                          (m-add (m-postinc :x)
                                 (m-id :x))))


  ;; == Monadischer Wert der State Monade:

  ;; Ist eine Funktion, die einen Zustand nimmt und ein Tupel aus
  ;; einem Wert und einem Folgezustand zurückgibt


  ;; return sieht so aus: (fn [v] (fn [env] [v env]))
  (defn state-return [v]
    (fn [env] [v env]))

  (with-monad state-m
    ;; Anmerkung: in der neueren Version von clojure.algo.monads
    ;; geht das irgendwie nicht.
    ;; Anhand der Commits sehe ich nicht warum?
    ;; TODO: future Jens soll das fixen. (pk, 24.01.18)


    (def m-int m-result) ;; state-return
    (def m-id fetch-val) ;; (fn [v] (fn [env] [(get env v) env]))
    (def m-add (m-lift 2 +)) ;; (fn [a b] (fn [env] [(+ a b) env]))
    (defn m-postinc [k]
      (domonad
       [x (fetch-val k)
        y (set-val k (inc x))] y))

    ;; (fn [k]
    ;;   (fn [env]
    ;;     (let [x (get env k)
    ;;           x' (inc x)] [x' (assoc env k x')])))

    (defn m-assign [n mv]
      (domonad [v mv
                r (set-val n v)] r)) ;; Übung !

    (defn m-do [a b] (domonad [x a y b] nil))

  )


  ((m-id :x) {:x 3})
  ((m-postinc :x)  {:x 1})
  (def compiled (eval m-input))

  (compiled {:x 6 :y 19 :a 0})

  (defn swap [a b]
    (m-do
     (m-do (m-assign :t (m-id a))
           (m-assign a (m-id b)))
     (m-assign b (m-id :t))))

  ((swap :x :y) {:x 1 :y 3 :z 5})


  ;; bind der Statemonade:

  ;; ein einfaches Beispiel: x++

  (defn x++ []
    (fn [{x :x :as e}]
      (let [x' (inc x)
            e' (assoc e :x x')]
        [x' e'])))


  ;; x++ ist eine monadische Funktion
  ;; (x++) ist ein monadischer Wert

  ((x++) {:x 4})


  ;; bind ... finally

  (defn state-bind [mv f]
    (fn [env]
      (let [ [v ss] (mv env)
            next-mv (f v)]
        (next-mv ss))))

  ((domonad state-m [r (x++) t (x++)] [r t]) {:x 4})

  ;; domonad desugaring

  ((domonad state-m [r (x++) t (x++)] [r t]) {:x 4})

  ((state-bind (x++) (fn [r]
                       (state-bind (x++) (fn [t]
                                           (state-return [r t]))))) {:x 4})

  ((state-bind (x++) (fn [r]
                       (state-bind (x++) (fn [t]
                                           (fn [env] [[r t] env]))))) {:x 4})

  ((state-bind (x++) (fn [r]
                       (fn [e1] (let [[v ss] ((x++) e1)
                                     nmv ((fn [t] (fn [env] [[r t] env])) v)]
                                 (nmv ss))))) {:x 4})


  ((fn [e0] (let [[v0 ss0] ((x++) e0)
                 nmv0 ((fn [r]
                         (fn [e1] (let [[v ss] ((x++) e1)
                                       nmv ((fn [t] (fn [env] [[r t] env])) v)]
                                   (nmv ss)))) v0)]
             (nmv0 ss0))) {:x 4})





  ;; =============================================================
  ;; Monaden Transformer

  ;; Hatten wir schon:
  (def +m (with-monad maybe-m (m-lift 2 +)))
  (def +s (with-monad sequence-m (m-lift 2 +)))



  (+s [1 2] [1 2 3])

  ;; Was passiert wenn nil auftaucht?
  (+s [1 2 4] nil)
  (+s [1 2] [1 nil 3])

  (def sequence-maybe-m (sequence-t maybe-m))
  (def +sm (with-monad sequence-maybe-m (m-lift 2 +)))
  (+sm [1 2 4] nil)
  (+sm [1 2] [1 nil 3])

  (def maybe-sequence-m (maybe-t sequence-m))
  (def +ms (with-monad maybe-sequence-m (m-lift 2 +)))
  (+ms [1 2] [1 nil 3])
  (+ms [1 2 4] nil)

  (def maybe-sequence-maybe-m (-> maybe-m sequence-t maybe-t))
  (def +msm (with-monad maybe-sequence-maybe-m (m-lift 2 +)))

  (+msm [1 2] [1 nil 3])
  (+msm [1 2] nil)

  ;; Whut ???
  (def sequence-sequence-m (sequence-t sequence-m))
  (def +ss (with-monad sequence-sequence-m (m-lift 2 +)))
  (+ss [[1 2] [3 4]] [[-1 0 1]])

  ;; Maybe?
  (def maybe-sequence-sequence-m (-> sequence-m sequence-t maybe-t))

  ;; etc...

  ;; Aus einem von Jens' Projekten (BLA):
  (def wd-state-m (maybe-t state-m))

)
