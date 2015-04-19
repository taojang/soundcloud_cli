(ns soundcloud-cli.http-test
  (:require-macros [cemerick.cljs.test :as tm :refer [is are deftest testing test-var run-tests done]]
                   [cljs.core.async.macros :as am :refer [go]])
  (:require [soundcloud-cli.http :as http]
            [cljs.nodejs :as node]
            [cemerick.cljs.test :as t]
            [cljs.core.async :as async :refer [<! >! chan]]))


(deftest ^:async http-get
  (testing "use http-req to perform get,using chan as output:
            - should return a map
            - map should have keys (:err-chan :res-chan)
            - err-chan should be closed
            - res-chan has 1 value to take
            - result in res-chan is a map
            - contains keys (:status :headers :body)
            - body is a chan"
    (let [req-cb (fn [req res]
                   (.write res "pong!")
                   (.end res))
          server (http/create-server req-cb 3333)
          chans (http/request {:hostname "localhost"
                               :port 3333
                               :method "GET"})
          err-chan  (:err-chan chans)
          body-chan (:body-chan chans)
          res-chan  (:res-chan chans)]
      (go
        (is (= true  (map? chans)))
        (is (= 3     (count chans)))
        (is (= true  (and (contains? chans :err-chan) (contains? chans :res-chan))))
        (is (= nil   (<! err-chan)))
        (is (= 200   (:status (<! res-chan))))
        (is (= "pong!" (-> (<! body-chan) (str))))
        (.close server)
      (done)))))

(deftest chan-test
  (is (= true (http/chan? (chan)))))
