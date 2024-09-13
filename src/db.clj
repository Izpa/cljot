(ns db
  (:require
   [honey.sql :as sql]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defmethod ig/init-key ::ds [_ db-config]
  (let [migratus-config {:store         :database
                         :migration-dir "migrations/"
                        ;;  :subprotocol "postgresql"
                        ;;  :classname "org.postgresql.Driver"
                         :db            db-config}]
    (migratus/init migratus-config)
    (migratus/migrate migratus-config)
    (jdbc/get-datasource db-config)))

(defn execute-sql-map!
  ([ds sql-map] (execute-sql-map! sql-map ds false))
  ([ds sql-map one?]
   ((if one?
      jdbc/execute-one!
      jdbc/execute!) ds
                     (sql/format sql-map)
                     {:builder-fn rs/as-unqualified-kebab-maps
                      :return-keys true
                      :pretty true})))

(defmethod ig/init-key ::execute! [_ {:keys [ds]}]
  (partial execute-sql-map! ds))
