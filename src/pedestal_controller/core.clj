(ns pedestal-controller.core
  (:require [clojure.string :as str]))

(defprotocol IController
  (interceptors [this])
  (handlers [this])
  (handler [this handler-name])
  (handler-route-name [this handler-name]))

(defn- collify
  ([v] (collify v {:as-vector? false}))
  ([v options]
   (cond
     (nil? v) []
     (coll? v) (if (:as-vector? options) (vec v) v)
     :else [v])))

(defn- camel->kebab
  [s]
  (-> s
      (str/replace #"(?<=[a-z0-9])([A-Z])" "-$1")
      (str/lower-case)))

(def ^:private controller->prefix
  (memoize
   (fn [controller]
     (-> controller
         (:controller-name)
         (name)
         (camel->kebab)
         (str/replace #"-?controller" "")))))

(defrecord Controller [controller-name
                       interceptors
                       handlers]
  IController
  (interceptors [this] interceptors)
  (handlers [this] handlers)
  (handler
    [this handler-name]
    (get handlers handler-name))
  (handler-route-name
   [this handler-name]
   (keyword
    (str (controller->prefix this) "-" (name handler-name)))))

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
         [(collify (first params) {:as-vector? true}) handlers])

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

(defn- parse-interceptor
  [handlers interceptor]
  (let [interceptor                  (collify interceptor {:as-vector? true})
        [item modifier handler-list] interceptor
        handler-list                 (collify handler-list {:as-vector? true})
        all-handlers-names           (set (keys handlers))
        missing-handlers             (filter (comp not all-handlers-names) handler-list)]
    (case (count interceptor)
      1 interceptor
      3 (do
          (assert (#{:only :except} modifier)
                  (str "Invalid modifier (" modifier ") in the interceptor list."))
          (assert (empty? missing-handlers)
                  (str "Handler(s) not found ("
                       (str/join ", " missing-handlers)
                       ") in the interceptors definitions"))
          [item modifier handler-list])
      (assert false
              "Invalid interceptor definition: [interceptor [:only|:except] handler-list]"))))

(defn- parse-controller-settings
  [settings]
  (let [settings                (collify settings)
        [interceptors handlers] (reduce-controller-settings settings)
        interceptors            (vec (map (partial parse-interceptor handlers) interceptors))]
    [interceptors handlers]))

(defmacro defcontroller
  [controller-name & settings]
  `(def ~controller-name
     (Controller. ~(keyword controller-name)
                  ~@(parse-controller-settings settings))))

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
      (vec
       (concat
        (flatten
         (reduce
          (fn [memo [interceptor modifier handler-list]]
            (if (or (nil? modifier)
                    (cond->> (some #(= handler %) handler-list)
                      (= modifier :only) (identity)
                      (= modifier :except) (not)))
              (concat memo [interceptor])
              memo))
          []
          interceptors))
        [handler-fn])))))

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
                   [:route-name (.handler-route-name controller handler)]))))
      ;; Nothing to do here
      (vec route))))

(defn remap-routes
  [routes]
  (set
   (map remap-route routes)))
