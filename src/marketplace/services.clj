(ns marketplace.services
  (:require
   [clojure.tools.logging :as log]
   [hiccup.page :refer [html5 include-css include-js]]
   [marketplace.users :as user]
   [marketplace.email :as email]
   [marketplace.products :as product]
   [marketplace.s3 :as s3]))

(defn confirm-email
  "Confirm Email."
  [token]
  (log/info "Confirm token: " token)
  (user/activate (parse-uuid token))
  "complete")

(defn register
  "Registration new User."
  [user-data]
  (try
    (let [{:keys [id email first_name last_name active]} (-> user-data user/new-user user/insert)
          register-link (email/register-link id)
          email-body (email/register-email register-link)]
      (email/send-to email email-body)
      {:status 200
       :body {:user-id id
              :first-name first_name
              :last-name last_name
              :email email
              :active active
              :register-link register-link}})
    (catch Exception e
      {:status 400
       :body {:error (println (str "caught exception: " (.getMessage e)))}})))

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
  [email password]
  (user/reset-password email password)
  {:status 200
   :body {:message "Password update"}})

(defn create-reset-link
  "Create reset link for User."
  [email]
  (when-let [user (user/get-user email)]
    (let [{:keys [id email]} user
          reset-link (email/reset-link id)]
      (->> (email/reset-email reset-link)
           (email/send-to email))
      reset-link)))

(defn reset-password-page
  "Reset password page."
  [token]
  {:status 200
   :body (str (html5
               [:head
                [:title "Reset Password"]
                [:meta {:charset "UTF-8"}]
                [:meta {:content "width=device-width, initial-scale=1.0", :name "viewport"}]
                (include-css "https://cdnjs.cloudflare.com/ajax/libs/tailwindcss/2.2.19/tailwind.min.css")
                [:style "
                    body {
                      background-color: #f9fafb;
                      font-family: Arial, sans-serif;
                    }
                    .form-container {
                      max-width: 400px;
                      margin: 50px auto;
                      background: white;
                      padding: 20px;
                      border-radius: 8px;
                      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                    }
                    .form-container h1 {
                      margin-bottom: 20px;
                      font-size: 1.5rem;
                      color: #333;
                      text-align: center;
                    }
                    .form-container input {
                      width: 100%;
                      padding: 10px;
                      margin: 10px 0;
                      border: 1px solid #ccc;
                      border-radius: 4px;
                    }
                    .form-container button {
                      width: 100%;
                      padding: 10px;
                      background: #4CAF50;
                      color: white;
                      border: none;
                      border-radius: 4px;
                      cursor: pointer;
                    }
                    .form-container button:hover {
                      background: #45a049;
                    }
                  "]]

               [:body
                [:div {:class "form-container"}
                 [:h1 "Reset Password"]
                 [:img {:src "https://marketplace-bucket-fpryyk.s3.eu-central-1.amazonaws.com/b0748993-897b-4447-9bbb-cf7b81fba781.png" :alt "Приклад зображення"}]
                 [:form {:id "reset-password-form"}
                  [:input {:type "email" :name "email" :placeholder "Enter your email" :required true}]
                  [:input {:type "password" :name "new-password" :placeholder "Enter new password" :required true}]
                  [:input {:type "password" :name "confirm-password" :placeholder "Confirm new password" :required true}]
                  [:button {:type "submit"} "Reset Password"]]]
                (include-js "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js")
                [:script "
                      $(document).ready(function() {
                        $('#reset-password-form').on('submit', function(event) {
                          event.preventDefault();

                          const email = $('input[name=\"email\"]').val();
                          const password = $('input[name=\"new-password\"]').val();
                          const confirmPassword = $('input[name=\"confirm-password\"]').val();

                          if (password !== confirmPassword) {
                            alert('Passwords do not match!');
                            return;
                          }

                          fetch('http://localhost:4000/api/v1/auth/reset-password',
                          {method: 'PATCH',
                           headers: {
                           'Content-Type': 'application/json'
                           },
                           body: JSON.stringify({ email, password })
                           })
                           .then(response => {
                                if (!response.ok) {
                                     throw new Error('Failed to reset password');
                                }            return response.json();
                           })
                           .then(data => {
                                alert('Password successfully reset!');
                                console.log('Response:', data);
                           })
                           .catch(error => {
                                console.error('Error:', error);
                           alert('Error resetting password. Please try again.');
                          });
                        });
                      });
                    "]]))})

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
    (let [values (mapv #(vector (parse-uuid user-id) (:category-id %)) categories)]
      (log/info "Create user categories " values)
      (user/create-user-categories values))
    {:status 201
     :body {:message "created"}}
    (catch Exception e
      (let [error (str "Create user categories exception " (.getMessage e))]
        (log/error error)
        {:status 400
         :body {:error error}}))))

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
