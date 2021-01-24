 (ns repl.15-test-check-b)

;; wer hier nachguckt, verdirbt sich den Spaß






(defn my-sort [coll] (seq (into (sorted-set) coll)))
(defn my-sort2 [coll] (into [] (into (sorted-set) coll)))

