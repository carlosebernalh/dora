(defproject dora "1.0.0-SNAPSHOT"
  :description "Dora la exploradora de datos"
  :dependencies [[cast "0.1.0-SNAPSHOT"]
                 [clj-http "3.7.0"]
                 [clj-pdf "2.2.29"]
                 [clj-time "0.14.0"]
                 ;[clj-zendesk "0.1.0"]
                 [clojail "1.0.6"]
                 [cloogle "0.1.0-SNAPSHOT"]
                 [com.cemerick/friend "0.3.0-SNAPSHOT"]
                 [com.cemerick/url "0.1.2-SNAPSHOT"]
                 [com.draines/postal "2.0.2"]
                 [commons-lang/commons-lang "2.6"]
                 [cheshire "5.8.0"]
                 [commons-validator "1.6"]
                 [com.novemberain/monger "3.1.0"]
                 [compojure "1.6.0"]
                 [digitalize "0.1.0-SNAPSHOT"]
                 [dgm-analytics "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]
                 [http-kit "2.2.0"]
                 [lib-noir "0.8.5"];"0.9.9"
                 [log4j/log4j "1.2.17"]
                 ;[medley "0.7.0"]
                 [mongerr "1.0.0-SNAPSHOT"]
                 [nillib "0.1.0-SNAPSHOT"]
                 [nlp "0.1.0-SNAPSHOT"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.reader "1.0.5"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.11"];"0.1.7"
                 [ring-middleware-format "0.7.2"]
                 [ring-server "0.5.0"]
                 [tentacles "0.5.1"]
                 ;[com.rpl/specter "0.9.3"]
                 [jarohen/chime "0.2.2"]
                 [formaterr "1.0.0-SNAPSHOT"]]
  :jvm-opts ["-Djava.security.policy=.java.policy" "-Xmx32g"]
  :main dora.server
  :plugins [[lein-exec "0.3.6"]
            [lein-ring "0.9.7"]
            [com.palletops/uberimage "0.4.1"]]
  :repl-options {:init-ns dora.repl
                 :timeout 1800000}
  :ring {:handler dora.server/app :port 5555})
