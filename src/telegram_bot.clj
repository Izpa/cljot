(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]))

(defmethod ig/init-key ::msg-handler [_ _]
  println)

(defn start-telegram-bot
  [bot url long-polling-config msg-handler]
  (if (nil? url)
    {:thread (long-polling bot long-polling-config msg-handler)}
    {:webhook (tbot/set-webhook bot {:url url
                                     :content-type :multipart})}))

(defn stop-telegram-bot
  [thread _webhook]
  (when thread (.interrupt ^Thread thread)))

(defmethod ig/halt-key! ::run-client [_ {:keys [thread url]}]
  (stop-telegram-bot thread url))

(defmethod ig/init-key ::run-client [_ {:keys [bot
                                               url
                                               long-polling-config
                                               msg-handler]}]
  (start-telegram-bot bot url long-polling-config msg-handler))

(defmethod ig/init-key ::client [_ {:keys [token]}]
  (log/info "Start client-bot")
  (if (nil? token)
    (log/error "No client-bot token")
    (let [bot (tbot/create token)]
      (log/info (tbot/get-me bot))
      bot)))
