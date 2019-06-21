(ns pedestal-controller.test-utils
  (:require [clojure.test :refer :all])
  (:import [clojure.lang Compiler Compiler$CompilerException]))

;; Original from https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj#L518
;; It's was changed to be compatible with Clojure 1.10.0 where the exceptions
;; raised during the macro expansion are wrapped into Compiler$CompilerException
;; object.
(defmethod assert-expr 'macro-thrown-with-msg? [msg form]
  ;; (is (macro-thrown-with-msg? c re expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Also asserts that the message string of the exception matches
  ;; (with re-find) the regular expression re.
  (let [klass (nth form 1)
        re    (nth form 2)
        body  (nthnext form 3)]
    `(try ~@body
          (do-report {:type     :fail
                      :message  ~msg
                      :expected '~form
                      :actual   nil})
          (catch Compiler$CompilerException ce#
            (let [cause# (.getCause ce#)
                  m#     (.. ce# getCause getMessage)]
              (if (and (instance? ~klass cause#)
                       (re-find ~re m#))
                (do-report {:type     :pass
                            :message  ~msg
                            :expected '~form
                            :actual   ce#})
                (do-report {:type     :fail
                            :message  ~msg
                            :expected '~form
                            :actual   ce#})))
            ce#)
          (catch ~klass e#
            (let [m# (.getMessage e#)]
              (if (re-find ~re m#)
                (do-report {:type     :pass
                            :message  ~msg
                            :expected '~form
                            :actual   e#})
                (do-report {:type     :fail
                            :message  ~msg
                            :expected '~form
                            :actual   e#})))
            e#))))
