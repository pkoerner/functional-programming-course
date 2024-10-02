(ns repl.04-destructuring)

;; This section takes a look at destructuring:
;; Basically, it is syntax to take a complex data structure apart and give names to (some) elements.

;; 1 Revision: Bindings
;; 2 Destructuring Sequentials
;; 3 Destructuring Maps
;; 4 Nesting Destructuring
;; 5 Named Arguments and Default values


(comment
  ;; Destructuring

  ;; If you have a sequence s and need the first few elements,
  ;; it totally sucks having to write (first s) (second s) (nth s 2) constantly

  ;; same with maps: (get m :foo) (get m :bar 42), ...
  ;; especially if you need those values more often


;; 1 Revision: Bindings
;; --------------------

  ;; first the concept of bindings:

  ;; the things in the vector of a let are bindings
  ;; (let [bindings] body)

  (let [x 3]
    (println x)
    (+ 3 x))

  ;; You will find them in many places, including 'for', 'doseq', 'fn', etc.
  (for [x [3]]
    (+ 3 x))

  (doseq [x [1 2 3]]
    (println x))

  ((fn [v] (+ (first v)
              (second v)
              (nth v 2)))
   [1 2 3 4])

  ;; Bindings are all things that associate a symbol with a value.

  ;; Destructuring is possible wherever there is a binding.
  ;; We will primarily focus on destructuring within let:


;; 2 Destructuring Sequentials
;; ---------------------------

  ;; we destructure the vector [1 2] into its first two elements x and y
  (let [a [1 2]
        [x y] a]
    (println :x x :y y))
  ;; 1 is bound to x, 2 is bound to y

  ;; Inlined from now on, to save myself some typing.
  ;; The destructuring vector does not need to cover all elements:
  (let [[x y] [3 4 6]]
    (println x y))

  ;; If the collection to destructure is too small,
  ;; the extra symbols are bound to nil by default
  (let [[x y z] [1 2]]
    (println x y z))

  ;; Convention: When you do not care for a value, use the symbol _
  (let [[x _ y] [1 2 3]]
    (println x y))

  ;; If you bind the same symbol multiple times,
  ;; the most inner binding determines its value
  (let [[x _ x] [1 2 3]]
    (println x))

  ;; We already know '&' from function definitions...
  (let [[x & y] [7 8 9]]
    (println x y))

  ;; ':as' binds the whole sequence to a symbol
  (let [[x & y :as l] [1 2 3]]
    [x y l])

  ;; vectors also destructure lists...
  (let [point (list 4 8)
        [x y] point]
    (println x y point))

  ;; and strings...
  (let [[a b c d] "foo" ]
    (println a b c d))

  ;; but not numbers (You cannot convert them into a sequence)
  (let [[a b c d] 55555 ]
    (println a b c d))



;; 2 Destructuring Maps
;; ---------------------------


  ;; maps are basically just some key-value tuples
  (map identity {:x 100 :y 200})

  ;; but there is no reasonable ordering for a map,
  ;; so destructuring them in some order makes no sense!
  (let [point {:x 100 :y 200}
        [x y] point]
    (println x y))



  ;; but we can destructure maps with map literals.
  ;; Strange at first: reversed order: First the identifier, then the keyword
  (let [point {:x 300 :y 500}
        {asd :x y :y} point]
    (println asd y))


  ;; ':as' binds the whole structure to a symbol as before
  (let [point {:x 100 :y 200}
        {x :x y :y :as m} point]
    (println x y m))

  ;; Identifier names can be chosen arbitrarily
  (let [point {:x 100 :y 200}
        {a :y b :x :as m} point]
    (println a b m))

  ;; :keys is syntax:
  ;; it binds the symbols to values of corresponding keywords
  (let [point (into {} [[:x 100] [:y 1033]])
        {:keys [x y] :as m} point]
    (println x y m))

  ;; The default for missing keys is nil by default
  (let [point {:x -210 :y 7100}
        {:keys [x y z]} point]
    (println x y z))

  ;; You can also specify defaults
  (let [point {:x -210 :y 7100}
        {:keys [x y z] :or {x 0 y 0 z 0} } point]
    (println x y z))


  ;; Besides keys there are also strs and syms
  ;; if the keys of a map are strings or symbols respectively

  (let [point {"x" -210 "y" 7100}
        {:strs [x y]} point]
    (println x y))

  ;; :x is a keyword here, 'y a symbol
  (let [point {:x -210 'y 7100}
        {:syms [x y]} point]
    (println x y))

  ;; :strs and :syms are used exceedingly rarely and
  ;; are listed for completeness sake only

  ;; You can also use all flavors of destructuring at once
  (let [data {[] 'ag,
              :kw "yo",
              'symb :sw,
              "string" [\l \o]}
        {:keys [kw missing]
         :strs [string]
         :syms [symb],
         xx [],
         :or {missing \space}} data]
    (println kw string missing symb xx))


;; 4 Nesting Destructuring
;; -----------------------


  ;; and nest them, too
  (let [db [{:name "Bendisposto" :first-name "Jens"}
            {:name "Leuschel" :first-name "Michael"}]
        [{n1 :name} {n2 :name :as second-entry}] db]
    (println n1 n2 second-entry))



  ;; An example of destructuring of function arguments
  ;; added bonus: Since there is no static typing: You can see from the destructuring how the passed data structure should look

;; 5 Named Arguments and Default values
;; ------------------------------------

  (defn run-tool [{tool :tool,
                 {:keys [host username port]
                  :or {username "anonymous" port 22}} :arguments}]
    (println "connecting" tool "to" (str username \@ host \: port)))

  (run-tool {:tool :ssh
             :arguments {:host "example.com"
                         :username "jdoe"}})

  ;; You can pass optional arguments as a map like this...
  (defn my-fancy-function [arg & {:as m}]
    (println arg m))
  
  (my-fancy-function 42 :john "witulski" :lecture "fp")
  
)

