(ns marketplace.core-test
  (:require [clojure.test :refer :all]
            [marketplace.email :as email]
            [marketplace.users :as user]
            [marketplace.db :as db]
            [mount.core :as mount]
            [marketplace.routes :as router]
            [ring.mock.request :as mock]
            [jsonista.core :as j]))

(deftest test-email
  (testing "Send email."
    (let [register-link (email/register-link "test-user")
          register-email (email/register-email register-link)
          response (email/send-to "alexandrvirtual@gmail.com" register-email)]
      (is (= "http://localhost:4000/api/v1/auth/confirm-email?token=test-user" register-link))
      (is (= "Thank you for registering!\n\nComplete link http://localhost:4000/api/v1/auth/confirm-email?token=test-user" register-email))
      (is (= {:code 0 :error :SUCCESS :message "messages sent"} response)))))

(deftest test-register-user
  (mount/start #'marketplace.db/*db*)
  (testing "Register user"
    (let [{:strs [user-id email last-name first-name active register-link]}
          (-> (mock/request :post "/api/v1/auth/register")
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
      (is (= "Sukhryn" last-name))
      (is (= "Alexandr" first-name))
      (is (= false active))
      (is (= (email/register-link user-id) register-link))))
  (mount/stop #'marketplace.db/*db*))

(deftest test-register-user-fail
  (mount/start #'marketplace.db/*db*)
  (testing "Register user with fail user-data"
    (let [response (-> (mock/request :post "/api/v1/auth/register")
                       (mock/json-body {:first-name "Al"
                                        :last-name "S"
                                        :email "alexandrvirtual987"
                                        :password "passw"})
                       router/app)]
      (is (= 400 (:status response)))))
  (mount/stop #'marketplace.db/*db*))
