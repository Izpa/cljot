(ns config
  (:gen-class)
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [utils :refer [e->ex-data-with-hidden-secrets]]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset [_ _ value]
  (ig/refset value))

(defn load-config
  ([] (load-config (or (keyword (System/getProperty "Profile"))
                       :default)))
  ([profile]
   (-> "common_config.edn"
       io/resource
       (aero/read-config {:profile profile}))))

(defn load-namespaces
  [cfg]
  (ig/load-namespaces cfg)
  cfg)

(defn init-and-hide-integrant-secrets-in-exception
  [cfg]
  (ig/init cfg))

(defn init!
  ([] (init! :default))
  ([profile]
   (-> (load-config profile)
       load-namespaces
       init-and-hide-integrant-secrets-in-exception)))
