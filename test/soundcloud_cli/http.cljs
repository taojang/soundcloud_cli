(ns soundcloud-cli.test.http
  (:require-macros [cemerick.cljs.test :as m :refer [is are deftest testing test-var run-tests done]])
  (:require [soundcloud-cli.http :as http]
            [cljs.nodejs :as node]
            [cemerick.cljs.test :as t]))

(deftest failing-test
  (testing "FIXME! I fail"
    (is (= 1 0))))
