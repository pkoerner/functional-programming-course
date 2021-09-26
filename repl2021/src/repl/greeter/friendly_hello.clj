(ns repl.greeter.friendly-hello
  (:require [clojure.string :as str]))

(defn msg! [& args]
  (println "Hello" (str/join " and " args)))
