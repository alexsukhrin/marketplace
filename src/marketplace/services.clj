(ns marketplace.services
  (:require
   [clojure.tools.logging :as log]
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
