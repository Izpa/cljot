(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defrecord Command
  [command-id
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

(defonce orders (atom {}))

(defn command->dialogue
  [bot chat-id content options]
  (log/info "Start new order" {:chat-id chat-id})
  (swap! orders assoc chat-id {})
  (tbot/send-message bot chat-id content options))

(def cmds
  [{:command-id :default
    :button-text ""
    :answer-main-content
    "Извинте, бот не поддерживает текстовый ввод, для навигации, пожалуйста, используйте кнопки меню"
    :button-ids [:main]}
   {:command-id :main
    :button-text "Вернуться на главную"
    :answer-main-content
    (str "Здравствуйте! Я — чат-бот мебельной фабрики «Мария», "
         "ваш персональный помощник в мире кухонь и мебели для всего дома.\n"
         "<b>Чем я могу вам помочь сегодня?</b>")
    :button-ids [:examples
                 :order
                 :promotions]}
   {:command-id :examples
    :button-text "Посмотреть примеры реализованных проектов"
    :answer-main-content
    (str "С удовольствием покажу вам примеры кухонь.\n\n"
         "<b>Наши кухни — это:</b>\n"
         "♦Стильные и современные решения\n"
         "♦Функциональность и комфорт\n"
         "♦Материалы высокого качества\n"
         "♦Но также с выгодными акциями (например, на встроенную технику)\n\n"
         "<b>Какой стиль вас интересует?</b>")
    :button-ids [:modern-example
                 :neoclassic-example
                 :classic-example
                 :main]}
   {:command-id :modern-example
    :button-text "Современный стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihY0hanwjKQ2REOYtP_5PhMRnoW9YhhdhHvMqhsfztKJ_LqjqgAi-tlAt5zV7iI-FHrw8l8wVj_nIF-TiGzXJ1FBHK_xzWekHMk=s1600-rw-v1"
    :button-ids [:neoclassic-example
                 :classic-example
                 :order
                 :main]}
   {:command-id :neoclassic-example
    :button-text "Неоклассический стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihYy2ILzoLP8vXxK5hvEcjHTboFDKZKRN-OV9VzmieK_8IdwhROvkXI6EgxKzg6Xz_mRGnruwQ8jvk9_gXQltj6pGy_G0l_Erw=s1600-rw-v1"
    :button-ids [:modern-example
                 :classic-example
                 :order
                 :main]}
   {:command-id :classic-example
    :button-text "Классический стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihanGxKQx06YHHXpyNPt5fq19a6O1uSXH1pB_ELG0EfVYTLuK3lqvmRvXI0sF_p582IBcQK15_MvLSFnt5xr1DgIop1sRnHkeZI=s1600-rw-v1"
    :button-ids [:modern-example
                 :neoclassic-example
                 :order
                 :main]}
   {:command-id :promotions
    :button-text "Узнать о скидках и акциях"
    :answer-main-content
    (str "<b>Покупать кухни в «Мария» выгодно!</b>\n\n"
         "Каждый месяц мы предлагаем нашим клиентам выгодные акции: скидки и подарки. Причем все акции суммируются.\n\n"
         "<b>Узнайте больше о наших предложениях:</b>")
    :button-ids [:table
                 :technic
                 :installment
                 :order
                 :main]}
   {:command-id :table
    :button-text "Скидка на столешницы до 80%"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihamzHQZHBu0XVqtNRNVSsw7N6sHMnYzTG689ZXqshd-uLMXspNQ-J8JplBLHFsZONW3oWyKRTNKetO48OBCDkqOb8kF2SpycUU=s1600-rw-v1"
    :answer-additional-contnent
    {:caption
     (str "<b>Скидка на столешницы до 80%</b>\n"
          "Получайте удовольствие от готовки на новой кухне «Мария»! "
          "А мы создадим невероятно стильное и удобное рабочее пространство со столешницей из искусственного камня со скидкой до 80 %.")}
    :button-ids [:technic
                 :installment
                 :order
                 :main]}
   {:command-id :installment
    :button-text "Рассрочка 0% на 12 месяцев"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpiha9jDmaBF_viiqc4kk2s6dnh-Ow6RhBSpwK3pS7-bsTB6pvxi6sClnnOAWJTg6Kr8GUNGk4AjqlLNRgVBVc6fetNcSmSHdUHQ=s1600-rw-v1"
    :answer-additional-contnent
    {:caption
     (str "<b>Рассрочка 0% на 12 месяцев</b>\n"
          "без первоначального взноса и переплаты. "
          "А также предложим выгодные условия по субсидированной рассрочке до 36 месяцев.")}
    :button-ids [:table
                 :technic
                 :order
                 :main]}
   {:command-id :technic
    :button-text "Техника в подарок"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihZqNc8Dvy-RDZ8cv-0oGYs1jpYJ_JBLGDJi3VYRz1linIEAjebz4-9PpiwMooREokZT-1RcKB-ocDLBqKCLClFLCvERK1XWNMA=s2560"
    :answer-additional-contnent
    {:caption
     (str "<b>Техника в подарок</b>\n"
          "Только по 31 июля дарим посудомоечную машину при покупке кухни «Мария» и двух единиц встраиваемой техники брендов Korting, Kuppersberg, Krona, Haier, Graude, Smeg или Hotpoint. "
          "Количество подарков ограниченное – успейте забрать свой!")}
    :button-ids [:table
                 :installment
                 :order
                 :main]}
   {:command-id :order
    :button-text "Получить бесплатный дизайн-проект"
    :answer-main-content
    (str "Создайте кухню своей мечты вместе с нашими дизайнерами! \n‍\n"
         "Мы предлагаем вам бесплатный дизайн-проект кухни, который поможет определиться с выбором. "
         "Наш дизайнер учтёт все ваши пожелания и предложит оптимальный вариант.\n\n"
         "<b>Бесплатный дизайн-проект включает:</b>\n"
         "♦Профессиональную  визуализацию мебели\n"
         "♦Индивидуальный подбор материалов и техники\n"
         "♦Расчет стоимости кухни\n"
         "♦Полезные советы и рекомендации\n\n"
         "<b>Для получения бесплатного дизайн-проекта, необходимо ответить на 3 вопроса.</b>")
    :button-ids [:start-order
                 :main]}
   {:command-id :start-order
    :button-text "Оставить заявку"
    :answer-main-content "Укажите, пожалуйста, ваше имя"
    :answer-fn command->dialogue}])

(defn command->key-val
  [command]
  [(:command-id command) command])

(def commands
  (into {}
        (mapv (comp command->key-val ->command) cmds)))

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
                                                                 [{:text (get-in commands [button-id :button-text])
                                                                   :callback_data (name button-id)}])
                                                               button-ids)}
                         :parse_mode "HTML"}
                        answer-additional-contnent))
      (throw (ex-info "Unexisted command-id"
                      {:command-id command-id})))))

