(ns marketplace.server
  (:require
   [org.httpkit.server :as hk]
   [mount.core :as mount :refer [defstate]]
   [marketplace.routes :refer [app]]))

(defn start-server []
  (when-let [server (hk/run-server #'app {:port 4000})]
    (println "Server has started!")
    server))

(defstate my-server
  :start (start-server)
  :stop  (my-server :timeout 100))
