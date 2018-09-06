(ns kubeedn.main
  (:gen-class)
  (:require [cli-matic.core :as cli]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [yaml.core :as yaml]))

(set! *warn-on-reflection* true)

(defn transform-edn [{:keys [file] kubectl-opts :_arguments :as opts}]
  (let [manifest (edn/read-string (slurp file))
        manifests (if (vector? manifest) manifest [manifest])
        yamls (map yaml/generate-string manifests)]
    (println (string/join "\n---\n" yamls))))

(def cli-config
  {:app {:command "kubeedn"
         :description "Write kubernetes manifests with edn"
         :version "0.0.1-SNAPSHOT"}
   :global-opts []
   :commands [{:command "transform" :short "xf"
               :description ["Transforms edn to yaml"]
               :opts [{:option "file" :short "f"
                       :type :string :as "Manifest edn file or directory"}]
               :runs transform-edn}]})

(defn -main [& args]
  ;; snake-yaml breaks on graal if this property returns nil.  If this
  ;; is set once at the top level it will be nil after building with
  ;; native-image.
  (when-not (System/getProperty "java.runtime.name")
    (System/setProperty "java.runtime.name" "GraalVM"))
  (cli/run-cmd args cli-config))
