(ns repl.14-spec
  (:use clojure.repl)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [schema.core :as schema]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

;; benötigt ein aktuelles Clojure + test.check (siehe project.clj)


(set! *print-length* 10)

(comment

  ;; Fallbeispiel:
  ;; Analyse von Informatik 1 Übungen - geben manche Korrektoren mehr Punkte als andere?
  ;; die Daten sind echt, aber anonymisiert
  (defn read-csv [filename]
    (with-open [in-file (io/reader filename)]
      (let [[header & data] (doall (csv/read-csv in-file))
            header (repeat (map keyword header))]
        (map zipmap header data))))

  ;; riesige CSV Datei
  (read-csv "info1.csv")

  ;; Was wir eigentlich haben wollen:
  ;; Eine Map-artige Struktur, die pro Blatt einen Korrektor auf die Durchschnittspunktzahl abbildet
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

  ;; also gruppieren wir Daten nach Blatt
  (defn nach-studi-blatt-gruppieren [data]
    (group-by #(select-keys % [:matnr :blatt :korrektor]) data))

  ;; und nach Korrektor
  (defn nach-korrektor-gruppieren [data]
    (group-by #(select-keys % [:blatt :korrektor]) data))

  ;; addieren alles auf
  (defn aufgaben-summieren [[k vs]]
    (let [sum (reduce (fn [a e] (+ a (:punkte e))) 0 vs)]
      [(assoc k :summe sum)]))

  ;; berechnen den Durchschnitt
  (defn avg-berechnen [[k vs]]
    (let [sum (reduce (fn [a e] (+ a (:summe e))) 0 vs)
          n (count vs)]
      [(assoc k :avg (double (/ sum n)))]))

  ;; und fügen alles Zusammen
  (defn durchschnitt-pro-korrektor-und-blatt
    "Extrahiert die durchschnittlichen Punkte pro Korrektor gruppiert nach Übungsblatt"
    [data]
    (as-> data d
      (nach-studi-blatt-gruppieren d)
      (map aufgaben-summieren d)
      (nach-korrektor-gruppieren d)
      (map avg-berechnen d)
      (group-by :blatt d)
      (for [[_ e] d]
        (map
         ;; {:blatt 1 :korrektor "lol" :avg 10} -> ["lol" 10]
         (fn [{:keys [korrektor avg]}] [korrektor avg])
         data)
        )))

  (durchschnitt-pro-korrektor-und-blatt (read-csv "info1.csv"))
  ;; geht nicht :-(

  ;; jetzt können wir Fehler suchen (es gibt mehrere!)

  ;; Es wäre schon nett
  ;;  - Wenn wir nicht auf println angewiesen wären beim Debugging
  ;;  - Wenn wir formale Aussagen über Eingabe/Ausgabe Strukturen treffen könnten
  ;;  - Wenn man Aussagen über das Verhalten von Funktionen machen könnte.
  ;;  -  ...
  ;; das sind nun halt Nachteile einer dynamischen Programmiersprache!


  ;; kleineres Beispiel:
  (cons 3 [4 5]) ;; okay
  (conj 3 [4 5]) ;; Argumente falsch rum

  ;; ClassCastException ... RLY ?!?


  ;; Yeah, well, I'm gonna build my own conj with blackjack and hookers.
  ;; In fact, forget the conj

  (defn conj' [coll ele]
    (assert (sequential? coll) "First argument must be a collection")
    (conj coll ele))

  ;; gute Fehlermeldungen? In MEINEM Clojure?
  (conj' 3 [4 5])


  ;; Seit Clojure 1.1 gibt es Pre- und Postconditions.
  ;; Das ist spezielle Syntax. Es muss eine Map nach den Parametern sein, und es muss ein weiterer Body folgen.
  ;; Es ist zumindest etwas schöner, wenn man mehrere Bedingungen spezifiziert.

  (defn conj' [coll ele]
    {:pre [(instance? Iterable coll)]}
    (conj coll ele))

  (conj' 3 [4 5])

  ;; Intervallschachtelung zur Wurzelberechnung
  (defn interval-sqrt
    ([n] (interval-sqrt n 0 n 1e-5))
    ([n a b eps]
     (let [m (/ (+ a b) 2)]
       (cond (< (- b a) eps) a
             (> (* m m) n) (recur n a m eps)
             :else (recur n m b eps)))))

  (interval-sqrt 4)
  (interval-sqrt 0)
  ;; eigentlich ist die Eingabe hier falsch
  (interval-sqrt -4)


  ;; wir können also Preconditions und Postconditions spezifizieren.
  ;; wenn die Eingabe größer null ist, ist die Ausgabe wirklich nach genug an der Wurzel dran
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

  ;; Das ist Design-by-contract.

  ;; Assertions sind nicht so gut geeignet für komplexe Strukturen/Zusammenhänge

  ;; Übung:

  (defn foo [x]
    {:pre []})

  ;; Eingabe soll eine Map sein, die Keywords auf Vektoren von Maps
  ;; abbildet. Diese Maps bilden Zahlen auf Strings oder Booleans ab.

  ;; Und nun als Precondition schreiben!

  ;; Viel Spass!



  ;; 2013 ... And along comes schema ...

  ;; "One of the difficulties with bringing Clojure into a team is the
  ;; overhead of understanding the kind of data [...] that a function
  ;; expects and returns. While a full-blown type system is one solution
  ;; to this problem, we present a lighter weight solution: schemas."


  ;; es gibt eingebaute Schemas
  (schema/validate schema/Int 4)
  (schema/validate schema/Str 4)
  (schema/validate schema/Str "5")

  ;; und man kann auch Container parametrisieren
  (schema/validate [schema/Int] [2 3 4])
  (schema/validate [schema/Int] [2 3 "s"])

  (schema/validate {:name schema/Str
                    :addresse schema/Str}
                   {:name "Jens"
                    :addresse "Universitätsstr.1"})

  (schema/validate {:name schema/Str
                    :addresse schema/Str}
                   {:name "Jens"
                    :addresse "Universitätsstr.1"
                    :plz 40225
                    :ort "Düsseldorf"})

  ;; Composition!
  ;; Schemas sind nur Daten
  (def IntVec [schema/Int])
  (def MMap {schema/Keyword {schema/Any IntVec}})
  MMap

  (schema/validate MMap {:a {"x" [1 2]}})
  (schema/validate MMap {:a {:q [1 2]}})
  (schema/validate MMap {:a {:fail "true"}})

(sequential? "true")
(seq? "true")
(seqable? "true")


  ;; Unsere kleine Übung von eben:
  ;; Eingabe soll eine Map sein, die Keywords auf Vektoren von Maps
  ;; abbildet. Diese Maps bilden Zahlen auf Strings oder Booleans ab.

  (def int->bool-or-string {schema/Int (schema/either schema/Str
                                                      schema/Bool)})
  (def foo-schema {schema/Keyword [int->bool-or-string]})

  (schema/validate foo-schema
                   {:foo [{1 true} {2 "2" 3 false}]})

  (schema/validate foo-schema
                   {:foo [{1 true} {2 "2" :a false}]})
  ;; Das war erstaunlich okay.


  ;; Nicht (nur) Struktur-Checks:
  ;; man kann auch beliebige Prädikate verwenden
  (def EvenInt (schema/both  schema/Int (schema/pred even? :gerade)))
  (schema/validate EvenInt 2)
  (schema/validate EvenInt 3)
  (schema/validate EvenInt 2.0)



  ;; Schema hat schnell Verbreitung gefunden, aber es hat ein Problem
  (schema/validate {:name schema/Str
                    :addresse schema/Str}
                   {:name "Jens"
                    :addresse "Universitätsstr.1"
                    :plz 40225
                    :ort "Düsseldorf"})


  ;; Schemas sind geschlossen.
  ;; Erweiterung ist nur durch Überschreiben machbar.
  ;; wenn also zusätzliche Information (zu allem notwendigen) dazukommt,
  ;; dann funktioniert es nicht mehr...


  ;; 2016 - Clojure 1.9 bekommt specs

  ;; clojure.spec ist Teil von Clojure (war vorher eine Bibliothek)
  ;; es gab dafür kleine Änderungen an Macro-Expansion und Doc-Strings und brauchte
  ;; automatische Verbesserung der Fehlermeldungen

  ;; ... macht den Editor LightTable kaputt :-(
  ;; Der war für die Vorlesung ganz angenehm, da alte Auswertungen angezeigt bleiben.
  ;; LightTable dependet auf eine alte Version von Clojurescript.
  ;; da gibt es folgenden Fehler in einer Namespace-Deklaration

  ;; (ns cljs.source-map.base64-vlq
  ;;   (require [clojure.string :as string] ;; require ist kein Keyword!
  ;;            [cljs.source-map.base64 :as base64]))

  ;; das wird von clojure.spec gefangen und lief vorher "zufällig" durch



  ;; falsche Funktionsdeklaration
  (defn oop!oop!oop! (x)
    (println x))


  ;; auf Clojure 1.10 upgraden
  (doc defn)
  ;; die spec daran wurde verletzt

  ;; Jede Clojure Funktion, die ein Argument bekommt und einen
  ;; truthy Wert liefert ist eine valide spec.
  ;; valid? prüft, ob ein Wert eine spec erfüllt

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

  ;; specs registrieren macht das spec-def
  ;; Name der spec ist ein vollständig qualifiziertes Keyword (und muss es auch sein!),
  ;; damit man bei gleichen Bezeichnungen weiß, welches gemeint ist

  (s/def ::postal-code int?)
  (s/def ::street string?)
  (s/def ::city string?)

  ;; Danach kann das Keyword wie eine spec benutzt werden
  ;; (::postal-code 1) verhält sich aber nicht anders als sonst!

  (s/valid? ::postal-code 4)
  (s/valid? ::postal-code "4")

  ;; Und warum ist es fehlgeschlagen?

  (s/explain ::postal-code 4)
  (s/explain ::postal-code "4")
  (s/explain-data ::postal-code "4")


  ;; Kombinatoren
  ;; man kann eine Funktion schreiben, die zwei Dige prüft
  (s/def ::big-int-p (fn [e] (and (int? e) (< 1000 e))))
  (s/explain ::big-int-p 320)

  ;; oder das spec-und verwenden für präzisere Fehlermeldungen
  (s/def ::big-int (s/and int? (fn [x] (< 1000 x))))
  (s/explain ::big-int 320)

  (s/explain ::big-int 9.0) ;; short circuit - nur der erste Fehler wird gemeldet

  ;; es gibt auch ein oder, da muss man den Alternativen Namen geben
  (s/def ::even-or-big (s/or :even even?
                             :big ::big-int))
  (s/explain ::even-or-big 333)

  (s/explain ::not-existing-spec "9")

  ;; Ein Spec ist:
  ;; - Ein Prädikat
  ;; - Eine Komposition aus specs (mit Hilfe von z.B. or oder and erzeugt)

  ;; Achtung! Prädikate sind Specs, die Umkehrung gilt nicht!

  ((s/and int? even?) 12)
  (s/valid? (s/and int? even?) 12)


  ;; Datenstrukturen
  ;; Drei typische Einsatzarten

  ;; a) homogene (oft grosse/unbeschränkte) Sammlung von Daten
  ;; b) heterogene (struct/objekt-ähnliche) Sammlung von Daten
  ;; c) Syntax (die Reihenfolge ist wichtig)


  ;; Beispiele

  ;; a) Homogene Sammlungen
  {"Etienne Weitzel" 67.25
   "Edelfriede Künzel" 74.10344827586206
   "Nushi Happel" 49.44186046511628}

  [1.0 -9.3 6.2]

  ;; b) Structs
  {:name "Jens Bendisposto"
   :plz 42103
   :ort "Wuppertal"}

  ;; c) Syntax
  ;; Funktion + Argumente
  (+ 1 2 3)

  [1.0 -9.3 6.2]
  ;; Das war eben doch eine homogene Sammlung?
  ;; Es kommt drauf an:
  ;; Sind es drei Messpunkte von Temperaturen, so ist es eine homogene Sammlung.
  ;; Sind es RGB-Werte, oder Höhe-Breite-Tiefe Maße, ist es Syntax!

  ;; Syntax ist komplex und sollte vermieden werden... bei RGB oder HBT wäre eine Map klarer.



  ;; und die Specs dazu

  ;; (a) Homogene Sammlungen

  (s/def ::punkte-eintrag number?)
  ;; feste Menge an Korrektoren
  (s/def ::korrektor #{"Etienne Weitzel" "Edelfriede Künzel" "Nushi Happel"})

  (s/def ::punkte-liste (s/coll-of ::punkte-eintrag))
  (s/def ::korrektor-punkte (s/map-of ::korrektor ::punkte-liste))
  (s/def ::korrektor-durchschnitt (s/map-of ::korrektor double?))

  (s/valid? ::korrektor-punkte {"Etienne Weitzel" [40 42 38]
                                "Edelfriede Künzel"  []
                                "Nushi Happel" [41 50 10]})

  (s/explain ::korrektor-punkte {"Etienne Weitzel" [40  3 "42" 34] ;; hier schmuggelt sich ein String ein
                                 "Edelfriede Künzel"  []
                                 "Nushi Happel" [41 50 10]})

  (s/valid? ::korrektor-durchschnitt  {"Etienne Weitzel" 67.25
                                       "Edelfriede Künzel" 74.10344827586206
                                       "Nushi Happel" 49.44186046511628})

  (s/valid? ::korrektor-durchschnitt  {"Etienne Weitzel" 80
                                       "Jens Bendisposto" 74.5
                                       "Nushi Happel" 49.44})

  ;; wer hätte spontan den Jens gesehen?
  (s/explain  ::korrektor-durchschnitt  {"Etienne Weitzel" 80
                                         "Jens Bendisposto" 74.5
                                         "Nushi Happel" 49.44})

  ;; Warum ["Jens Bendisposto" 0] ?
  ;; das ist der Pfad in der Datenstruktur zum kaputten Eintrag (z.B. via get-in extrahierbar)



  ;; (b) Heterogene Sammlungen / Structs

  ;; keys spezifiziert eine Map
  ;; :req sind Schlüssel, die notwendig sind, :opt sind optional.
  ;; Die Keywords müssen so die Schlüssel sein, und der Wert dieselbe Spec erfüllen.
  (s/def ::address (s/keys :req [::street ::postal-code ::city]
                           :opt [::state]))

  ;; Syntax: man kann einen Namespace allen Keywords in der Map unterschmuggeln - das spart Tipparbeit
  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :city "Düsseldorf"})

  (s/explain ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :city "Düsseldorf"})
  ;; keine Postleitzahl

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "40225"
                                     :city "Düsseldorf"})

  (s/explain ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                      :postal-code "40225"
                                      :city "Düsseldorf"})
  ;; Postleitzahl war als int definiert

  ;; Eigentlich ist es uns egal ob es ein int ist, oder ein String der einen int enthält.
  ;; Beides ist int genug.
  (s/def ::postal-code (s/or :int int?
                             :string-int (comp int? read-string)))

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "40225"
                                     :city "Düsseldorf"})

  (s/valid? ::address #:repl.14-spec{:street "Universitätsstr. 1"
                                     :postal-code "lol nope"
                                     :city "Düsseldorf"})


  ;; (s/valid? ... ) ist true ... now what?
  ;; conform kann angeben, welche Alternative gewählt wurde und
  ;; transformiert die Eingabe in einen "conformed value"

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

  ;; da kommen wir gleich drauf zurück...

  ;; eines noch: Keywords ohne Namespace? req-un (das steht für required unqualified)
  (s/conform (s/keys :req-un [::a ::b]
                     :opt-un [::c])
             {:a 1 :b 2})



  ;; (c) Syntax / Sequenzen
  ;; cat ist eine Konkatenierung. Alle Elemente brauchen einen Namen.
  (s/def ::voxel (s/cat :x number? :y number? :z number?))

  (s/valid? ::voxel [1 2 3])
  (s/explain ::voxel [1 2])
  (s/explain ::voxel [1 2 :drei])

  ;; zurück zu conform...
  (s/conform ::voxel [2.8 0 1/2])

  (s/def ::temperature-value (s/cat :volume int? :scale #{:C :F :K}))
  (s/def ::time-series (s/* ::temperature-value))

  (s/conform ::time-series [10 :C 20 :F 70 :F 380 :K])

  ;; Man kann schon kleine Parser mit conform bauen!
  ;; Dann kann man mit demselben Code
  ;;  - die Struktur der Daten verifizieren
  ;;  - Daten in eine andere Struktur überführen

  ;; Die Regex Expressions sind:
  ;; Konkatenation: cat
  ;; Alternative: alt
  ;; 0-beliebig oft: *
  ;; 1-beliebig oft: +
  ;; 0 oder 1 mal: ?
  ;; Zusatzconstraint: &


  ;; exercise benötigt test.check Generatoren und kann dann Werte generieren,
  ;; die mögliche Eingaben zu einer Spec generieren.
  ;; Sehr nützlich, um ein Gefühl dafür zu bekommen, wie die Struktur aussieht!
  ;; dabei werden direkt der Wert und der conformed value zusammen generiert.
  (s/exercise (s/alt :option1 (s/cat :at-least-a-string (s/+ string?)
                                     :num-or-not (s/? int?))
                     :option2 (s/* int?)))


  ;; das ist relativ mächtig: Strings und Strings mit gerader Länge
  (s/def ::strings (s/* string?))
  (s/def ::even-strings (s/& ::strings  #(even? (count %))))
  ;; oder nicht als Regex
  ;; (s/def ::even-strings (s/and ::strings  #(even? (count %))))

  (s/exercise ::strings)
  (s/exercise ::even-strings)


  ;; Achtung! Es wird eine flache Sequenz verarbeitet!
  ;; Nesting muss man explizit mit (spec ...) schreiben

  ;; Vergleiche
  (s/exercise (s/cat :data (s/+ ::voxel)))
  (s/exercise (s/cat :data (s/+ (s/spec ::voxel))))



  ;; ---

  ;; und wie benutzt man das jetzt?

  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b] ,,,) ;; Übung: implementieren


  ;; Die Argumentliste ist auch nur eine sequentielle Datenstruktur!

  ;; passende Spec dazu
  (s/def ::index-of-args (s/cat :src string? :search string?))
  (s/conform ::index-of-args ["Hello World" "orl"])
  ;; unform ist übrigens die Inverse von conform
  (s/unform ::index-of-args {:search "es geht auch so" :src "toll"})
  (s/unform ::index-of-args (s/conform ::index-of-args ["Hello World" "orl"]))


  ;; eine Möglichkeit: Assert

  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b]
    (s/assert ::index-of-args [a b]))


  (my-index-of "aaa" "b")
  (my-index-of 2 4)
  ;; ups


  ;; Spec-Asserts sind ein Werkzeug, um Aufrufer zu debuggen.
  ;; Im Produktivcode will man die vielleicht nicht haben.
  ;; Deshalb sind sie billiger, wenn sie nicht eingeschaltet sind (Standard) :-)
  (s/check-asserts true)

  ;; nochmal
  (my-index-of "aaa" "b")
  (my-index-of 2 4) ; aha!


  ;; und wieder ausmachen
  (s/check-asserts false)



  ;; es geht aber ein wenig besser:
  ;; man kann Funktionen mit Specs versehen!
  ;; :args ist der Argumentvektor
  ;; :ret gibt den return-value an
  ;; :fn setzt die conformed-values von Argument und Rückgabe in Zusammenhang

  (s/fdef my-index-of
          :args ::index-of-args
          :ret nat-int?
          :fn (fn [cfd] (<= (-> cfd :ret)
                            (-> cfd :args :src count))))

  ;; wir klauen einfach die andere Implementierung
  (defn my-index-of [a b]
    (clojure.string/index-of a b))

  ;; guck mal, wir haben eine Spec dadran!
  (doc my-index-of)


  ;; Instrumentieren
  (my-index-of nil "2")
  ;; ich dachte, ich krieg jetzt Bescheid?
  ;; ach so, erstmal anschalten!

  ;; die Funktion ist die einzige, die instrumentiert werden kann
  (stest/instrumentable-syms)

  (stest/instrument `my-index-of) ;; für den Namespace: syntax quote!

  ;; Prüfe bei allen aufrufen, ob die Parameter mit :args konform sind
  (my-index-of nil "2")

  ;; Noch ein falscher Aufruf:
  (my-index-of "" "0")



  ;; instrument checkt nur die Precondition (nicht die Postcondition, nicht die Funktion)
  ;; vgl. Design-by-contract
  ;; Postcondition verletzt: Meine Schuld
  ;; Precondition verletzt: Deine Schuld!

  ;; instrument ist dazu da, um Aufrufer zu debuggen


  ;; Macros mit Spec versehen
  ;; genauso wie Funktionen, der Macroexpander checkt die specs
  ;; Unterschied zu Funktionen: kein instrument erforderlich

  (declare 100)

  (s/fdef clojure.core/declare
          :args (s/cat :names (s/* simple-symbol?))
          :ret any?)

  (declare 100)




  ;; Testing

  ;; warum jetzt :ret und :fn?
  ;; So testet man eine Funktion:
  (stest/check `my-index-of)

  ;; Und so bekommt man vernünftigen Output:
  (->> (stest/check `my-index-of) stest/summarize-results)

  ;; hier wurde der Fehler von oben automatisch gefunden!
  (my-index-of "" "0")


  ;; FIX ME PLEASE
  (defn my-index-of
    "Searches for b in a, returns index or -1 if not found"
    [a b]
    ; Übung
    )

  (s/fdef my-index-of
          :args ::index-of-args
          :ret int?
          :fn (fn [cfd]
                (<= (-> cfd :ret) (-> cfd :args :src count))))


  (->> (stest/check `my-index-of) stest/summarize-results)


  (stest/check `my-index-of)


  ;; Mehr Tests
  (stest/check `my-index-of {:clojure.spec.test.check/opts {:num-tests 5000}})


  ;; Generatoren
  ;; manche Sachen wie (read ...) sind nicht umgekehrbar
  (s/exercise ::address)

  ;; der fehlt auch noch
  (s/def ::state string?)
  ;; dann braucht man halt seinen eigenen Generator:
  (defn gen-plz []
    (gen/one-of
     [(s/gen nat-int?)
      (gen/fmap str (s/gen nat-int?))]))


  ;; Generatoren kann man dann mitgeben
  (s/exercise ::address 10 {::postal-code gen-plz})


  )

