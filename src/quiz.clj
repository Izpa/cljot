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

(defn telegram-send
  ([bot to-id main-content] (telegram-send bot to-id main-content {}))
  ([bot to-id main-content additional-content]
   (tbot/send-message bot to-id main-content additional-content)))

(defmethod ig/init-key ::telegram-send [_ {:keys [bot]}]
  (fn
    ([to-id main-content] (telegram-send bot to-id main-content))
    ([to-id main-content additional-content] (telegram-send bot to-id main-content additional-content))))

(defmethod ig/init-key ::user-welcome [_ {:keys [db-execute!]}]
  (fn [answer chat]
    (db-execute! {:insert-into :users
                  :values [(select-keys chat
                                        [:id
                                         :username
                                         :last_name
                                         :first_name])]}
                 true)
    (answer (str "Давай знакомиться?\n\n"
                 "Мы - Центр инноваций и Клуб LANIT Product manager.\n\n"
                 "Топим за продуктовый подход и развиваем продуктовую культуру в корпорации.\n\n"
                 "Нас уже 400 и мы приглашаем тебя подписаться на наш канал, там  супер! "
                 "В нем мы рассказываем про наши события и инновации внутри ЛАНИТ и в мире. "
                 "А сегодня разыгрываем 15 футболок от Центра инноваций и SlovoDna? "
                 "Условия простые - просто подписаться на наш канал!\n\n"
                 "После подписки нажми кнопку “Я подписался”")
            {:reply_markup {:inline_keyboard [[{:text "Я подписался"
                                                :callback_data "subscribed"}]]
                            :remove_keyboard true}
             :parse_mode "HTML"})))

(defmethod ig/init-key ::user-main-chain [_ {:keys [db-execute! subscribed?]}]
  (fn [msg answer]
    (let [{{:keys [id]
            :as chat} :chat
           :keys [data]} msg]
      (answer "Not implemented yet"))))

(defmethod ig/init-key ::user-answer [_ {:keys [db-execute! user-welcome user-main-chain]}]
  (fn [msg answer]
    (let [{{:keys [id]
            :as chat} :chat} msg
          user (db-execute! {:select :*
                             :from :users
                             :where [:= :id id]}
                            true)]
      (if (not user)
        (user-welcome answer chat)
        (user-main-chain msg answer)))))

(defmethod ig/init-key ::admin-answer [_ {:keys [db-execute]}]
  (fn [msg answer]
    (answer "admin not implemente yet")))

(defmethod ig/init-key ::msg->answer [_ {:keys [telegram-send admin? user-answer admin-answer]}]
  (fn [msg]
    (let [{{:keys [id]} :chat} msg
          answer (partial telegram-send id)]
      (if #_(admin? id) false ;; TODO change back after dev
          (admin-answer msg answer)
          (user-answer msg answer)))))
