(ns pedestal-controller.core)

(defprotocol IController
  (interceptors [this])
  (handlers [this])
  (handler [this handler-name]))

(defrecord Controller [interceptors
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

(defn- fail!
  [message]
  (throw (Exception. message)))

(defn- validate-interceptors-params!
  [params]
  (when (not= (count params) 1)
    (fail! "Invalid arguments for interceptors: (interceptors [...]).")))

(defn- validate-handler-params!
  [params]
  (when (not= (count params) 2)
    (fail! "Invalid arguments for handler: (handler handler-name handler-fn).")))

(defn- reduce-controller-settings
  [settings]
  (reduce
   (fn [[interceptors handlers] [item & params]]
     (case item
       interceptors
       (if (empty? interceptors)
         (do
           (validate-interceptors-params! params)
           [(collify (first params)) handlers])
         (fail! "interceptors is defined more than once."))

       handler
       (let [[handler-name handler-fn] params
             handler-name              (keyword handler-name)]
         (if (contains? handlers handler-name)
           (fail! (str handler-name " is already defined."))
           (do
             (validate-handler-params! params)
             [interceptors (assoc handlers handler-name handler-fn)])))

       ;; Anything else
       (fail! (str item " is an unknown setting."))))
   ;; [Interceptors Handlers]
   [[] {}]
   settings))

(defmacro defcontroller
  [controller-name & settings]
  `(def ~controller-name
     (Controller. ~@(reduce-controller-settings (map collify settings)))))

(defn controller?
  [o]
  (instance? Controller o))

(defn remap-routes
  [routes])
