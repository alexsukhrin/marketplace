(ns marketplace.services
  (:require
   [clojure.tools.logging :as log]
   [hiccup2.core :refer [html]]
   [marketplace.users :as user]
   [marketplace.email :as email]
   [marketplace.products :as product]
   [marketplace.s3 :as s3]))

(defn confirm-email
  [token]
  (log/info "Confirm token: " token)
  (user/activate (parse-uuid token))
  (html
   [:html {:lang "uk"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "Підтвердження реєстрації"]]
    [:body
     [:div {:style "text-align: center; margin-top: 50px;"}
      [:h1 "Дякуємо!"]
      [:p "Ви успішно підтвердили реєстрацію."]
      [:p "Тепер ви можете користуватися всіма можливостями нашого сервісу."]]]]))

(defn register
  "Registration new User."
  [user-data]
  (try
    (let [{:keys [id email]} (-> user-data user/new-user user/insert)
          register-link (email/register-link id)
          email-body (email/register-email register-link)
          token (user/sign-jwt {:id id :email email})]
      (email/send-to email email-body)
      (log/info "User register to:" email)
      {:status 200
       :body {:message "Login successful"
              :token   token}})
    (catch Exception e
      (log/error e)
      {:status 400
       :body {:error "User data not valid."}})))

(defn login
  "Login a user and return a JWT token."
  [email password]
  (if-let [user (user/authenticate email password)]
    (do (let [token (user/sign-jwt user)]
          (user/set-token token user)
          (log/info "User logged in:" email)
          {:status 200
           :body   {:message "Login successful"
                    :token   token}}))
    {:status 400
     :body {:error "Invalid email or password"}}))

(defn otp-verify
  "Verify otp User."
  [email otp]
  (log/info "Verify otp: " otp "email: " email)
  (if-let [user (user/get-user email)]
    (if (user/otp-verify (:id user) otp)
      (let [token (user/sign-jwt user)]
        (user/set-token token (dissoc user :password))
        {:status 200
         :body   {:message "Login successful"
                  :token   token}})
      {:status 403
       :body {:error "Otp not verified"}})

    {:status 404
     :body {:error "User not found."}}))

(defn logout
  "API endpoint to handle user logout."
  [request]
  (let [token (user/extract-token request)]
    (log/info "User extract token " token)
    (when (user/get-token token)
      (user/delete-token token))))

(defn refresh-token
  "API endpoint refresh user jwt-token."
  [request]
  (let [token (user/extract-token request)]
    (log/info "User extract token " token)
    (when-let [user (user/get-token token)]
      (user/set-token token user))))

(defn update-password
  "Reset password User."
  [email password]
  (try
    (user/update-password email password)
    {:status 200
     :body {:message "Password update"}}
    (catch Exception e
      (let [error (str "Update user password exception " (.getMessage e))]
        (log/error error)
        {:status 400
         :body {:error error}}))))

(defn create-reset-code
  "Create reset link for User."
  [email]
  (if-let [user (user/get-user email)]
    (let [{:keys [id email]} user
          {:keys [otp]} (user/create-otp id)]
      (->> (email/reset-password otp)
           (email/send-to email))
      {:status 200
       :body {:message otp}})
    {:status 404
     :body {:error "User not found."}}))

(defn create-user
  "Create user."
  [user-id is-buyer is-seller]
  (try
    (let [user-uuid (parse-uuid user-id)]
      (when is-buyer
        (user/create-buyer user-uuid))
      (when is-seller
        (user/create-seller user-uuid)))
    {:status 201
     :body {:message "created"}}
    (catch Exception e
      (let [error (str "Create user exception " (.getMessage e))]
        (log/error error)
        {:status 400
         :body {:error error}}))))

(defn get-product-categories
  "Categories for products."
  []
  (mapv s3/get-url-image (product/get-categories)))

(defn create-user-categories
  "Create user categories."
  [user-id categories]
  (try
    (let [values (mapv #(vector (parse-uuid user-id) (:category_id %)) categories)]
      (log/info "Create user categories " values)
      (user/create-user-categories values))
    {:status 201
     :body {:message "created"}}
    (catch Exception e
      (log/error (str "Create user category " e))
      {:status 400
       :body {:error "Create user categories data not valid."}})))

(defn create-product-category
  "Create product category."
  [name file]
  (try
    {:status 201
     :body (product/create-product-category name (s3/upload (:tempfile file)))}
    (catch Exception e
      (let [error (str "Create product category exception " (.getMessage e))]
        (log/error error)
        {:status 400
         :body {:error error}}))))
