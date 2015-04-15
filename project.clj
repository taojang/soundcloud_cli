(defproject soundcloud-cli "0.1.0-SNAPSHOT"
  :description "Command line client for soundcloud"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0-beta1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-3165"]]

  :node-dependencies [[source-map-support "0.2.10"]
                      [term-list "0.2.1"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-npm "0.5.0"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "soundcloud-cli"
              :source-paths ["src"]
              :compiler {
                :output-to "out/soundcloud_cli.js"
                :output-dir "out"
                :target :nodejs
                :optimizations :none
                :source-map true}}]}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.0"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :hooks [leiningen.cljsbuild])