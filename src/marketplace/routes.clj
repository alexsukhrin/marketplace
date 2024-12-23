(ns marketplace.routes
  (:require
   [clojure.tools.logging :as log]
   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [muuntaja.core :as m]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.dev.pretty :as pretty]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.parameters :as parameters]
   [clojure.spec.alpha :as s]
   [ring.middleware.cors :refer [wrap-cors]]
   [marketplace.services :as handler]
   [marketplace.users :as user]))

(s/def ::file multipart/temp-file-part)
(s/def ::name string?)
(s/def ::category-id int?)
(s/def ::file-response (s/keys :req-un [::name ::category-id]))
(s/def ::file-params (s/keys :req-un [::file ::name]))
(s/def ::otp (s/and string? #(= (count %) 6)))

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
                       :tags [{:name "auth", :description "registration and authorization routes api"}
                              {:name "users", :description "create users routes api"}
                              {:name "products", :description "products and categories routes api"}
                              {:name "ping", :description "health check status server api"}]}
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
                :parameters {:body {:first_name ::user/first-name
                                    :last_name ::user/last-name
                                    :email ::user/email
                                    :password ::user/password}}
                :response {200 {:body {:user_id ::user/user-id
                                       :first_name ::user/first-name
                                       :last_name ::user/last-name
                                       :email ::user/email}}}
                :handler (fn [{{:keys [body]} :parameters}]
                           (handler/register body))}}]

       ["/login"
        {:post {:summary     "login user"
                :description "This route does not require authorization."
                :parameters  {:body {:email    ::user/email
                                     :password ::user/password}}
                :response    {200 {:body {:message string?
                                          :token   string?}}
                              400 {:body {:error string?}}}
                :handler     (fn [{{{:keys [email password]} :body} :parameters}]
                               (handler/login email password))}}]

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
                           :body (str (handler/confirm-email token))})}}]

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
        {:post {:summary "user create reset link"
                :description "This route does not require authorization."
                :parameters {:body {:email ::user/email}}
                :response {200 {:body {:message string?}}
                           400 {:body {:error string?}}}
                :handler (fn [{{{:keys [email]} :body} :parameters}]
                           (handler/create-reset-code email))}}]

       ["/otp"
        {:post {:summary "user verify otp"
                :description "This route does not require authorization."
                :parameters {:body {:email ::user/email
                                    :otp ::otp}}
                :response {200 {:body {:message string?
                                       :token string?}}
                           400 {:body {:error string?}}}
                :handler (fn [{{{:keys [email otp]} :body} :parameters}]
                           (handler/otp-verify email otp))}}]

       ["/update-password"
        {:patch {:summary "user update password"
                 :description "This route requires authorization."
                 :parameters {:body {:password ::user/password}}
                 :response {200 {:body {:message string?}}
                            400 {:body {:error string?}}}
                 :handler (fn [{:keys [user parameters]}]
                            (let [{{:keys [password]} :body} parameters]
                              (handler/update-password (:email user) password)))}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]]

      ["/users"
       {:tags #{"users"}}

       ["/create"
        {:post {:summary "Create user"
                :description "This route requires authorization."
                :parameters {:body {:is_buyer boolean?
                                    :is_seller boolean?}}
                :response {201 {:body {:message string?}}
                           400 {:body {:error string?}}}
                :handler (fn [{:keys [user parameters]}]
                           (let [{{:keys [is-buyer is-seller]} :body} parameters]
                             (handler/create-user (:id user) is-buyer is-seller)))}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]

       ["/categories"
        {:post {:summary "Create user categories"
                :description "This route requires authorization."
                :parameters {:body {:categories [{:category_id int?}]}}
                :response {201 {:body {:message string?}}
                           400 {:body {:error string?}}}
                :handler (fn [{:keys [user parameters]}]
                           (let [{{:keys [categories]} :body} parameters]
                             (handler/create-user-categories (:id user) categories)))}
         :swagger {:security [{:apiAuth []}]}
         :middleware [wrap-jwt-auth]}]]

      ["/products"
       {:tags #{"products"}}

       ["/categories"
        {:get {:summary "Get all categories for products"
               :description "This route requires authorization."
               :response {200 {:body {:categories [{:category_id int?
                                                    :name string?
                                                    :photo string?}]}}}
               :handler (fn [_]
                          {:status 200
                           :body {:categories (handler/get-product-categories)}})}
         :post {:summary "Create category for product."
                :description "This route requires authorization."
                :parameters {:multipart ::file-params}
                :responses {200 {:body ::file-response}}
                :handler (fn [{{{:keys [file name]} :multipart} :parameters}]
                           (handler/create-product-category name file))}
         :swagger {:security [{:apiAuth []}]
                   :produces ["image/png"]}
         :middleware [wrap-jwt-auth]}]]]]

    {:exception pretty/exception
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :exception-handler (fn [req ex]
                                 (log/error req)
                                 {:status 400
                                  :body {:error (str "Request coercion failed: " ex)}})
            :middleware [;; swagger & openapi
                         swagger/swagger-feature
                           ;; query-params & form-params
                         parameters/parameters-middleware
                           ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                           ;; encoding response body
                         muuntaja/format-response-middleware
                           ;; exception handling
                         ;;exception/exception-middleware
                           ;; decoding request body
                         muuntaja/format-request-middleware
                           ;; coercing response bodys
                         coercion/coerce-response-middleware
                           ;; coercing request parameters
                         coercion/coerce-request-middleware
                           ;; multipart
                         multipart/multipart-middleware
                         [wrap-cors
                          :access-control-allow-headers #{"accept"
                                                          "accept-encoding"
                                                          "accept-language"
                                                          "authorization"
                                                          "content-type"
                                                          "origin"}
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-credentials "true"
                          :access-control-allow-methods [:delete :get :patch :post :put]]]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :urls [{:name "swagger", :url "swagger.json"}]
               :urls.primaryName "swagger"
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))
