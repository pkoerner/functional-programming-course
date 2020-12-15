;; hier eine Namespacedeklaration in (fast) ganzer Pracht:
;; man kann noch mehr Spaß in Kombination mit Host-Interop dort unterbringen.
;; Teile der Erklärungen folgen unten genauer.

(ns repl.08-namespaces
  ;; Clojure wird automatisch eingebunden
  ;; manchmal möchte man aber gewisse Bezeichner überschreiben.
  ;; Hier möchten wir eine andere replace-Funktion.
  (:refer-clojure :exclude [replace])
  ;; Lade die clojure.test Bibliothek. Diese kriegt den Alias t.
  (:require [clojure.test :as t])
  ;; clojure.string wird geladen und replace sowie join werden im Namespace direkt verknüpft.
  ;; clojure.repl wird auch komplett geladen, alle Symbole werden übernommen.
  ;; Nur die dir-Funktion wird, um dem Idiom auf einem UNIX-Systems gerecht zu werden, in ls umbenannt.
  (:use [clojure.string :only (replace join)]
        [clojure.repl :rename {dir ls}])
  ;; wir laden ein paar Java Klassen, auch mehrere aus demselben Paket
  (:import (java.util Date Timer Random)
           java.io.File
           (javax.swing JFrame JPanel)))


(comment

  ;; für source brauchen wir clojure.repl
  (source source)
  ;; damit source die Funktion findet, muss das entsprechende Modul geladen sein.
  ;; Wie geht das nun?

  ;; Laden von Modulen


  ;; 1) Java Klassen
  ;; eine Javaklasse ist nicht unbedingt mit Namen geladen
  Console
  ;; vollständig qualifiziert haben wir darauf Zugriff
  java.io.Console
  ;; wenn wir sie importieren, können wir sie direkt verwenden
  (import java.io.Console)
  Console
  (type Console)
  ;; alle Klassen aus dem java.lang Paket werden automatisch geladen
  String



  ;; 2) Clojure Namespaces
  ;; Namespaces erlauben nur - im Namen und keinen Unterstrich.
  ;; Für den Dateinamen müssen alle - jedoch durch Unterstriche ersetzt werden.
  ;; siehe hier: der Namespace repl.08-namespaces liegt im Ordner repl als 08_namespaces.clj
  ;; Grund ist eine Mischung aus Lisp- und Java-Konvention.
  
  ;; ein Namespace, der nicht geladen ist, kann auch bei vollständig qualifiziertem Symbol nichts machen.
  (repl.greeter.friendly-hello/msg! "John" "Michael")

  ;; require = load
  ;; Datei $CLASSPATH/repl/greeter/friendly_hello.clj
  (require 'repl.greeter.friendly-hello)

  ;; die Datei ist recht klein und hat folgenden Inhalt (kann man auch selbst nachschauen):
  ;; (ns repl.greeter.friendly-hello
  ;;    (:require [clojure.string :as str]))
  ;; (defn msg! [& args]
  ;;    (println "Hello" (str/join " and " args)))

  ;; vollständig qualifiziert geht
  (repl.greeter.friendly-hello/msg! "John" "Michael")
  ;; ohne Namespace aber nicht
  (msg! "John" "Michael")

  ;; Aliasing:
  ;; damit wir nicht so viel tippen müssen, kann man dem Namespace einen Alias verpassen
  (require '[repl.greeter.friendly-hello :as hello])
  (hello/msg! "John" "Jens")

  ;; gibt es nicht, error
  zipper
  ;; die Funktion liegt in clojure.zip, aber ist noch nicht geladen
  clojure.zip/zipper

  ;; geladener Namespace, selbe Regel
  (require 'clojure.zip)
  zipper
  clojure.zip/zipper
  zippy/zipper ;; wurde nicht alias'd

  ;; refer fügt alle Symbole aus dem Namespace dem jetzigen hinzu
  ;; da wir aus dem Clojure-Kern schon next und replace haben, sollen die aber nicht rein.
  ;; remove gibt es auch in beiden, das ist die Warnung, die es dann gibt.
  (refer 'clojure.zip :exclude '[next replace])
  ;; nun geht zipper direkt
  zipper

  ;; use macht require + refer

  ;; file gibt uns das Java File Objekt zu einer Pfad als String - wenn die Bibliothek geladen ist.
  (file "project.clj")
  (use 'clojure.java.io)
  (file "project.clj")
  (type (file "project.clj"))

  ;; jetzt erklärt sich das Namespace Macro (s. oben)

  ;; Man lädt Module in der Regel im Namespace Macro.
  ;; require :as bzw. refer von wenigen Symbolen (es geht auch :refer :all) ist bevorzugt.
  ;; So ist dem Leser klar, wo was herkommt!
  ;; :use verwendet man daher eher möglichst wenig, außer es sind sehr bekannte Bibliotheken
  ;; oder welche, die unmittelbar zusammenhängen (z.B. Wrapper).




  ;; Was ist ein Namespace?


  (def v 4)

  ;; def assoziiert das Symbol v mit einem Var
  ;; Das Var ist ein Container für einen konstanten Wert

  ;; Die Assoziation findet im Namespace statt
  ;; Namespaces sind maps (so in etwa jedenfalls)


  ;; Namespaces und Introspektion
  ;; ------------------------------------------------------------------------------------

  ;; jetzt usen wir den ganzen Kram einfach
  (use 'repl.greeter.friendly-hello)
  (msg! "Kurs" "du daheim")

  ;; hinter dem Namespace steht echt eine Map
  (def m (ns-map 'repl.greeter.friendly-hello))

  ;; eine große Map sogar...
  (keys m)
  (vals m)

  ;; es werden Symbole auf Vars gemapped
  (first m)
  (map type (first m))

  ;; die kann man dann in der Namespacemap nachschlagen...
  (def f (get m (symbol "msg!")))
  ;; und das Ergebnis verwenden
  f
  (f "jens" "michael")

  ;; es gibt einige Namespace-Funktionen
  (apropos "ns-")

  (def n 'repl.08-namespaces)

  ;; öffentlich definiert haben wir recht wenig
  (ns-publics n)
  (keys (ns-publics n))
  ;; insgesamt haben wir hier wenig definiert
  (keys (ns-interns n))

  ;; defn- oder def ^{:private true} schmeißt etwas nur ins interne Mapping
  ;; und gibt es nicht nach außen frei
  (defn- internal [] -1)
  (keys (ns-interns n))

  (def ^{:private true} lol :lol)
  ;; ^{...} ist eine Metadaten-Annotation.
  ;; Dies und Type Hints (ein anderes Mal) sind in der Regel die einzigen, die man braucht.

  ;; die sind tatsächlich intern und nicht öffentlich
  (clojure.set/difference
   (set (keys (ns-interns n)))
   (set (keys (ns-publics n))))

  ;; und welche Aliase haben wir definiert?
  (ns-aliases n)



)
