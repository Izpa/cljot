(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]))

(defmethod ig/init-key ::msg-handler [_ {:keys [bot courier-chat-id]}]
  (fn [{{:keys [first_name last_name username]} :from
        {:keys [id]} :chat
        :keys [text]
        :as msg}]
    (log/info "Received bot message " msg)
    (when (< 0 id)
      (let [{:keys [ok]
             {:keys [message_id]} :result} (tbot/send-message bot
                                                              courier-chat-id
                                                              (str "Новый заказ от "
                                                                   first_name
                                                                   " "
                                                                   last_name
                                                                   " (@"
                                                                   username
                                                                   "):\n"
                                                                   text))
            _ (tbot/pin-chat-message courier-chat-id message_id)
            requester-answer (tbot/send-message bot
                                                id
                                                (if ok
                                                  "Ваш заказ принят! Пожалуйста, ожидайте"
                                                  "Что-то пошло не так, скажите Максу или Паше, чтобы пнули Артёма"))]
        (log/info requester-answer)))))

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
