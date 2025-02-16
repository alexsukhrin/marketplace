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
(s/def ::first-name (s/and string? #(re-matches #"[\p{L}]{3,}" %)))
(s/def ::last-name (s/and string? #(re-matches #"[\p{L}]{3,}" %)))
(s/def ::email (s/and string? #(re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$" %)))
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
  [{:keys [password] :as user-data}]
  (map->User (update user-data :password (fn [_] (hashers/derive password)))))

(stest/instrument `new-user)

(defn insert
  "Create new user."
  [^User user]
  (try
    (db/create-user user)
    (catch Exception e
      (throw (ex-info "Invalid DB user data"
                      {:errors (s/explain-str ::user (str e))})))))

(defn create-buyer
  "Create new buyer."
  [user-id]
  (let [props {:user_id user-id}]
    (db/delete-buyer props)
    (db/create-buyer props)))

(defn create-seller
  "Create new seller."
  [user-id]
  (let [props {:user_id user-id}]
    (db/delete-seller props)
    (db/create-seller props)))

(defn get-user
  "Get user."
  [email]
  (db/get-user {:email email}))

(defn delete
  "Remove user."
  [email]
  (db/delete-user {:email email}))

(defn activate
  "Activate registration user."
  [user-id]
  (db/active-user {:user-id user-id}))

(def secret-key (System/getenv "SECRET_KEY"))

(defn sign-jwt
  "Generate JWT token for User."
  [{:keys [id email]}]
  (jwt/sign {:id id
             :email email
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
  [user-email user-password]
  (let [{:keys [email password] :as user} (get-user user-email)]
    (when (and (= user-email email)
               (:valid (hashers/verify user-password password)))
      (dissoc user :password))))

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

(defn update-password
  "Update password User."
  [email password]
  (db/update-password-user {:email email
                            :password (hashers/derive password)}))

(defn create-user-categories
  [user-category-pairs]
  (let [params {:categories user-category-pairs}
        user-id (-> user-category-pairs first first)]
    (println user-id)
    (db/delete-user-categories {:user_id user-id})
    (db/create-user-categories params)))

(defn create-otp [user-id]
  (db/create-otp {:user_id user-id}))

(defn otp-verify
  "User verify otp."
  [user-id otp]
  (db/get-otp {:otp otp :user_id user-id}))

(comment
  (def user-id "a207511b-20bd-4249-9866-adc374b4d491")
  (def user-uuid (parse-uuid user-id))
  (def categories {:categories [{:category-id 1}
                                {:category-id 2}]})
  (def user-categories (mapv #(vector user-uuid (:category-id %)) (:categories categories)))
  (db/create-user-categories {:values user-categories})
  :end)
