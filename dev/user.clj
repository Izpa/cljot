(ns user
  (:require
   [clojure-watch.core :refer [start-watch]]
   [config :refer [prepare]]
   [integrant.core :as ig]
   [integrant.repl :as igr]))

(defn start!
  []
  (integrant.repl/set-prep! #(ig/prep (prepare)))
  (igr/go)
  (start-watch [{:path "src"
                 :event-types [:create
                               :modify
                               :delete]
                 :bootstrap (fn [path]
                              (println "Starting to watch " path))
                 :callback (fn [event filename]
                             (println event filename)
                             (binding [*ns* *ns*]
                               (igr/reset)))
                 :options {:recursive true}}]))

(comment
  (System/getenv "CLIENT_BOT_TELEGRAM_TOKEN")

  (def stop-system! (start!))

  (stop-system!) ;;stop watch

  (igr/halt))
