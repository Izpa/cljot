(ns core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [core :as sut]))

(deftest hello-somebody-test
  (testing "Simple test example"
    (is (= "Hello, world!"
           (sut/hello-somebody "world")))))
