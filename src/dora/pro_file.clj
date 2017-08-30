(ns dora.pro-file
  (:require [clojure.java.shell :refer :all]
            [clojure.set :refer :all]
            [clojure.string :as s]
            [digitalize.core :refer :all]
            [dora.data :refer :all]
            [dora.p.agente-web :refer :all]
            [dora.p.calificacion :refer :all]
            [dora.recommendations :refer :all]
            [dora.util :refer :all]
            [environ.core :refer [env]]
            [formaterr.core :refer :all]
            [mongerr.core :refer :all]
            [nillib.text :refer :all]))

(defn shsh
  "Execute command in shell"
  [& command]
  (let [result (sh "/bin/sh" "-c" (s/join " " command))]
    (str (:err result) (:out result))))

(defn nils-csv-validation
  "Are there elements in a rel with too many nils?"
  [rel]
  (let [total (count (first rel))
        totals (map #(count (remove-nils (vals %))) rel)
        mini (apply min totals)]
    (if (> total 2)
      (if (> 3 mini)
        true
        false)
      false)))

(defn number-weird-format?
  "Does the number have a weird format
  such as '$' 'Kg' or anything that is not a number?"
  [s]
  (if-let [num (first (re-find number-regex s))]
    (not= s num)
    false))

(defn has-weird-format-numbers?
  [rel]
  (or* (map number-weird-format? (vals (first rel)))))

(defn rel-sample
  "take some random elements from the rel"
  ([rel]
    (rel-sample 20 rel))
  ([n rel]
    (take n (shuffle rel))))

(defn has-numbers?
  "The string has numbers in it?"
  [s]
  (if (re-find number-regex s)
      true
      false))

(defn vec-has-mixed-formats
  "Does a vector have a mix of numbers and strings?"
  [v]
  (apply not= (remove-nils (map has-numbers? v))))

(defn has-mixed-formats
  "Does the rel contain a mix of numbers and strings in some field?"
  [rel]
  (let [rel (rel-sample rel)
        ks (keys (first rel))]
    (or* (map vec-has-mixed-formats
              (map (fn [k] (map k rel))
                   ks)))))

(def metas
  "Vector of 'validations' or scripts whose output we collect"
  ["head -n 1" ;first line
   "file"      ;file type, encoding
   "wc -l"     ;line count
   "du -h"     ;file size
   "validaciones/IDMX/code/prep_proc"
   "validaciones/repetidos" ;conteo de lineas repetidas
   ])

(def csv-validations
  "Vector of validations aplicable only to rels generated from CSV"
  [{:name "nils-csv-validation" :fn nils-csv-validation}
   {:name "has-weird-format-numbers" :fn has-weird-format-numbers?}
   {:name "has-mixed-formats" :fn has-mixed-formats}])

(defn eval-csv-validation
  "Instead of just running the function, handle its exceptions"
  [f rel]
  (try (f rel)
       (catch Exception e "error")))

(defn csv-engine
  "Run validations specific to CSV"
  [file]
  (try
    (let [rel (take 1000 (csv file))]          ;just a 1000 rows sample for performance
      (zipmap (map :name csv-validations)
              (map #(eval-csv-validation % rel)
                   (map :fn csv-validations))))
    (catch Exception e {:csv-engine-error (str e)})))

(comment(defn dora-insert
          "Insert a newly read url into db"
          [url]
          (db-insert :dora {:url url :active true})))

(defn download-if-url
  "If name is a url, download and return relative path.
  nil if any error"
  [url]
  (try
    (if (re-find url-regex url)
      (let [file-name (str "/tmp/" (rand))
            tmp (spit file-name (clean-get url))]
        file-name)
      url)
    (catch Exception e)))

(defn execute-validations
  "Execution engine ;)"
  [file-name url]
  (remove-nils (merge (zipmap metas
                              (pmap #(try                   ;(if (string? %)
                                       (shsh % file-name)   ;    (% data))
                                       (catch Exception e (str e)))
                                    metas))
                      (if (re-find #"csv|CSV" url)
                        (csv-engine file-name)))))

