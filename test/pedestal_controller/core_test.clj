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
