(ns user
  (:require
   [config :refer [init!]]))

(defn run-system!
  [& _]
  (init!))

(comment
  (run-system!)
  )
