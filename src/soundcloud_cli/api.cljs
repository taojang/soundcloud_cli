(ns soundcloud-cli.api
  (:require [soundcloud-cli.http :as http]
            [cljs.core.async :as async :refer [<! >! alts! close! chan timeout]]
            [shodan.console :as console :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop]]))

(def base-url "https://api.soundcloud.com")

(defn get-token
  [c-id c-sec uname pass time-out]
  (go
    (let [tc     (timeout time-out)
          res-ch (http/request {:uri (str base-url "/oauth22/token")
                             :method "POST"
                             :form {:client_id c-id
                                    :client_secret c-sec
                                    :username uname
                                    :password pass
                                    :grant_type "password"
                                    :scope "non-expiring"}})]
      (if-let [[res _] (alts! [res-ch tc])]
        (do
          (if (and (nil? (:err res)) (= 200 (:status res)))
            (.-access-token (.parse js/JSON (:body res)))
            (do
              (console/error (:err res))
              (when-let [b (:body res)] (console/error b))
              nil)))
        :time-out-err))))
