(ns quiz
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::msg->answer [_ {:keys [bot]}]
  (fn [msg]
    (let [{{:keys [id]} :chat
           :keys [data]} msg
          command-id (cond
                       data (keyword data)
                       :else :default)]
      (log/info (pformat msg))
      (tbot/send-message bot id (pformat msg)))))
