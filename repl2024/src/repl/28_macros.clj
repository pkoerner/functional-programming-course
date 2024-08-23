(ns repl.28-macros
  (:use [clojure.repl]))

;; Look behind you, a three headed monkey!
;; Don't pay attention to what is defined here (but evaluate it)
                                                                                                                                                                                                                   (def x -3)


;; 1 Introduction
;; 2 Quoting 2: Electric Boogalo - The Syntax Quote `
;;   - rules for scalar values
;;   - rules for compound values
;; 3 The gensym #
;; 4 Don't Quote Me on That - Unquote ~
;; 5 Unquote-Splice ~@
;; 6 Practicing the Syntax Quote
;; 7 Writing a Macro and Staying Sane
;; 8 and now?
;;   - debugging with macroexpand and its variants 
;;   - letting intermediate results to avoid double evaluation
;;   - comparison: fn vs macro (evaluation and side effects)

;; new today:
;; `, ~, ~@, #


(comment

;; 1 Introduction and Revision
;; ---------------------------

  ;; What does it take to understand the true power of Lisp?



  ;; Revision:

  ;; We generate the resulting code by using (list ...) to glue the lists together,
  ;; which then become the resulting function calls.
  ;; You can write macros this way (and there are some that are implemented this way)
  ;; However, Clojure also has a kind of template syntax.



  ;; Syntax Quoting & Reader Experiments

  ;; Helper function that gives us input,
  ;; the result from the reader and the evaluated result (as before).
  (defn my-read [foo]
    (let [data (read-string foo)
          result (try (eval data) (catch Throwable t (.getMessage t)))]
      {:input foo
       :read data
       :eval result
       :data-type (class data)
       :eval-type (class result)}))

  (def my-readable my-read)
  ;; You might find it useful to run the output through a pretty printer.
  ;; In that case you should load clojure.pprint and use this version:
  ;; (def my-readable (comp clojure.pprint/pprint my-read))


  ;; Revision Part 2:
  ;; Quoting = anti-eval
  (my-readable "(quote (2 3 4))")
  (my-readable "''(:a :b)")
  (my-readable "(eval ''(1 2 3))")

  (my-readable "'my-readable")


;; 2 Quoting 2: Electric Boogalo - The Syntax Quote `

  ;; Rule #1 of the syntax quote:
  ;; - symbols receive the matching or current namespace
  ;; - other scalars are not altered

  (my-readable "`my-readable")
  (my-readable "`+")
  (my-readable "`this-doesnt-exist")
  (my-readable "`:foo")
  (my-readable "`1")
  (my-readable "`if") ;; we talked about this already - special forms are special.

  ;; Data structure
  (my-readable "'(1 2 3)") ;; normal quote
  (my-readable "`(1 2 3)") ;; syntax quote
  (my-readable "`[1 2 3]")



  ;; Rule #2 of the syntax quote:
  ;; syntax quoting compound structures generates code that
  ;; produces the syntax quoted structure when you evaluate it.

  ;; You can use it to create a kind of code template.
  ;; Its main use are macros, but you can also use it in regular
  ;; functions.


;; 3 The gensym #
;; --------------


  ;; where we had to call (gensym) ourselves last time,
  ;; we can make use of syntax (in scope of the syntax quote) instead:
  (my-readable "`a#")
  (:eval (my-read "`[a# a#]"))
  (:eval (my-read "`(let [x# 15 y# 2] (+ x# y#))"))

  ;; the last expression generates something like
  (let [x0 15
        y1 2]
    (+ x0 y1))

  ;; Yes, you can generate collisions with existing identifiers.
  ;; No, that's not a good idea.


  ;; gensym'd symbols within **the same** syntax quotes are the same
  ;; in case of different nesting or other syntax quotes
  ;; you get other symbols!


  ;; in different or nested syntax quotes
  ;; other symbols are generated
  (:eval (my-read "[`a# `a#]"))
  (:eval (my-read "`[x# `x#]"))




;; 4 Don't Quote Me on That - Unquote ~
;; ------------------------------------

  (def foo 42)
  (my-readable "`foo")
  (my-readable "`~foo")
  (my-readable "`(+ 1 2)")
  (my-readable "`~(+ 1 2)")

  ;; The unquote cancels the syntax quote for the next expression.
  ;; Therefore, you can only use it within a syntax quote.

  ;; The difference is plain to see here:
  (:eval (my-read "`(* 2 ~(+ 4 5) (* 3 4))"))
  ;; For (+ 4 5), code that replaces the data structure is *not* generated
  ;; instead the form is inserted as is
  ;; That is why 9 is calculated directly in the evaluation step.


  ;; Once again but slower:

  (my-readable "`(* 2 (+ 4 5))")

  ;; reads the following code

  (clojure.core/seq
   (clojure.core/concat
    (clojure.core/list (quote clojure.core/*))
    (clojure.core/list 2)
    (clojure.core/list ;; [X]
     (clojure.core/seq
      (clojure.core/concat
       (clojure.core/list (quote clojure.core/+))
       (clojure.core/list 4)
       (clojure.core/list 5))))))

  ;; with unquote
  (my-readable "`(* 2 ~(+ 4 5))")

  (clojure.core/seq
   (clojure.core/concat
    (clojure.core/list (quote clojure.core/*))
    (clojure.core/list 2) ;; the same up to here
    (clojure.core/list (+ 4 5)))) ;; (*)
  ;; The syntax unquote causes the form (*) to be inserted directly.
  ;; and not the code that generates the form [X].



  ;; A surprising combination
  (my-readable "`~'a")
  ;; That is not as useless as you think!
  ;; Sometimes (admittedly rarely) you want to create an exact symbol in a macro.




;; 5 Unquote-Splice ~@
;; -------------------

  ;; Sometimes you receive a list and you want to pass the content to a function.
  ;; In those cases you can use apply...
  (my-readable "`(+ [1 2 3])")
  (my-readable "`(apply + [1 2 3])")
  ;; but what if it is not a function like +, but a macro?
  ;; then apply does not work...
  ;; Unquoting by itself does not help.
  (my-readable "`(+ ~[1 2 3])")
  ;; the splicing operator ~@ however does!
  (my-readable "`(+ ~@[1 2 3])")
  ;; it circumvents one (list ...) in the code generated by `.
  ;; ~@ acts like a quasi apply for macros.
  (my-readable "`(+ ~@42)")
  (read-string "`(+ ~@42)")




;; 6 Practicing the Syntax Quote
;; -----------------------------

  (defn mk-adder [x]
    (eval `(fn [x#] (+ x x#))))
  ;; the function receives a fresh parameter so no collision is caused

  ;; always add 12
  (def a12 (mk-adder 12))

  ;; 12 + 1 =
  (a12 1)

  ;; P R A N K E D

  ;; what happened here?
  ;; the call is as expected
  (my-readable "(a12 1)")
  ;; this one also checks out
  (my-readable "(mk-adder 12)")
  (my-readable "(eval `(fn [x#] (+ x x#)))")
  (:read (my-readable "(eval `(fn [x#] (+ x x#)))"))
  ;; aha!
  ;; this is expanded to (fn [x-fresh] (+ repl.28-macros/x x-fresh))
  ;; that is not the locally defined x!

  ;; but the one defined in line 6 of the file...
  ;; the rule was: symbols are namespaced





  ;; this is the right way to do it:

  (defn mk-adder [x]
    (eval `(fn [x#] (+ ~x x#))))

  (def a12 (mk-adder 12))

  (a12 1)



;; 7 Writing a Macro and Staying Sane
;; -----------------------------------


  ;; "Be reasonable. Do it my way." 
  ;;   - Coach Ralph Maughan


  ;; So you want to write a macro?

  ;; Step 1: Write what the code should be replaced by
  ;;         within a syntax quote
  (defmacro if-not2 [test then else]
    `(if (not test) then else))

  ;; now all symbols are namespaced, so this does not work yet of course
  (if-not2 true 1 2)
  (macroexpand-1 '(if-not2 true 1 2))

  ;; Step 2: Unquote all parameters
  (defmacro if-not2 [test then else]
    `(if (not ~test) ~then ~else))

  (if-not2 true 1 2)
  (if-not2 true (println :then) (println :else))

  ;; Step 3: Verify the result with macroexpand-1
  (macroexpand-1 '(if-not2 true (println :then) (println :else)))


  ;; Step 4: use ~@ (splicing) and # (gensym), where necessary!

  (use 'clojure.repl) ;; for source
  ;; if-not is actually implemented exactly like this
  (source if-not)


  ;; How can you recreate 'when'?
  (when true
    (println 1)
    (println 2))

  ;; 'when' is an 'if' without else
  ;; since the body may contain multiple expressions, we need to use ~@
  (defmacro whenn [test & body]
    `(if ~test (do ~@body)))


  ;; and it works
  (whenn true
    (println 1)
    (println 2))

  ;; alternatively: write the else-branch explicitly
  (defmacro when2 [test & body]
    `(if ~test
       (do ~@body)
       nil))

  ;; Original implementation:
  (source when)
  ;; this was probably written when the template syntax did not exist yet...



;; 8 and now?
;; ----------

  ;; NOT a revision: the same thing as last week, but this time with templating syntax!
  ;; observe the same issues:

  ;; How do you recreate 'and'?
  (and true false true)
  ;; 'and' is not a function
  (and false (println :hallo))
  ;; but aborts the evaluation after the first falsey value
  ;; (analogously with or and the first truthy value)

  (defmacro and2
    ([] true) ;; the empty conjunction is true (neutral element)
    ([x] x)   ;; 'and' of one argument is exactly the argument
    ([x & rest] `(if ~x (and2 ~@rest) ~x)))  ;; Recursion
    ;; if the first argument is true, the rest must hold,
    ;; otherwise return the result from the first argument

  ;; seems to work
  (and2 true true 17)
  (and2 true (println :huhu)) 
  ;; but why do I see :huhu twice with this call?
  (and2 true (println :huhu) true)

  (use 'clojure.walk) ; for macroexpand-all
  ;; Debugging Macros:
  ;; macroexpand-1 does one expansion step
  ;; macroexpand expands macros until the first call is no longer a macro
  ;; (macros can expand to other macros)
  ;; macroexpand-all expands all macros in a call

  ;; often useful:
  ;(def macroexpand-nice (comp clojure.pprint/pprint macroexpand-all))

  ;; this is okay
  (macroexpand-all '(and2 true (println :huhu)))
  ;; the whole form (println :huhu) is used twice (each occurrence of ~x in and2)
  (macroexpand-all '(and2 true (println :huhu) true))


  ;; and2 evaluates x twice! This is a bug!!!
  ;; This is called an unhygienic macro

  ;; actually, we want to expand to something like this:
  (let [x true] ;; so that we have an x we define one locally
    (if x
      (let [y (println :huhu)]
        (if y true y))
      x))

  ;; we do this with a gensym
  (defmacro and1
   ([] true)
   ([x] x)
   ([x & rest]
    `(let [x# ~x] (if x# (and1 ~@rest) x#))))

  ;; now it is correct:
  (macroexpand-all '(and1 true (println :huhu) true))
  (and1 true (println :huhu) true)

  ;; lessons learned (again): Unquoting the same form twice can hurt
  ;;                  when side effects are involved
  ;; *sometimes* you want to evaluate something multiple times
  ;; *in the vast majority of cases* this is an accident.
  ;; Then you need a let with gensym!
  ;; rule of thumb: NEVER unqoute the same thing twice!


)
