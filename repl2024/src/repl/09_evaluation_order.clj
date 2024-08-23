(ns repl.09-evaluation-order)


;; A closer look at Symbol Evaluation

;; 1 Basic Mechanism
;; 2 The Weird #' thing: var
;; 3 Var Aliasing and Chaining
;; 4 Order of Evaluation Attempts for Symbols
;; 5 Special Delivery: Extra Rules
;; 6 Closing Remarks

(comment

;; 1 Basic Mechanism
;; -----------------

  ;; We know that literals are evaluated directly
  1
  :a

  ;; in data structures, literals the elements are evaluated from left to right (though this might not be the case for maps and sets)
  [:a 1]
  [(println 1) (println 2)]

  ;; Function calls: first the expression that the function returns, then that of the first argument (and recursively the same pattern), the second argument, and so on.
  (+ 1 2)

  ;; some things can not be evaluated...
  ding ;; error

  ;; ...unless they are defined
  (def ding 1)
  ding


;; 2 The Weird #' thing: var
;; -------------------------

  ;; Let us define a function.
  ;; The return value is some weird #'repl.09-evaluation-order/foo thing
  (defn foo [] 42)
  ;; that results in a function object
  foo

  ;; Does the symbol 'foo' stand for a function? apparently...
  (supers (type foo))

  ;; we can call it without problems
  (foo)

  ;; This way you will get the strange #'-thing again
  (var foo)

  ;; or we write it directly
  #'foo
  ;; this #'-thing is callable (that's why the function call works),
  ;; but is mainly a reference
  (supers (type #'foo))

  ;; the #'-thing is a var.
  ;; This can be thought of as a box that points to the value behind it.

  ;; The symbol foo is mapped to the var in the namespace pointing to the defined function.


  ;; when we call the var, the function it points to is evaluated
  ((var foo))

  ;; both forms are equivalent
  ;; @ is to deref what ' is to quote
  @#'foo
  (deref (var foo))
  ;; So if we dereference the var, we get the thing it points to.

  ;; the function object can also be called directly
  (@#'foo)

  ;; equivalent without syntactic sugar
  ((deref (var foo)))


  ;; once again:

  ;; Namespace:
  ;; symbol foo -> a var

  ;; Var:
  ;; Var -> The function object


;; 3 Var Aliasing and Chaining
;; ---------------------------

;; It is okay not to understand all the details in this section.
;; Simply don't do this. Please.


  ;; what happens with multiple vars (aliasing)?
  (def bar1 foo)

  ;; bar1 also stands for the function
  ;; because foo is directly resolved and replaced by the value
  bar1

  ;; you can call it
  (bar1)

  (var bar1)  ;; #'bar1
  (var foo)   ;; #'foo
  ;; they are different Vars...

  ;; that stand for the same thing
  (identical? foo bar1)
  ;; but are not the same object!
  (identical? (var foo) (var bar1))

  ;; What happens if you point a Var to another Var?
  (def bar2 #'foo)
  bar2

  (def bar3 #'bar2)
  bar3
  ;; We have:
  ;; bar3 ---> bar2 ---> foo --> fn

  ;; the evaluated expression bar3 results in the Var bar2
  (eval bar3)
  (type (eval bar3))

  ;; if we dereference this again (follow the pointer), we get...
  @bar3
  ;; and dereferencing it twice results in...
  @@bar3
  bar3

  ;; sixty-four-dollar question: what happens if we call the Var
  ;; that points to a Var that points to yet another Var that points to a function?
  (bar3)
  ;; ...the Var-chain is traced and the function is called

  ;; what if we now point to something else?
  (def foo (var bar3))
  (bar3)
  ;; Welcome to the endless loop of dereferencing!
  ;; we actually created a cycle
  ;; bar3 --> bar2 --> foo
  ;;  ^                 v
  ;;   <----------------
  (-> bar3 deref deref)

  ;; Lessons learned: You really don't want to use Vars.
  ;; The only useful use cases are constant definitions!
  ;; def, defn is used only at the top level of the program.

  ;; Whoever use Vars differently will be punished with questions
  ;; in the exam about it, not less than 5!





;; 4 Order of Evaluation Attempts for Symbols
;; ------------------------------------------

  ;; 1) fully qualified symbols, i.e. with namespace, trump everything else
  ;; java.io.Writer is a Java class
  (def java.io.Writer 101)
  ;; fully qualified symbol gives the defined value
  repl.09-evaluation-order/java.io.Writer
  ;; without qualification the Java class wins
  java.io.Writer

  ;; 2) Java classes cannot be used within bindings
  ;; and have priority over ordinary symbols
  (let [java.io.Writer 12] java.io.Writer) ;; error


  ;; 3) local variables have priority over global variables
  ;; the deeper inside a scope they are, the higher the priority
  (def foo 10)
  foo
  (let [foo 11] foo)
  ;; and in combination with the fully qualified symbol
  (let [foo 11] (+ foo repl.09-evaluation-order/foo))


  ;; 4) global symbols come next in priority
  foo

  ;; 5) if then nothing is found by then, an error occurs
  sdfjks




;; 5 Special Delivery: Extra Rules
;; -------------------------------


  ;; special forms are the basis of Clojure. There are only very few of them
  ;; (depending on how you count, there are 13). Then there are some for Java Interop.
  ;; Examples are def, fn, if, do, let and loop/recur.
  ;; Var and quote are also special forms.
  (def if +)
  if
  ;; is the regular 'if' gone now?

  ;; if is now +
  (apply if [1 2 3])

  ;; and somehow also not
  (if 1 2 3)

  ;; special forms win beat out namespace symbols,
  ;; IF they are what is called
  ;; the compiler does that for us...

  ;; other quote: the syntax quote `
  ;; We will encounter it again at another time.
  ;; for now we need to know: it not only quotes the symbol,
  ;; but prepends the namespace to it as well
  `foo

  ;; special forms are not prepended with a namespace by the syntax quote!
  `do
  `if
  ;; They do not live in any namespace but are actually part of the language.


;; 6 Closing Remarks
;; -----------------

  ;; lessons learned today:
  ;; - def(n) is for constants and functions.
  ;; - you should not try to overwrite special forms.
  ;; - when is a symbol resolved and how?

  ;; - who against all warnings does any of the nonsense mentioned above,
  ;;   must figure out what the following does in the exam:

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
      ((do do) ((do do) catch var (deref (var var)) (eval (quote quote))) quote))))
