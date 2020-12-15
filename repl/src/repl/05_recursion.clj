(ns repl.05-recursion
  (:use [clojure.tools.trace]))

;; benötigt tools.trace in der project.clj
;; heute new:
;; debugging Library: clojure.tools.trace: deftrace
;; clojure.core: recur, loop

(comment

  ;; deftrace ist ein defn, was noch Debug-Informationen zu den Aufrufen ausgibt
  (deftrace ! [n]
    (if (= 1 n)
      n
      (*' n (! (dec n)))))

  ;; man sieht, wie sich die Stackframes aufbauen
  (! 8)

  ;; aus Prolog (und einigen anderen Sprachen) kennen wir vielleicht:
  ;; ist der letzte Aufruf in einer Funktion wieder dieselbe Funktion,
  ;; dann muss kein neuer Stackframe erzeugt werden, sondern der alte kann wiederwendet werden.
  ;; dies nennt man Tail-Call Optimisation (TCO)

  ;; Version mit Akkumulator, der das bisherige Produkt speichert. Jetzt ist es tail recursive:
  (deftrace !
    ([n] (! n 1))
    ([n a] (if (= 1 n)
             a
             (! (dec n) (*' a n)))))

  ;; immer noch stackframes
  (! 8)

  ;; Clojure hat keine automatische TCO.
  ;; Die JVM bietet keine sinnvolle Möglichkeit dafür.

  ;; Rich Hickey hat sich dagegen entschieden, dass manchmal magisch eine Optimierung greift und manchmal nicht.
  ;; Deshalb muss man explizit recur dranschreiben.

  (deftrace !
    ([n] (! n 1))
    ([n a] (if (= 0 n)
             a
             (recur (dec n) (*' a n)))))

  (! 8)

  ;; Version ohne trace!
  ;; fn / defn erzeugt eine Rücksprungmarke:
  ;; wenn die Anzahl der Argumente an recur passt, wird die Funktion also erneut aufgerufen.
  (defn !
    ([n] (! n 1))
    ([n a] (if (= 0 n)
             a
             (recur (dec n) (*' a n)))))

  (! 10000)

  ;; recur geht nicht überall:
  ;; es MUSS der letzte Aufruf sein, der gemacht wird, sonst wird der Compiler traurig
  (defn ! [n]
    (if (= 1 n)
      n
      (*' n (recur (dec n)))))

  ;; loop setzt eine weitere Rücksprungmarke für recur, die anstelle der Funktion verwendet wird:
  ;; recur setzt dann die Bindings von den in loop definierten Symbolen um
  (defn ! [n]
    (loop [n n
           a 1]
      (if (= 0 n)
        a
        (recur (dec n) (*' a n)))))






  ;; mutual recursion


  ;; alle Symbole müssen à la C definiert sein, bevor sie verwendet werden
  ;; Vorwärtsdeklaration macht declare:
  (declare my-odd? my-even?)
  (macroexpand-1 '(declare my-odd? my-even?))

  ;; so kriegt man Funktionen hin, die voneinander abhängen
  ;; 0 ist gerade.
  ;; 1 ist nicht gerade.
  ;; Eine Zahl n ist gerade, wenn n-1 ungerde ist.
  (defn my-even? [n]
    (cond (= 0 n)  true
          (= 1 n)  false
          :otherwise (my-odd? (dec n))))

  ;; analoge Definition von ungerade.
  (defn my-odd? [n]
    (cond (= 0 n) false
          (= 1 n) true
          :otherwise (my-even? (dec n))))

  ;; das funktioniert auch...
  (my-even? 6)
  (my-even? 5)
  (my-odd? 5)
  (my-odd? 6)

  ;; ...nur nicht, wenn die Zahl groß wird
  (my-even? 100000)


  ;; recur geht nicht, weil wir eine andere Funktion aufrufen wollen!
 
  ;; die Lösung ist trampoline:
  ;; trampoline nimmt einen Aufruf entgegen, den es auswertet.
  ;; ist die Rückgabe keine Funktion, wird diese zurückgegeben.
  ;; ist die eine Rückgabe eine Funktion, wird diese ohne Parameter aufgerufen (und so weiter, und so weiter)

  ;; also geben wir eine Funktion zurück, statt die gegenseitige Rekursion zu triggern:
  (defn my-even? [n]
    (cond (= 0 n)  true
          (= 1 n)  false
          :otherwise (fn [] (my-odd? (dec n)))))

  ;; hier auch...
  (defn my-odd? [n]
    (cond (= 1 n)  true
          (= 0 n)  false
          :otherwise (fn [] (my-even? (dec n)))))

  
  ;; und der Aufruf geht via trampoline
  (trampoline (my-even? 5))
  (trampoline (my-even? 100000))
  (trampoline (my-even? 100000111)) ;; dauert etwas


  ;; Achtung! Was ist, wenn das berechnete Ergebnis der gegenseitigen Rekursion selbst eine Funktion ist?

  ;; Dann muss man die in eine Datenstruktur (Liste, Vektor) einpacken, damit sie nicht aufgerufen wird,
  ;; und außen wieder auspacken.

)
