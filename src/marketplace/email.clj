(ns marketplace.email
  (:require [postal.core :as postal]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [send]))

(defonce smtp-config {:host (System/getenv "EMAIL_HOST")
                      :port (Integer/parseInt (or (System/getenv "EMAIL_PORT") "587"))
                      :user (System/getenv "EMAIL_USER")
                      :pass (System/getenv "EMAIL_PASSWORD")
                      :tls  true})

(def ^:const from (System/getenv "EMAIL_FROM"))

(defrecord Email [to-email subject body])

(s/def ::to-email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def ::subject string?)
(s/def ::body string?)

(s/def ::email (s/keys :req-un [::to-email ::subject ::body]))

(defn new-email
  "Create Email."
  [email-data]
  (if (s/valid? ::email email-data)
    (map->Email email-data)
    (throw (ex-info "Invalid email data"
                    {:errors (s/explain-str ::email email-data)}))))

(defn send
  "Send email for SMTP Gmail."
  [^Email email]
  (postal/send-message smtp-config {:from    from
                                    :to      (:to-email email)
                                    :subject (:subject email)
                                    :body    (:body email)}))

(defn register
  "Registration user email confirm."
  [user-id]
  (str "Thank you for registering!\n\nComplete link "
       "http://" (System/getenv "SERVER_HOST") ":" (System/getenv "SERVER_PORT")
       "/api/v1/auth/confirm-email?token=" user-id))

(comment

  (send-email "alexandrvirtual@example.com" "Welcome!" "Thank you for registering!")

  :end)
