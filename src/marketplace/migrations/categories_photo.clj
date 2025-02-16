(ns marketplace.migrations.categories_photo
  (:require
   [marketplace.s3 :as s3]
   [marketplace.db :as db]))

(defn migrate-up [config]
  (println config)
  (for [i (range 1 14)
        :let [name (str i ".jpg")
              image (s3/read-image (str "resources/public/img/categories/" name))]]
    (do
      (s3/put-img name image)
      (db/set-photo-product-category {:photo name
                                      :category_id i})))
  (println "migrate categories up"))

(defn migrate-down [config]
  (println config)
  (println "migrate categories down"))
