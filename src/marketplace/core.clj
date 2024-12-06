(ns marketplace.core
  (:require
   [mount.core :as mount]
   [marketplace.server :as server])
  (:gen-class))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn -main
  "Start Marketplace Service"
  [& args]
  (start))
