(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defrecord Command [command-id
                    button-text
                    answer-fn
                    answer-main-content
                    answer-additional-contnent
                    button-ids])

(defn ->command
  [{:keys [command-id
           button-text
           answer-fn
           answer-main-content
           answer-additional-contnent
           button-ids]
    :or {button-text "TODO"
         answer-fn tbot/send-message
         answer-main-content "TODO"
         answer-additional-contnent {}
         button-ids []}}]
  (->Command command-id
             button-text
             answer-fn
             answer-main-content
             answer-additional-contnent
             button-ids))

(def cmds
  [(->command {:command-id :default
               :button-text ""
               :answer-main-content "TODO не понял вас"
               :button-ids [:main]})
   (->command {:command-id :main
               :button-text "Вернуться на главную"
               :answer-main-content "Здравствуйте! Я — чат-бот мебельной фабрики «Мария», ваш персональный помощник в мире кухонь и мебели для всего дома.
  <b>Чем я могу вам помочь сегодня?</b>"
               :button-ids [:examples
                            :order
                            :promotions]})
   (->command {:command-id :examples
               :button-text "Посмотреть примеры реализованных проектов"
               :answer-main-content "С удовольствием покажу вам примеры кухонь.
  
  <b>Наши кухни — это:</b>
  ♦Стильные и современные решения
  ♦Функциональность и комфорт
  ♦Материалы высокого качества
  ♦Но также с выгодными акциями (например, на встроенную технику)
  
  <b>Какой стиль вас интересует?</b>"
               :button-ids [:modern-example
                            :neoclassic-example
                            :classic-example
                            :main]})
   (->command {:command-id :modern-example
               :button-text "Современный стиль"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIEEGaSuGFOD_4DudPc_z0CYp4zPf7vAALY2TEbz0WQSCQ0sJmbBHQ8AQADAgADbQADNQQ"
               :button-ids [:neoclassic-example
                            :classic-example
                            :order
                            :main]})
   (->command {:command-id :neoclassic-example
               :button-text "Неоклассический стиль"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIEH2aSu3HMmWzi859JMhuVV4-oPUcRAAIz4DEb4ceQSB5dNSH0a9fPAQADAgADeQADNQQ"
               :button-ids [:modern-example
                            :classic-example
                            :order
                            :main]})
   (->command {:command-id :classic-example
               :button-text "Классический стиль"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIEIWaSvDsfRblbSOjO87ait3qRvafQAAIj2jEbz0WQSLrtCi_PolHMAQADAgADeQADNQQ"
               :button-ids [:modern-example
                            :neoclassic-example
                            :order
                            :main]})
   (->command {:command-id :promotions
               :button-text "Узнать о скидках и акциях"
               :answer-main-content "<b>Покупать кухни в «Мария» выгодно!</b>

Каждый месяц мы предлагаем нашим клиентам выгодные акции: скидки и подарки. Причем все акции суммируются.

<b>Узнайте больше о наших предложениях:</b>"
               :button-ids [:table
                            :technic
                            :installment
                            :order
                            :main]})
   (->command {:command-id :table
               :button-text "Скидка на столешницы до 80%"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIEMWaSvqzFjuCAwEuZNF8ZFvFEsFOXAAI12jEbz0WQSIrnDJx8ZhBEAQADAgADeQADNQQ"
               :answer-additional-contnent {:caption "<b>Скидка на столешницы до 80%</b>
Получайте удовольствие от готовки на новой кухне «Мария»! А мы создадим невероятно стильное и удобное рабочее пространство со столешницей из искусственного камня со скидкой до 80 %."}
               :button-ids [:technic
                            :installment
                            :order
                            :main]})
   (->command {:command-id :installment
               :button-text "Рассрочка 0% на 12 месяцев"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIENmaSv9guYneFiswaycal9dxUgyFAAALy1zEbz0WQSCcBPzCl0touAQADAgADeQADNQQ"
               :answer-additional-contnent {:caption "<b>Рассрочка 0% на 12 месяцев</b>
без первоначального взноса и переплаты. А также предложим выгодные условия по субсидированной рассрочке до 36 месяцев."}
               :button-ids [:table
                            :technic
                            :order
                            :main]})
   (->command {:command-id :technic
               :button-text "Техника в подарок"
               :answer-fn tbot/send-photo
               :answer-main-content "AgACAgIAAxkBAAIEOGaSwSBP7LtD2x-kca3zUh7GmMbuAAJE2jEbz0WQSDSi7WNjJF4oAQADAgADeAADNQQ"
               :answer-additional-contnent {:caption "<b>Техника в подарок</b>
Только по 31 июля дарим посудомоечную машину при покупке кухни «Мария» и двух единиц встраиваемой техники брендов Korting, Kuppersberg, Krona, Haier, Graude, Smeg или Hotpoint. Количество подарков ограниченное – успейте забрать свой!"}
               :button-ids [:table
                            :installment
                            :order
                            :main]})
   (->command {:command-id :order
               :button-text "Получить бесплатный дизайн-проект"
               :answer-main-content "Создайте кухню своей мечты вместе с нашими дизайнерами! ‍

Мы предлагаем вам бесплатный дизайн-проект кухни, который поможет определиться с выбором. Наш дизайнер учтёт все ваши пожелания и предложит оптимальный вариант.

<b>Бесплатный дизайн-проект включает:</b>
♦Профессиональную  визуализацию мебели
♦Индивидуальный подбор материалов и техники
♦Расчет стоимости кухни
♦Полезные советы и рекомендации

<b>Для получения бесплатного дизайн-проекта, необходимо ответить на 3 вопроса.</b>"
               :button-ids [:main]})])

(defn command->key-val
  [command]
  [(:command-id command) command])

(def commands
  (into {}
        (mapv command->key-val cmds)))

(defn ->answer
  [commands bot command-id chat-id]
  (let [{:keys [answer-fn
                answer-main-content
                answer-additional-contnent
                button-ids]} (get commands command-id)]
    (if answer-fn
      (answer-fn bot
                 chat-id
                 answer-main-content
                 (merge {:reply_markup {:inline_keyboard (mapv (fn [button-id]
                                                                 [{:text (->> button-id
                                                                              (get commands)
                                                                              :button-text)
                                                                   :callback_data (name button-id)}])
                                                               button-ids)}
                         :parse_mode "HTML"}
                        answer-additional-contnent))
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
