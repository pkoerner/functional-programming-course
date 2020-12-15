(ns repl.00-syntax) ;; Namespace Macro - wird später genauer untersucht

; (Zeilen-)Kommentare werden von ; eingeleitet

;; Hinweis: falls das Editor-Plugin ein Literal alleine nicht auswerten möchte: in do verpacken
(do 3)


;; Zahlen
3
; ganze Zahlen sind standardmäßig Logs
(type 3)
; außer es passt nicht (siehe N am Ende)
;; Literale werden automatisch in einen passenden Typ gelesen
12345678901234567890
(type 12345678901234567890)
; ...N ist das Literal, das einen BigInt erzwingt
3N

;; andere Basen
;; hexadezimal
0x33
;; oktal
012
;; binär
2r1011
;; Basis 7
7r666
;; Basis 36
36rCrazy
;; Literalsyntax für alle Basen <= 36
;; Warum 36? danach gehen einem die Buchstaben aus...



3.141529
;; Gleitkommazahlen sind Doubles
(type 3.141529)
3.14159265359
;; Fließkommazahlen werden nicht in einen passenden Typ gelesen
3.141592653589793238
;; präziser wird es mit einem BigDecimal (-M Suffix)
3.141592653589793238M
(type 3.141592653589793238M)


;; Brüche sind Literale mit Integerwert links und rechts vom /
1/4
;; und werden vollständig gekürzt
4/8
;; syntaktischer Murks:
;; (+ 1 2)/3 oder 3.14/2 oder 3N/42
;; erlaubt große Zahlen:
438750928436573298756238497653487563289523/43657493825642387956432987562349875643
;; Warnung: Rechnen mit Brüchen kann sehr langsam werden, da sie Zahlen exakt über Zähler und Nenner repräsentieren.
;; Sie werden zwar gekürzt, aber können nach vielen Schritten sehr große Zähler und Nenner produzieren.
;; Oft sind Doubles präzise genug und sind signifikant schneller



;; Strings, Character
"foo"
;; Clojure Strings sind Java Strings
(type "foo")
;; auch in Multi-Line
"foo
bar"
;; oder mit gutem, altem Escaping drin
"foo\nbar"

;; Character 'x' mit Backslash vorm Zeichen
\x
;; Character sind Java Character
(type \f)
;; Spezialfälle, an die man sich gewöhnen muss
\space
\newline ;; \n ist schon das Zeichen 'n'
;; Unicode Characters
\u03bb




;; Keywords
;; einfaches Keyword
:hi
;; zwei Doppelpunkte: Keyword in einem Namespace (eindeutiger Identifier im Projekt)
::ich-bin-genamespaced
;; synaktischer Murks: drei Doppelpunkte
; :::test

;; Symbole
;; Symbol + steht für ein Funktionsobjekt
+
(comment kaboom!) ;; nicht definiert, also Exception
;; mit einem Quote werden Symbole nicht aufgelöst
'kaboom?

;; Listen
;; mit runde Klammern
;; leere Liste:
()
;; Liste mit Elementen müssen gequoted werden, um sie von Funktionsaufrufen zu unterscheiden (sonst wird es ausgewertet)
'(1 2 3)
;; Vorsicht: mit einem ' wird innen nichts mehr ausgewertet!
'(1 2 (+ 1 2))

;; Vektoren
;; eckige Klammern
[] ;; leer
[1 2 3] ;; darf man ohne Quote hinschreiben

;; Maps
;; geschwungene Klammern assoziativer Container
{:key :value, 1 2, true [1], '(1) false}
;; Kommas sind übrigens Whitespace, werden nur zur Lesbarkeit eingefügt
{:key :value 1 2,,,,,,, true [1] '(1) false}
;; syntaktischer Murks: zweimal vom selben Schlüssel auf etwas abzubilden
; {:a :b, :a :c}


;; Sets
;; Hashtag mit geschwungenen Klammern
#{1 2 3}
;; synaktischer Murks: Sets mit Duplikaten
; #{1 1 1}

;; null in Java heißt hier nil
nil


;; Funktionsaufrufe sind Listen!
;; das erste Element ist die Funktion, der Rest die Argumente
(identity :x)
(inc 1)
(range 1 5)

