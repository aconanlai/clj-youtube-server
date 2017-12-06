(ns clj-youtube-server.core
  (:require [clojure.java.jdbc :as sql]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.middleware.json :as middleware]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.adapter.jetty :as ring]
            [compojure.route :as route]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/youtuber"))

(defn generate-table-name
  "generates table name from youtube video id, stripping non-alphanumeric characters and ensuring table name starts with 'video'"
  [id]
  (str "video_" (apply str (re-seq #"[a-zA-Z0-9]" id))))

(defn table-exists?
  [name]
  (-> (sql/query spec
                 ["select count(*) from information_schema.tables where table_name =?" name])
      first :count pos?))

;; TODO: error handling
;; TODO: handle non-alpha first character in id
(defn create-table
  [name]
  (when-not (table-exists? name)
        (try
         (sql/db-do-commands spec
          (sql/create-table-ddl name [[:comment "varchar(120)"] [:time :int]]))
         (catch Exception e (println (.getNextException e))))))
  

(defn create-comment [id comment time]
  (try
   (sql/insert! spec id [:comment :time] [comment (read-string time)])
   (catch Exception e (println (.getNextException e)))))

(defn get-comments [id]
  (if (table-exists? id)
   (into [] (sql/query spec [(str "select distinct time, comment from " id " order by time")]))
   []))

;; defroutes macro defines a function that chains individual route
;; functions together. The request map is passed to each function in
;; turn, until a non-nil response is returned.
(defroutes app-routes
  ; serve root
  (GET "/" [] {:status 200
               :body {}})

  ; serve a specific video id
  (GET "/video/:id" [id]
    (let [id (generate-table-name (clojure.string/lower-case id))]
     {:status 200
      :body {:id id
             :comments (get-comments (clojure.string/lower-case id))}}))

  (POST "/video" 
    request
    (let [id (generate-table-name (clojure.string/lower-case (get-in request [:body :id])))
          comment (get-in request [:body :comment])
          time (get-in request [:body :time])]
      (do
        (create-table id)
        (create-comment id comment (str time))
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
    middleware/wrap-json-response
    (wrap-cors routes #".*")
    (wrap-cors routes identity)))
  
(defn -main []
  (ring/run-jetty #'app-routes {:port 8080 :join? false}))