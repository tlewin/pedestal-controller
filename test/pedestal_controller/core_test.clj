(ns pedestal-controller.core-test
  (:require [clojure.test :refer :all]
            [pedestal-controller.test-utils :refer :all]
            [pedestal-controller.core :refer :all]))

(deftest test-defcontroller
  (testing "validates already defined interceptor"
    (is (macro-thrown-with-msg?
         AssertionError
         #"interceptors is defined more than once."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (interceptors [:a])
                    (interceptors [:b])))))))
  (testing "validates missing argument for interceptors"
    (is (macro-thrown-with-msg?
         AssertionError
         #"Invalid arguments for interceptors"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (interceptors)))))))
  (testing "validates trailing argument for interceptors"
    (is (macro-thrown-with-msg?
         AssertionError
         #"Invalid arguments for interceptors"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (interceptors :a :b)))))))
  (testing "validates already defined handler"
    (is (macro-thrown-with-msg?
         AssertionError
         #":h1 is already defined."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (handler :h1 identity)
                    (handler :h1 identity)))))))
  (testing "validates missing argument for handler - 1"
    (is (macro-thrown-with-msg?
         AssertionError
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (handler)))))))
  (testing "validates missing argument for handler - 2"
    (is (macro-thrown-with-msg?
         AssertionError
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (handler :h1)))))))
  (testing "validates trailing argument for handler"
    (is (macro-thrown-with-msg?
         AssertionError
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (handler :h1 identity :extra)))))))
  (testing "validates invalid controller setting"
    (is (macro-thrown-with-msg?
         AssertionError
         #"typo is an unknown setting."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller TestController
                    (interceptors [:a])
                    (handler :h1 identity)
                    (typo)))))))
  (testing "generates a controller"
    (defn add-one [x] (inc x))
    (defcontroller TestController
      (interceptors [:a :b])
      (handler :h1 identity)
      (handler :h2 add-one))
    (is (controller? TestController))
    (is (= (:controller-name TestController) :TestController))
    (is (= (.interceptors TestController) [:a :b]))
    (is (= (.handlers TestController) {:h1 identity
                                       :h2 add-one}))
    (is (= (.handler TestController :h1) identity))
    (is (= (.handler TestController :h2) add-one))))

(defcontroller TestController
  (interceptors [:common :auth])
  (handler :h1 identity))

(deftest test-remap-routes
  (testing "returns a set"
    (is (set? (remap-routes #{["/path" :get TestController :h1]}))))
  (testing "expands the controller"
    (is (= (remap-routes #{["/path" :get TestController :h1]})
           #{["/path" :get [:common :auth identity] :route-name :test-h1]})))
  (testing "don't overwrite the route-name attribute"
    (is (= (remap-routes #{["/path" :get TestController :h1 :route-name :other]})
           #{["/path" :get [:common :auth identity] :route-name :other]})))
  (testing "preserves other arguments"
    (is (= (remap-routes #{["/path" :get TestController :h1 :arg1 1 :arg2 2 :route-name :other]})
           #{["/path" :get [:common :auth identity] :arg1 1 :arg2 2 :route-name :other]})))
  (testing "preserves paths without controllers"
    (is (= (remap-routes #{["/path" :get `inc :route-name :other]})
           #{["/path" :get `inc :route-name :other]})))
  (testing "fails if the handler is not found"
    (is (thrown-with-msg?
         AssertionError
         #":h2 not found for :get /path"
         (remap-routes #{["/path" :get TestController :h2 :route-name :other]}))))
  (testing "flattens the interceptors list"
    (def common [:a :b])
    (defcontroller TestController
      (interceptors [common :auth])
      (handler :h1 identity))
    (is (= (remap-routes #{["/path" :get TestController :h1]})
           #{["/path" :get [:a :b :auth identity] :route-name :test-h1]}))))
