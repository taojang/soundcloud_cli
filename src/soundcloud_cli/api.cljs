(ns soundcloud-cli.api
  (:require [soundcloud-cli.http :as http]
            [cljs.core.async :as async :refer [<! >! alts! close! chan timeout]]
            [shodan.console :as console :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]]))

(def base-url "https://api.soundcloud.com")

(def base-url-v2 "https://api-v2.soundcloud.com")

(defn- query
  "returns a channel will deliver nil when error occurred, otherwise
  return the result of apply success-fn.
  success-fn takes the parsed json, failure-fn takes the response map"
  [opts success-fn failure-fn time-out]
  (go
    (let [tc     (timeout time-out)
          res-ch (http/request opts)]
      (alt!
        tc :time-out-err
        res-ch ([v _] (if (and (nil? (:err v)) (= 200 (:status v)))
                        (success-fn (.parse js/JSON (:body v)))
                        (do
                          (failure-fn v)
                          nil)))))))

(defn- err-cb
  [msg]
  (fn [res]
    (console/error
     (str msg " http error: "
          (if(nil? (:err res))
            "nil"
            (:err res))
          "; error in response: "
          (if (map? res) (:body res) nil)))))

(defn- parse-body
  [body]
  (js->clj body :keywordize-keys true))

(defn wrap-app-cred
  ; TODO optionally wrap client_id client_secret oauth_token
  [c-id c-sec {:keys [method]
               :or {method "GET"}
               :as opts}]
  (let [c-map {:client_id c-id
               :client_secret c-sec}]
    (if (= "GET" method)
      (assoc opts :qs (merge (:qs opts) c-map))
      (assoc opts :form (merge (:form opts) c-map)))))

(defn get-token
  [c-id c-sec uname pass time-out]
  (go
    (let [opts {:uri (str base-url "/oauth2/token")
                :method "POST"
                :form {:client_id c-id
                       :client_secret c-sec
                       :username uname
                       :password pass
                       :grant_type "password"
                       :scope "non-expiring"}}]
      (<! (query opts
                 (fn [body]
                   (.-access-token body))
                 (err-cb "Error found while getting access-token.")
                 time-out)))))

(defn get-user
  [c-id uid time-out]
  (go
    (let [opts {:uri (str base-url "/users/" uid ".json")
                :qs {:client_id c-id}
                :method "GET"}]
      (<! (query opts
                 parse-body
                 (err-cb (str "Error found while getting user: " uid "."))
                 time-out)))))
(defn get-me
  [token time-out]
  (go
    (let [opts {:uri (str base-url "/me.json")
                :method "GET"
                :qs {:oauth_token token}}]
      (<! (query opts
                 parse-body
                 (err-cb "Error found while getting current logged in user")
                 time-out)))))

(defn get-stream
  [token time-out & offset]
  (go
    (let [opts {:uri (str base-url-v2 "/stream")
                :method "GET"
                :qs {:oauth_token token}}]
      (<! (query opts
                 parse-body
                 (err-cb "Error found while getting stream of current logged in user")
                 time-out)))))
