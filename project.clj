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
                 [compojure "1.7.1"]
                 [hikari-cp "3.0.1"]
                 [conman/conman "0.9.6"]
                 [org.postgresql/postgresql "42.7.2"]
                 [mount "0.1.18"]]
  :main ^:skip-aot marketplace.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
