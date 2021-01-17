(ns repl.07-java-interop
  (:require [clojure.repl :refer [doc]]))


(comment


  ;; Clojure - Java Interop
  ;; ------------------------------------------------------------------------------------

  ;; these are ordinary Java classes
  (type java.util.ArrayList)

  String
  ArrayList

  ;; java.util has to be imported first
  (import java.util.ArrayList)

  ArrayList

  ;; instances can be created via the special form 'new'
  (new ArrayList)
  (type (new ArrayList))
  (doc new)
  ;; also with parameters
  (new String "1")

  ;; or the '.' macro
  (ArrayList.)
  (macroexpand-1 '(ArrayList.))


  ;; static fields can be accessed via '/'

  Math/PI  ;; Math.PI
  System/out
  (System/out) ;; or
  ;; static function call
  (Math/floor 102.3)

  ;; Attributes
  (deftype Foo [x]) ;; generates a Java class with field x

  (def foo (Foo. 12))
  ;; Access instance-attributes with '.' or '.-'
  ;; comes in several flavors
  (. foo x) ;; foo.x
  (. (Foo. 124) x)
  (.x (Foo. 12))
  (macroexpand '(.x (Foo. 12)))


  ;; Access to attributes is preferably done with -x
  ;; This is mandatory in ClojureScript
  ;; to distinguish functions from other values.
  (. (Foo. 122) -x)
  (.-x (Foo. 123))


  ;; Call instance methods with '.'
  (def r (java.util.Random.))

  (. r nextInt)
  (.nextInt r)      ;; r.nextInt()
  (macroexpand '(.nextInt r))
  ;; and the same with parameters once again
  (.nextInt r 100)  ;; r.nextInt(100)

  ;; Chaining of calls
  (.. r nextInt toString hashCode)  ;; r.nextInt().toString().hashCode()
  (macroexpand '(.. r (nextInt 50) toString hashCode))

  ;; Can someone count the parentheses and compare with Java? ;-)



  (doto
   (javax.swing.JFrame. "Tralala")
    (.setSize 800 600)
    (.setVisible true))

  ;; is the same as

  ;; j = new JFrame("Tralala");
  ;; j.setSize(800,600);
  ;; j.setVisible(true);



  ;; Why do Clojure programmers dislike doto?
  ;; It requires mutable objects to make sense!







  ;; ------------------------------------------------------------------------------------

  ;; Java -> Clojure

  ;; idiomatic Java code (apache.commons):

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


  ;; Step 1: Get rid of the types

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


  ;; Step 2: Replace loops with HOF

  ;;  public isBlank(cs) {
  ;;          if (cs == null || (strLen = cs.length()) == 0) {
  ;;              return true;
  ;;          }
  ;;          return every (c in cs) {
  ;;              Character.isWhitespace(c)
  ;;          }
  ;;      }


  ;; Step 3: eliminate edge cases - Clojure can handle nil!

  ;;  public isBlank(cs) {
  ;;         return  every (c in cs) {
  ;;              Character.isWhitespace(c)
  ;;          }
  ;;      }


  ;; Step 4: Clojure syntax

  (defn blank? [cs]
    (every? (fn [c] (Character/isWhitespace c)) cs))

  (blank? "      x      ")
  (blank? "               ")
  (blank? "")
  (blank? nil)
  (blank? "\n\t")

  ;; Let us compare once again:

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




  ;; Records introduce new classes that have certain fixed immutable fields.
  ;; Nevertheless, the classes are still flexible,
  ;; i.e. you can add new fields with assoc
  (defrecord Foo [x y z])

  (def foo (Foo. :a :b :c))
  (type foo)
  (:y foo)
  (.-y foo)
  foo ;; resembles a map somewhat

  (supers (type foo))

  (def bar (assoc foo :n 1))

  bar
  (type bar)

  ;; you get two macros (free of charge) to construct the object
  ;; 1. ->Record, which assigns the fields in order
  (def bar2 (->Foo 3 4 5))
  bar2
  ;; 2. map->Foo, which assigns the attributes by keywords
  (map->Foo {:y 1 :x 3 :z 11})
  (map->Foo {:y 1 :x 3 :z 11 :n 3})

  (:y bar2)

  ;; Literal as reader macro
  (def bar3  #repl.07_java_interop.Foo{:x 1 :y 7 :z 10})
  (:z bar3)




  ;; types are somewhat similar to records

  (deftype Bar [x y z])

  (def bar (Bar. :a :b :c))
  (.-x bar)
  (:x bar)

  ;; deftype does not implement a Map!

  ;; deftypes are quasi records without any of the stuff you get for free
  ;; (except for the constructor)
  ;; - no Map implementation
  ;; - no reader Form
  ;; - no map->Bar
  ;; - and mutable fields upon request




  ;; other constructs

  ;; *reify* is used to implement interfaces
  ;; the first argument of the implemented functions is the object,
  ;; which is created by reify

  ;; reify only allows to implement Java interfaces
  ;; if you want to inherit from classes, you need a *proxy*.

  ;; Use case for proxy:
  ;; some Java methods expect a certain class
  ;; e.g. a concrete subclass of a reader


  ;; Last thingy: *gen-class*
  ;; also exists as key :gen-class in namespace declarations
  ;; generates a class file at compile time
  ;; which can then also be called from Java!
  )
