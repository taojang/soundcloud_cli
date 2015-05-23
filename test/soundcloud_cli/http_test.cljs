(ns soundcloud-cli.http-test
  (:require-macros [cemerick.cljs.test :as tm :refer [is are deftest testing test-var run-tests done]]
                   [cljs.core.async.macros :as am :refer [go]])
  (:require [soundcloud-cli.http :as http]
            [cljs.nodejs :as node]
            [cemerick.cljs.test :as t]
            [cljs.core.async :as async :refer [<! >! chan]]))


(deftest ^:async http-get
  (testing "use http-req to perform get, using chan as output:
            - res-chan has 1 value to take
            - result in res-chan is a map
            - contains keys (:status :headers :body)"
    (let [req-cb (fn [req res]
                   (.write res "pong!")
                   (.end res))
          server (http/create-server req-cb 3333)
          res-ch (http/request {:uri "http://localhost:3333"
                                :method "GET"})]
      (go
        (let [res (<! res-ch)]
          (is (= nil     (:err res)))
          (is (= 200     (:status res)))
          (is (= "pong!" (:body res)))
          (.close server))
        (done)))))

(deftest ^:async core-async-test
  (let [inputs (repeatedly 10000 #(go 1))]
    (go (is (= 10000 (<! (reduce
                           (fn [sum in]
                             (go (+ (<! sum) (<! in))))
                           inputs))))
        (done))))
