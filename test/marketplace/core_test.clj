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
    (let [_ (user/delete "alexandrvirtual@gmail.com")
          {:strs [message token]}
          (-> (mock/request :post "/api/v1/auth/register")
              (mock/json-body {:first_name "Alexandr"
                               :last_name "Sukhryn"
                               :email "alexandrvirtual@gmail.com"
                               :password "password"})
              router/app
              :body
              slurp
              j/read-value)]
      (is (= true (string? token)))
      (is (= "Login successful" message)))))

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

(deftest test-user-create-categories
  (testing "Create user categories"
    (let [{:strs [token]} (-> (mock/request :post "/api/v1/auth/login")
                              (mock/json-body {:email "alexandrvirtual@gmail.com"
                                               :password "password"})
                              router/app
                              :body
                              slurp
                              j/read-value)
          {:strs [message]} (-> (mock/request :post "/api/v1/users/categories")
                                (mock/header "authorization" (str "Bearer " token))
                                (mock/json-body {:categories [{:category_id 1}
                                                              {:category_id 2}]})
                                router/app
                                :body
                                slurp
                                j/read-value)]
      (is (= "created" message)))))

(deftest test-create-user
  (testing "Create user"
    (let [{:strs [token]} (-> (mock/request :post "/api/v1/auth/login")
                              (mock/json-body {:email "alexandrvirtual@gmail.com"
                                               :password "password"})
                              router/app
                              :body
                              slurp
                              j/read-value)
          {:strs [message]} (-> (mock/request :post "/api/v1/users/create")
                                (mock/header "authorization" (str "Bearer " token))
                                (mock/json-body {:is_buyer true
                                                 :is_seller true})
                                router/app
                                :body
                                slurp
                                j/read-value)]
      (is (= "created" message)))))

(deftest test-user-reset-password-otp
  (testing "Reset user password"
    (let [{:strs [message]} (-> (mock/request :post "/api/v1/auth/reset-password")
                                (mock/json-body {:email "alexandrvirtual@gmail.com"})
                                router/app
                                :body
                                slurp
                                j/read-value)
          {:strs [message token]} (-> (mock/request :post "/api/v1/auth/otp")
                                      (mock/json-body {:email "alexandrvirtual@gmail.com"
                                                       :otp message})
                                      router/app
                                      :body
                                      slurp
                                      j/read-value)]
      (is (= "Login successful" message))
      (is (= true (string? token))))))
