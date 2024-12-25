(ns marketplace.s3
  (:require
   [cognitect.aws.client.api :as aws]
   [clojure.java.io :as io]))

(def ^:const bucket-name (System/getenv "AWS_MARKETPLACE_BUCKET"))
(def ^:const media "media")
(def ^:const region (System/getenv "AWS_REGION"))

(defn client-s3 []
  (aws/client {:api :s3 :region region}))

(defn- build-key
  [name]
  (str media "/" name))

(defn get-img [name]
  (aws/invoke (client-s3)
              {:op :GetObject
               :request {:Bucket bucket-name
                         :Key (build-key name)}}))

(defn put-img [name body]
  (aws/invoke (client-s3)
              {:op :PutObject
               :request {:Bucket bucket-name
                         :Key (build-key name)
                         :Body body
                         :ContentType "image/jpeg"}}))

(defn img-send-s3 [images]
  (doseq [[name img] images]
    (put-img name img)))

(defn blurp [f]
  (let [dest (java.io.ByteArrayOutputStream.)]
    (with-open [src (io/input-stream f)]
      (io/copy src dest))
    (.toByteArray dest)))

(defn read-image [^String filepath]
  (-> filepath
      io/file
      io/input-stream
      blurp))

(defn upload [f]
  (let [name (str (random-uuid) ".jpeg")]
    (put-img name (blurp f))
    name))

(defn get-url-image
  "Build url image."
  [record]
  (update record :photo (fn [_] (format "https://%s.s3.%s.amazonaws.com/media/%s" bucket-name region (:photo record)))))

(comment

  (def image (read-image "resources/public/img/img.png"))

  (put-img "test1.png" image)

  (def img (get-img "test.png"))

  :end)