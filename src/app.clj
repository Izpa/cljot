(ns app
  (:gen-class)
  (:require
   [config :refer [init!]]
   [integrant.core :as ig]
   [org.httpkit.server :as httpkit]
   [taoensso.timbre :as log]
   [utils :refer [->num]]))

(defmethod ig/halt-key! ::server [_ server]
  (log/info "Stopping server" server)
  (when server
    (server :timeout 100)))

(defmethod ig/init-key ::server [_ {:keys [host port middlewares] :as config}]
  (log/info (pr-str config))
  (let [server (httpkit/run-server (middlewares)
                                   (assoc config
                                          :max-ws (* 1024 1024 100) ; max ws message size = 100M, temporary
                                          :port (->num port)))]
    (log/info "👉" (str "http://" host ":" port))
    server))

(defn -main
  "Main java entrypoint into Cljamp."
  [& _]
  (init!))
