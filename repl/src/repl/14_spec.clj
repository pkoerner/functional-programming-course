(ns repl.14-spec
  (:use clojure.repl)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [schema.core :as schema]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

;; Requires a recent Clojure version + test.check
;; (see project.clj -> 14 clojure.spec)


(set! *print-length* 10)

(comment

  ;; Case study:
  ;; Analysis of computer science 1 exercises - do some correctors give more points than others?
  ;; the data is genuine, but anonymized
  (defn read-csv [filename]
    (with-open [in-file (io/reader filename)]
      (let [[header & data] (doall (csv/read-csv in-file))
            header (repeat (map keyword header))]
        (map zipmap header data))))

  ;; huge CSV file
  (read-csv "info1.csv")

  ;; What we actually want to have:
  ;; A map-like structure that maps one corrector to the average score, per sheet
  ;; ((["Etienne Weitzel" 67.25]
  ;;   ["Edelfriede Künzel" 74.10344827586206]
  ;;   ["Nushi Happel" 49.44186046511628]
  ;;    ...)
  ;;  (["Berhane Schott" 65.28571428571429]
  ;;   ["Soundakov Reichardt" 62.857142857142854]
  ;;   ["Bao-Ha Wesemann" 78.14285714285714]
  ;;   ...)
  ;;  (["Etienne Weitzel" 71.46666666666667]
  ;;   ["Soundakov Reichardt" 81.9]
  ;;   ["Berhane Schott" 76.75]
  ;;   ...)
  ;;  ...)

  ;; so we group the data by sheet
  (defn group-by-sheet [data]
    (group-by #(select-keys % [:matnr :sheet :corrector]) data))

  ;; and by corrector
  (defn group-by-corrector [data]
    (group-by #(select-keys % [:sheet :corrector]) data))

  ;; sum points by exercise
  (defn sum-exercise [[k vs]]
    (let [sum (reduce (fn [a e] (+ a (:points e))) 0 vs)]
      [(assoc k :sum sum)]))

  ;; calculate the average
  (defn calc-avg [[k vs]]
    (let [sum (reduce (fn [a e] (+ a (:sum e))) 0 vs)
          n (count vs)]
      [(assoc k :avg (double (/ sum n)))]))

  ;; and put everything together
  (defn average-by-corrector-and-sheet
    "Extracts the average points per corrector grouped by exercise sheet"
    [data]
    (as-> data d
      (group-by-sheet d)
      (map sum-exercise d)
      (group-by-corrector d)
      (map calc-avg d)
      (group-by :sheet d)
      (for [[_ e] d]
        (map
         ;; {:sheet 1 :corrector "lol" :avg 10} -> ["lol" 10]
         (fn [{:keys [corrector avg]}] [corrector avg])
         data))))

  (average-by-corrector-and-sheet (read-csv "info1.csv"))
  ;; does not work :-(

  ;; now let us look for errors (there are several!)

  ;; It would be nice
  ;;  - If we didn't have to rely on println for debugging
  ;;  - If we could make formal statements about input/output structures
  ;;  - If one could make statements about the behavior of functions.
  ;;  -  ...
  ;; these are now simply the disadvantages of a dynamic programming language!


  ;; small example:
  (cons 3 [4 5]) ;; okay
  (conj 3 [4 5]) ;; parameters are the other way around

  ;; ClassCastException ... RLY ?!?


  ;; Yeah, well, I'm gonna build my own conj with blackjack and hookers.
  ;; In fact, forget the conj

  (defn conj' [coll ele]
    (assert (sequential? coll) "First argument must be a collection")
    (conj coll ele))

  ;; good error messages? In MY Clojure? It's more likely than you think
  (conj' 3 [4 5])


  ;; pre- and postconditions exist since Clojure 1.1
  ;; This is special syntax. A map must be defined after the parameters
  ;; and the body of the function must follow.
  ;; It is at least somewhat nicer to specify multiple conditions this way.

  (defn conj' [coll ele]
    {:pre [(instance? Iterable coll)]}
    (conj coll ele))

  (conj' 3 [4 5])

  ;; Interval nesting for root calculation
  (defn interval-sqrt
    ([n] (interval-sqrt n 0 n 1e-5))
    ([n a b eps]
     (let [m (/ (+ a b) 2)]
       (cond (< (- b a) eps) a
             (> (* m m) n) (recur n a m eps)
             :else (recur n m b eps)))))

  (interval-sqrt 4)
  (interval-sqrt 0)
  ;; the input is wrong here
  (interval-sqrt -4)


  ;; we can specify preconditions and postconditions.
  ;; if the input is greater than zero, the output is really close enough to the root
  (defn interval-sqrt
    ([n]
     {:pre [(>= n 0)]
      :post [(not (neg? (- n (* % %))))
             (< (- n (* % %)) 1e-5)]}
     (interval-sqrt n 0 n 1e-5))
    ([n a b eps]
     (let [m (double (/ (+ a b) 2))]
       (cond (< (- b a) eps) a
             (> (* m m) n) (recur n a m eps)
             :else (recur n m b eps)))))

  ;; This is design-by-contract.

  ;; Assertions are not well suited for complex structures/relationships

  ;; Exercise:

  (defn foo [x]
    {:pre []})

  ;; The input of foo should be a map that maps keywords to vectors of maps.
  ;; These maps map numbers to strings or booleans

  ;; And now write this as a precondition!

  ;; Have fun!



  ;; 2013 ... And along comes schema ...

  ;; "One of the difficulties with bringing Clojure into a team is the
  ;; overhead of understanding the kind of data [...] that a function
  ;; expects and returns. While a full-blown type system is one solution
  ;; to this problem, we present a lighter weight solution: schemas."


  ;; there are built-in schemes
  (schema/validate schema/Int 4)
  (schema/validate schema/Str 4)
  (schema/validate schema/Str "5")

  ;; and you can also parameterize containers
  (schema/validate [schema/Int] [2 3 4])
  (schema/validate [schema/Int] [2 3 "s"])

  (schema/validate {:name schema/Str
                    :address schema/Str}
                   {:name "Jens"
                    :address "Universitätsstr.1"})

  (schema/validate {:name schema/Str
                    :address schema/Str}
                   {:name "Jens"
                    :address "Universitätsstr.1"
                    :postal-code 40225
                    :ort "Düsseldorf"})

  ;; Composition!
  ;; Schemas are just data
  (def IntVec [schema/Int])
  (def MMap {schema/Keyword {schema/Any IntVec}})
  MMap

  (schema/validate MMap {:a {"x" [1 2]}})
  (schema/validate MMap {:a {:q [1 2]}})
  (schema/validate MMap {:a {:fail "true"}})

  (sequential? "true")
  (seq? "true")
  (seqable? "true")


  ;; Our little exercise from before:
  ;; The input of foo should be a map that maps keywords to vectors of maps.
  ;; These maps map numbers to strings or booleans

  (def int->bool-or-string {schema/Int (schema/cond-pre schema/Str
                                                        schema/Bool)})
  (def foo-schema {schema/Keyword [int->bool-or-string]})

  (schema/validate foo-schema
                   {:foo [{1 true} {2 "2" 3 false}]})

  (schema/validate foo-schema
                   {:foo [{1 true} {2 "2" :a false}]})
  ;; That was surprisingly okay.


  ;; Not (only) structural checks are possible:
  ;; you can also use any predicate
  (def EvenInt (schema/constrained  schema/Int even?))
  (schema/validate EvenInt 2)
  (schema/validate EvenInt 3)
  (schema/validate EvenInt 2.0)



  ;; Scheme has quickly spread, but it has a problem
  (schema/validate {:name schema/Str
                    :address schema/Str}
                   {:name "Jens"
                    :address "Universitätsstr.1"
                    :postal-code 40225
                    :ort "Düsseldorf"})


  ;; Schemes are closed.
  ;; Extension is only possible by overwriting previous definitions.
  ;; so when additional information (to already defined schemas)
  ;; needs to be added, it is no longer possible


  ;; 2016 - Clojure 1.9 introduce specs

  ;; clojure.spec is part of Clojure (it was a library before)
  ;; there were small changes to macro expansion and doc strings for this
  ;; and needed automatic improvement of error messages

  ;; ... and broke the LightTable editor :-(
  ;; The editor was quite convenient for the lecture,
  ;; because old evaluations remain displayed in it.
  ;; LightTable depends on an old version of Clojurescript.
  ;; which contains the following error in a namespace declaration

  ;; (ns cljs.source-map.base64-vlq
  ;;   (require [clojure.string :as string] ;; require is no keyword!
  ;;            [cljs.source-map.base64 :as base64]))

  ;; this is caught by clojure.spec and previously worked "by accident"



  ;; incorrect function declaration
  (defn oop!oop!oop! (x)
    (println x))


  ;; upgrade to Clojure 1.10
  (doc defn)
  ;; the spec was violated on this

  ;; Any Clojure function that receives an argument and returns a
  ;; truthy value is a valid spec.
  ;; valid? checks if a value satisfies a spec

  (s/valid? pos-int? 17)
  (s/valid? pos-int? -3)
  (s/valid? pos-int? 9.0)

  (s/valid? (fn [e] (and (< 0 e) (number? e))) 9.0)
  (s/valid? (fn [e] (and (int? e) (< 1000 e))) 3021)
  (s/valid? + 7)
  (s/valid? conj 1)
  (s/valid? + :a)
  (s/valid? s/valid? s/valid?)
  ;; щ（ﾟДﾟщ）

  ;; specs are registered via 'spec-def'.
  ;; The name of spec is a fully qualified keyword (and has to be!),
  ;; so that specs with the same name can be distinguished

  (s/def ::postal-code int?)
  (s/def ::street string?)
  (s/def ::city string?)

  ;; Afterwards the keyword can be used like a spec
  ;; (::postal-code 1) does not behave differently than usual!

  (s/valid? ::postal-code 4)
  (s/valid? ::postal-code "4")

  ;; And why did it fail?

  (s/explain ::postal-code 4)
  (s/explain ::postal-code "4")
  (s/explain-data ::postal-code "4")


  ;; Combinators
  ;; you can write a function that checks two things
  (s/def ::big-int-p (fn [e] (and (int? e) (< 1000 e))))
  (s/explain ::big-int-p 320)

  ;; or use spec/and for more precise error messages
  (s/def ::big-int (s/and int? (fn [x] (< 1000 x))))
  (s/explain ::big-int 320)

  (s/explain ::big-int 9.0) ;; short circuited - only the first error is reported

  ;; there is also an 'or', you have to give names to the alternatives
  (s/def ::even-or-big (s/or :even even?
                             :big ::big-int))
  (s/explain ::even-or-big 333)

  (s/explain ::not-existing-spec "9")

  ;; A Spec is:
  ;; - A predicate
  ;; - A composition of specs (generated with the help of e.g. or or and)

  ;; Caution! Predicates are specs, the inverse does not hold!

  ((s/and int? even?) 12)
  (s/valid? (s/and int? even?) 12)


  ;; Data structures
  ;; Three common types of use

  ;; a) homogenous (often large/unlimited) collections of data
  ;; b) heterogeneous (struct/object-like) collections of data
  ;; c) syntax (i.e. the order is important)


  ;; Examples

  ;; a) homogenous collections
  {"Etienne Weitzel" 67.25
   "Edelfriede Künzel" 74.10344827586206
   "Nushi Happel" 49.44186046511628}

  [1.0 -9.3 6.2]

  ;; b) Structs
  {:name "Jens Bendisposto"
   :postal-code 42103
   :city "Wuppertal"}

  ;; c) Syntax
  ;; function + arguments
  (+ 1 2 3)

  [1.0 -9.3 6.2]
  ;; Was this not a homogeneous collection just now?
  ;; It depends:
  ;; If they are three temperature samples, it is a homogeneous collection.
  ;; If they are RGB or x-y-z-values instead it is syntax!

  ;; Syntax is complex and should be avoided... a map would make RGB and x-y-z clearer.



  ;; corresponding specs

  ;; (a) Homogenous collections

  (s/def ::points-entry number?)
  ;; fixed amount of correctors
  (s/def ::corrector #{"Etienne Weitzel" "Edelfriede Künzel" "Nushi Happel"})

  (s/def ::points-list (s/coll-of ::points-entry))
  (s/def ::corrector-points (s/map-of ::corrector ::points-list))
  (s/def ::corrector-average (s/map-of ::corrector double?))

  (s/valid? ::corrector-points {"Etienne Weitzel" [40 42 38]
                                "Edelfriede Künzel"  []
                                "Nushi Happel" [41 50 10]})

  (s/explain ::corrector-points {"Etienne Weitzel" [40  3 "42" 34] ;; a string was snuck in
                                 "Edelfriede Künzel"  []
                                 "Nushi Happel" [41 50 10]})

  (s/valid? ::corrector-average  {"Etienne Weitzel" 67.25
                                  "Edelfriede Künzel" 74.10344827586206
                                  "Nushi Happel" 49.44186046511628})

  (s/valid? ::corrector-average  {"Etienne Weitzel" 80
                                  "Jens Bendisposto" 74.5
                                  "Nushi Happel" 49.44})

  ;; who would have spontaneously spotted the invalid corrector?
  (s/explain  ::corrector-average  {"Etienne Weitzel" 80
                                    "Jens Bendisposto" 74.5
                                    "Nushi Happel" 49.44})

  ;; Why does ["Jens Bendisposto" 0] appear the error message?
  ;; this is the path in the data structure to the faulty entry
  ;; (e.g. extractable via get-in)



  ;; (b) Heterogeneous collections / structs

  ;; keys specify a map
  ;; :req are keys, that are necessitated, :opt are optional.
  ;; The Keywords must be the keys as is, and the value must satisfy the same spec.
  (s/def ::address (s/keys :req [::street ::postal-code ::city]
                           :opt [::state]))

  ;; you can sneak in the namespace for all keywords in the map
  ;; this saves work
  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :city "Düsseldorf"})

  (s/explain ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :city "Düsseldorf"})
  ;; no postal-code

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "40225"
                                     :city "Düsseldorf"})

  (s/explain ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :postal-code "40225"
                                      :city "Düsseldorf"})
  ;; postal-code was defined as int

  ;; We don't care if it's an int, or a string containing an int.
  ;; Both are close to an actual int for us, in this case.
  (s/def ::postal-code (s/or :int int?
                             :string-int (comp int? read-string)))

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "40225"
                                     :city "Düsseldorf"})

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "lol nope"
                                     :city "Düsseldorf"})


  ;; (s/valid? ... ) is true ... now what?
  ;; conform can specify which alternative was chosen and
  ;; transforms the input into a "conformed value"

  (s/conform int? 2)
  (s/def ::even-int (s/and int? even?))
  (s/conform ::even-int 12)
  (s/conform (s/coll-of ::even-int) [2 4 6])
  (s/def ::some-name (s/or :it-is-a-string string?
                           :it-is-a-keyword keyword?))
  (s/conform ::some-name "a")
  (s/conform (s/coll-of ::some-name) ["lol" :rofl])


  (s/conform ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :postal-code "40225"
                                      :city "Düsseldorf"})

  (s/conform ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :postal-code 40255
                                      :city "Düsseldorf"})

  ;;  we will come back to that in a moment...

  ;; one more thing: keywords without namespace?
  ;; req-un (that stands for required unqualified)
  (s/conform (s/keys :req-un [::a ::b]
                     :opt-un [::c])
             {:a 1 :b 2})



  ;; (c) Syntax / Sequences
  ;; cat is a concatenation. All elements require a name.
  (s/def ::voxel (s/cat :x number? :y number? :z number?))

  (s/valid? ::voxel [1 2 3])
  (s/explain ::voxel [1 2])
  (s/explain ::voxel [1 2 :three])

  ;; conform once again...
  (s/conform ::voxel [2.8 0 1/2])

  (s/def ::temperature-value (s/cat :volume int? :scale #{:C :F :K}))
  (s/def ::time-series (s/* ::temperature-value))

  (s/conform ::time-series [10 :C 20 :F 70 :F 380 :K])

  ;; You can even build small parsers with conform!
  ;; Then you can use the same code to
  ;;  - Verify the structure of the data
  ;;  - Translate the data to another structure

  ;;  The regex expressions are:
  ;; Concat: cat
  ;; Alternative: alt
  ;; arbitrarily many: *
  ;; at least one: +
  ;; none or at most once: ?
  ;; Additional constraint: &


  ;; exercise requires test.check generators and can generate
  ;; possible inputs for a spec.
  ;; Very useful to get a feel for what the structure looks like!
  ;; the value and the conformed value are generated at the same time.
  (s/exercise (s/alt :option1 (s/cat :at-least-a-string (s/+ string?)
                                     :num-or-not (s/? int?))
                     :option2 (s/* int?)))


  ;; this is relatively powerful: strings and strings with even length
  (s/def ::strings (s/* string?))
  (s/def ::even-strings (s/& ::strings  #(even? (count %))))
  ;; or without the use of regex
  ;; (s/def ::even-strings (s/and ::strings  #(even? (count %))))

  (s/exercise ::strings)
  (s/exercise ::even-strings)


  ;; Note: A flat sequence is processed!
  ;; Nested sequences must be specified explicitly with (spec ...)

  ;; Compare
  (s/exercise (s/cat :data (s/+ ::voxel)))
  (s/exercise (s/cat :data (s/+ (s/spec ::voxel))))



  ;; ---

  ;; and how do you use it?

  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b]) ;; Exercise: implement this function


  ;; The parameter list is also just a sequential data structure!

  ;; appropriate spec for this
  (s/def ::index-of-args (s/cat :src string? :search string?))
  (s/conform ::index-of-args ["Hello World" "orl"])
  ;; unform is the inverse of conform, by the way
  (s/unform ::index-of-args {:search "it also works like this" :src "great"})
  (s/unform ::index-of-args (s/conform ::index-of-args ["Hello World" "orl"]))


  ;; one possibility: Assert

  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b]
    (s/assert ::index-of-args [a b]))


  (my-index-of "aaa" "b")
  (my-index-of 2 4)
  ;; oops


  ;; Spec-Asserts are a tool to debug callers.
  ;; You might not want them in production code.
  ;; That is why they are cheaper when they are not on (default) :-)
  (s/check-asserts true)

  ;; once again
  (my-index-of "aaa" "b")
  (my-index-of 2 4) ; gotcha!


  ;; turn it off again
  (s/check-asserts false)



  ;; You can do it even better:
  ;; you can add specs to functions!
  ;; :args is the argument vector
  ;; :ret specifies the return-value
  ;; :fn correlates the conformed-values of argument and return

  (s/fdef my-index-of
    :args ::index-of-args
    :ret nat-int?
    :fn (fn [cfd] (<= (-> cfd :ret)
                      (-> cfd :args :src count))))

  ;; we'll just steal the other implementation
  (defn my-index-of [a b]
    (clojure.string/index-of a b))

  ;; look at that, we have a spec defined for the function!
  (doc my-index-of)


  ;; Instrument
  (my-index-of nil "2")
  ;; I thought I would get errors now?
  ;; ah, switch it on first!

  ;; this function is the only one that can be instrumented
  (stest/instrumentable-syms)

  (stest/instrument `my-index-of) ;; to prepend the namespace: syntax quote!

  ;; Check for all calls if the parameters are compliant with :args
  (my-index-of nil "2")

  ;; Another incorrect call
  (my-index-of "" "0")



  ;; instrument only checks the precondition (not the post condition and neither the function)
  ;; vmp. Design-by-contract
  ;; Post condition violated: Shame on me
  ;; Pre condition violated: Shame on you!

  ;; instrument exists to debug caller


  ;; Add specs to Mmacros
  ;; same as functions, the macroexpander checks the specs
  ;; The difference to functions: no instrument required

  (declare 100)

  (s/fdef clojure.core/declare
    :args (s/cat :names (s/* simple-symbol?))
    :ret any?)

  (declare 100)




  ;; Testing

  ;; what is the point of :ret and :fn?
  ;; You use them to test a function:
  (stest/check `my-index-of)

  ;; And this is how you get reasonable output:
  (->> (stest/check `my-index-of) stest/summarize-results)

  ;; the error from above was found automatically!
  (my-index-of "" "0")


  ;; FIX ME PLEASE
  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b]
    ; Exercise
    )

  (s/fdef my-index-of
    :args ::index-of-args
    :ret int?
    :fn (fn [cfd]
          (<= (-> cfd :ret) (-> cfd :args :src count))))


  (->> (stest/check `my-index-of) stest/summarize-results)


  (stest/check `my-index-of)


  ;; More tests
  (stest/check `my-index-of {:clojure.spec.test.check/opts {:num-tests 5000}})


  ;; Generators
  ;; some things like (read ...) are not reversible
  (s/exercise ::address)

  ;; this one is missing, too
  (s/def ::state string?)
  ;; then you just need to define your own generator:
  (defn gen-plz []
    (gen/one-of
     [(s/gen nat-int?)
      (gen/fmap str (s/gen nat-int?))]))


  ;; Generators can then be provided
  (s/exercise ::address 10 {::postal-code gen-plz}))


  

