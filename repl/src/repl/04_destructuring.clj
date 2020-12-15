(ns repl.04-destructuring)

(comment
  ;; Destructuring
  
  ;; wenn man eine Sequenz bekommt und die ersten paar Elemente braucht,
  ;; nervt es total die ganze Zeit (first s) (second s) (nth s 2) zu schreiben

  ;; analog auch bei Maps: (get m :foo) (get m :bar 42), ...
  ;; das gilt besonders, wenn man die Werte öfter braucht


  ;; zuerst der Begriff des Bindings:

  ;; was im let im Vektor steht sind die Bindings
  ;; (let [bindings] body)

  (let [x 3]
    (println x)
    (+ 3 x))

  ;; die gibt es an vielen Stellen, auch in for, doseq, fn, etc.
  (for [x [3]]
    (+ 3 x))

  (doseq [x [1 2 3]]
    (println x))

  ((fn [v] (+ (first v)
              (second v)
              (nth v 2)))
   [1 2 3 4])

  ;; Bindings sind alle Dinge, die ein Symbol mit einem Wert assoziieren

  ;; destructuring geht überall, wo es Bindings gibt.
  ;; Wir gucken uns das hauptsächlich im let an:

  ;; sequenzmäßige Objekte:

  ;; wir zerlegen den Vektor [1 2] in die ersten beiden Elemente x und y
  (let [a [1 2]
        [x y] a]
    (println :x x :y y))
  ;; x wird 1, y wird 2

  ;; ab jetzt geinlined, um ein wenig Tipparbeit zu sparen.
  ;; der destrukturierende Vektor muss nicht alle Elemente abdecken:
  (let [[x y] [3 4 6]]
    (println x y))

  ;; default ist nil, wenn es nicht ausreicht
  (let [[x y z] [1 2]]
    (println x y z))

  ;; Konvention: wenn ein Wert egal ist, ist das Symbol _
  (let [[x _ y] [1 2 3]]
    (println x y))

  ;; bei mehreren Auftreten bestimmt wie im let das letzte Symbol den Wert innen
  (let [[x _ x] [1 2 3]]
    (println x))

  ;; & kennen wir schon von Funktionen...
  (let [[x & y] [7 8 9]]
    (println x y))

  ;; :as gibt der ganzen Sequenz einen Namen
  (let [[x & y :as l] [1 2 3]]
    [x y l])

  ;; Vektoren destrukturieren auch Listen...
  (let [point (list 4 8)
        [x y] point]
    (println x y point))

  ;; und Strings...
  (let [[a b c d] "foo" ]
    (println a b c d))

  ;; aber keine Zahlen (die kriegt man nicht in eine Seq gepresst)
  (let [[a b c d] 55555 ]
    (println a b c d))

  ;; maps sind eigentlich nur einige Key-Value Tupel
  (map identity {:x 100 :y 200})

  ;; eine Map hat aber keine sinnvolle Reihenfolge, sodass Destrukturierung so keinen Sinn ergibt!
  (let [point {:x 100 :y 200}
        [x y] point]
    (println x y))



  ;; aber wir können Map mit dem Mapliteral destrukturieren.
  ;; seltsam wirkend: umgekehrte Reihenfolge: erst der Identifier, dann Keyword
  (let [point {:x 300 :y 500}
        {asd :x y :y} point]
    (println asd y))


  ;; :as gibt wieder dem Ganzen einen Namen
  (let [point {:x 100 :y 200}
        {x :x y :y :as m} point]
    (println x y m))

  ;; Identifiernamen können beliebig gewählt werden
  (let [point {:x 100 :y 200}
        {a :y b :x :as m} point]
    (println a b m))

  ;; :keys ist wieder Syntax:
  ;; es bindet die Symbole an die Werte, die hinter den passenden Keywords stehen
  (let [point (into {} [[:x 100] [:y 1033]])
        {:keys [x y] :as m } point]
    (println x y m))

  ;; default ist auch hier nil
  (let [point {:x -210 :y 7100}
        {:keys [x y z]} point]
    (println x y z))

  ;; man kann auch Standardwerte mitgeben
  (let [point {:x -210 :y 7100}
        {:keys [x y z] :or {x 0 y 0 z 0} } point]
    (println x y z))


  ;; neben keys gibt es auch strs und syms
  ;; falls die Schlüssel entsprechend strings oder Symbole sind

  (let [point {"x" -210 "y" 7100}
        {:strs [x y]} point]
    (println x y))

  ;; :x ist hier ein Keyword, 'y ein Symbol
  (let [point {:x -210 'y 7100}
        {:syms [x y]} point]
    (println x y))

  ;; :strs und :syms verwendet man sehr, sehr selten und sind nur der Vollständigkeit halber gelistet

  ;; es geht auch alles durcheinander
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



  ;; und auch verschachtelt
  (let [db [{:name "Bendisposto" :vorname "Jens"}
            {:name "Leuschel" :vorname "Michael"}]
        [{n1 :name} {n2 :name :as second-entry}] db]
    (println n1 n2 second-entry))



  ;; hier mal ein Einsastz im Funktionsargument
  ;; zusätzlicher Vorteil, da wir keine statische Typisierung haben: man kann ablesen, wie die Datenstruktur aussehen muss
  (defn run-tool [{tool :tool,
                 {:keys [host username port]
                  :or {username "anonymous" port 22}} :arguments}]
    (println "connecting" tool "to" (str username \@ host \: port)))

  (run-tool {:tool :ssh
             :arguments {:host "example.com"
                         :username "jdoe"}})

  ;; so kann man optionale Argumente als Map weiterreichen...
  (defn my-fancy-function [arg & {:as m}]
    (println arg m))
  
  (my-fancy-function 42 :john "witulski" :vorlesung "fp")
  
)

