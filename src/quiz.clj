(ns quiz
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::subscribed? [_ {:keys [bot channel-id]}]
  #(let [{:keys [ok error_code description]
          {:keys [status]} :result
          :as response} (tbot/get-chat-member bot channel-id %)]
     (when (and (not ok)
                (= error_code 400)
                (= description "Bad Request: PARTICIPANT_ID_INVALID"))
       (log/error "Unexpected get-chat-memeber response" response))
     (not= status "left")))

(defmethod ig/init-key ::admin? [_ {:keys [admin-chat-ids]}]
  #(contains? admin-chat-ids %))

(defmethod ig/init-key ::telegram-send [_ {:keys [bot]}]
  (fn [to-id content]
    (tbot/send-message bot to-id content)))

(defmethod ig/init-key ::msg->answer [_ {:keys [db-execute! telegram-send admin? subscribed?]}]
  (fn [msg]
    (let [{{:keys [id]
            :as chat} :chat
           :keys [data]} msg
          answer (partial telegram-send id)]
      (if #_(admin? id) false ;; TODO change back after dev
          (answer "Вы админ")
          (let [user (db-execute! {:select :*
                                   :from :users
                                   :where [:= :id id]}
                                  true)]
            (when (not user)
              (db-execute! {:insert-into :users
                            :values [(select-keys chat
                                                  [:id
                                                   :username
                                                   :last_name
                                                   :first_name])]}
                           true))
            (answer (if (subscribed? id)
                      "Вы подписаны"
                      "Вы не подписаны")))))))
