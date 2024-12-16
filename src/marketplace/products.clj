(ns marketplace.products
  (:require
   [marketplace.db :as db]))

(defn get-categories
  "Product categories."
  []
  (db/get-product-categories))

(defn create-product-category
  "Create category."
  [name photo]
  (db/create-product-category {:name name
                               :photo photo}))
