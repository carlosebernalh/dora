(ns dora.pro-file
  (:require [dora.p.agente-web :refer :all]
            [dora.p.calificacion :refer :all]
            [dora.recommendations :refer :all]
            [environ.core :refer [env]]
            [mongerr.core :refer :all]))

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
