(ns marketplace.core
  (:require
   [mount.core :as mount]
   [marketplace.server :as server]
   [marketplace.db :as db])
  (:gen-class))

(defn start-db
  "Start db service."
  []
  (mount/start #'marketplace.db/*db*))

(defn stop-db
  "Stop db service."
  []
  (mount/stop #'marketplace.db/*db*))

(defn start
  "Start services."
  []
  (mount/start))

(defn stop
  "Stop services."
  []
  (mount/stop))

(defn -main
  "Start Marketplace Service"
  [& args]
  (start))
