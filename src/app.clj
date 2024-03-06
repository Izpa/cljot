(ns app
  (:gen-class)
  (:require
   [config :refer [init!]]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]))

(defn handle-message!
  [msg]
  (println msg))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot config]
   (poll-updates bot config nil))

  ([bot config offset]
   (let [resp (tbot/get-updates bot {:offset offset
                                     :timeout (:timeout config)})]
     (if (contains? resp :error)
       (log/error "tbot/get-updates error:" (:error resp))
       resp))))

(defn long-polling
  [bot config]
  (log/info "Long polling with timeout " config)
  (let [update-id (atom nil)
        set-id! #(reset! update-id %)]
    (loop []
      (log/debug "checking for chat updates.")
      (let [updates (poll-updates bot config @update-id)
            messages (:result updates)]
        (doseq [msg messages]
          (handle-message! (:message msg))
          (-> msg
              :update_id
              inc
              set-id!))
        (Thread/sleep (long (:sleep config))))
      (recur))))

(defn start-telegram-bot
  [token url long-polling-config]
  (log/info "Start bot...")
  (let [bot (tbot/create token)]
    (log/info "Bot started")
    (log/info (tbot/get-me bot))
    (if url
      (log/info "Try to set webhook " url)
      (long-polling bot long-polling-config))
    bot))

(defmethod ig/init-key ::client-bot [_ {:keys [token url long-polling-config]}]
  (log/info "Start client-bot")
  (if token
    (start-telegram-bot token url long-polling-config)
    (log/error "No client-bot token")))

(defn -main
  "Main java entrypoint into Cljot."
  [& _]
  (init!))
