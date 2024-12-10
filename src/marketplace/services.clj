(ns marketplace.services
  (:require
   [clojure.tools.logging :as log]
   [hiccup2.core :as h]
   [hiccup.page :refer [include-js include-css]]
   [marketplace.users :as user]
   [marketplace.email :as email]))

(defn confirm-email
  "Confirm Email."
  [token]
  (log/info "Confirm token: " token)
  (user/activate (parse-uuid token))
  "complete")

(defn register
  "Registration new User."
  [user-data]
  (let [{:keys [id email first_name last_name active]} (-> user-data user/new-user user/insert)
        register-link (email/register-link id)
        email-body (email/register-email register-link)]
    (email/send-to email email-body)
    {:user-id id
     :first-name first_name
     :last-name last_name
     :email email
     :active active
     :register-link register-link}))

(defn login
  "Login a user and return a JWT token."
  [{:keys [email password]}]
  (if-let [user (user/authenticate email password)]
    (let [token (user/sign-jwt user)]
      (user/set-token token user)
      (log/info "User logged in:" email)
      token)))

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

(defn reset-password
  "Reset password User."
  [{:keys [email password]}]
  (user/reset-password email password)
  "Password update")

(defn create-reset-link
  "Create reset link for User."
  [email]
  (when-let [user (user/get-user email)]
    ))

(defn reset-password-page []
  (h/html
    ;; HTML document
    [:html
     [:head
      [:meta {:charset "UTF-8"}]
      [:meta {:content "width=device-width, initial-scale=1.0", :name "viewport"}]

      [:link {:rel "stylesheet", :href "/css/styles.css"}]
      [:title "Reset password"]]
     [:body

      [:form.input-form {:action "/api/v1/auth/reset-password" :method "PATCH"}
       [:h1.form-title "Login now"]
       [:p.form-intro-text
        "Enter your email address and password to access your personal dashboard"]

       [:div.form-field
        [:label.form-label {:for "email-address"} "Email address"]
        [:input.text-input
         {:id "email-address",
          :name "email-address",
          :autofocus true,
          :type "text"}]]

       [:div.form-field
        [:label.form-label {:for "password"} "Password"]
        [:input.text-input
         {:id "password", :name "password", :autofocus false, :type "password"}]]

       [:div.form-action-buttons
        [:button.primary-submit-button {:type "submit"} "Reset now"]

        ;; Cancel button navigates back to '/home' using javascript
        [:button.cancel-button
         {:type "button", :onclick "javascript:window.location='/'"}
         "Cancel"]]]]]))
