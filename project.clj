(defproject dtbrowser "0.1.0-SNAPSHOT"
  :description "Digitrad data browser"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [cljs-ajax "0.2.3"]
                 [servant "0.1.3"]
                 [om "0.5.0"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "dtbrowser-dev.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "dtbrowser.js"
                :source-map "dtbrowser.js.map"
                :optimizations :simple
                :pretty-print false
                :preamble ["react/react.min.js"]
                :externs ["react/externs/react.js" "libs/lunr.min.js"]}}
             ]})
