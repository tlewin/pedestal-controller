(ns pedestal-controller.core)

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

(defn remap-routes
  [routes])
