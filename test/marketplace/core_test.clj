(ns marketplace.core-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [marketplace.db :as db]
            [marketplace.users :as user]
            [marketplace.routes :as router]
            [ring.mock.request :as mock]
            [jsonista.core :as j]))

(use-fixtures :once
  (fn [tests]
    (mount/start #'marketplace.db/*db*)
    (try
      (tests)
      (finally
        (mount/stop #'marketplace.db/*db*)))))

(deftest test-register-user
  (testing "Register user"
    (let [{:strs [email last-name first-name active register-link]}
          (-> (mock/request :post "/api/v1/auth/register")
              (mock/json-body {:first-name "Alexandr"
                               :last-name "Sukhryn"
                               :email "alexandrvirtual@gmail.com"
                               :password "password"})
              router/app
              :body
              slurp
              j/read-value)]
      (is (= "alexandrvirtual@gmail.com" email))
      (is (= "Sukhryn" last-name))
      (is (= "Alexandr" first-name))
      (is (= false active))
      (is (= true (string? register-link))))))

(deftest test-login-user
  (testing "Login user"
    (let [{:strs [message token]} (-> (mock/request :post "/api/v1/auth/login")
                                      (mock/json-body {:email "alexandrvirtual@gmail.com"
                                                       :password "password"})
                                      router/app
                                      :body
                                      slurp
                                      j/read-value)]
      (is (= true (string? token)))
      (is (= "Login successful" message)))))

(deftest test-reset-password-user
  (testing "Reset password user"
    (let [{:strs [message]} (-> (mock/request :post "/api/v1/auth/reset-password")
                                (mock/json-body {:email "alexandrvirtual@gmail.com"})
                                router/app
                                :body
                                slurp
                                j/read-value)]
      (is (= true (string? message))))))

(deftest test-register-user-fail
  (testing "Register user with fail user-data"
    (let [response (-> (mock/request :post "/api/v1/auth/register")
                       (mock/json-body {:first-name "Al"
                                        :last-name "S"
                                        :email "alexandrvirtual987"
                                        :password "passw"})
                       router/app)]
      (is (= 400 (:status response))))))

(deftest test-user-create-categories
  (testing "Create user categories"
    (let [{:strs [token]} (-> (mock/request :post "/api/v1/auth/login")
                              (mock/json-body {:email "alexandrvirtual@gmail.com"
                                               :password "password"})
                              router/app
                              :body
                              slurp
                              j/read-value)
          response (-> (mock/request :post "/api/v1/users/category")
                       (mock/header "authorization" (str "Bearer " token))
                       (mock/json-body {:categories [{:category-id 1}
                                                     {:category-id 2}]})
                       router/app)]
      response)))
;(is (= "" response))

(deftest test-user-delete
  (testing "Delete user"
    (is (= 0 (user/delete "alexandrvirtual@gmail.com")))))

(deftest test-user-reset-password-otp
  (testing "Create user categories"
    (let [{:strs [message]} (-> (mock/request :post "/api/v1/auth/reset-password")
                                (mock/json-body {:email "rashiki44@gmail.com"})
                                router/app
                                :body
                                slurp
                                j/read-value)
          response (-> (mock/request :post "/api/v1/auth/otp")
                       (mock/json-body {:email "rashiki44@gmail.com"
                                        :otp message})
                       router/app
                       :body
                       slurp
                       j/read-value)]
      response)))
