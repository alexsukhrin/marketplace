(defproject marketplace "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [http-kit "2.7.0"]
                 [ring/ring-core "1.12.0"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-defaults "0.4.0"]
                 [ring-logger/ring-logger "1.1.1"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [metosin/jsonista "0.3.8"]
                 [metosin/reitit "0.7.2"]
                 [metosin/ring-swagger-ui "5.9.0"]
                 [hikari-cp "3.0.1"]
                 [conman/conman "0.9.6"]
                 [org.postgresql/postgresql "42.7.2"]
                 [mount "0.1.18"]
                 [org.clojure/core.cache "1.1.234"]
                 [com.draines/postal "2.0.4"]
                 [migratus "1.6.3"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.4.11"]
                 [buddy/buddy-hashers "2.0.167"]
                 [buddy/buddy-sign "3.5.351"]]
  :plugins [[migratus-lein "0.7.3"]]
  :main ^:skip-aot marketplace.core
  :target-path "target/%s"
  :profiles {:prod {:migratus {:store :database
                               :migration-dir "migrations"
                               :db {:dbtype "postgresql"
                                    :dbname ~(System/getenv "POSTGRES_DB")
                                    :host ~(System/getenv "POSTGRES_HOST")
                                    :user ~(System/getenv "POSTGRES_USER")
                                    :password ~(System/getenv "POSTGRES_PASSWORD")}}}
             :test {:migratus {:store :database
                               :migration-dir "migrations"
                               :db {:dbtype "postgresql"
                                    :dbname ~(System/getenv "POSTGRES_TEST_DB")
                                    :host ~(System/getenv "POSTGRES_HOST")
                                    :user ~(System/getenv "POSTGRES_USER")
                                    :password ~(System/getenv "POSTGRES_PASSWORD")}}
                    :dependencies [[ring/ring-mock "0.4.0"]]}
             :dev {:migratus {:store :database
                              :migration-dir "migrations"
                              :db {:dbtype "postgresql"
                                   :dbname ~(System/getenv "POSTGRES_DEV_DB")
                                   :host ~(System/getenv "POSTGRES_HOST")
                                   :user ~(System/getenv "POSTGRES_USER")
                                   :password ~(System/getenv "POSTGRES_PASSWORD")}}}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]}})
