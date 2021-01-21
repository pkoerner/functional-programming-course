(ns unit09.edit-distance)

(defn levenshtein [[h1 & t1 :as s1] [h2 & t2 :as s2]]
  (cond (empty? s1) (count s2)
        (empty? s2) (count s1)
        :otherwise (min
                    (inc (levenshtein t1 s2))
                    (inc (levenshtein s1 t2))
                    (if (= h1 h2)
                      (levenshtein t1 t2)
                      (inc (levenshtein t1 t2))))))

(defn mapHelper "Which letter should be kept" [hold keep]
  (if (= hold keep)
    hold
    false))

(defn levenshtein-Helper "compares two strings, keeps equal letters and adds all not equal up" [stringSeq1 stringSeq2]
  (count (filter #(= false %) (map mapHelper stringSeq1 stringSeq2))))

(defn prepareString "makes the strings equaly long" [string lengthOfTheOther]
  (let [dif (- (count string) lengthOfTheOther)]
    (if (< dif 0)
      (concat (seq string) (repeat (- dif) " "))
      (seq string))))

(defn levenschtein "prepare the strings and compares them in two different kinds, return result" [string1 string2]
  (let [prepString11 (prepareString string1 (count string2))
        prepString12 (prepareString string2 (count string1))
        prepString21 (prepareString (reverse string1) (count string2))
        prepString22 (prepareString (reverse string2) (count string1))
        x (levenshtein-Helper prepString11 prepString12)
        y (levenshtein-Helper prepString21 prepString22)]
    (if (< x y)
      x
      y)))
