(ns repl.07-java-interop
  (:require [clojure.repl :refer [doc]]))


(comment


  ;; Clojure - Java Interop
  ;; ------------------------------------------------------------------------------------

  ;; es gibt ganz normale Javaklassen
  (type java.util.ArrayList)

  String
  ArrayList

  ;; java.util muss man erst importieren
  (import java.util.ArrayList)

  ArrayList

  ;; Instanzen erzeugen geht einmal mit der Special Form new
  (new ArrayList)
  (type (new ArrayList))
  (doc new)
  ;; auch mit Parametern
  (new String "1")

  ;; oder mit dem . Macro
  (ArrayList.)
  (macroexpand-1 '(ArrayList.))


  ;; auf statische Felder kann man mit / zugreifen

  Math/PI  ;; Math.PI
  System/out
  (System/out) ;; oder auch
  ;; statischer Funktionsaufruf
  (Math/floor 102.3)

  ;; Attribute
  (deftype Foo [x]) ;; Java Klasse mit Feld x generieren

  (def foo (Foo. 12))
  ;; Zugriff auf ein Instanz-Attribut mit . bzw .-
  ;; geht in mehreren Geschmacksrichtungen
  (. foo x) ;; foo.x
  (. (Foo. 124) x)
  (.x (Foo. 12))
  (macroexpand '(.x (Foo. 12)))


  ;; Zugriff auf Attribute besser mit -x
  ;; In ClojureScript ist das Pflicht, um Funktionen von anderen Werten unterscheiden zu können.
  (. (Foo. 122) -x)
  (.-x (Foo. 123))


  ;; Instanz-Methoden aufrufen: mit .
  (def r (java.util.Random.))

  (. r nextInt)
  (.nextInt r)      ;; r.nextInt()
  (macroexpand '(.nextInt r))
  ;; und auch das mit Parametern
  (.nextInt r 100)  ;; r.nextInt(100)

  ;; Chaining von Aufrufen
  (.. r nextInt toString hashCode)  ;; r.nextInt().toString().hashCode()
  (macroexpand '(.. r (nextInt 50) toString hashCode))

  ;; Kann jemand mal die Klammern zählen und mit Java vergleichen? ;-)



  (doto
    (javax.swing.JFrame. "Tralala")
    (.setSize 800 600)
    (.setVisible true))

  ;; ist das gleiche wie

  ;; j = new JFrame("Tralala");
  ;; j.setSize(800,600);
  ;; j.setVisible(true);



  ;; Warum mögen Clojure Programmierer doto nicht besonders?
  ;; Das braucht mutable Objekte, um Sinn zu ergeben!







  ;; ------------------------------------------------------------------------------------

  ;; Java -> Clojure

  ;; idiomatischer Java code (apache.commons):

  ;;  public static boolean isBlank(final CharSequence cs) {
  ;;          int strLen;
  ;;          if (cs == null || (strLen = cs.length()) == 0) {
  ;;              return true;
  ;;          }
  ;;          for (int i = 0; i < strLen; i++) {
  ;;              if (Character.isWhitespace(cs.charAt(i)) == false) {
  ;;                  return false;
  ;;              }
  ;;          }
  ;;          return true;
  ;;      }


  ;; Schritt 1: Weg mit den Types

  ;;  public isBlank(cs) {
  ;;          if (cs == null || (strLen = cs.length()) == 0) {
  ;;              return true;
  ;;          }
  ;;          for (i = 0; i < strLen; i++) {
  ;;              if (Character.isWhitespace(cs.charAt(i)) == false) {
  ;;                  return false;
  ;;              }
  ;;          }
  ;;          return true;
  ;;      }


  ;; Schritt 2: HOF statt Schleife

  ;;  public isBlank(cs) {
  ;;          if (cs == null || (strLen = cs.length()) == 0) {
  ;;              return true;
  ;;          }
  ;;          return every (c in cs) {
  ;;              Character.isWhitespace(c)
  ;;          }
  ;;      }


  ;; Schritt 3: Corner Cases eliminieren - Clojure kann mit nil umgehen!

  ;;  public isBlank(cs) {
  ;;         return  every (c in cs) {
  ;;              Character.isWhitespace(c)
  ;;          }
  ;;      }


  ;; Schritt 4: Clojure Syntax

  (defn blank? [cs]
    (every? (fn [c] (Character/isWhitespace c)) cs))

  (blank? "      x      ")
  (blank? "               ")
  (blank? "")
  (blank? nil)
  (blank? "\n\t")

  ;; Vergleichen wir noch einmal:

  (defn blank? [cs]
    (every? #(Character/isWhitespace %) cs))

  ;;  public static boolean isBlank(final CharSequence cs) {
  ;;          int strLen;
  ;;          if (cs == null || (strLen = cs.length()) == 0) {
  ;;              return true;
  ;;          }
  ;;          for (int i = 0; i < strLen; i++) {
  ;;              if (Character.isWhitespace(cs.charAt(i)) == false) {
  ;;                  return false;
  ;;              }
  ;;          }
  ;;          return true;
  ;;      }




  ;; Records führen neue Klassen ein, die bestimmte feste immutable Felder haben.
  ;; Trotzdem sind die Klassen immer noch flexibel, d.h. man kann neue Felder mit
  ;; assoc hinzufügen
  (defrecord Foo [x y z])

  (def foo (Foo. :a :b :c))
  (type foo)
  (:y foo)
  (.-y foo)
  foo ;; sieht fast wie eine Map aus

  (supers (type foo))

  (def bar (assoc foo :n 1))

  bar
  (type bar)

  ;; man bekommt zwei Macros geschenkt, um das Objekt zu konstruieren
  ;; 1. ->Record, was die Felder in Reihenfolge füllt
  (def bar2 (->Foo 3 4 5))
  bar2
  ;; 2. map->Foo, was die Attribute nach Keywords einsetzt
  (map->Foo {:y 1 :x 3 :z 11})
  (map->Foo {:y 1 :x 3 :z 11 :n 3})

  (:y bar2)

  ;; Literal als Readermacro
  (def bar3  #repl.07_java_interop.Foo{:x 1 :y 7 :z 10})
  (:z bar3)




  ;; Typen sind so ähnlich wie Records

  (deftype Bar [x y z])

  (def bar (Bar. :a :b :c))
  (.-x bar)
  (:x bar)

  ;; deftype implementiert keine Map!

  ;; deftype sind quasi Records ohne Geschenke (außer dem Konstruktor)
  ;; - keine Map Implementierung
  ;; - keine Reader Form
  ;; - kein map->Bar
  ;; - auf Wunsch auch noch mutable Felder




  ;; andere Konstrukte

  ;; *reify* verwendet man, wenn man "mal eben" ein Interface implementieren muss
  ;; erstes Argument von Funktionen ist dabei das Objekt, was durch reify erzeugt wird

  ;; reify erlaubt es nur Java Interfaces zu implementieren
  ;; wenn man von Klassen erben will, benötigt man einen *proxy*

  ;; Anwendungsfall für Proxy:
  ;; manche Java Methoden erwarten eine bestimmte Klasse
  ;; z.B. eine konkrete Subklasse von einem Reader


  ;; letztes Ding: *gen-class*
  ;; gibt es auch als key :gen-class in Namespace Deklarationen
  ;; generiert zur Compilezeit ein Classfile
  ;; dieses kann man dann auch aus Java aufrufen!

  )
