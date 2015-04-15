(ns soundcloud-cli.http
  (:require [cljs.core.async :as async :refer [<! >! chan alts! close!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def node-http (node/require "http"))

(defn get [url]
  "wrapper of node/http get function, takes target url, returns vector of [error-channel response-channel]"
  (let [err-chan  (chan)
        res-chan  (chan)
        data      (atom "")
        response  (atom {})
        data-cb   (fn [chunk] (swap! data #(str % chunk) @data))
        err-cb    (fn [err] (go (>! err-chan err)
                                (close! res-chan) (close! err-chan)))
        end-cb    (fn []
                    (go (>! res-chan (assoc @response :body @data))
                        (close! res-chan) (close! err-chan)))
        get-cb    (fn [res]
                    (swap! response #(-> %
                                         (assoc :status  (.-statusCode res))
                                         (assoc :headers (js->clj (.-headers res))))
                           @response)
                    (.on res "data" data-cb)
                    (.on res "error" err-cb)
                    (.on res "end" end-cb))]
    (-> (.get node-http url get-cb) (.on "error" err-cb))
    [err-chan res-chan]))
