 (ns repl.15-test-check-b)

;; whoever reads this, spoils his own fun






(defn my-sort [coll] (seq (into (sorted-set) coll)))
(defn my-sort2 [coll] (into [] (into (sorted-set) coll)))

