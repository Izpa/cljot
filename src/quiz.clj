(ns quiz
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::subscribed? [_ {:keys [bot channel-id]}]
  #(tbot/get-chat-member bot channel-id %))

(defmethod ig/init-key ::admin? [_ {:keys [admin-chat-ids]}]
  #(contains? admin-chat-ids %))

(defmethod ig/init-key ::msg->answer [_ {:keys [bot db-execute! admin? subscribed?]}]
  (fn [msg]
    (let [{{:keys [id]
            :as chat} :chat
           :keys [data]} msg]
      (log/info (pformat msg))
      (tbot/send-message bot id (pformat msg))
      (tbot/send-message bot id (try (subscribed? 769254814 #_id)
                                     (catch Exception e (println "catched: " (ex-data e)))))
      (if-let [user (db-execute! {:select :*
                                  :from :users
                                  :where [:= :id id]}
                                 true)]
        (tbot/send-message bot id (pformat user))
        (do (db-execute! {:insert-into :users
                          :values [(select-keys chat [:id :username :last_name :first_name])]})
            (tbot/send-message bot id "Привет")))
      (if (admin? id)
        (tbot/send-message bot id "Вы админ")
        (if-let [user (db-execute! {:select :*
                                    :from :users
                                    :where [:= :id id]}
                                   true)]
          (tbot/send-message bot id (pformat user))
          (do (db-execute! {:insert-into :users
                            :values [(select-keys chat [:id :username :last_name :first_name])]})
              (tbot/send-message bot id "Привет")))))))