(defn trim-macugly-line
  "Take the first line whith line breaks as \r
  whch are generated on old mac systems"
  [s]
  (re-find #"[^\r]+" s))

(defn trimming
  "Trim strings for head -n 1"
  [s]
  (s/join (take 500 (trim-macugly-line s))))

(defn first-numbers
  "Take only the first numeric substring"
  [s]
  (re-find #"[0-9]+" s))

(defn sfirst
  "second first"
  [coll]
  (second (first coll)))

(defn update-all-in
  "update-in with each key and fn pair"
  [m updates]
  (loop [m m
         updates updates]
    (if (empty? updates)
      m
      (recur (update-in m [(ffirst updates)] (sfirst updates))
             (rest updates)))))

(defn trim-file-script
  [s]
  (second (re-seq #"[^;]+" s)))

(defn process-validations
  "Post process validations, clean them and shit"
  [m]
  (update-all-in m {"head -n 1" trimming
                    "wc -l" first-numbers
                    "du -h" first-numbers
                    "validaciones/repetidos" first-numbers
                    "file" trim-file-script
                    }))

(defn to-validate []
  (difference (file-names) (set (map :id (db :resource-metadata)))))

(defn validate-dgm
  ([names]
   (doall (pmap #(db-update :resource-metadata
                            {:id %}
                            (assoc (process-validations (execute-validations (data-directory %) "csv"))
                                   :id %))
                names)))
  ([]
   (validate-dgm (to-validate))))

(defn validate-dgm-batched
  ([]
   (validate-dgm-batched (to-validate)))
  ([names] (loop [names names]
             (if-not (empty? names)
               (validate-dgm (take 100 names))
               (recur (drop 100 names))))))

(def resources (db :resources))

(defn find-resource-by-url [url]
  (first (filter #(= url (:url %))
                 resources)))

(defn resource-metadata
  "Find stored metadata for a id"
  [id]
  (db-findf :resource-metadata {:id id}))

(def datasets (db :datasets))

(defn find-resources-dataset
  "Find stored dataset metadata on a resource"
  [resource]
  (first (filter #(= (:id %)
                     (:dataset_id resource))
                 datasets)))

(defn dataset
  "Given a Resource or dataset name, return its dataset metadata"
  [o]
  (if (map? o)
    (if-let [dataset-id (:dataset_id o)] (db-findf :datasets {:id dataset-id}))
    (db-findf :datasets {:name o})))

(defn analytics
  "get the stored google analytics history"
  ([url]
   (try (:value (db-findf :google_analytics {:url url}))
        (catch java.lang.NullPointerException e)))
  ([url analytics-data]
   (if-not (empty? url)
     (apply + (map :value (filter #(try (re-find (re-pattern url) (:url %))
                                        (catch Exception e (= url (:url %))))
                                  analytics-data))))))

(defn inventory-resources
  []
  (mapcat :distribution (mapcat :dataset (db-find :adela-inventories))))

(defn flatten-inventory [i]
  (map #(hash-map :dataset %
                  :inventory (dissoc i :dataset))
       (:dataset i)))

(defn flatten-inventory-dataset
  [d]
  (map #(hash-map :dataset (dissoc (:dataset d) :distribution)
                  :inventory (:inventory d)
                  :resource %)
       (:distribution (:dataset d))))

(def ^:dynamic *adela-data-collection* (or (env :adela-data-collection) :adela-inventories))
;;:adela-catalogs ;:adela-inventories;; TODO: cuando entre en vigor el nuevo adela, cambiar a: :adela-inventories

(defn inventory-resources-denormalized
  []
  (mapcat flatten-inventory-dataset
          (mapcat flatten-inventory
                  (db *adela-data-collection*))))

(defn dora-view-inventory    ;will expect entries from inventory-resources-denormalized
  "hace la fusion de datos de los recursos de adela"
  ([]
   (let [analytics  (db :google_analytics)
         analytics-views (db :google_analytics_views)
         broken (broken-today)]
     (map #(dora-view-inventory % analytics analytics-views broken)
          (inventory-resources-denormalized))))
  ([result analytics-data analytics-data-views todays-broken]
   (try (let [url (:downloadURL (:resource result))
              resource (find-resource-by-url url)
              dataset (find-resources-dataset resource)
              metadata (resource-metadata (:id resource))
              recommendations (remove string?
                                      (recommendations url metadata (:resource result) todays-broken))]
          (assoc {:adela result}
                 :ckan {:resource resource
                        :dataset (dissoc dataset :resources)}
                 :analytics {:downloads {:total (analytics url analytics-data)}
                             ;:pageviews {:total (analytics (:id resource) analytics-data-views)}
                             }
                 :file-metadata metadata
                 :recommendations recommendations
                 :id (str (:id (:dataset result)))
                 ;:ieda (ieda? url)
                 :calificacion (calificacion result recommendations)))
        (catch Exception e (println "Exception: " e)))))

(defn orphan-resources [inventories]
  (let [inv-urls (map #(:downloadURL (:resource (:adela %))) inventories)]
    (filter #(empty? (filter (fn [url] (= url (% :url)))
                             inv-urls))
            (db :resources))))

(defn dora-view-resources
  "Hace la fusion de datos para los recursos huerfanos, osea los que estan en ckan pero no en adela"
  ([inventories]
   (let [analytics  (db :google_analytics)
         analytics-views (db :google_analytics_views)
         broken (broken-today)]
     (map #(dora-view-resources %
                                analytics
                                analytics-views
                                broken)
          (orphan-resources inventories))))
  ([resource analytics-data analytics-data-views todays-broken]
   (try (let [url (:url resource)
              dataset (find-resources-dataset resource)
              metadata (resource-metadata (:id resource))
              recommendations (remove string?
                                      (recommendations url metadata resource todays-broken))]
          {:ckan {:resource resource
                  :dataset (dissoc dataset :resources)}
           :analytics {:downloads {:total (analytics url analytics-data)}
                      ;:pageviews {:total (analytics (:id resource) analytics-data-views)}
                       }
           :file-metadata metadata
           :recommendations recommendations})
        (catch Exception e (println "Exception: " e)))))

(defn data-fusion []
  (let [inventories (dora-view-inventory)]
    (concat inventories (dora-view-resources inventories))))

(defn data-fusion-analytics& []
  (let [data (db :data-fusion)]
    (println "resources table: " (count (db :resources)))
    (println "adela inventories table: " (count (db :adela-inventories)))
    (println "whole data-fusion: " (count data))
    (println "data-fusion ckan: " (count (filter #(seq (:resource (:ckan %))) data)))
    (println "data-fusion adela: " (count (filter :adela data)))
    (println "data-fusion ckan & adela: " (count (filter :adela (filter #(seq (:resource (:ckan %))) data))))))