(defn continue-dialogue
  [bot {{:keys [id]} :chat
        :keys [text]
        :as _msg}]
  (let [{:keys [nam
                city
                phone]} (get @orders id)
        answer (partial tbot/send-message bot id)]
    (cond
      (nil? nam) (do
                   (swap! orders assoc-in [id :nam] text)
                   (answer "Укажите, пожалуйста, ваш город"))
      (nil? city) (do
                    (swap! orders assoc-in [id :city] text)
                    (answer "Укажите, пожалуйста, ваш телефон"))
      (nil? phone) (if text ; TODO: check phone
                     (let [order (-> @orders
                                     (get id)
                                     (assoc :phone text))]
                       (log/info "New order" order)
                       ;; TODO: sent email
                       (swap! orders dissoc id)
                       (answer (str "<b>Спасибо за заявку!</b>\n\n"
                                    "Наш менеджер свяжется с вами в ближайшее время, чтобы обсудить детали вашего дизайн-проекта и помочь вам максимально выгодно приобрести кухню вашей мечты.\n\n"
                                    "<b>Вдохновения вам и скорейшего завершения ремонта!💫</b>")
                               {:reply_markup {:inline_keyboard [[{:text (get-in commands [:main :button-text])
                                                                   :callback_data (name :main)}]]}
                                :parse_mode "HTML"}))
                     (answer "Укажите, пожалуйста, ваш телефон")))))

(defonce members (atom #{}))

(defn bot+msg->answer
  [bot msg]
  (let [{{:keys [id]} :chat
         :keys [data]} msg
        command-id (cond
                     data (keyword data)
                     (get @members id) :default
                     :else (do (swap! members conj id)
                               :main))]
    (when (pos? id)
      (if (get @orders id)
        (continue-dialogue bot msg)
        (->answer commands bot command-id id)))))

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
