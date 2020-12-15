(ns repl.02-data)

(comment
;; besonders wichtig, wenn wir mit unendlichen Datenstrukturen arbeiten

(set! *print-length* 20)

;; Datenstrukturen in Clojure


;; ---------------------------------------------------------
;; Funktionen / Higher Order Functions
;; ---------------------------------------------------------


;; Wiederholung: apply

;; geht nicht: Typfehler (Liste statt Integer)
(+ 4 [1 2 3])

;; apply packt die Liste aus
(apply + 4 [1 2 3])


;; apply kann auch mehrere Argumente "fest verdrahten"
(apply + 4 1 2 3 [42])

;; das letzte muss aber eine Liste sein
(apply + 4 1 2 3)


;; Def: Higher Order Function - Funktion, die eine Funktion als Parameter bekommt oder zurückgibt

;; Standard-Beispiele (bereits gesehen): map filter reduce

(map inc (range 2 7))
(map + [1 2 3] [3 4 5] [4 6])

(filter (fn [x] (< 2 x 6)) [1 2 3 4 5 6])

(reduce * 1 (range 2 7))
(reduce * (range 2 7))


;; reduce bezeichnet man als "Mutter aller HOF"
;; es kann verwendet werden, um z.B. eine Version von map und filter zu erstellen

(defn mymap [f c]
  (reduce (fn [a e] ;; a steht für Akkumulator, e für Element
            (conj a (f e)))
          []
          c))
(mymap inc [1 2 3])


;; gibt einen Vektor anstatt einer Liste zurück, aber gut genug
(type (mymap inc [1 2 3]))
;; das echte Map ist auch noch lazy, unseres nicht
(type (map inc [1 2 3]))


;; Filter geht auch
(defn myfilter [pred c]
  (reduce (fn [a e]
            (if (pred e)
              (conj a e) ;; einfügen
              a)) ;; nicht einfügen
          []
          c))
(myfilter even? [1 2 3 4])

;; mapcat ist map + concat auf den Ergebnissen
(map (fn [e] (range 1 e)) [2 3 4])
(mapcat (fn [e] (range 1 e)) [2 3 4])

;; also das gleiche wie
(apply concat
       (map (fn [e] (range 1 e)) [2 3 4]))

;; Übung: mit reduce schreiben




;; ---------------------------------------------------------
;; Lazyness Selbst gemacht
;; ---------------------------------------------------------

;; Range mit Datenstrukturen

;; folgende API, etwas anders als (range)
 ; (ranje 0) ; (0 1 2 3 4 ...)
 ; (ranje 10) ; (10 11 12 ...)
;; ranje soll also etwas zurückgeben, was alle Zahlen ab dem Startwert gibt

;; also eher sowas wie
  (drop 10 (range))

;; das definieren wir wie folgt:
;; es wird nur das erste Element ausgewertet.
;; den Rest verzögern wir mit Hilfe einer Funktion
(defn ranje [n]
  {:first n
   :rest (fn [] (ranje (inc n)))})
;; entweder wir nehmen das erste Element
(defn head [r]
  (:first r))
;; oder rufen die Funktion auf, die den rest generiert
(defn tail [r]
  ((:rest r)))

(head (tail  (tail (ranje 0))))

;; hier verwenden wir noch irgendwelche Datenstrukturen (die Map)
;; im nächsten Schritt werden wir die jetzt los

;; Range a la Houdini - jetzt mit 20 % mehr magic

;; wir verstecken die Auswertung vom tail immer noch hinter einer Funktion
(defn ranje2 [head]
  (fn [head?] (if head? 
                head 
                (ranje2 (inc head)))))

(defn head2 [ranje-fn]
  (ranje-fn true))

(defn tail2 [ranje-fn]
  (ranje-fn false))

(head2 (tail2  (tail2 (ranje2 0))))

;; grundsätzliche Idee:
;; eine lazy Datenstruktur hält einen Pointer auf den Rest und weiß,
;; was für Funktionen noch auf den Elementen aufgerufen werden müssen



;; Was man beachten muss:
;;   1. Keine Seiteneffekte

;; lazyness funktioniert, wie man es erwartet
(def lz (map (fn [e] (println :doh) (inc e))
             (take 100 (range))))
(nth lz 4)
(nth lz 4)
(nth lz 31)
(nth lz 32)

;; wir definieren dieselbe Sequenz etwas anders
(def lz (map (fn [e] (println :dah) (inc e))
             (range 0 100)))


(nth lz 4)
(nth lz 4)
;; huch
(nth lz 31)
(nth lz 32)


;; 32 Elemente auf einmal zu verarbeiten ist auf aktuellen Prozessoren in der Regel effizienter
;; es ist aber nicht offensichtlich, wann dieses 32er-Chunking passiert.
;; das hängt von der Datenstruktur ab!
;; das heißt aber auch, dass manchmal viele Seiteneffekte auf einmal feuern...
;; also ganz vermeiden!


;;   2. Bestimmte Fragen bei unendlichen Sequenzen vermeinden:
;;      - Was ist das letzte Element? (last (range)) dauert ein wenig...
;;      - Wie lang ist die Sequenz? (count (range)) dauert auch...



  )

