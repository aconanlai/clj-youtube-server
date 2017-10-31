(ns clj-youtube-server.core
  (:require [clojure.java.jdbc :as sql]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [ring.adapter.jetty :as ring]
            [compojure.route :as route]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/youtuber"))


; TODO: only create table if doesn't already exist

; (sql/db-do-commands spec
;                            (sql/create-table-ddl :comments [[:comment "varchar(120)"] [:time :int]]))

(defn create [comment time]
  (sql/insert! spec :comments [:comment :time] [comment (read-string time)]))

(defn all []
  (into [] (sql/query spec ["select * from comments"])))

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
            :comments (all)}})

  (POST "/video" 
    request
    (let [id (get-in request [:params :id]) comment (get-in request [:params :comment]) time (get-in request [:params :time])]
      (do (create comment time)
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