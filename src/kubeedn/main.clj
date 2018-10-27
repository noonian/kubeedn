(ns kubeedn.main
  (:gen-class)
  (:require [cli-matic.core :as cli]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [yaml.core :as yaml])
  (:import (java.io FileNotFoundException)))

(set! *warn-on-reflection* true)

(def whitespace-re #"[\s\p{Z}]")

(defn printerr [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn transform-edn* [file]
  (try
    (let [manifest (edn/read-string (slurp file))
          manifests (if (vector? manifest) manifest [manifest])
          yamls (map yaml/generate-string manifests)]
      (str/join "\n---\n" yamls))
    (catch FileNotFoundException ex
      (binding [*out* *err*]
        (println (format "Could not read file \"%s\"" file))))))

(defn transform-edn [{:keys [file] :as opts}]
  (println (transform-edn* file)))

(defn kube-apply [{:keys [file kubectl] :as opts}]
  (if-let [^String yaml (transform-edn* file)]
    (let [yaml-stream (io/input-stream (.getBytes yaml "UTF-8"))
          kubectl-opts (if (and kubectl (not (str/blank? kubectl)))
                         (str/split (or kubectl "") whitespace-re)
                         [])
          apply-args (into kubectl-opts ["-f" "-" :in yaml-stream])
          {:keys [out err exit]} (apply sh/sh "kubectl" "apply" apply-args)]
      (print out)
      (flush)
      (when-not (zero? exit)
        (printerr "Error from kubectl:")
        (printerr)
        (printerr err)
        (flush)
        exit))
    ;; Signal failure to cli-matic
    -1))

(def cli-config
  {:app {:command "kubeedn"
         :description "Write kubernetes manifests with edn"
         :version "0.0.1-SNAPSHOT"}
   :global-opts []
   :commands [{:command "transform" :short "xf"
               :description ["Transforms edn to yaml"]
               :opts [{:option "file" :short "f"
                       :type :string :as "Manifest edn file or directory"}]
               :runs transform-edn}
              {:command "apply"
               :description ["Kubectl apply after preprocessing with kubeedn"]
               :opts [{:option "file" :short "f"
                       :type :string :as "Manifest edn file or directory"}
                      {:option "kubectl" :short "kf"
                       :type :string :as "String of flags to be passed directly to kubectl"}]
               :runs kube-apply}]})

(defn -main [& args]
  ;; snake-yaml breaks on graal if this property returns nil.  If this
  ;; is set once at the top level it will be nil after building with
  ;; native-image.
  (when-not (System/getProperty "java.runtime.name")
    (System/setProperty "java.runtime.name" "GraalVM"))
  (cli/run-cmd args cli-config))
