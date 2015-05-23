(ns soundcloud-cli.api
  (:require [soundcloud-cli.http :as http]
            [cljs.core.async :as async :refer [<! >! alts! close! chan timeout]])
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop]]))

;; (defn- grant-code-cb
;;   [chan path]
;;   (fn [req res]
;;     (let [parsed-url (.parse http/node-url (.-url req) true)
;;           query (.-query parsed-url)
;;           pathname  (.-pathname parsed-url)]
;;       (when (= path pathname)
;;         (when-let [code (.-code query)]
;;           (go
;;             (>! chan code)
;;             (close! chan))))
;;       (.write res "Please close the tab.")
;;       (.end res))))

;; (defn- get-grant-code
;;   [path port]
;;   (go
;;     (let [c (chan)
;;           s (http/create-server (grant-code-cb c path) port)
;;           t (<! c)]
;;       (do
;;         (.close s)
;;         t))))

;; (defn get-token
;;   "Use port to create a server which listen on this port and request on supplied path with 'code' query string. Soundcloud will redirect user after authentication on https://soundcloud.com/connect "
;;   [c-id c-sec path port time-out]
;;   (go
;;     (let [tc (timeout time-out)
;;           gc (get-grant-code path port)]
;;       (if-let [[grant-code _] (alts! [tc gc])]
;;         (let [opts {:hostname "api.soundcloud.com"
;;                     :method "POST"
;;                     :protocol "https:"
;;                     :path "/oauth2/token"
;;                     ;; looks like params should be request body...
;;                     :params {:client_id c-id
;;                              :client_secret c-sec
;;                              :grant_type "authorization_code"
;;                              :redirect_url (str "http://localhost:" port path)
;;                              :code grant-code}
;;                     :headers {:Content-Length 0}} ;; TODO  write function to generate headers....
;;               {:keys [err-chan body-chan res-chan]} (http/request opts)]
;;           (prn grant-code)
;;           (if-let [[res _] (alts! [body-chan (timeout time-out)])]
;;             (do (.log js/console (str res))
;;                 (.-access-token (.parse js/JSON res)))
;;             :time-out-err))
;;         :time-out-err))))
