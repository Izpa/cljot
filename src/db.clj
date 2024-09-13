(ns db
  (:require
   [honey.sql :as sql]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defmethod ig/init-key ::ds [_ {:keys [db] :as db-config}]
  (migratus/init db-config)
  (migratus/migrate db-config)
  (jdbc/get-datasource db))

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
