(ns repl.17-homoiconicity)

;; Precondition
(defn myfunction-please-provide-int [x]
  {:pre [(integer? x)]}
  (println "value ok:" x))

(myfunction-please-provide-int 4)
(myfunction-please-provide-int "foo")


;; Syntactically, the precondition is expressed as part of a map.
;; Postconditions can be added via :post and a function that takes the result
;; as an argument and validates it.

;; All code in Clojure is expressed in data structures of the language (often dubbed as "code is data (is code)").
;; Such programming languages are called "homoiconic".
;; Another homoiconic language is Prolog.

;; ---------------------------------------------------------
;; REPL - Read - Eval - Print - Loop
;; ---------------------------------------------------------


;; In the beginning, there was text
(read-string "(+ 1 2 3)")  ;; text -> reader -> data structure
(eval (read-string "(+ 1 2 3)")) ;; data structure (list) -> eval -> data structure (number)
(println (eval (read-string "(+ 1 2 3)"))) ;; data structure (number) -> printer -> text

(eval (list + 1 2 3))

;; ---------------------------------------------------------
;; Quoting
;; ---------------------------------------------------------

;; "[1 2 a]" -> Reader -> Datastructure -> Eval -> Value -> Print
(read-string "[1 2 3]")
(eval [1 2 3])
;; a is undefined and cannot be evaluated
(eval (read-string "[1 2 a]"))
;; quoting circumvents evaluation
(read-string "'[1 2 a]")
(eval (read-string "'[1 2 a]"))

(type (read-string "5"))


;; Homoiconicty allows simple implementations of a REPL.
;; Additionally, we get macros:
;; Macros in Clojure are not text-replacement (à la C-preprocessor),
;; but rather source-to-source transformations
;; that are calculcated via functions (data to data).
;; Such functions can be really simple (e.g. if-not)
;; oder really sophisticated and involved (e.g. clojure.core.async/go).


;; Macro examples:

;; (defn ...) is expanded to (def ... (fn ...))
(macroexpand '(defn foo [x] x))

;; -> threads the result of an expression into the first argument of the next form
(-> 3
    (+  ,,, 5)
    inc ,,,
    (*  ,,, 2))
;; is the same as
(* (inc (+ 3 5)) 2)

;; Proof:
(macroexpand '(-> 3
                  (+ 5)
                  inc
                  (* 2)))

;; ->> threads the result of an expression into the last argument of the next form
(->> 3
     (- 5 ,,,)
     inc ,,,
     (* 2 ,,,))
;; wird also zu
(* 2 (inc (- 5 3)))

;; as-> assign a name to the result, which is bound in every following form to the result from the expression before
(as-> 3 expr
    (+ expr 5)
    (inc expr)
    (* 2 expr))

(macroexpand '(as-> 3 expr
                (+ expr 5)
                (inc expr)
                (* 2 expr)))

;; for is a very complicated macro
(macroexpand '(for [x [1 2 3]] x))


;; Macros allow us to write easier code (more or less DSLs),
;; by specifiying how the DSL Code is translated into "real" Clojure code
