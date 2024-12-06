(ns marketplace.core-test
  (:require [clojure.test :refer :all]
            [marketplace.email :as email]
            [marketplace.users :as user]
            [marketplace.db :as db]
            [mount.core :as mount]
            [marketplace.services :as handler]
            [marketplace.routes :as router]
            [ring.mock.request :as mock]
            [jsonista.core :as j]))

(deftest email-test
  (testing "Send email."
    (is (= (-> {:to-email "alexandrvirtual@gmail.com"
                :subject "Welcome!"
                :body "Thank you for registering!"}
               email/new-email
               email/send)
           {:code    0
            :error   :SUCCESS
            :message "messages sent"}))))

(deftest users-test
  (mount/start #'marketplace.db/*db*)
  (user/delete "alexandrvirtual1@gmail.com")
  (testing "Create new instance User."
    (let [email "alexandrvirtual1@gmail.com"
          first-name "Alexandr"
          last-name "Sukhryn"
          _ (user/delete email)
          user-data {:first-name first-name
                     :last-name last-name
                     :email email
                     :password "password"}
          new-user (-> user-data user/new-user user/insert)]
      (user/delete "alexandrvirtual1@gmail.com")
      (is (= (:email new-user) email))
      (is (= (:first_name new-user) first-name))
      (is (= (:last_name new-user) last-name))))
  (mount/stop #'marketplace.db/*db*))

(deftest register-user-test
  (mount/start #'marketplace.db/*db*)
  (user/delete "alexandrvirtual1232@gmail.com")
  (testing "Register new User."
    (let [email "alexandrvirtual1232@gmail.com"
          first-name "Alexandr"
          last-name "Sukhryn"
          _ (user/delete email)
          user-data {:first-name first-name
                     :last-name last-name
                     :email email
                     :password "password"}
          {:keys [status body]} (handler/register user-data)]
      (user/delete "alexandrvirtual1232@gmail.com")
      (is (= status 201))
      (is (= (:email body) email))
      (is (= (:first_name body) first-name))
      (is (= (:last_name body) last-name))))
  (mount/stop #'marketplace.db/*db*))

(deftest server-api-test
  (mount/start #'marketplace.db/*db*)
  (testing "register"
    (let [{:strs [email last_name first_name active]} (-> (mock/request :post "/api/v1/auth/register")
                                                          (mock/json-body {:first-name "Alexandr"
                                                                           :last-name "Sukhryn"
                                                                           :email "alexandrvirtual987@gmail.com"
                                                                           :password "password"})
                                                          router/app
                                                          :body
                                                          slurp
                                                          j/read-value)]
      (user/delete "alexandrvirtual987@gmail.com")
      (is (= "alexandrvirtual987@gmail.com" email))
      (is (= "Sukhryn" last_name))
      (is (= "Alexandr" first_name))
      (is (= false active))))
  (mount/stop #'marketplace.db/*db*))
