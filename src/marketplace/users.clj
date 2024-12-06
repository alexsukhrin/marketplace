(ns marketplace.users
  (:require
   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]
   [clojure.spec.alpha :as s]
   [marketplace.db :as db]
   [clojure.core.cache.wrapped :as w]
   [clojure.spec.test.alpha :as stest]))

(defrecord User [first-name last-name email password])

(s/def ::user-id uuid?)
(s/def ::first-name (s/and string? #(re-matches #"\w{3,}" %)))
(s/def ::last-name (s/and string? #(re-matches #"\w{3,}" %)))
(s/def ::email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def ::password (s/and string? #(>= (count %) 8)))
(s/def ::user (s/keys :req-un [::first-name ::last-name ::email ::password]))

(s/fdef new-user
  :args (s/cat :user-data ::user)
  :ret map?
  :fn (fn [{:keys [args ret]}]
        (let [original-password (:password (:user-data args))
              hashed-password (:password ret)]
          (and (not= original-password hashed-password)
               (string? hashed-password)))))

(defn new-user
  "New user."
  [user-data]
  (map->User (update user-data :password (fn [_] (hashers/derive (:password user-data))))))

(stest/instrument `new-user)

(defn insert
  "Create new user."
  [^User user]
  (try
    (db/create-user user)
    (catch Exception e
      (throw (ex-info "Invalid DB user data"
                      {:errors (s/explain-str ::user (str e))})))))

(defn get-user
  "Get user."
  [email]
  (if (s/valid? ::email email)
    (db/get-user {:email email})
    (throw (ex-info "Invalid user email"
                    {:errors (s/explain-str ::email email)}))))

(defn delete
  "Remove user."
  [email]
  (if (s/valid? ::email email)
    (db/delete-user {:email email})
    (throw (ex-info "Invalid user email"
                    {:errors (s/explain-str ::email email)}))))

(defn activate
  "Activate registration user."
  [user-id]
  (if (s/valid? ::user-id user-id)
    (db/active-user {:user-id user-id})
    (throw (ex-info "Invalid user ID"
                    {:errors (s/explain-str ::user-id user-id)}))))

(def secret-key (System/getenv "SECRET_KEY"))

(defn sign-jwt
  "Generate JWT token for User."
  [user]
  (jwt/sign {:id (:id user)
             :email (:email user)
             :exp (-> 3600000
                      (+ (System/currentTimeMillis))
                      (/ 1000))}
            secret-key))

(defn unsign-jwt
  "Check jwt token."
  [token]
  (jwt/unsign token secret-key))

(defn authenticate
  "Login for User."
  [email password]
  (let [user (get-user email)]
    (if (and (= email (:email user))
             (hashers/verify password (:password user)))
      (dissoc user :password)
      nil)))

(def user-tokens-cache (w/fifo-cache-factory {} :ttl 3600000))

(defn set-token
  "Auth set user token."
  [jwt-token user]
  (w/through-cache user-tokens-cache jwt-token (fn [_] user)))

(defn get-token
  "Get token user cache."
  [token]
  (w/lookup user-tokens-cache token))

(defn delete-token
  "Get token user cache."
  [token]
  (w/evict user-tokens-cache token))

(defn extract-token [request]
  "Header JWT token with Authorization."
  (when-let [auth-header (get-in request [:headers "authorization"])]
    (second (re-find #"Bearer (.+)" auth-header))))

(defn reset-password
  "Reset password User."
  [email password]
  (db/reset-password-user {:email email
                           :password (hashers/derive password)}))

(comment

  (def user (new-user {:first-name "Olexandr"
                       :last-name "Sukhryn"
                       :email "alexandrvirtual@gmail.com"
                       :password "password"}))

  (delete "alexandrvirtual@gmail.com")
  (insert user)
  (def user-id "18e4bb6a-b33a-43c6-ad9a-be005d59df95")
  (s/valid? ::user-id (parse-uuid user-id))
  (activate user-id)
  (def request {:headers {"authorization" "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpZCI6IjE0YzE5YzFkLTc4NDktNDQwYy1hZTgwLTZlNjgzMjBkODlmYSIsImVtYWlsIjoiYWxleGFuZHJ2aXJ0dWFsQGdtYWlsLmNvbSIsImV4cCI6MS43MzMyMzQ1MTU2NDVFOX0.FAPZlqelUZDJtnqHB_xBq4wNhJn0Zgpcx6svtMO709o"}})
  (def token-jwt (extract-token request))
  (unsign-jwt token-jwt)

  :end)
