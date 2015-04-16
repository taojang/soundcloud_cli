(ns soundcloud-cli.http
  (:require [cljs.core.async :as async :refer [<! >! chan alts! close!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def node-http (node/require "http"))

(def node-url (node/require "url"))

(defn chan-or-atom?
  "Determine if supplied argument is a channel or vector"
  [arg]
  (cond
    (satisfies? cljs.core.async.impl.protocols/Channel arg) ::channel
    (atom? arg) ::atom
    :else ::unknown))

(defn- gen-put
  "Generate a function put data into channel or conj to vector inside an atom"
  [out]
  (condp = (chan-or-atom? out)
    ::channel (fn [data] (go (>! out data)))
    ::atom    (fn [data] (swap! out conj data))))

(defn- gen-close
  "Generate a function to close a channel or do nothing for atom"
  [c]
  (condp = (chan-or-atom? c)
    ::channel (fn [] (close! c))
    ::atom    (fn [] nil)))

(defn- gen-data-cb
  "Generate callback function for response on data event. Take either atom or chan as argument. If there are multiple output channels/atoms, generated callback will put same data on all of them"
  [out & others]
  (let [fns (map gen-put (conj others out))]
    (fn [data]
      (doall (map #(apply % data) fns)))))

(defn- gen-end-cb
  [c & others]
  (let [fns (map gen-close (conj others c))]
    (fn []
      (doseq [f fns]
        (f)))))


(defn get
  "Wrapper of node/http get function, takes target url, returns vector of [error-channel response-channel]"
  [url]
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
