(ns quiz
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::telegram-send [_ {:keys [bot]}]
  (fn [to-id content]
    (tbot/send-message bot to-id content)))

(defmethod ig/init-key ::msg->answer [_ {:keys [telegram-send]}]
  (fn [msg]
    (let [{{:keys [id]} :chat
           :keys [data]} msg
          command-id (cond
                       data (keyword data)
                       :else :default)]
      (log/info (pformat msg))
      (telegram-send id (pformat msg)))))
