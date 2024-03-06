(ns app
  (:gen-class)
  (:require
   [config :refer [init!]]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [long-polling-telegram-bot :refer [long-polling]]))

(defn handle-message
  [msg]
  (println msg))

(defn start-telegram-bot
  [token url long-polling-config]
  (if (nil? token)
    (log/error "No client-bot token")
    (let [bot (tbot/create token)]
      (log/info "Bot started")
      (log/info (tbot/get-me bot))
      (when (nil? url)
        (long-polling bot long-polling-config handle-message))
      bot)))

(defmethod ig/init-key ::client-bot [_ {:keys [token url long-polling-config]}]
  (log/info "Start client-bot")
  (start-telegram-bot token url long-polling-config))

(defn -main
  "Main java entrypoint into Cljot."
  [& _]
  (init!))
