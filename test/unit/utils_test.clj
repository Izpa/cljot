(ns utils-test
  (:require
   [clojure.test :refer [deftest is]]
   [utils :as sut]))

(def simple-map {:foo "bar" :foo1 "bar1"})
(def simple-vector [:foo "bar" :foo1 "bar1"])
(def map-with-secret {:foo "bar" :secret-foo "bar1"})
(def map-with-secret-hidden {:foo "bar", :secret-foo "hidden-secret"})

(def complicated-map
  {:vector simple-vector
   :secret-vector simple-vector
   :map simple-map
   :secret-map simple-map
   :nested-map map-with-secret})

(def complicated-map-hidden
  {:vector [:foo
            "bar"
            :foo1
            "bar1"]
   :secret-vector ["hidden-secret"
                   "hidden-secret"
                   "hidden-secret"
                   "hidden-secret"]
   :map {:foo "bar"
         :foo1 "bar1"}
   :secret-map {:foo "hidden-secret"
                :foo1 "hidden-secret"}
   :nested-map {:foo "bar"
                :secret-foo "hidden-secret"}})

(def complicated-nested-map (assoc complicated-map :nested-complicated-map complicated-map))

(def complicated-nested-map-hidden
  {:vector [:foo
            "bar"
            :foo1
            "bar1"]
   :secret-vector ["hidden-secret"
                   "hidden-secret"
                   "hidden-secret"
                   "hidden-secret"]
   :map {:foo "bar"
         :foo1 "bar1"}
   :secret-map {:foo "hidden-secret"
                :foo1 "hidden-secret"}
   :nested-map {:foo "bar"
                :secret-foo "hidden-secret"}
   :nested-complicated-map {:vector [:foo "bar"
                                     :foo1 "bar1"]
                            :secret-vector ["hidden-secret"
                                            "hidden-secret"
                                            "hidden-secret"
                                            "hidden-secret"]
                            :map {:foo "bar"
                                  :foo1 "bar1"}
                            :secret-map {:foo "hidden-secret"
                                         :foo1 "hidden-secret"}
                            :nested-map {:foo "bar"
                                         :secret-foo "hidden-secret"}}})

(deftest secret?-test
  (is (sut/secret? :secret))
  (is (sut/secret? :very-secret))
  (is (sut/secret? :secret-foo))
  (is (sut/secret? :secret/foo))
  (is (sut/secret? :very-secret/foo))
  (is (sut/secret? :secret-foo/foo))
  (is (sut/secret? :foo.secret/foo))
  (is (sut/secret? :foo.very-secret/foo))
  (is (sut/secret? :foo.secret-foo/foo))
  (is (sut/secret? :secret.foo/foo))
  (is (sut/secret? :very-secret.foo/foo))
  (is (sut/secret? :secret-foo.foo/foo))
  (is (sut/secret? :secret/secret))
  (is (sut/secret? :secret.secret/secret))
  (is (not (sut/secret? :sec)))
  (is (not (sut/secret? :sec/foo)))
  (is (not (sut/secret? :foo/sec)))
  (is (not (sut/secret? :foo.sec/foo)))
  (is (not (sut/secret? :sec.foo/foo))))

(deftest replace-all-coll-values-with-test
  (is (= {:foo "hidden-secret", :foo1 "hidden-secret"}
         (sut/replace-all-coll-values-with "hidden-secret" simple-map)))
  (is (= ["hidden-secret" "hidden-secret" "hidden-secret" "hidden-secret"]
         (sut/replace-all-coll-values-with "hidden-secret" simple-vector)))
  (is (= {:vector ["hidden-secret"
                   "hidden-secret"
                   "hidden-secret"
                   "hidden-secret"]
          :secret-vector ["hidden-secret"
                          "hidden-secret"
                          "hidden-secret"
                          "hidden-secret"]
          :map {:foo "hidden-secret"
                :foo1 "hidden-secret"}
          :secret-map {:foo "hidden-secret"
                       :foo1 "hidden-secret"}
          :nested-map {:foo "hidden-secret"
                       :secret-foo "hidden-secret"}}
         (sut/replace-all-coll-values-with "hidden-secret" complicated-map)))
  (is (= {:vector ["hidden-secret"
                   "hidden-secret"
                   "hidden-secret"
                   "hidden-secret"]
          :secret-vector ["hidden-secret"
                          "hidden-secret"
                          "hidden-secret"
                          "hidden-secret"],
          :map {:foo "hidden-secret"
                :foo1 "hidden-secret"}
          :secret-map {:foo "hidden-secret"
                       :foo1 "hidden-secret"}
          :nested-map {:foo "hidden-secret"
                       :secret-foo "hidden-secret"}
          :nested-complicated-map
          {:vector ["hidden-secret"
                    "hidden-secret"
                    "hidden-secret"
                    "hidden-secret"]
           :secret-vector ["hidden-secret"
                           "hidden-secret"
                           "hidden-secret"
                           "hidden-secret"],
           :map {:foo "hidden-secret"
                 :foo1 "hidden-secret"}
           :secret-map {:foo "hidden-secret"
                        :foo1 "hidden-secret"}
           :nested-map {:foo "hidden-secret"
                        :secret-foo "hidden-secret"}}}
         (sut/replace-all-coll-values-with "hidden-secret" complicated-nested-map))))

(deftest replace-secrets-in-coll-test
  (is (= simple-map
         (sut/replace-secrets-in-coll simple-map)))
  (is (= simple-vector
         (sut/replace-secrets-in-coll simple-vector)))
  (is (= {:foo "bar", :secret-foo "hidden-secret"}
         (sut/replace-secrets-in-coll map-with-secret)))
  (is (= complicated-map-hidden
         (sut/replace-secrets-in-coll complicated-map)))
  (is (= complicated-nested-map-hidden
         (sut/replace-secrets-in-coll complicated-nested-map))))

(defn- check-exception
  [expected-exception-data test-exception-data]
  (is (= expected-exception-data
         (sut/e->ex-data-with-hidden-secrets (ex-info "Foo" test-exception-data)))))

(deftest e->e-with-hidden-secrets-test
  (check-exception simple-map simple-map)
  (check-exception map-with-secret-hidden map-with-secret)
  (check-exception complicated-map-hidden complicated-map)
  (check-exception complicated-nested-map-hidden complicated-nested-map))

(deftest ->num-test
  (is (= 42 (sut/->num "42")))
  (is (= 42 (sut/->num 42)))
  (is (= 42 (sut/->num "i42"))))
