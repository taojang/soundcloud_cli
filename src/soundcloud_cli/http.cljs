(ns soundcloud-cli.http
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as async :refer [<! >! chan alts! close!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def node-http (node/require "http"))
(def node-https (node/require "https"))
(def node-url (node/require "url"))

(defn atom?
  [arg]
  (= (type (atom)) (type arg)))

(defn chan?
  [arg]
  (satisfies? cljs.core.async.impl.protocols/Channel arg))

(defn chan-or-atom?
  "Determine if supplied argument is a channel or vector"
  [arg]
  (cond
    (chan? arg) ::channel
    (atom? arg) ::atom
    :else (throw js/Error "unsupported argument type")))


(defn- gen-put
  "Generate a function put data into channel or conj to vector inside an atom"
  [out]
  (condp = (chan-or-atom? out)
    ::channel (fn [data] (go (println (str "putting: " data)) (>! out data) (println "done putting")))
    ::atom    (fn [data] (swap! out conj data))))

(defn- gen-close
  "Generate a function to close a channel or do nothing for atom"
  [out]
  (condp = (chan-or-atom? out)
    ::channel (fn [] (close! out))
    ::atom    (fn [] nil)))

(defn- gen-end-cb
  [out & others]
  (let [fns (map gen-close (conj others out))]
    (fn []
      (doseq [f fns]
        (f)))))

(defn- gen-err-cb
  [err-out & others]
  (let [close-out! (apply gen-end-cb (conj others err-out))
        put-err! (gen-put err-out)]
    (fn [err]
      (put-err! err)
      (close-out!))))

(defn- request-cb
  [err-chan res-chan body-chan]
  (fn [res]
    (.pause res)
    (go (>! res-chan {:status (.-statusCode res) :headers (js->clj (.-headers res))}))
    (.on res "readable"
         (fn []
           (go-loop []
             (when-let [d (.read res)]
               (>! body-chan d)
               (recur)))))
    (.on res "end"
         (gen-end-cb err-chan res-chan body-chan))
    (.on res "close"
         (gen-end-cb err-chan res-chan body-chan))
    (.on res "error"
         (gen-err-cb err-chan res-chan body-chan))))

;; only handles string input for now
(defn request
  "Wrapper of http(s) request function on node.js. Takes a map of request options or string will be later parsed by url.parse.
  Input could be optionally supplied"
  [opts & {:keys [input]
           :or {input ""}
           :as more}]
  (let [opts      (if (string? opts) (.parse node-url opts) (clj->js opts))
        h         (if (= "https:" (.-protocol opts)) node-https node-http)
        err-chan   (chan)
        body-chan  (chan)
        res-chan   (chan)
        req        (.request h opts
                             (request-cb err-chan res-chan body-chan))]
    (.write req input)
    (.end req)
    {:err-chan err-chan :res-chan res-chan :body-chan body-chan}))
