-- :name get-user :? :1
SELECT id, email, password FROM users WHERE email = :email

-- :name create-user :? :1
INSERT INTO users (first_name, last_name, email, password)
VALUES(:first-name, :last-name, :email, :password) RETURNING id, first_name, last_name, email, active

-- :name active-user :? :1
UPDATE users SET active=true WHERE id = :user-id

-- :name delete-user :? :*
DELETE FROM users WHERE email = :email

-- :name update-password-user :? :1
UPDATE users SET password = :password WHERE email = :email

-- :name create-buyer :? :1
INSERT INTO buyers (user_id) VALUES (:user_id)

-- :name delete-buyer :? :1
DELETE FROM buyers WHERE user_id = :user_id

-- :name create-seller :? :1
INSERT INTO sellers (user_id) VALUES (:user_id)

-- :name delete-seller :? :1
DELETE FROM sellers WHERE user_id = :user_id

-- :name get-product-categories :? :*
SELECT category_id, name, photo FROM categories ORDER BY name

-- :name create-product-category :? :1
INSERT INTO categories (name, photo) VALUES (:name, :photo) RETURNING category_id, name

-- :name set-photo-product-category :? :1
UPDATE categories SET photo = :photo WHERE category_id = :category_id

-- :name create-user-categories :? :*
INSERT INTO user_categories (user_id, category_id) VALUES :t*:categories

-- :name delete-user-categories :? :1
DELETE FROM user_categories WHERE user_id = :user_id

-- :name create-otp :? :1
INSERT INTO otp_tokens (user_id) VALUES (:user_id) RETURNING otp

-- :name get-otp :? :1
SELECT user_id FROM otp_tokens WHERE otp = :otp AND user_id = :user_id AND expires_at >= NOW()
