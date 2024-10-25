(ns marketplace.server
  (:require
   [org.httpkit.server :as hk]
   [mount.core :as mount :refer [defstate]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [compojure.core :refer [defroutes GET POST PATCH context]]
   [compojure.route :refer [not-found]]
   [ring.logger :as logger]))

(defroutes
  app-routes
  (context "/api/v1" []
    (GET "/ping" _ {:status 200 :text "pong"}))
  (not-found "<h1>404 Error!</h1>"))

(def app
  (-> #'app-routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (logger/wrap-with-logger)))

(defn start-server []
  (when-let [server (hk/run-server #'app {:port 4000})]
    (println "Server has started!")
    server))

(defstate my-server
  :start (start-server)
  :stop  (my-server :timeout 100))
