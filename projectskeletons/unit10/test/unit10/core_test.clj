(ns unit10.core-test
  (:require
   [clojure.test :refer :all]
   [unit10.core :as b10]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.string :as st]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]))

;; 10.1

(s/check-asserts true)
(stest/instrument `b10/discard)
(def ^:private values-set #{2 3 4 5 6 7 8 9 10 :jack :queen :king :ace})
(def ^:private suits-set #{:hearts :spades :diamonds :clubs})

(defn- card? [[value suite :as card]]
  (and (= 2 (count card))
       (contains? values-set value)
       (contains? suits-set suite)))

(defn- vector-of-cards? [[card & cards]]
  (cond
    (and (not card) (empty? cards)) true
    (card? card) (recur cards)
    :else false))

(defn- valid-player? [{:cards/keys [name hand]}]
  (and (string? name)
       (not-empty hand)
       (vector-of-cards? hand)))

;; Expects a player spec in blatt11.core
;; Generates players from the defined spec and checks if it is a valid player.
;;
;; The output of a failed test in the form of
;; {... :smallest [player] ... }
;; means that player was not valid according to `valid-player?`
(defspec generative-player-spec-prop-test 50
  (prop/for-all [p (s/gen ::b10/player)]
                (valid-player? p)))

(deftest discard-existing-card-test
  (testing "Discarding an existing card should remove it from the players hand."
    (is (= [{:cards/name "Philipp"
             :cards/hand [[:ace :spades]]}]
           (b10/discard [{:cards/name "Philipp"
                          :cards/hand [[3 :clubs] [:ace :spades]]}]
                        "Philipp"
                        [3 :clubs])))))

(deftest discard-non-existent-card-test
  (testing "Discarding an existing card should have no effect."
    (is (= [{:cards/name "Philipp"
             :cards/hand [[3 :clubs] [:ace :spades]]}]
           (b10/discard [{:cards/name "Philipp"
                          :cards/hand [[3 :clubs] [:ace :spades]]}]
                        "Philipp"
                        [:ace :clubs])))))

(deftest discard-card-from-non-existent-player-test
  (testing "Discarding a card from a non-existent player should have no effect."
    (is (= [{:cards/name "Philipp"
             :cards/hand [[3 :clubs] [:ace :spades]]}]
           (b10/discard [{:cards/name "Philipp"
                          :cards/hand [[3 :clubs] [:ace :spades]]}]
                        "Walpurgis"
                        [3 :clubs])))))

(deftest discard-card-multiple-players-test
  (testing "Discarding a card should remove it from the correct players hand."
    (is (= [{:cards/name "Philipp"
             :cards/hand [[3 :clubs] [:ace :spades]]}
            {:cards/name "Woldemar"
             :cards/hand [[2 :hearts] [:king :clubs]]}
            {:cards/name "Zacharias"
             :cards/hand [[6 :diamonds]
                          [:jack :hearts]
                          [:queen :clubs]]}]
           (b10/discard [{:cards/name "Philipp"
                          :cards/hand [[3 :clubs] [:ace :spades]]}
                         {:cards/name "Woldemar"
                          :cards/hand [[2 :hearts] [4 :spades] [:king :clubs]]}
                         {:cards/name "Zacharias"
                          :cards/hand [[6 :diamonds]
                                       [:jack :hearts]
                                       [:queen :clubs]]}]
                        "Woldemar"
                        [4 :spades])))))

;; 10.2

(def ^:private digit-characters
  (reduce (fn [s d] (conj s (char (+ 48 d)))) #{} (range 10)))

(defn- three-digit-characters? [digits]
  (and
   (= 3 (count digits))
   (every? digit-characters digits)))

(defn- satisfies-requirements? [{:keys [first-name last-name id]}]
  (if (and (string? first-name)
           (string? last-name)
           (string? id))
    (let [[bef aft+digits] (split-at (min 2 (count first-name)) id)
          [aft digits]     (split-at (min 3 (count last-name)) aft+digits)]
      (and (= bef (take 2 (st/lower-case first-name)))
           (= aft (take 3 (st/lower-case last-name)))
           (three-digit-characters? digits)))
    false))

;; Generates users from the implemented user-generator
;; and checks whether the created user satisfies the requirements
;; described in the exercise (e.g. the id ends in three digits etc.)
;;
;; The output of a failed test in the form of
;; {... :smallest [[user]] ... }
;; means that `user` did no fulfill the requirements
(defspec generative-user-generator-prop-test 200
  (prop/for-all [user b10/user-generator]
                (satisfies-requirements? user)))
