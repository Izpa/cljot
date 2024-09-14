(ns quiz
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::subscribed? [_ {:keys [bot channel-id]}]
  #(let [{:keys [ok error_code description]
          {:keys [user]} :result
          :as response} (tbot/get-chat-member bot channel-id %)]
     (when (and (not ok)
                (= error_code 400)
                (= description "Bad Request: PARTICIPANT_ID_INVALID"))
       (log/error "Unexpected get-chat-memeber response" response))
     (and ok (not-empty user))))

(defmethod ig/init-key ::admin? [_ {:keys [admin-chat-ids]}]
  #(contains? admin-chat-ids %))

(defmethod ig/init-key ::msg->answer [_ {:keys [bot db-execute! admin? subscribed?]}]
  (fn [msg]
    (let [{{:keys [id]
            :as chat} :chat
           :keys [data]} msg]
      (if #_(admin? id) false ;; TODO change back after dev
          (tbot/send-message bot id "Вы админ")
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
            (tbot/send-message bot
                               id
                               (if (subscribed? id)
                                 "Вы подписаны"
                                 "Вы не подписаны")))))))
