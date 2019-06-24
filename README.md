# pedestal-controller [![Build Status](https://travis-ci.org/tlewin/pedestal-controller.svg?branch=master)](https://travis-ci.org/tlewin/pedestal-controller) [![Clojars Project](https://img.shields.io/clojars/v/pedestal-controller.svg)](https://clojars.org/pedestal-controller)

Simple controller mechanism for [Pedestal](http://pedestal.io/) applications.

## Usage

The main idea behind controllers is to define a set of
handlers that shares the same interceptors/ settings,
making the entire process less error-prone and easy to maintain.

```clojure
(defcontroller Product
  (interceptors [common-interceptors
                 [auth :only [:create]]
                 [json-body :except [:search]]])

  (handler create
           (fn [request]
             (let [{{data :data} :params} request]
               (ring-resp/response (build-product! data)))))

  (handler search
           (fn [request]
             (let [{{query :query} :params} request]
               (ring-resp/response (render (search query)))))))

(interceptors Product)    ;; [default auth]
(handlers Product)        ;; {:create (fn ...) :search (fn ...)}
(handler Product :create) ;; (fn ... )
```

With the controllers, it's possible to add routes without defining
interceptors configuration:

```clojure
(def routes
  (remap-routes
   #{["/products"        :post Product :create]
     ["/products/search" :get  Product :search]
     ;; Although it's not mandatory and the old "syntax" still valid.
     ["/about"           :get (conj common-interceptors `about-page)]}))
```

## License

Copyright © 2019 Thiago Lewin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
