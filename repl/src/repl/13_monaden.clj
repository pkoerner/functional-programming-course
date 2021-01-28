(ns repl.13-monaden
  (:use [clojure.algo.monads]
        [repl.19-wiederholung :refer [unscharf half]]
        [clojure.walk :refer [macroexpand-all]]))

(comment


  ;; Strictly speaking, a monad consists of a type constructor,
  ;; the return function and a function for composition
  ;; (bind is only one possibility).

  ;; The type constructor plays no role in Clojure as a dynamic language.


  ;; Types (only based on Haskell, not real Haskell):

  ;; inc           : Long -> Long
  ;; half          : Long -> Long | nil
  ;; unscharf      : Long -> [Long]

  ;; The functions take a value and outputs a value with context.
  ;; A value with context is called a monadic value.
  ;; Functions that turn a normal value into a monadic value
  ;; are called monadic functions.

  ;; mf : a -> m b

  ;; The context can be, for example,
  ;; that the calculation failed or was non-deterministic.


  ;; return: function that takes a normal value as parameter
  ;;         and returns a monadic value
  ;; return : a -> m b
  ;; -> return is a monadic function

  ;; bind: Function that gets a monadic value and
  ;;       a monadic function as parameter and returns a monadic value.
  ;; bind : m a -> (a -> m b) -> m b



  ;; There are several valid combinations of bind and return
  ;; that produce different behavior.

  ;; bind and return must satisfy the following rules to work with uberlet:

  ;; Right/Left Identity
  ;; (>>= (return a) f) =  (f a)
  ;; (>>= m return) =  m

  ;; Associativity
  ;; (>>= (>>= m f) g) = (>>= m (fn [x] (>>= (f x) g)))


  ;; From here on out we use the library algo.monades

  ;; uberlet is called domonad
  ;; bind is called m-bind
  ;; return is called m-result

  ;; trivial monad / identity monad (let)
  (domonad identity-m [a 3
                       b (+ a 4)
                       c (* a b)] c)

  ;; sequence monad / list monad (for)
  (domonad sequence-m [a (unscharf 10)
                       b (unscharf a)
                       c (unscharf b)] c)





  ;; maybe monad
  (domonad maybe-m [a 20
                    b (half a)
                    c (half b)] c)

  (domonad maybe-m [a 10
                    b (half a)
                    c (half b)] c)


  ;; If identity-m did not exist:

  (defmonad trivial-m
    [m-result identity
     m-bind (fn [mv f] (f mv))])

  (domonad trivial-m [a 3
                      b (+ a 4)
                      c (* a b)] c)


  ;; The first parameter is a map with bind and return
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



  ;; So: monads are everywhere


  ;; you can define 'let' as monad
  ;; as well as 'for'


  ;; Generators (from test.check) are just monads

  ;; bind und return are even called as such!

  ;; bind: gen a -> (a -> gen b) -> gen b
  ;; return: a -> gen a



  ;; behind the code analyses in core.async for go blocks
  ;; you will find the state monad


  ;; Goal: stateful calculations
  ;;       in a pure context


  ;; State monad in an example:

  ;; An interpreter for a (very simple) language is to be written:

  ;; x = 4
  ;; y = x++ + x

  ;; Assume a parser already exist for this,
  ;; that generate the following data structure

  (def input '($do ($assign :x ($int 4))
                   ($assign :y
                            ($add ($postinc :x)
                                  ($id :x)))))



  ;; Task: Write an interpreter for the language
  ;; The result of the program call should be 9!
  ;; The type of each variable is Long.
  ;;   The default value of an unset variable is 0.


  ;; Deadline: 10 minutes














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



  ;; How do you test this?

  ;; meh!

  (defn test1 []
    (dosync (let [s @state]
              (alter state (constantly {:x 1}))
              ($postinc :x)
              (assert (= 2 (get @state :x)))
              (alter state (constantly s)))))

  (do (test1) @state)

  ;; It would be better if the functions would receive the state explicitly as input.
  ;; If the state is modified, the new state must also be explicitly returned.
  ;; The return value is then [<value> <new state>].

  (defn $postinc' [env n] (let [v (get env n)] [v (assoc env n (inc v))]))

  (defn test2 [x]
    (= ($postinc' {:x x} :x) [x {:x (inc x)}]))

  (test2 1)
  (test2 2)


  ;; This will be quite time-consuming, by hand!
  ;; In particular, we need to change the input

  ;; state-m to the rescue


  ;; Renaming is only done for clarity here.
  ;; It would have worked with the original too!
  (def m-input '(m-assign :y
                          (m-add (m-postinc :x)
                                 (m-id :x))))


  ;; == monadic value of the state monad:

  ;; Is a function that takes a state and
  ;; returns a tuple of a value and a subsequent state


  ;; return looks like this: (fn [v] (fn [env] [v env]))
  (defn state-return [v]
    (fn [env] [v env]))

  (with-monad state-m
    ;; Note: in the newer version of clojure.algo.monads
    ;;       this does not work for some reason.
    ;; Based on the commits I don't see why
    ;; TODO: future Jens should fix this. (pk, 24.01.18)


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
                r (set-val n v)] r)) ;; Exercise !

    (defn m-do [a b] (domonad [x a y b] nil)))


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


  ;; bind of the  state monad:

  ;; A simple example: x++

  (defn x++ []
    (fn [{x :x :as e}]
      (let [x' (inc x)
            e' (assoc e :x x')]
        [x' e'])))


  ;; x++ is a monadic function
  ;; (x++) is a monadic value

  ((x++) {:x 4})


  ;; bind ... finally

  (defn state-bind [mv f]
    (fn [env]
      (let [[v ss] (mv env)
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
  ;; monad transformer

  ;; We already looked at this one before:
  (def +m (with-monad maybe-m (m-lift 2 +)))
  (def +s (with-monad sequence-m (m-lift 2 +)))



  (+s [1 2] [1 2 3])

  ;; What happens if nil is involved?
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

  ;; From one of Jens' projects (BLA):
  (def wd-state-m (maybe-t state-m)))
