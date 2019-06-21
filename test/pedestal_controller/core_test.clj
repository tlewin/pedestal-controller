(ns pedestal-controller.core-test
  (:require [clojure.test :refer :all]
            [pedestal-controller.test-utils :refer :all]
            [pedestal-controller.core :refer :all]))

(deftest test-defcontroller
  (testing "validates already defined interceptor"
    (is (macro-thrown-with-msg?
         Exception
         #"interceptors is defined more than once."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (interceptors [:a])
                    (interceptors [:b])))))))
  (testing "validates missing argument for interceptors"
    (is (macro-thrown-with-msg?
         Exception
         #"Invalid arguments for interceptors"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (interceptors)))))))
  (testing "validates trailing argument for interceptors"
    (is (macro-thrown-with-msg?
         Exception
         #"Invalid arguments for interceptors"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (interceptors :a :b)))))))
  (testing "validates already defined handler"
    (is (macro-thrown-with-msg?
         Exception
         #":h1 is already defined."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (handler :h1 identity)
                    (handler :h1 identity)))))))
  (testing "validates missing argument for handler - 1"
    (is (macro-thrown-with-msg?
         Exception
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (handler)))))))
  (testing "validates missing argument for handler - 2"
    (is (macro-thrown-with-msg?
         Exception
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (handler :h1)))))))
  (testing "validates trailing argument for handler"
    (is (macro-thrown-with-msg?
         Exception
         #"Invalid arguments for handler"
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (handler :h1 identity :extra)))))))
  (testing "validates invalid controller setting"
    (is (macro-thrown-with-msg?
         Exception
         #"typo is an unknown setting."
         (binding [*ns* (find-ns 'pedestal-controller.core)]
           (eval '(defcontroller Invalid
                    (interceptors [:a])
                    (handler :h1 identity)
                    (typo))))))))
