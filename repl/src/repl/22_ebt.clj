(ns repl.22-ebt
  (:require [clojure.test :as t]))

;; neulich in der Übung:

(defn levenshtein [[h1 & t1 :as s1] [h2 & t2 :as s2]]
  (cond (empty? s1) (count s2)
        (empty? s2) (count s1)
        :otherwise (min
                     (inc (levenshtein t1 s2))
                     (inc (levenshtein s1 t2))
                     (if (= h1 h2)
                       (levenshtein t1 t2)
                       (inc (levenshtein t1 t2))))))


;; Lösung eines anonymen Studierenden in einer früheren Ausgabe der Veranstaltung.
;; Angenommen, das wäre eine supereffiziente Implementierung des Problems.
(defn mapHelper "Which letter should be keept" [hold keep]
  (if (= hold keep)
    hold
    false))

(defn levenshtein-Helper "compares two strings, keeps equal letters and adds all not equal up" [stringSeq1 stringSeq2]
  (count (filter #(= false %) (map mapHelper stringSeq1 stringSeq2))))

(defn prepareString "makes the strings equaly long" [string lengthOfTheOther]
  (let [dif (- (count string) lengthOfTheOther)]
    (if (< dif 0 )
      (concat (seq string) (repeat (- dif) " "))
      (seq string))))

(defn levenschtein "prepare the strings and compares them in two different kinds, return result" [string1 string2]
  (let [prepString11 (prepareString string1 (count string2))
        prepString12 (prepareString string2 (count string1))
        prepString21 (prepareString (reverse string1) (count string2))
        prepString22 (prepareString (reverse string2) (count string1))
        x (levenshtein-Helper prepString11 prepString12)
        y (levenshtein-Helper prepString21 prepString22)]
        (if (< x y)
          x
          y)))

;; ist die zweite Implementierung korrekt?
;; die erste verstehe ich und kann darüber nachdenken...
;; die zweite ist weniger offensichtlich.

;; Eine perfekte Gelegenheit für Tests!

(comment 

;; normalerweise kommt das in eine Datei unter test/ statt src/
;; deftest definiert einen test, is ist eine Assertion im Test
(t/deftest empty-test 
  (t/is (= (levenschtein "" "") 0)))

;; das macht dann normalerweise lein test 
(t/run-tests) ; =>  {:type :summary, :pass 1, :test 1, :error 0, :fail 0}

;; mit are kann man viele ähnliche Assertions etwas kompakter schreiben
(t/deftest examples 
  (t/are [x y] (= (levenshtein x y) (levenschtein x y))
         "" "hallo"
         "Brett" "nett"
         "foo" "ffoo"
         "ffoo" "foo"
         "foon" "fax"
         "simple" "easy"))

(t/run-tests) ; =>  {:type :summary, :pass 7, :test 2, :error 0, :fail 0}

;; Können wir uns *jetzt* sicher sein, dass levenschtein korrekt ist?


;; Das "ebt" im Dateinamen steht für "example-based testing".
;; Solche Tests sind wichtig!
;; Allerdings sind sie nur so gut wie die Beispiele, die ich da hereinwerfe.
;; Vom gesamten Eingaberaum haben wir ungefähr nichts abgedeckt...

;; Wir kommen in den Übungen hierauf zurück!

)



