(ns repl.16-transducer
  (:require [clojure.core.reducers :as r])
  (:use clojure.repl))


(comment

  ;; ein Blick auf map
  ;; map nimmt irgendetwas Sequenziges und gibt eine LazySeq zurück
  (map inc [1 2 3])
  ;; andere Geschmacksrichtung: steck es noch in einen Vektor
  (mapv inc [1 2 3])
  ;; was ist mit Sets?
  (map inc #{1 2 3})
  ;; muss man sich selbst zusammenfrickeln
  (into #{} (map inc #{1 2 3}))
  ;; map gibt es auch in parallel
  (pmap inc (range 100))
  ;; in core.async gibt kann man eine Funktion auf alle Werte anwenden, die aus einem Channel fallen
  ;; (core.async/map< inc channel)




  ;; wie implementiert man ein einfaches map?
  ;; Der Einfachheit halber:
  ;;  - nur einstellige Fkt.
  ;;  - keine Lazyness + Chunking

  (defn map2 [f c]
    (when (seq c)
      (cons (f (first c))
            (map2 f (rest c)))))
  (map2 inc (range 19))
  ;; zu einfach, der Stack fliegt weg
  (map2 inc (range 1900000))

  (map2 inc [1 2 3])

  ;; bessere Alternative: map mit reduce
  (defn map3 [f c]
    (reduce (fn [a e]
              (conj a (f e))) [] c))

  ;; eigentlich ist das mapv, ist uns aber erst mal egal
  (map3 inc [1 2 3])


  ;; so geht filter
  (defn filter3 [p c]
    (reduce (fn [a e]
              (if (p e)
                (conj a e)
                a))
            [] c))

  (filter3 even? (range 10))

  ;; mapcat via concat
  (defn mapcat2 [f c]
    (reduce (fn [a e]
              (concat a (f e))) [] c))

  (mapcat2 (fn [e] [e e e]) [1 2 3])


  ;; oder doppeltes reduce
  (defn mapcat3 [f c]
    (reduce (fn [a e]
              (reduce conj a (f e))) [] c))

  (mapcat3 (fn [e] [(inc e) e (dec e)]) [1 2 3])


  ;; Die sehen alle ähnlich aus ...


  ;; Es gibt zwei Aspekte, die hier zusammencomplected werden!
  ;;   - die Datenstruktur selbst (das concat / conj)
  ;;   - die Essenz von map / filter / ... (Transformation von Werten mit Funktion f / Test, ob Element inkludiert werden soll)

  ;; Was machen wir, wenn man eine neue Datenstruktur ohne conj haben (Java Streams, core.async Channel, GUI Elemente)?


  ;; Wir wollen versuchen das conj loszuwerden.
  ;; Das machen wir, indem wir es als Parameter rausziehen.
  (defn mape [f]
    (fn [step]
      (fn [a e] (step a (f e)))))

  ;; unsere konkrete Map-Funktion für Collection nach Vektor sieht dann so aus
  (defn map4 [f c]
    (reduce ((mape f) conj) [] c))

  ;; reduce geht mit dem Eingabetypen um (Collection)
  ;; conj mit dem Ausgabetypen (hier Vektor)

  (map4 inc [5 2 4])

  ;; das gleiche für Filter
  (defn filtere [p]
    (fn [step]
      (fn [a e] (if (p e)
                 (step a e)
                 a))))

  ;; vergleiche mit map4!
  (defn filter4 [p c]
    (reduce ((filtere p) conj) [] c))

  (filter4 even? (range 10))


  ;; das Ergebnis:
  ;; mape und filtere haben keine Ahnung mehr, wie Ergebnisse kombiniert werden
  ;;
  ;; Die Essenz von map: Gib mir eine step Funktion und ich gebe dir eine
  ;; modifizierte Version von step, bei der f vorher auf die Werte angewandt wird
  ;; das nennt man (in etwa) einen Transducer


  ;; Komposition von Transducern ist einfach comp


  (reduce
   ((comp
     (filtere even?)
     (mape inc)
     (filtere (fn [e] (<= e 10))))
    conj)
   []
   (range 20))

  ;; die Reihenfolge ist ein wenig seltsam:
  ;; erst werden alle geraden Elemente gefiltert, dann erhöht und dann auf <= gefiltert
  ;; das ist die andere Reihenfolge als ohne Transducer:
  ((comp (partial filter even?)
         (partial map inc)
         (partial filter (fn [e] (<= e 10)))) (range 20))

  ;; das liegt daran, dass Funktionen zurück- und durchgegeben werden, die wie ein Stack abgearbeitet werden



  ;; Clojure hat eingebaute Transducer
  ;; (map f) gibt einen Transducer zurück

  ;; (fn [step]
  ;;   (fn [a e] (step a (f e))))

  ;; Transducer sind entkoppelt, sie haben keine Ahnung, was step macht,
  ;; Die einzige Entscheidung, die ein Transducer treffen kann, ist wie oft
  ;; step aufgerufen wird

  ;; Transducer geben eine Reduce-Funktion zurück. Reduce Funktionen bekommen
  ;; einen Akkumulator und einen Wert, und geben einen neuen Akkumulator-Wert
  ;; zurück.

  ;; Wichtig: Der neue Akkumulatorwert muss mit der step Funktion erzeugt werden
  ;; (oder der alte Akkumulatorwert sein)
  ;; Das Element e darf modifiziert werden




  ;; Ist das Alles?
  ;; Leider noch nicht ganz so einfach :-(

  (take-while (fn [e] (<=  (Math/pow 2 e) (* e e e))) (range 2 91))

  ;; take-while muss die Reduktion vorzeitig abbrechen können.
  ;; Kann man das als Transducer schreiben?


  ;; Wenn ein Wert mit reduced gewrappt ist, bricht reduce ab.
  ;; Das ist quasi eine Box, wo der Wert drin verpackt wird,
  ;; und die als Abbruchsignal fungiert.
  ;; Vorher packt reduce die Box aber aus.
  (reduce (fn [a e] (println e) (if (> a 100) (reduced a) (+ a e)))
          (range 10000))


  ;; damit kann auch ein transducer vorzeitig abbrechen
  (defn take-whilee [p]
    (fn [step]
      (fn [a e]
        (if (p e)
          (step a e)
          (reduced a)))))

  (defn take-while4 [p c]
    (reduce ((take-whilee p) conj) [] c))

  (last (take-while4 (fn [e] (<= e 99)) (range 1000)))


  ;; Und nun unser Lieblingsproblem: Zustand!
  ;; Manche Transducer haben lokalen State
  ;; z.B. drop-while oder partition-by

  ;; Hässlicher Riesen-Transducer-Alarm!
  ;; ok, hier kommt ein stateful Reducer in seiner mutable glory:

  (partition-by even? [2 4 3 5 7 8])

  (defn partition-bye [f]
    (fn [step]
      (let [temparray (new java.util.ArrayList)
            pv (volatile! ::none)]
        (fn [a e]
          (let [pval @pv
                val (f e)]
            (vreset! pv val)
            (if (or (identical? pval ::none)
                    (= val pval))
              (do
                (.add temparray e)
                a)
              (let [v (vec (.toArray temparray))]
                (.clear temparray)
                (.add temparray e)
                (step a v))))))))

  (defn partition-by4 [f c]
    (reduce ((partition-bye f) conj) [] c))

  ;; sieht gut aus, *aber*: es fehlt die 14
  (partition-by4 (fn [e] (< 3 (mod e 5))) (range 15))


  ;; noch schlimmer:
  ;; Was passiert, wenn man einen Transducer, der abbricht
  ;; mit einem Transduce koppelt, der State hat?

  (->> (range 20)
       (take-while (fn [e] (< e 8)))
       (partition-by (fn [e] (< 3 (mod e 5)))))

  (reduce
   ((comp
     (take-whilee (fn [e] (< e 8)))
     (partition-bye (fn [e] (< 3 (mod e 5)))))
    conj)
   []
   (range 20))


  ;; Der zustandsbehaftete Transducer muss die Gelegenheit
  ;; bekommen, den Zustand zu flushen.
  ;; -> Alle Transducer (auch die, die nicht mit State behaftet sind) müssen mit vorzeitiger Terminierung umgehen können!

  ;; Flushing wird durch Aufruf einer einstelligen Funktion gehandhabt. Transducer sind also multi-arity.
  ;; Statelose Transducer reichen einfach weiter, was auch immer sie so grad haben.

  (defn mape [f]
    (fn [step]
      (fn
        ([a] (step a)) ;; <--
        ([a e]
         (step a (f e))))))

  ;; Bei manchen Transducern kann das aber komplex werden (nur skizziert)
  (defn partition-bye-bye
    [f]
    (fn [rf]
      (let [a (java.util.ArrayList.)
            pv (volatile! ::none)]
        (fn
          ([result input] #_[... fast wie vorher ...])
          ([result] ;; flush
           (let [result (if (.isEmpty a)
                          result
                          ;; Flush
                          (let [v (vec (.toArray a))]
                            (.clear a)
                            (unreduced (rf result v))))]
             (rf result)))))))

  ;; Es kommt noch (optional) eine 0-stellige Funktion dazu, die einen init-Wert erzeugt.
  ;; Üblicherweise indem step ohne argumente aufgerufen wird.
  ;; Ansonsten weiß man nicht, wo der Wert herkommen soll, da man die Datenstruktur eben nicht kennt!

  (defn mape [f]
    (fn [step]
      (fn
        ([] (step)) ;; <--
        ([a] (step a))
        ([a e]
         (step a (f e))))))

  (conj) ;; ach so, deshalb gibt conj ohne Argumente den leeren Vektor zurück!


  ;; hier ein Aufruf mit transduce
  (transduce
   (comp (map inc)
         (map #(* % %))
         (filter even?))
   conj
   (range 10))

  (as-> (range 10) c
    (map inc c)
    (map #(* % %) c)
    (filter even? c))


  ;; transduce haben wir schon die ganze Zeit gehabt, das ist in etwa die Implementierung
  (defn my-transduce [transducer f init coll]
    (reduce (transducer f) init coll))

  ;; Es gibt noch andere Funktionen, die mit Transducern arbeiten

  ;; into (benutzt transduce)
  (into #{}
        (comp (map inc)
              (map #(* % %))
              (filter even?))
        (range 20))

  ;; anderer stateful transducer: dedupe entfernt aufeinanderfolgende Duplikate

  (dedupe [1 2 3 3 3 4 5])
  (transduce (comp (map inc) (dedupe)) conj [1 2 3 3 3 4 5])

  ;; sequence gibt uns lazyness zurück
  ;; Dumme Idee: (take 10 (transduce (map inc) conj (range)))

  (take 10 (sequence (map inc) (range)))



  ;; Parallele Verarbeitung geht mit Transducern schneller:
  (let [v (vec (range 10000000))]
    (System/gc)
    (time (reduce + (map inc v)))
    (System/gc)
    (time (r/fold + ((map inc) +) v)) ;; <-- reducers (im wesentlichen: paralleles transduce via fork/join)
    )
  (let [v (vec (range 1000000))]
    (time (reduce + (map inc v)))
    (System/gc)
    (time (reduce + (pmap inc v))) ;; <-- pmap hat zu viel Overhead um + zu beschleunigen
    (System/gc)
    (time (r/fold + ((map inc) +) v)))


  ;; Toll, wir haben decomplected, aber map ist jetzt komplizierter geworden :-/

  ;; Der Hauptvorteil ist: Man bekommt extrem viel geschenkt, wenn man neue Datenquellen/Senken schreibt
  ;; Pre-Transducers Version von core.async hatte praktisch jede HOF auf Channeln reimplementiert (map, filter, ...)

  ;; Jetzt braucht man nur transduce (oder sequence, fold) zu implementieren und
  ;; bekommt praktisch alle Sequenz-Funktionen geschenkt.



  )
