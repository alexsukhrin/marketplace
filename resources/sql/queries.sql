-- :name get-user :? :1
SELECT id, email, password FROM users WHERE email = :email

-- :name create-user :? :1
INSERT INTO users (first_name, last_name, email, password)
VALUES(:first-name, :last-name, :email, :password) RETURNING id, first_name, last_name, email, active

-- :name active-user :? :1
UPDATE users SET active=true WHERE id = :user-id

-- :name delete-user :? :*
DELETE FROM users WHERE email = :email

-- :name reset-password-user :? :1
UPDATE users SET password = :password WHERE email = :email
