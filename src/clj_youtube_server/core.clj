(ns clj-youtube-server.core
  (:require [clojure.java.jdbc :as sql]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [ring.adapter.jetty :as ring]
            [compojure.route :as route]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/youtuber"))


;; todo: schema validation on routes (e.g. no spaces in id, etc)

(defn db-exists?
  [name]
  (-> (sql/query spec
                 [(str "select count(*) from information_schema.tables "
                       "where table_name='" name "'")])
      first :count pos?))

(defn create-table
  [name]
  (when (not (db-exists? name))
   (sql/db-do-commands spec
    (sql/create-table-ddl name [[:comment "varchar(120)"] [:time :int]]))))

(defn create-comment [id comment time]
  (sql/insert! spec id [:comment :time] [comment (read-string time)]))

(defn get-comments [id]
  (into [] (sql/query spec [(str "select * from " id)])))

;; defroutes macro defines a function that chains individual route
;; functions together. The request map is passed to each function in
;; turn, until a non-nil response is returned.
(defroutes app-routes
  ; serve root
  (GET "/" [] {:status 200
               :body {}})

  ; serve a specific video id
  (GET "/video/:id" [id]
    {:status 200
     :body {:id id
            :comments (get-comments id)}})

  (POST "/video" 
    request
    (let [id (get-in request [:params :id]) comment (get-in request [:params :comment]) time (get-in request [:params :time])]
      (do
        (create-table id)
        (create-comment id comment time)
       {:status 200
        :body {:id id
               :comment comment}}))))

  ; if page is not found
(route/not-found "404")

;; site function creates a handler suitable for a standard website,
;; adding a bunch of standard ring middleware to app-route:
(def handler
  (-> (handler/site app-routes)
    (middleware/wrap-json-body {:keywords? true})
    middleware/wrap-json-response))
  
(defn -main []
  (ring/run-jetty #'app-routes {:port 8080 :join? false}))