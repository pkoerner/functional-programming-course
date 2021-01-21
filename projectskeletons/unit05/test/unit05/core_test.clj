(ns unit05.core-test
  (:require
   [clojure.test :refer :all]
   [unit05.core :refer :all]
   [clojure.set :as s]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

;; 5.1

(deftest fixedpoint-identity-test
  (testing "The fixed point of the identity funciton is every guess."
    (is (= 1234 (fixedpoint identity 1234 =)))))

(def ^:private epsilon 1e-5)

(defn abs-diff [x y]
  (let [z (- x y)]
    (max z (- z))))

(defn- approximately? [a b]
  (> epsilon (abs-diff a b)))

(deftest fixedpoint-square-test
  (testing "The fixed point of the parabola function with guess 0.9 is approximately 0."
    (is (approximately? 0
                        (fixedpoint (fn [x] (* x x))
                                    0.9
                                    (fn [a b] (approximately? a b)))))))

(def ^:private
  obf {3 apply
       1 coll?
       4 conj
       10 [zipmap apply map filter]
       2 reduce})

(defn- func [c]
  (let [[f1 f2] ((juxt #(% 1) #(% 4)) obf)
        z (- (inc (* 3 2)) 4)]
    ((obf (/ (dec (+ 12 (count obf))) 8))
     (fn [a e]
       (cond
         (f1 e) (((obf 10) 1) (obf (inc z)) a e)
         (fn? (obf 10)) (recur ((obf 1) a) ((obf 10) e))
         :else (f2 a e)))
     []
     c)))

(deftest fixedpoint-obfuscated-test
  (testing "(fixedpoint func [[[[:a] :b] :c]] =) should yield [:a :b :c]."
    (is (= [:a :b :c] (fixedpoint func [[[[:a] :b] :c]] =)))))

(deftest newton-linear-test
  (testing "newton: f=x, f'=1 should yield approximately 0."
    (let [newt (newton
                (fn [x] x)
                (fn [_] 1))]
      (is (approximately? 0 (newt 10 epsilon))))))

(deftest newton-quadratic-test
  (testing "newton: f=x^2-9, f'=2x should yield approximately 3."
    (let [newt (newton
                (fn [x] (- (* x x) 9))
                (fn [x] (* 2 x)))]
      (is (approximately? 3 (newt 1 epsilon))))))

(deftest newton-cubic-test
  (testing "newton: f=x^3+8, f'=3x^2 should yield approximately -2."
    (let [newt (newton
                (fn [x] (+ (* x x x) 8))
                (fn [x] (* 3 x x)))]
      (is (approximately? -2 (newt -7 epsilon))))))

;; 5.2

;; --- Graph Generation ---

(defn- add-reach-of-node
  "Adds the reachable nodes of one node to all others."
  [reach node]
  (let [reachable-nodes (get reach node)]
    (reduce (fn [r k] (update r k s/union reachable-nodes))
            reach
            (keys reach))))

(defn- add-node
  "Adds a node to the reach of the other discovered nodes."
  [reach node]
  ;; Add this node as reachable from all previous nodes
  (let [new-reach (reduce (fn [r k] (update r k conj node))
                          reach
                          (keys reach))]
    (if-not (get reach node) ; If the discovered node is new
      (assoc new-reach node #{})
      ;; Otherwise a cycle was found and all nodes within this cycle
      ;; can reach the same nodes
      (add-reach-of-node new-reach node))))

(defn- reach->edges
  "Transforms a reach into edges.
   e.g. [:a #{:b :c}]
   would yield #{[:a :b] [:a :c]}"
  [edges [a bs]]
  (reduce (fn [e b] (conj e [a b]))
          edges
          bs))

(defn- tc-edges
  "Returns the edges that a tc-call would add."
  [reach]
  (reduce reach->edges #{} (vec reach)))

(defn- build-graph
  "Builds a graph from a path and collects various information on it during the
  creation."
  [[a b & _ :as path] graph+info]
  (if (and a b)
    ;; There is more path to consume
    (let [g+i (-> graph+info
                  (update-in ,,, [:graph :edges] conj [a b])
                  (update-in ,,, [:info :reachable] add-node a))]
      (recur (rest path) g+i))
    ;; The path is consumed -> Extract the tc-edges
    (let [g+i (update-in graph+info [:info :reachable] add-node a)]
      (assoc-in g+i
                [:info :tc-edges]
                (tc-edges (-> g+i :info :reachable))))))

(defn- graph [nodes]
  {:nodes (set nodes)
   :edges #{}})

(defn- info [path nodes]
  {:dom (set (drop-last path))
   :ran (set (rest path))
   ;; Maps a node to a set of nodes which are reachable via a path
   :reachable {}
   ;; The reflexive loops of a trc-graph
   :loops (reduce (fn [l n] (conj l [n n]))
                  #{}
                  nodes)})

(defn- nodes->graph+info
  "Creates a graph and information about that graph from a tuple of nodes and a path."
  [[nodes path]]
  (let [g+i {:graph (graph nodes)
             :info (info path nodes)}]
    (build-graph path g+i)))

(def ^:private nodes-gen (gen/not-empty (gen/vector gen/keyword)))

(def ^:private nodes+path-gen
  (gen/bind (gen/tuple nodes-gen
                       gen/nat
                       (gen/fmap inc gen/nat))
            (fn [[nodes repeats n]]
              (gen/tuple
               (gen/return nodes)
               ; Create a random path through the nodes
               (gen/fmap #(take-nth n %)
                         (gen/shuffle (apply concat (repeat repeats nodes))))))))

(def ^:private graph-gen (gen/fmap nodes->graph+info nodes+path-gen))

;; --- Graph Generation END ---

(def ^:private g- {:nodes #{:a :c :b :d :e}
                   :edges #{[:b :c] [:e :e] [:c :e]
                            [:a :b] [:a :e] [:d :b]
                            [:b :a]}})

(def ^:private edgeless {:nodes #{:a :c :b :d :e}
                         :edges #{}})

(def ^:private c3 {:nodes #{:a :b :c}
                   :edges #{[:a :b] [:b :c] [:c :a]}})

(def ^:private s5 {:nodes #{:a :b :c :d :e}
                   :edges #{[:a :b] [:a :c] [:a :d] [:a :e]}})

(deftest dom-g-test
  (testing "(dom g-) should yield #{:a :b :c :d :e}."
    (is (= #{:a :b :c :d :e} (dom g-)))))

(deftest dom-edgeless-test
  (testing "(dom edgeless) should yield #{}."
    (is (= #{} (dom edgeless)))))

(deftest dom-c3-test
  (testing "(dom c3) should yield #{:a :b :c}."
    (is (= #{:a :b :c} (dom c3)))))

(deftest dom-s5-test
  (testing "(dom s5) should yield #{:a}."
    (is (= #{:a} (dom s5)))))

;; Generates graphs and a map with relevant information about the graph
;; Calls dom with the graph and compares the result to the info
;; provided by the generator
;;
;; The output of a failed test in the form of
;; {... :smallest [[graph info]] ... }
;; means that `(dom graph)` did no yield the same range as provided in `info`
;; under the key :dom
(defspec generative-dom-prop-test 10
  (prop/for-all [{:keys [graph info]} graph-gen]
                (= (:dom info) (dom graph))))

(deftest ran-g-test
  (testing "(ran g-) should yield #{:a :b :c :e}."
    (is (= #{:a :b :c :e} (ran g-)))))

(deftest ran-edgeless-test
  (testing "(ran edgeless) should yield #{}."
    (is (= #{} (ran edgeless)))))

(deftest ran-c3-test
  (testing "(ran c3) should yield #{:a :b :c}."
    (is (= #{:a :b :c} (ran c3)))))

(deftest ran-s5-test
  (testing "(ran s5) should yield #{:b :c :d :e}."
    (is (= #{:b :c :d :e} (ran s5)))))

;; Generates graphs and a map with relevant information about the graph
;; Calls ran with the graph and compares the result to the info
;; provided by the generator
;;
;; The output of a failed test in the form of
;; {... :smallest [[graph info]] ... }
;; means that `(ran graph)` did no yield the same range as provided in `info`
;; under the key :ran
(defspec generative-ran-prop-test 10
  (prop/for-all [{:keys [graph info]} graph-gen]
                (= (:ran info) (ran graph))))

(deftest tc-edgeless-test
  (testing "(tc edgless) should have no effect."
    (is (= edgeless (tc edgeless)))))

(deftest tc-s5-test
  (testing "(tc s5) should have no effect."
    (is (= s5 (tc s5)))))

(deftest tc-c3-test
  (testing "(tc c3) should add edges [:b :a], [:a :c], [:c :b], [:a :a], [:b :b] and [:c :c]."
    (let [exp (update c3 :edges conj
                      [:b :a] [:a :c] [:c :b]   ; New edges
                      [:a :a] [:b :b] [:c :c])] ; New loops
      (is (= exp (tc c3))))))

(deftest tc-g-test
  (testing "(tc g-) should add edges [:b :e], [:d :e], [:a :a], [:d :a], [:b :b], [:a :c] and [:d :c]"
    (let [exp (update g- :edges conj
                      [:b :e] [:d :e] [:d :a] [:a :c] [:d :c] ; New edges
                      [:a :a] [:b :b])] ; New loops
      (is (= exp (tc g-))))))

;; Generates graphs and a map with relevant information about the graph
;; Calls tc with the graph and compares the result to the info
;; provided by the generator
;;
;; The output of a failed test in the form of
;; {... :smallest [[graph info]] ... }
;; means that `(tc graph)` did no yield the same graph
;; as expected by `info`
(defspec generative-tc-prop-test 10
  (prop/for-all [{:keys [graph info]} graph-gen]
                (let [exp (update graph
                                  :edges
                                  ;; Add the tc-edges
                                  s/union (:tc-edges info))]
                  (= exp (tc graph)))))

(deftest trc-edgless-test
  (testing "(trc edgeless) should only add loops."
    (let [exp (update edgeless
                      :edges
                      conj [:a :a] [:b :b] [:c :c] [:d :d] [:e :e])]
      (is (= exp (trc edgeless))))))

(deftest trc-c3-test
  (testing "(trc c3) should add [:b :a], [:b :b], [:a :c], [:c :b], [:a :a] and [:c :c]."
    (let [exp (update c3 :edges conj
                      [:b :a] [:a :c] [:c :b]  ; New edges
                      [:a :a] [:b :b] [:c :c])] ; New loops
      (is (= exp (trc c3))))))

(deftest trc-s5-test
  (testing "(trc s5) should only add loops."
    (let [exp (update s5
                      :edges
                      conj [:a :a] [:b :b] [:c :c] [:d :d] [:e :e])]
      (is (= exp (trc s5))))))

(deftest trc-g-test
  (testing "(trc g-) should add [:b :e], [:d :e], [:d :a], [:a :c], [:d :c] and loops."
    (let [exp (update g- :edges conj
                      [:b :e] [:d :e] [:d :a] [:a :c] [:d :c] ; New edges
                      [:a :a] [:b :b] [:c :c] [:d :d])] ; New loops
      (is (= exp (trc g-))))))

;; Generates graphs and a map with relevant information about the graph
;; Calls trc with the graph and compares the result to the info
;; provided by the generator
;;
;; The output of a failed test in the form of
;; {... :smallest [[graph info]] ... }
;; means that `(trc graph)` did no yield the same graph
;; as the informaiton from `info` would
(defspec generative-trc-prop-test 10
  (prop/for-all [{:keys [graph info]} graph-gen]
                (let [exp (update graph
                                  :edges
                                  ;; Add the tc-edges and loops
                                  s/union (:tc-edges info) (:loops info))]
                  (= exp (trc graph)))))

(deftest path-g-neighbouring-test
  (testing "(path? g- :b :c) should yield true"
    (is (path? g- :b :c))))

(deftest path-g-length-two-test
  (testing "(path? g- :b :e) should yield true"
    (is (path? g- :b :e))))

(deftest path-g-self-loop-test
  (testing "(path? g- :e :e) should yield true"
    (is (path? g- :e :e))))

(deftest path-c3-loop-test
  (testing "(path? c3 :a :a) should yield true"
    (is (path? c3 :a :a))))

(deftest path-edgless-nonexistent-path-test
  (testing "(path? edgeless :a :b) should yield false"
    (is (not (path? edgeless :a :b)))))

(def ^:private node-select-gen
  (gen/bind graph-gen
            (fn [{:keys [graph]
                  :as   g+i}]
              (gen/tuple
               (gen/return g+i)
               ;; Choose the start and end
               (gen/fmap #(take 2 %)
                         (gen/shuffle
                          (apply concat
                                 (repeat 2 (:nodes graph)))))))))

;; Generates graphs and a map with relevant information about the graph
;; Calls path? with the graph and compares the result to the info
;; provided by the generator
;;
;; The output of a failed test in the form of
;; {... :smallest [[graph info] [s t]] ... }
;; means that `(path? graph s t)` did no yield the same result
;; as the provided information
(defspec generative-path-prop-test 10
  (prop/for-all [[{:keys [graph info]} [s t]] node-select-gen]
                (let [reachable (boolean (if (= s t)
                                           true
                                           (-> info
                                               :reachable
                                               s ; Get all nodes reachable from s
                                               t)))] ; Check if t is reachable
                  (= reachable (boolean (path? graph s t))))))