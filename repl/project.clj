(defproject repl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; 05 Rekursion
                 [org.clojure/tools.trace "0.7.9"]
                 ;; 06 Polymorphism
                 [instaparse "1.4.1"]

                 ;; 13 Monaden
                 ;; [org.clojure/clojure "1.7.0"]
                 [org.clojure/algo.monads "0.1.5"]

                 ;; 15 test.check
                 ;; braucht älteres leiningen (<= 2.8.1)
                 ;;[org.clojure/clojure "1.5.1"]
                 ;;[org.clojure/test.check "0.7.0"] ;; aktuelle Version: [org.clojure/test.check "0.9.0"]
                 ; [criterium "0.4.3"]

                 ;; 14 clojure.spec
                 ;[org.clojure/clojure "1.10.0"]
                 [prismatic/schema "1.0.4"]
                 [korma "0.4.3"]
                 [org.clojure/data.csv "0.1.3"]
                 ;[org.clojure/test.check "0.9.0"]

                 ;;[mysql/mysql-connector-java "6.0.5"]


                 ;[org.clojure/clojure "1.8.0"]
                 ;[org.clojure/core.async "0.3.465"]

                 ])
