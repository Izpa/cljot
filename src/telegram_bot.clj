(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defrecord Command [command-id button-text answer-fn answer-content button-ids])

(defn command->key-val
  [command]
  [(:command-id command) command])

(def commands
  (into {}
        (mapv command->key-val
              [(->Command :default
                          "На главную"
                          tbot/send-message
                          "не понял вас" [])])))

(defn ->answer
  [commands bot command-id chat-id]
  (let [{:keys [answer-fn
                answer-content
                button-ids]} (get commands command-id)]
    (if answer-fn
      (answer-fn bot
                 chat-id
                 answer-content
                 {:reply_markup {:inline_keyboard [(mapv (fn [button-id]
                                                           {:text (->> button-id
                                                                       (get commands)
                                                                       :button-text)
                                                            :callback_data (name button-id)})
                                                         button-ids)]}})
      (throw (ex-info "Unexisted command-id"
                      {:command-id command-id})))))

(defn bot+msg->answer
  [bot msg]
  (let [{{:keys [id]} :chat
         :keys [data]} msg
        command-id (if data
                     (keyword data)
                     :default)]
    (when (> id 0)
      (->answer commands bot command-id id))))

(defn msg-handler
  [bot {:keys [message callback_query] :as upd}]
  (if-let [msg (or message (-> callback_query
                               :message
                               (assoc :data (:data callback_query))))]
    (do (log/info "Received message")
        (log/info (pformat msg))
        (try (log/info "Answer: "
                       (bot+msg->answer bot msg))
             (catch Exception e
               (log/error "Catch exception " e))))
    (log/error "unexpected message type" {:msg upd})))

(defmethod ig/init-key ::msg-handler [_ {:keys [bot]}]
  (partial msg-handler bot))

(defn start-telegram-bot
  [bot url long-polling-config msg-handler]
  (merge
   {:bot bot}
   (if (nil? url)
     {:thread (long-polling bot long-polling-config msg-handler)}
     {:webhook (tbot/set-webhook bot {:url url
                                      :content-type :multipart})})))

(defn stop-telegram-bot
  [thread bot]
  (when thread
    (log/info "Stop telegram bot")
    (tbot/delete-webhook bot)
    (.interrupt ^Thread thread)))

(defmethod ig/halt-key! ::run-client [_ {:keys [thread bot]}]
  (stop-telegram-bot thread bot))

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
