(ns soundcloud-cli.http
  (:require [cljs.core.async :as async :refer [<! >! chan alts! close!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; (defn get [url]
;;   "wrapper of node/http get function, takes an url and optional map of options, returns vector of [error-channel result channel]"
;;   (let [err-chan (chan)
;;         res-chan (chan)]
;;     (go )))

(def fs-chans
  (let [err-chan (chan)
        res-chan (chan)]
    (go
      (.readFile fs "/tmp/testing" "utf8"
                 (fn [err, res]
                   (if (nil? err)
                     (do
                       (close! err-chan)
                       (>! res-chan res))
                     (do
                       (close! res-chan)
                       (>! err-chan err)))))
      [err-chan res-chan])))
