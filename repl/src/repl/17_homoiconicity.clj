(ns repl.17-homoiconicity)

;; Precondition
(defn myfunction-please-provide-int [x]
  {:pre [(integer? x)]}
  (println "value ok:" x))

(myfunction-please-provide-int 4)
(myfunction-please-provide-int "foo")


;; Die Syntax von dem Precondition-Check ist eine Map.
;; Postconditions gehen mit :post und einer Funktion, die das Resultat der Funktion
;; bekommt und validiert.

;; In Clojure ist *alles* an Code genau in Datenstrukturen der Sprache ausgedrückt.
;; Solche Programmiersprachen bezeichnet man als "homoikonisch".
;; Ein anderes Beispiel für eine homoikonische Sprache ist Prolog.


;; ---------------------------------------------------------
;; REPL - Read - Eval - Print - Loop
;; ---------------------------------------------------------


;; Am Anfang war der Text
(read-string "(+ 1 2 3)")  ;; Text -> Reader -> Datenstruktur
(eval (read-string "(+ 1 2 3)")) ;; Datenstruktur (Liste) -> Eval -> Datenstruktur (Zahl)
(println (eval (read-string "(+ 1 2 3)"))) ;; Datenstruktur (Zahl) -> Printer -> Text

(eval (list + 1 2 3))

;; ---------------------------------------------------------
;; Quoting
;; ---------------------------------------------------------

;; "[1 2 a]" -> Reader -> Datastructure -> Eval -> Value -> Print
(read-string "[1 2 3]")
(eval [1 2 3])
;; a ist nicht definiert und kann daher nicht evaluiert werden
(eval (read-string "[1 2 a]"))
;; ein Quote verhindert eine Evaluation
(read-string "'[1 2 a]")
(eval (read-string "'[1 2 a]"))

(type (read-string "5"))


;; Die Homoikonizität erlaubt eine simple Implementierung einer REPL.
;; Zudem werden Macros ermöglicht:
;; Macros in Clojure sind keine Textersetzung (à la C-Präprozessor),
;; sondern source-to-source Transformationen,
;; die von Funktionen berechnet werden (Daten zu Daten).
;; Diese Funktionen können sehr einfach sein (wie if-not),
;; oder unendlich kompliziert (wie clojure.core.async/go).


;; Beispiele für Macros:

;; (defn ...) wird zu (def ... (fn ...))
(macroexpand '(defn foo [x] x))

;; -> schleift das Ergebnis der Expression in das erste Argument der nächsten Form mit
(-> 3
    (+  ,,, 5)
    inc ,,,
    (*  ,,, 2))
;; ist das gleiche wie
(* (inc (+ 3 5)) 2)

;; Beweis:
(macroexpand '(-> 3
                  (+ 5)
                  inc
                  (* 2)))

;; ->> schleift das Ergebnis der Expression in das letzte Argument der nächsten Form mit
(->> 3
     (- 5 ,,,)
     inc ,,,
     (* 2 ,,,))
;; wird also zu
(* 2 (inc (- 5 3)))

;; as-> gibt dem Ergebnis einen Namen, der in jeder weiteren Form an das Ergebnis vorher gebunden wird
(as-> 3 expr
    (+ expr 5)
    (inc expr)
    (* 2 expr))

(macroexpand '(as-> 3 expr
                (+ expr 5)
                (inc expr)
                (* 2 expr)))

;; for ist ein kompliziertes Macro
(macroexpand '(for [x [1 2 3]] x))


;; Macros erlauben uns, angenehmeren Code zu schreiben (also (quasi) eine DSL),
;; indem wir angeben, wie der Code in "echten" Clojure Code übersetzt wird.
