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
  (GET "/" _ {:status 200 :body "<!DOCTYPE html>
<html lang=\"uk\">
<head>
  <meta charset=\"UTF-8\">
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
  <title>Вітання команди</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      font-family: Arial, sans-serif;
      background: linear-gradient(135deg, #4CAF50, #1A73E8);
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      color: white;
      text-align: center;
    }
    
    .container {
      max-width: 600px;
      padding: 30px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 10px;
      box-shadow: 0px 4px 20px rgba(0, 0, 0, 0.2);
    }
    
    h1 {
      font-size: 2.5em;
      margin-bottom: 20px;
      color: #FFEB3B;
    }
    
    p {
      font-size: 1.2em;
      line-height: 1.6;
    }
    
    .team-list {
      margin-top: 20px;
      font-weight: bold;
    }
    
    .button {
      margin-top: 30px;
      padding: 10px 20px;
      font-size: 1em;
      color: #4CAF50;
      background-color: #FFEB3B;
      border: none;
      border-radius: 5px;
      cursor: pointer;
      text-decoration: none;
      transition: background-color 0.3s;
    }
    
    .button:hover {
      background-color: #FDD835;
    }
  </style>
</head>
<body>
  <div class=\"container\">
    <h1>Вітаємо в команді Маркетплейсу!</h1>
    <p>Ми раді вітати тебе у нашій команді! Разом ми створюємо нові можливості для бізнесу, допомагаючи людям знаходити те, що їм потрібно. Дякуємо, що ти з нами!</p>
    
    <div class=\"team-list\">
      <p>З найкращими побажаннями,<br>Команда Маркетплейсу</p>
    </div>
    
    <a href=\"#start\" class=\"button\">Почати роботу</a>
  </div>
</body>
</html>"})
  (context "/api/v1" []
    (GET "/ping" _ {:status 200 :body "pong"}))
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
