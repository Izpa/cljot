(ns db
  (:require
   [integrant.core :as ig]
   [next.jdbc :as jdbc]))

(defmethod ig/init-key ::ds [_ {:keys [dbname user password-secret port]}]
  (jdbc/get-datasource {:dbtype "postgres"
                        :dbname dbname
                        :user user
                        :password password-secret
                        :port port}))

(defmethod ig/init-key ::execute! [_ {:keys [ds]}]
  (partial jdbc/execute! ds))
