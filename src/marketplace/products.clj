(ns marketplace.products
  (:require
   [marketplace.db :as db]))

(defn get-categories
  "Product categories."
  []
  (db/get-product-categories))
