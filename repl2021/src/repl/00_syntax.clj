(ns repl.00-syntax) ;; Namespace macro - is going to be examined in greater detail later on

; (Single line-)comments are preceded by ';'

;; Note: In case an editor-plugin cannot evaluate a literal: wrap it in a do
(do 3)


;; Numbers
3
; Integral numbers are longs by default
(type 3)
; unless the value does not fit into a long (Note the 'N' at the end of the number)
;; Literals are automatically of the suitable type
12345678901234567890
(type 12345678901234567890)
; ...N is the literal that forces a BigInt
3N

;; Other bases
;; hexadecimal
0x33
;; octal
012
;; binary
2r1011
;; base 7
7r666
;; base 36
36rCrazy
;; Literalsyntax exist for all bases <= 36
;; Why 36? Afterwards we run out of letters of the alphabet...



3.141529
;; Floating-point number literals are doubles
(type 3.141529)
3.14159265359
;; Floating-point number literals are not automatically of the suitable type
3.141592653589793238
;; More precision can be attained from a BigDecimal (-M suffix)
3.141592653589793238M
(type 3.141592653589793238M)


;; Fractions are literals with integers left and right of a '/'
1/4
;; and are fully reduced
4/8
;; syntactical nonsense
;; (+ 1 2)/3 or 3.14/2 or 3N/42
;; Fractions permit large numbers
438750928436573298756238497653487563289523/43657493825642387956432987562349875643
;; Warning: Calculations with fractions can be slow as they represent numbers exactly by numerator and denominator.
;; They are reduced, but can stil produce large numerator and denominator values after many steps.
;; Often, doubles are precise enough and significantly faster.



;; Strings, characters
"foo"
;; Clojure strings are Java strings
(type "foo")
;; Multi-Line, too
"foo
bar"
;; or with good ol' escaping
"foo\nbar"

;; The char 'x' is obtained with backslash preceding the character
\x
;; Characters are Java characters
(type \f)
;; Some special cases you have to get used to
\space
\newline ;; \n is the char 'n'
;; Unicode characters
\u03bb




;; Keywords
;; Simple keyword
:hi
;; Two double colons: keyword in a namespace (unique identifier in a namespace)
::i-am-namespaced
;; syntactical nonsense: Three double colons
; :::test

;; Symbols
;; Symbol + stands for a function object
+
(comment kaboom!) ;; not defined, thus an exception
;; with a preceding quote, symbols are not resolved
'kaboom?

;; Lists
;; with parentheses
;; empty list:
()
;; Lists with elements must be quoted to differentiate them from function calls (otherwise they are evaluated)
'(1 2 3)
;; Careful: When quoting via ' nothing within an expression is evaluated!
'(1 2 (+ 1 2))

;; Vectors
;; brackets
[] ;; empty
[1 2 3] ;; No quote is needed

;; Maps
;; Braces, associative container
{:key :value, 1 2, true [1], '(1) false}
;; Commas are whitespace by the way and are only used for readability
{:key :value 1 2,,,,,,, true [1] '(1) false}
;; syntactical nonsense: mapping to values from the same key twice
;; 
; {:a :b, :a :c}


;; Sets
;; Hash (Number sign, Pound sign, Hashtag) followed by braces
#{1 2 3}
;; syntactical nonsense: Sets with duplicates
; #{1 1 1}

;; null in Java is named nil here
nil


;; Function calls are lists!
;; The first element is the function, the rest are the arguments
(identity :x)
(inc 1)
(range 1 5)

