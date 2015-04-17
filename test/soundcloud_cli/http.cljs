(ns soundcloud-cli.test.http
  (:require-macros [cemerick.cljs.test :as tm :refer [is are deftest testing test-var run-tests done]]
                   [cljs.core.async.macros :as am :refer [go]])
  (:require [soundcloud-cli.http :as http]
            [cljs.nodejs :as node]
            [cemerick.cljs.test :as t]
            [cljs.core.async :as async :refer [<!]]))

(deftest ^async http-req
  (testing "use http-req to perform get,using chan as output:
            - should return a map
            - map should have keys (:err-chan :res-chan)
            - err-chan should be closed
            - res-chan has 1 value to take
            - result in res-chan is a map
            - contains keys (:status :headers :body)
            - body is a chan"
    (let [chans (http/http-req {:hostname "ipecho.net"
                                :path "/plain"
                                :method "GET"}
                               :out-type :chan)
          err-chan (:err-chan chans)
          res-chan (:res-chan chans)]
      (go
        (are [x y] (= x y)
           true  (map? chans)
           2     (count chans)
           true  (and (contains? chans :err-chan) (contains? chans :res-chan))
           nil   (<? err-chan))
        (done)))))
