(ns dora.recommendations
  (:require [nillib.worm :refer [remove-nils]]))

(defmacro try-catch [body]
  `(try
     ~body
     (catch Exception e# (str "error " e#))))

(defn broken-link-recommendation
  "If the URL was reported as broken today, raise this alert"
  [url todays-broken]
  (if (seq (filter #(= url %) (map :url todays-broken)))
    {:name "La URL no pudo ser leída por el robot"
     :description "Esto puede significar que la URL está caída, cargue demasiado lento, o no sea accesible para robots."
     :more-info "http://datos.gob.mx/guia/publica/paso-2-1.html"
     :clave "d01"
     :categoria "disponibilidad"}))

(def acento-regex #"[áéíóúÁÉÍÓÚñÑ]")

(defn acento-recommendation
  "Si la string tiene acentos, emite una recomendacion de no usar acentos en la URL"
  [s]
  (if (re-find acento-regex s)
    {:name "La URL contiene acentos"
     :description "Es recomendable que las urls no contengan acentos."
     :clave "i11"
     :categoria "interoperabilidad"}))

(defn encoding-recommendation
  "Check file encoding against a list of blacklisted encodings"
  [metadata]
  (if (re-find #"8859" (metadata "file"))
    {:name "El archivo está en una codificación no universal"
     :description "Es recomendable utilizar la codificación UTF-8"
     :clave "i12"
     :categoria "interoperabilidad"}))

(defn duplicated-url-recommendation
  "Checks if there are more than one resource with this url"
  [url]
  (if (> (count (re-find :resources {:url url})) 1)
    {:name "Hay mas de un recurso de datos con esta URL"
     :description "No es necesario que los mismos datos estén dados de alta mas de una vez. Revisar otras áreas, o dependencias que tengan estos datos publicados."
     :clave "c42"
     :categoria "calidad"}))

(defn resource-description-recommendation
  "Checks if the resource has no description"
  [resource]
  (if (empty? (:description resource))
    {:name "El recurso no tiene descripción"
     :description "Es necesario agregar una descripción al recurso, para que sea fácil entender la finalidad del recurso"
     :clave "u22"
     :categoria "documentacion"}))

(defn has-mixed-formats-recommendation
  [m]
  (if (seq (filter #(and (re-find #"has_mixed_formats" (:meta %))
                         (true? (:data %)))
                   m))
    {:name "El archivo contiene columnas en las que algunos elementos tienen números y otros no"
     :description "Evitar tener valores de diferentes tipos (como texto y número) para una columna en diferentes registros o filas."
     :clave "c41"
     :categoria "calidad"}))

(defn nils-csv-validation-recommendation
  [m]
  (if (seq (filter #(and (re-find #"nils-csv-validation" (:meta %))
                         (true? (:data %)))
                   m))
    {:name "El archivo contiene filas vacías"
     :description "Eliminar filas y columnas vacías, al igual que cálculos adicionales en formatos tabulares, p. ej. una celda “Total” con la suma de una de las columnas."
     :clave "m31"
     :categoria "legibilidad"}))

(defn has-weird-format-numbers?-recommendation
  [m]
  (if (seq (filter #(and (re-find #"has-weird-format-numbers?" (:meta %))
                         (true? (:data %)))
                   m))
    {:name "Hay campos numéricos que contienen caracteres no numéricos"
     :description "Los campos numéricos, incluyendo los monetarios y magnitudes, deben permanecer en un formato numérico de tipo entero o flotante. Es decir, evitar agregar símbolos monetarios o de unidades de medición, p. ej. “200 m2” o “£20”, haciendo explícitas las unidades en el nombre de la columna, o en una segunda columna. P. ej. “Monto en MXN”, “Distancia en KM”."
     :clave "m33"
     :categoria "legibilidad"}))

(comment
  (defn -recommendation
    [m]
    (if (seq (filter #(and (re-find #"" (:meta %))
                           (true? (:data %)))
                     m))
      {:name ""
       :description ""
       :clave ""})))

(defn recommendations
  "Generate recommendations from a file url and its metadata"
  [url metadata resource todays-broken]
  (remove-nils (flatten [(try-catch (broken-link-recommendation url todays-broken))
                         (try-catch (acento-recommendation url))
                         (try-catch (encoding-recommendation metadata))
                         (try-catch (duplicated-url-recommendation url))
                         (try-catch (resource-description-recommendation resource))
                         (if (and (not (nil? url)) (re-find #"csv|CSV" url))
                           [(try-catch (has-mixed-formats-recommendation metadata))
                            (try-catch (nils-csv-validation-recommendation metadata))
                            (try-catch (has-weird-format-numbers?-recommendation metadata))])])))
