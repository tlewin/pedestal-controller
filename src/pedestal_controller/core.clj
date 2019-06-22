(ns pedestal-controller.core
  (:require [clojure.string :as str]))

(defprotocol IController
  (interceptors [this])
  (handlers [this])
  (handler [this handler-name]))

(defrecord Controller [controller-name
                       interceptors
                       handlers]
  IController
  (interceptors [this] interceptors)
  (handlers [this] handlers)
  (handler
    [this handler-name]
    (get handlers handler-name)))

(defn- collify
  [v]
  (cond
    (nil? v) []
    (coll? v) v
    :else [v]))

(defn- camel->kebab
  [s]
  (-> s
      (str/replace #"(?<=[a-z0-9])([A-Z])" "-$1")
      (str/lower-case)))

(defn- reduce-controller-settings
  [settings]
  (reduce
   (fn [[interceptors handlers] [item & params]]
     (case item
       interceptors
       (do
         (assert (empty? interceptors)
                 "interceptors is defined more than once.")
         (assert (= (count params) 1)
                 "Invalid arguments for interceptors: (interceptors [...]).")
         [(collify (first params)) handlers])

       handler
       (let [[handler-name handler-fn] params
             handler-name              (keyword handler-name)]
         (assert (not (contains? handlers handler-name))
                 (str handler-name " is already defined."))
         (assert (= (count params) 2)
                 "Invalid arguments for handler: (handler handler-name handler-fn).")
         [interceptors (assoc handlers handler-name handler-fn)])

       ;; Anything else
       (assert false (str item " is an unknown setting."))))
   ;; [Interceptors Handlers]
   [[] {}]
   settings))

(defmacro defcontroller
  [controller-name & settings]
  `(def ~controller-name
     (Controller. ~(keyword controller-name)
                  ~@(reduce-controller-settings (map collify settings)))))

(defn controller?
  [o]
  (instance? Controller o))

(defn- build-interceptors-handler-chain
  [controller handler]
  (let [interceptors (.interceptors controller)
        handler-fn   (.handler controller handler)]
    (if (some? handler-fn)
      ;; NOTE: concat was choosen over conj to ensure that
      ;; handler-fn is always the last element
      (concat interceptors
              [handler-fn]))))

(defn- build-handler-name
  [controller handler]
  (let [prefix (-> controller
                   (:controller-name)
                   (name)
                   (camel->kebab)
                   (str/replace #"-?controller" ""))]
    (keyword
     (str prefix "-" (name handler)))))

(defn- remap-route
  [route]
  (let [[path verb handler & args] route]
    (if (controller? handler)
      (let [controller       handler
            [handler & args] args
            handler-chain    (build-interceptors-handler-chain controller handler)]
        (assert (some? handler-chain)
                (str handler " not found for " verb " " path))
        (remap-route
         (concat [path verb handler-chain]
                 (collify args)
                 (if (some #(= :route-name %) args)
                   [] ;; Add nothing
                   [:route-name (build-handler-name controller handler)]))))
      ;; Nothing to do here
      route)))

(defn remap-routes
  [routes]
  (set
   (map remap-route routes)))
