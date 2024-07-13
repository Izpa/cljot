(ns utils)

(defn ->num
  "The report number reads differently in different cases.
   This function is guaranteed to cast it to int"
  [n]
  (->> n
       str
       (re-find  #"\d+")
       Integer/parseInt))
