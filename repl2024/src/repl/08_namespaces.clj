;; 1 Namespace Declaration
;;   - import
;; 2 Importing Namespaces  
;;   - refer-clojure
;;   - require as, refer
;;   - use
;; 3 What is a namespace?


;; 1 Namespace Declaration
;; -----------------------

;; A namespace declaration in (nearly) all its glory:
;; You can put even more stuff there in combination with host interop.
;; Parts of the explanations follow in more detail below.

(ns repl.08-namespaces
  ;; Clojure is automatically imported
  ;; but sometimes you want to overwrite certain identifiers.
  ;; Here we would like to use another replace function.
  (:refer-clojure :exclude [replace])
  ;; Load the clojure.test library and give it the alias 't'.
  (:require [clojure.test :as t])
  ;; clojure.string is loaded and replace and join are
  ;; directly linked in the namespace.
  ;; clojure.repl is also loaded completely, every symbols is included as is.
  ;; Only the 'dir' function is renamed to 'ls' to match
  ;; the idiom on a UNIX system.
  (:use [clojure.string :only (replace join)]
        [clojure.repl :rename {dir ls}])
  ;; we load a few Java classes, several from the same package
  (:import (java.util Date Timer Random)
           java.io.File
           (javax.swing JFrame JPanel)))


(comment

;; 2 Importing Namespaces  

  ;; We require clojure.repl for 'source'
  (source source)
  ;; so 'source' can find the function, the corresponding module must be loaded.
  ;; How does that work?

  ;; Loading modules


  ;; 1) Java classes
  ;; A Java class is not necessarily accessible with its name alone
  Console
  ;; We have access to it fully qualified
  java.io.Console
  ;; if we import it, we can use it directly
  (import java.io.Console)
  Console
  (type Console)
  ;; all classes from the java.lang package are loaded automatically
  String



  ;; 2) Clojure Namespaces
  ;; Namespaces do not allow underscore in the name, only '-'.
  ;; But for the file name, all '-' must be replaced by underscores.
  ;; see here: the namespace repl.08-namespaces is located in the folder
  ;; repl as 08_namespaces.clj
  ;; The reason is a mixture of Lisp and Java convention.

  ;; A namespace that is not loaded cannot do anything
  ;; even if the symbol is fully qualified.
  (repl.greeter.friendly-hello/msg! "John" "Michael")

  ;; require = load
  ;; File: $CLASSPATH/repl/greeter/friendly_hello.clj
  (require 'repl.greeter.friendly-hello)

  ;; the file is quite small and has the following content
  ;; (you can also look it up yourself):
  ;; (ns repl.greeter.friendly-hello
  ;;    (:require [clojure.string :as str]))
  ;; (defn msg! [& args]
  ;;    (println "Hello" (str/join " and " args)))

  ;; full qualification
  (repl.greeter.friendly-hello/msg! "John" "Michael")
  ;; does not work without namespace
  (msg! "John" "Michael")

  ;; Aliasing:
  ;; you can give the namespace an alias, so we do not have to type as much
  (require '[repl.greeter.friendly-hello :as hello])
  (hello/msg! "John" "Jens")

  ;; not defined, error
  zipper
  ;; a definition exists in clojure.zip, but is not loaded yet
  clojure.zip/zipper

  ;; loaded namespace, same rule
  (require 'clojure.zip)
  zipper
  clojure.zip/zipper
  zippy/zipper ;; was not aliased

  ;; refer adds all symbols from the namespace to the current one
  ;; since we already loaded 'next' and 'replace' from the Clojure core,
  ;; they should not be loaded.
  ;; A definition of 'remove' also exists in both,
  ;; which is the reason for the warning.
  (refer 'clojure.zip :exclude '[next replace])
  ;; now we can refer to zipper directly
  zipper

  ;; use is require + refer

  ;; file gives us a Java file object to a path as a string,
  ;; if the library is loaded.
  (file "project.clj")
  (use 'clojure.java.io)
  (file "project.clj")
  (type (file "project.clj"))

  ;; now the namespace macro explains itself (see above)

  ;; You usually load modules as part of the namespace macro.
  ;; ':require :as' or ':refer' of a few symbols (:refer :all also works) is preferred.
  ;; This way it is clear to the reader where definitions come from!
  ;; :use is used as little as possible, unless it is a very well-known library
  ;; or ones which are directly related (e.g. wrappers).




;; 3 What is a namespace?
;; ----------------------


  (def v 4)

  ;; def associates the symbol 'v' with a Var
  ;; A Var is a container for a constant value

  ;; The association happens in the namespace
  ;; Namespaces are maps (kind of anyway)


  ;; Namespaces and introspection
  ;; ------------------------------------------------------------------------------------

  ;; now we will just 'use' everything
  (use 'repl.greeter.friendly-hello)
  (msg! "Kurs" "du daheim")

  ;; behind the namespace you will actually find a map
  (def m (ns-map 'repl.greeter.friendly-hello))

  ;; a very large map in fact...
  (keys m)
  (vals m)

  ;; symbols are mapped to Vars
  (first m)
  (map type (first m))

  ;; which you can look up in the namespace map...
  (def f (get m (symbol "msg!")))
  ;; and use
  f
  (f "jens" "michael")

  ;; there are some namespace-functions
  (apropos "ns-")

  (def n 'repl.08-namespaces)

  ;; There is quite little that we have defined publicly
  (ns-publics n)
  (keys (ns-publics n))
  ;; overall we have defined little here
  (keys (ns-interns n))

  ;; Things defined with defn- or def ^{:private true}
  ;; only show up in the internal mapping
  ;; and are not visible to the outside
  (defn- internal [] -1)
  (keys (ns-interns n))

  (def ^{:private true} lol :lol)
  ;; ^{...} is a metadata annotation.
  ;; These and type hints (left for another time) are usually the
  ;; only ones you will need

  ;; they are actually internal and not public
  (clojure.set/difference
   (set (keys (ns-interns n)))
   (set (keys (ns-publics n))))

  ;; and which aliases did we define?
  (ns-aliases n))
