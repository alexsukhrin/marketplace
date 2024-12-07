(ns marketplace.routes
  (:require
   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [muuntaja.core :as m]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.dev.pretty :as pretty]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.parameters :as parameters]
   [marketplace.services :as handler]
   [marketplace.users :as user]))

(defn wrap-jwt-auth [handler]
  "Middleware jwt auth user."
  (fn [request]
    (let [token (user/extract-token request)]
      (if-let [claims (and token (user/unsign-jwt token))]
        (handler (assoc request :user claims))
        {:status 500
         :body {:message "Invalid or missing JWT token"}}))))

(def app
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "Marketplace"
                              :description "swagger api docs"
                              :version "0.0.1"}
                       :securityDefinitions {:apiAuth {:type :apiKey
                                                       :in :header
                                                       :name "authorization"}}
                       :tags [{:name "auth", :description "auth api"}
                              {:name "ping", :description "health api"}]}
             :handler (swagger/create-swagger-handler)}}]

     ["/api/v1"

      ["/ping"
       {:tags #{"ping"}
        :get {:summary "healthcheck service"
              :description "This route does not require authorization."
              :response {200 {:body {:message string?}}}
              :handler (fn [_] {:status 200
                                :body {:message "pong"}})}}]

      ["/auth"
       {:tags #{"auth"}}

       ["/register"
        {:post {:summary "register user"
                :description "This route does not require authorization."
                :parameters {:body {:first-name ::user/first-name
                                    :last-name ::user/last-name
                                    :email ::user/email
                                    :password ::user/password}}
                :response {200 {:body {:user-id ::user/user-id
                                       :first-name ::user/first-name
                                       :last-name ::user/last-name
                                       :email ::user/email}}}
                :handler (fn [{{:keys [body]} :parameters}]
                           {:status 200
                            :body (handler/register body)})}}]

       ["/login"
        {:post {:summary     "login user"
                :description "This route does not require authorization."
                :parameters  {:body {:email    ::user/email
                                     :password ::user/password}}
                :response    {200 {:body {:message string?
                                          :token   string?}}
                              400 {:body {:error string?}}}
                :handler     (fn [{{:keys [body]} :parameters}]
                               (if-let [token (handler/login body)]
                                 {:status 200
                                  :body   {:message "Login successful"
                                           :token   token}}
                                 {:status 400
                                  :body {:error "Invalid email or password"}}))}}]

       ["/logout"
        {:delete {:summary "logout user"
                  :description "This route requires authorization."
                  :response {200 {:body {:message string?}}
                             500 {:body {:error string?}}}
                  :handler (fn [request]
                             (if (handler/logout request)
                               {:status 200
                                :body {:message "Logout successful"}}
                               {:status 500
                                :body {:error "Failed to log out"}}))}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]

       ["/confirm-email"
        {:get {:summary "user confirm registration"
               :description "This route does not require authorization."
               :parameters {:query {:token string?}}
               :response {200 {:body {:message string?}}}
               :handler (fn [{{{:keys [token]} :query} :parameters}]
                          {:status 200
                           :body {:message (handler/confirm-email token)}})}}]

       ["/refresh-token"
        {:post {:summary "user refresh token"
                :description "This route requires authorization."
                :response {200 {:body {:message string?
                                       :token string?}}
                           400 {:body {:error string?}}}
                :handler (fn [request]
                           (if-let [token (handler/refresh-token request)]
                             {:status 200
                              :body {:message "Token update"
                                     :token token}}
                             {:status 500
                              :body {:error "Failed token"}}))}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]

       ["/reset-password"
        {:patch {:summary "user update password"
                 :description "This route requires authorization."
                 :parameters {:body {:email ::user/email
                                     :password ::user/password}}
                 :response {200 {:body {:message string?}}
                            400 {:body {:error string?}}}
                 :handler (fn [{{:keys [body]} :parameters}]
                            {:status 200
                             :body {:message (handler/reset-password body)}})}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]]]]

    {:exception pretty/exception
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [;; swagger & openapi
                         swagger/swagger-feature
                           ;; query-params & form-params
                         parameters/parameters-middleware
                           ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                           ;; encoding response body
                         muuntaja/format-response-middleware
                           ;; decoding request body
                         muuntaja/format-request-middleware
                           ;; coercing response bodys
                         coercion/coerce-response-middleware
                           ;; exception handling
                         exception/exception-middleware
                           ;; coercing request parameters
                         coercion/coerce-request-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :urls [{:name "swagger", :url "swagger.json"}]
               :urls.primaryName "swagger"
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))

