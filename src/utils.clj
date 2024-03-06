(ns utils
  (:require
   [clojure.string :refer [includes?]]))

(defn secret?
  [k]
  (let [nk (name k)
        ns-k (namespace k)]
    (or (and nk (includes? nk "secret"))
        (and ns-k (includes? ns-k "secret")))))

(defn replace-all-coll-values-with
  [replacement coll]
  (cond
    (map-entry? coll) (let [[k v] coll]
                        [k (replace-all-coll-values-with replacement v)])
    (sequential? coll) (mapv (partial replace-all-coll-values-with replacement) coll)
    (map? coll) (->> coll
                     (mapv (partial replace-all-coll-values-with replacement))
                     (reduce (fn [eax [k v]] (assoc eax k v)) {}))
    :else replacement))

(defn replace-secrets-in-coll
  ([coll] (replace-secrets-in-coll "hidden-secret" coll))
  ([replacement coll]
   (cond
     (map-entry? coll) (let [[k v] coll]
                         (if (secret? k)
                           [k (replace-all-coll-values-with replacement v)]
                           [k (replace-secrets-in-coll replacement v)]))
     (sequential? coll) (mapv (partial replace-secrets-in-coll replacement) coll)
     (map? coll) (->> coll
                      (mapv (partial replace-secrets-in-coll replacement))
                      (reduce (fn [eax [k v]] (assoc eax k v)) {}))
     :else coll)))

(defn e->ex-data-with-hidden-secrets
  [e]
  (replace-secrets-in-coll (ex-data e)))

(defn ->num
  "The report number reads differently in different cases.
   This function is guaranteed to cast it to int"
  [n]
  (->> n
       str
       (re-find  #"\d+")
       Integer/parseInt))
