(ns user
  (:require
   [config :refer [prepare]]
   [integrant.core :as ig]
   [integrant.repl :as igr]
   [clojure-watch.core :refer [start-watch]]))

(defn start! []
  (integrant.repl/set-prep! #(ig/prep (prepare)))
  (igr/go)
  (start-watch [{:path "src"
                 :event-types [:create
                               :modify
                               :delete]
                 :bootstrap (fn [path]
                              (println "Starting to watch " path)
                              (binding [*ns* *ns*]
                                (igr/reset)))
                 :callback (fn [event filename]
                             (println event filename))
                 :options {:recursive true}}]))


(comment
  (System/getenv "CLIENT_BOT_TELEGRAM_TOKEN")

  (def system (start!))

  (system) ;;stop watch

  (igr/halt))
