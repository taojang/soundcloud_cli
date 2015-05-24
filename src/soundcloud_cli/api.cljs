(ns soundcloud-cli.api
  (:require [soundcloud-cli.http :as http]
            [cljs.core.async :as async :refer [<! >! alts! close! chan timeout]]
            [shodan.console :as console :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop]]))

(def base-url "https://api.soundcloud.com")

(defn- query
  "returns a channel will deliver nil when error occurred, otherwise return the result of apply success-fn. success-fn takes the parsed json, failure-fn takes the response map"
  [opts success-fn failure-fn time-out]
  (go
    (let [tc     (timeout time-out)
          res-ch (http/request opts)]
      (if-let [[res _] (alts! [res-ch tc])]
        (if (and (nil? (:err res)) (= 200 (:status res)))
          (success-fn (.parse js/JSON (:body res)))
          (do
            (failure-fn res)
            nil))
        :time-out-err))))



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
                 (fn [res]
                   (console/error
                    (str "Error found while getting access-token. http error: "
                         (if(nil? (:err res))
                           "nil"
                           (:err res))
                         "; error in response: "
                         (if (map? res) (:body res) nil))))
                 time-out)))))
