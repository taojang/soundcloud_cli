(ns soundcloud-cli.http
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as async :refer [<! >! chan alts! close!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def n-rq (node/require "request"))
(def node-http (node/require "http"))

(defn- rq-cb
  [res-chan]
  (fn [err res body]
    (let [status (if (nil? res) nil (.-statusCode res))
          headers (if (nil? res) {} (.-headers res))
          error (if (nil? err) nil (.toString err))]
    (go
      (>! res-chan {:status status
                    :headers (js->clj headers)
                    :err error
                    :body body})
      (close! res-chan)))))

(defn request
  "Wrapper of request lib on nodejs. opts should be a clojure map"
  [opts]
  (let [res-chan (chan)]
    (n-rq (clj->js opts) (rq-cb res-chan))
    res-chan))

(defn create-server
  "Create a http server on given port with supplied callback."
  [cb port]
  (let [s (.createServer node-http cb)]
    (.listen s port)
    s))
