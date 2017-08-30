(ns dora.validadora
  (:require [clojure.java.shell :refer [sh]]
            [clojure.set :as s :refer [difference]]
            [digitalize.core :refer [remove-nils]]
            [dora.data :refer [data-directory file-names]]
            [dora.util :refer [clean-get or*]]
            [formaterr.core :refer [csv]]
            [mongerr.core :refer [db db-update]]
            [nlp.core :refer [number-regex url-regex]]))

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
