(ns dora.p.calificacion)

(defn broken-link-recommendation?
  "La liga no sirve?
  Dentro del vector de recomendaciones se encuentra la de liga rota"
  [recommendations]
  (seq (filter #(= "La URL no pudo ser leída por el robot" (:name %))
                   recommendations)))

(defn falla-bronce
  "Condiciones para que un recurso no pueda obtener el rango de Bronce"
  [dataset resource recommendations]
  (or (empty? (dataset :issued))
      (empty? (dataset :title))
      (empty? (dataset :description))
      (empty? (dataset :keyword))
      (empty? (dataset :theme))
      (broken-link-recommendation? recommendations)))

(defn falla-plata
  "Condiciones para que un recurso no pueda obtener el rango de Plata"
  [dataset resource recommendations]
  (or (nil? (-> dataset :publisher :name))
      (nil? (-> dataset :publisher :mbox))))

(defn falla-oro
  "Condiciones para que un recurso no pueda obtener el rango de Oro"
  [dataset resource recommendations]
  ;TODO cotejar contra la accrualPeriodicity
  (not (empty? recommendations)))

(defn calificacion
  "Determina la calificación del recurso"
  [adela recommendations]
  (let [dataset  (:dataset adela)
        resource (:resource adela)]
    (cond (falla-bronce dataset resource recommendations) :none
          (falla-plata dataset resource recommendations) :bronce
          (falla-oro dataset resource recommendations) :plata
          :default :oro)))
