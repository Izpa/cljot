(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]))

(defn msg-handler
  [bot {:keys [message callback_query] :as upd}]
  (if-let [{{:keys [first_name last_name username]} :from
         {:keys [id]} :chat
         :keys [text]
         :as msg}
        (or message (:message callback_query))]
    (do (log/info "Received update " msg)
        (try (when (< 0 id)
               (log/info "Answer: "
                         (tbot/send-message bot
                                            id
                                            "test answer"
                                            {:reply_markup {:inline_keyboard [[{:text "Button1"
                                                                                :callback_data "1"}
                                                                               {:text "Button2"
                                                                                :callback_data "2"}]
                                                                              [{:text "Button3"
                                                                                :callback_data "3"}]]}})))
             (catch Exception e
               (log/error "Catch exception " e))))
    (log/error "unexpected message type" {:msg upd})))

(defmethod ig/init-key ::msg-handler [_ {:keys [bot]}]
  (partial msg-handler bot))

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
  (log/info "Start telegram bot: " (or url long-polling-config))
  (start-telegram-bot bot url long-polling-config msg-handler))

(defmethod ig/init-key ::client [_ {:keys [token]}]
  (log/info "Start client-bot")
  (if (nil? token)
    (log/error "No client-bot token")
    (let [bot (tbot/create token)]
      (log/info (tbot/get-me bot))
      bot)))
