(defproject clj-youtube-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0"]
                 [ring/ring-json "0.3.1"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :plugins [[lein-ring "0.8.8"]
            [compojure "1.1.6"]]
  :main clj-youtube-server.core
  :ring {:handler clj-youtube-server.core/handler})
