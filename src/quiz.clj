(ns quiz
  (:require
   [integrant.core :as ig]
   [honey.sql :as sql]
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
   (let [sent_message (tbot/send-message bot
                                         to-id
                                         main-content
                                         additional-content)]
     (log/info "Send message: "
               (pformat sent_message))
     sent_message)))

(defmethod ig/init-key ::telegram-send [_ {:keys [bot]}]
  (partial telegram-send bot))

(def subscribed-callback-data "subscribed")

(def subscribed-additional-content
  {:reply_markup {:inline_keyboard [[{:text "Я подписался"
                                      :callback_data subscribed-callback-data}]]}
   :parse_mode "HTML"})

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
                 "После подписки нажми кнопку “Я подписался”") ;;TODO: нет ссылки на канал
            subscribed-additional-content)))

(defn questions [db-execute! subscribed? msg answer]
  (let [{{:keys [id]} :chat
         :keys [data
                text]} msg
        any-answers? (-> {:select [[(sql/call :count :*)]]
                          :from [:user-answers]
                          :where [:= :user-id id]}
                         (db-execute! true)
                         :count
                         (not= 0))]
    (if (or (subscribed? id)
            any-answers?)
      (let [db-question (db-execute! {:with [[:next-unanswered-question
                                              {:select [[:q.id :question-id]
                                                        [:q.text :question-text]
                                                        [:a.question-message-id :question-message-id]]
                                               :from [[:questions :q]]
                                               :left-join [[:user-answers :a]
                                                           [:and
                                                            [:= :q.id :a.question-id]
                                                            [:= :a.user-id id]]]
                                               :where [:and
                                                       [:is :a.answer-text nil]
                                                       [:is :a.option-id nil]]
                                               :order-by [[:q.sort-order]]
                                               :limit 1}]]
                                      :select [[:nuq.question-id :question-id]
                                               [:nuq.question-text :question-text]
                                               [:nuq.question-message-id :question-message-id]
                                               [:o.id :option-id]
                                               [:o.text :option-text]]
                                      :from [[:next-unanswered-question :nuq]]
                                      :left-join [[:question-options :o]
                                                  [:= :nuq.question-id :o.question-id]]
                                      :order-by [[:o.sort-order]]}
                                     false)
            {:keys [question-id
                    question-text
                    question-message-id
                    options]} (reduce (fn [q {:keys [option-id option-text]}]
                                        (update q :options assoc option-id option-text))
                                      (-> db-question
                                          first
                                          (select-keys [:question-id
                                                        :question-text
                                                        :question-message-id])
                                          (assoc :options {}))
                                      db-question)]
        (log/debug "data: " data);; TODO delete me
        (if question-message-id
          (if options
            (if (and data
                     (get options data))
              (do (answer "Not implemented (correct answer for question with options)")
                  (questions db-execute! subscribed? msg answer))
              (answer "Пожалуйста, используйте кнопки для ответа"))
            (if text
              (do (db-execute! {:update :user-answers
                                :set {:answer-text text}
                                :where [:and
                                        [:= :user-id id]
                                        [:= :question-id question-id]]})
                  (questions db-execute! subscribed? msg answer))
              (answer "Пожалуйста, используйте текст для ответа")))
          (->> (answer question-text)
               :result
               :message_id
               (assoc {:user-id id :question-id question-id} :question-message-id)
               (conj [])
               (assoc {:insert-into :user-answers} :values)
               (db-execute!))))
      (answer "Надо всё-таки подписаться" subscribed-additional-content))))

(defmethod ig/init-key ::user-main-chain [_ {:keys [db-execute! subscribed?]}]
  (partial questions db-execute! subscribed?))

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
