(ns dora.refineria
  (:require [clj-time.core :as t]
            [clojure.java.shell :refer :all]
            [clojure.string :as str]
            [digitalize.core :refer :all]
            [dora.p.ckan :refer :all]
            [dora.p.data-core :refer :all]
            [dora.pro-file :refer :all]
            [dora.util :refer :all]
            [environ.core :refer [env]]
            [formaterr.core :refer :all]
            [monger.core :as mg]
            [mongerr.core :refer :all]
            [tentacles.repos :refer [create-org-repo]]))

(def mirrored-files
  {:proyectos-opa "http://www.transparenciapresupuestaria.gob.mx/work/models/PTP/OPA/datosabiertos/proyectos_opa.csv"
   :prog-avance-de-indicadores "http://www.transparenciapresupuestaria.gob.mx/work/models/PTP/DatosAbiertos/Programas/prog_avance_de_indicadores.csv"
   :opa "http://www.transparenciapresupuestaria.gob.mx/work/models/PTP/OPA/datosabiertos/opa.csv"
   "san-salvador-TfP02"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP02.csv",
   "san-salvador-MaR05"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar05_veg_nat_rem_eco.csv",
   "san-salvador-TfE02"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE02.csv",
   "san-salvador-CaR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car02_tasa_alfab_pob_ind.csv",
   "san-salvador-Mar01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar01_pob_acceso_agua.csv",
   "san-salvador-CaR14b"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car14b_pob_afro.csv",
   "san-salvador-UfE01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/UfE01.csv",
   "san-salvador-UdR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/udr01_sindicalizacion_gpos_pob.csv",
   "san-salvador-MaR14"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar14_num_vehiculos_automotores.csv",
   "san-salvador-CcP01d"
   "http://datosdgai.cultura.gob.mx/pss/CcP01d.csv",
   "san-salvador-MaR13"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Concentracion_promedio_de_particulas_PM10.csv",
   "san-salvador-TdP01" nil,
   "san-salvador-CfE03"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE03.csv",
   "san-salvador-AaR05b"
   "http://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_gpos.csv",
   "san-salvador-AaR02"
   "http://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_gpos.csv",
   "san-salvador-TdP03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdp03_inst_salud_pob.csv",
   "san-salvador-CdR06"
   "http://www.cdi.gob.mx/datosabiertos/2018/Cultura/SNEDH.csv",
   "san-salvador-McR07"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr07_eliminacion_excretas.csv",
   "san-salvador-McR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr02_energia_elect_hogar.csv",
   "san-salvador-EaP06c"
   "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Primaria.csv",
   "san-salvador-CcP03b" "http://datosdgai.cultura.gob.mx/pss/CcP03b.csv",
   "san-salvador-TdR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr02_ing_lab_per_cap.csv",
   "san-salvador-AaR03"
   "http://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR03_porcentaje_de_poblacion_por_debajo_del_nivel_minimo_de_consumo_de_energia_alimentaria.csv",
   "san-salvador-EaP06a"
   "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Alfabetizacion.csv",
   "san-salvador-CaR14a"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car14a_tasa_crec_pob_ind.csv",
   "san-salvador-TdR01d"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01d_brecha_tasa_sin_contrato.csv",
   "san-salvador-TjE01a"
   "http://www.dgepj.cjf.gob.mx/DatosAbiertos/tje01.csv",
   "san-salvador-TaR08b"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar08b_muj_fun_pub.csv",
   "san-salvador-TfP01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP01.csv",
   "san-salvador-CaR16"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car16_pob_lee_libros.csv",
   "san-salvador-McR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr01_agua_entubada_hogar.csv",
   "san-salvador-MaR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar01_pob_acceso_agua.csv",
   "san-salvador-Mar11"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Emisiones_de_Gases_Efecto_Invernadero_(GEI).csv",
   "san-salvador-TaR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar01_tasa_ocup_infantil.csv",
   "san-salvador-TdR01c"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01c_brecha_tasa_infor_lab.csv",
   "san-salvador-MaR08a"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Emisiones%20de%20dioxido%20de%20carbono%20per%20capita.csv",
   "san-salvador-CfP01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfP01.csv",
   "san-salvador-TiR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tir01_usu_anuales_portal_inegi.csv",
   "san-salvador-CcP01a"
   "http://datosdgai.cultura.gob.mx/pss/CcP01a.csv",
   "san-salvador-CfP04b"
   "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/CfP04bBecasExtranjerosOtorgadasPorLaAMEXCID.csv",
   "san-salvador-CcR01"
   "http://datosabiertos.impi.gob.mx/Documents/patenteshab.csv",
   "san-salvador-AfP02a"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02a.csv",
   "san-salvador-TaR09"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar09_tasa_part_per_dis.csv",
   "san-salvador-TdR04"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr04_homb_ocup.csv",
   "san-salvador-CaR15b"
   "http://datosdgai.cultura.gob.mx/pss/CaR15b.csv",
   "san-salvador-UaR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/uar01_tasa_sindica.csv",
   "san-salvador-MfR02"
   "http://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndProdEneFueRenAltProtSanSalvador-MFR02.csv",
   "san-salvador-Mar04"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Porcentaje_de_areas_afectadas_por_degradacion_ambiental.csv",
   "san-salvador-MaR08b"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Consumo_de_clorofluorocarburos_que_agotan_la_capa_de_ozono.csv",
   "san-salvador-MdR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mdr02_saneamiento_mejorado.csv",
   "san-salvador-CcR02" "http://datosdgai.cultura.gob.mx/pss/CcR02.csv",
   "san-salvador-TaR06"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar06_muj_tot_asal_no_agro.csv",
   "san-salvador-UfP01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/UfP01.csv",
   "san-salvador-AfP01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/afp01_indice_rur_ent_fed.csv",
   "san-salvador-CcR03a"
   "http://datosdgai.cultura.gob.mx/pss/CcR03a.csv",
   "san-salvador-CdR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/cdr02_var_hogar_gast_nec_bas.csv",
   "san-salvador-AdR03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/adr03_ingresos_corr_der_trab.csv",
   "san-salvador-CdP05"
   "http://www.conapred.org.mx/datosabiertos/Derechos_Culturales_2011_2016.csv",
   "san-salvador-TjP02b"
   "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02b-Porcentaje-juicios-resueltos-favorablemente.csv",
   "san-salvador-AcP01"
   "http://www.sagarpa.gob.mx/quienesomos/datosabiertos/sagarpa/Documents/AcP01_.csv",
   "san-salvador-TaR05a"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar05a_trab_ingr_inf_sal_min.csv",
   "san-salvador-CcP01c"
   "http://datosdgai.cultura.gob.mx/pss/CcP01c.csv",
   "san-salvador-CdR04"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/cdr04_crec_pob_leg_ind.csv",
   "san-salvador-AdR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/adr02_gasto_hogar_alimentos.csv",
   "san-salvador-CaR09"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car09_eventos_culturales.csv",
   "san-salvador-MfR03"
   "http://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndConEneRenProtSanSalvador-MFR03.csv",
   "san-salvador-CaR04" "http://datosdgai.cultura.gob.mx/pss/CaR04.csv",
   "san-salvador-TdR05"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr05_discr_sal_muj_homb.csv",
   "san-salvador-AaR05a"
   "http://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_gpos.csv",
   "san-salvador-TiP02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tip02_solic_anuales_inf_inegi.csv",
   "san-salvador-Mar03"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Porcentaje_de_la_superficie_cubierta_por_bosques_y_selvas.csv",
   "san-salvador-CaR03" "http://datosdgai.cultura.gob.mx/pss/CaR03.csv",
   "san-salvador-TdR01a"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01a_brecha_tasa_part_lab.csv",
   "san-salvador-UcR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/ucr02_sindicalizacion_ent_fed.csv",
   "san-salvador-MaR10"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar10_pob_acceso_gas.csv",
   "san-salvador-MfP01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/MfP01.csv",
   "san-salvador-TaR02a"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar02a_tasa_desocupacion.csv",
   "san-salvador-TdR01b"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01b_brecha_tasa_deso_lab.csv",
   "san-salvador-TjP02a"
   "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02a-Porcentaje-conflictos-resueltos-favorables-conciliación.csv",
   "san-salvador-CaR01"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car01_tasa_alfabetismo.csv",
   "san-salvador-TdR03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr03_muj_ocup_prest_lab.csv",
   "san-salvador-CaR06"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car06_hogares_computadora.csv",
   "san-salvador-MaR09"
   "http://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_ef.csv",
   "san-salvador-CiR01" "http://datosdgai.cultura.gob.mx/pss/CiR01.csv",
   "san-salvador-TjR03b" nil,
   "san-salvador-McR05"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr05_tasa_reciclaje.csv",
   "san-salvador-TcR03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr03_desocu_larga_dur.csv",
   "san-salvador-CcE04"
   "http://archivos.diputados.gob.mx/adela/CcE04.csv",
   "san-salvador-McR04a"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Generacion_de_residuos_solidos_per_capita.csv",
   "san-salvador-CaR05" "http://datosdgai.cultura.gob.mx/pss/CaR05.csv",
   "san-salvador-MfE01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/MfE01.csv",
   "san-salvador-CaR07"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car07_hogares_conexion_internet.csv",
   "san-salvador-CcP01b"
   "http://datosdgai.cultura.gob.mx/pss/CcP01b.csv",
   "san-salvador-CaR10"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car10_hrs_pob_even_culturales.csv",
   "san-salvador-MaR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar02_pob_acceso_serv.csv",
   "san-salvador-McR03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr03_recole_basura.csv",
   "san-salvador-CfR04"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/cfr04_gasto_hog_art_esp.csv",
   "san-salvador-TaR04"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar04_tasa_infor_lab.csv",
   "san-salvador-TfE01"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE01.csv",
   "san-salvador-TcR05"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr05_pob_ocu.csv",
   "san-salvador-CfR03"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfR03.csv",
   "san-salvador-TaR03"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar03_trab_asala_ocup.csv",
   "san-salvador-McR04b"
   "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Generacion_de_residuos_peligrosos_por_empresas.csv",
   "san-salvador-EaP06b"
   "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Secundaria.csv",
   "san-salvador-TjP01"
   "https://repositorio.stps.gob.mx/JFCA/Datos_Abiertos/Datos_AbiertosJFCA.csv",
   "san-salvador-CcR03b"
   "http://datosdgai.cultura.gob.mx/pss/CcR03b.csv",
   "san-salvador-AaR04"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/aar04_pob_acceso_serv_san_mej.csv",
   "san-salvador-CjR01"
   "http://www.dgepj.cjf.gob.mx/DatosAbiertos/CjR01.csv",
   "san-salvador-CiR04" "http://datosdgai.cultura.gob.mx/pss/CiR04.csv",
   "san-salvador-AfP02b"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02b.csv",
   "san-salvador-TcR02"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr02_duracion_desocupacion.csv",
   "san-salvador-TaR05b"
   "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar05b_tasa_cond_crit.csv",
   "san-salvador-CfE02"
   "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE02.csv",
   "san-salvador-CcP03a"
   "http://datosdgai.cultura.gob.mx/pss/CcP03a.csv",
   "san-salvador-TjE01b"
   "http://www.dgepj.cjf.gob.mx/DatosAbiertos/tje02.csv",
   "san-salvador-MfE02"
   "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/MfE02ProyectosAMEXCIDdedicadosProteccionAmbiental.csv"})

(defn apify-files []
  (doall
   (map #(try (update-db (first %) (csv (second %)))
              (catch Exception e (println "Exception on endpoint " (first %) ", file " (second %) "\n\n" e)))
        mirrored-files)))

;; Por ahora los recursos vip. despues sera (db :resources)
(defn mirrored-resources []
  (map resource ["http://web.coneval.gob.mx/Informes/Pobreza/Datos_abiertos/Rezago-social-2000-2005-2010_mun_DA.csv" "http://www.cofepris.gob.mx/Transparencia/Documents/datos_abiertos/Agua_Calidad.csv" "http://201.116.60.46/DatosAbiertos/Datos_de_calidad_del_agua_de_5000_sitios_de_monitoreo.zip" "http://www.siap.gob.mx/datosAbiertos/Estadist_Produc_Agricola/Agt_cierre_2013_2014.csv" "http://www.datos.economia.gob.mx/RegulacionMinera/ConcesionesMineras.csv" "http://dsiappsdev.semarnat.gob.mx/datos/aire/datos%20RETC.csv" "http://www.sectur.gob.mx/DATOS/0116/19-Inventario-Turistico-por-entidad-federativa.csv" "http://www.sectur.gob.mx/DATOS/0116/24-Localidades-que-cuentan-con-el-nombramiento-de-Pueblo-Magico.csv" "http://www.datatur.beta.sectur.gob.mx/Documentos%20compartidos/6_1_td.csv" "http://catalogo.datos.gob.mx/dataset/54ae9a90-418c-4088-88d0-edea59814826/resource/ffc1323a-bf46-4d9d-86a8-237315c2036e/download/matriculaporinstitucionyentidadfederativa.csv" "http://data.sct.gob.mx:8082/datos/datos/abiertos/11601MexicoConectado.xlsx" "http://www.correosdemexico.gob.mx/datosabiertos/cp/cpdescarga.txt" "http://www.correosdemexico.gob.mx/datosabiertos/poligonos/mapapoligonos.zip"]))

(defn mirrored-datasets []
  (mapcat #(:resources (dataset %))
          ["directorio-estadistico-nacional-de-unidades-economicas-denue-por-actividad-economica"
           "indicadores-urbanos"
           "censo-de-escuelas-maestros-y-alumnos-de-educacion-basica-y-especial"
           "proyecciones-de-la-poblacion-de-mexico"
           "regionalizacion-funcional-de-mexico"]))

(defn resource-urls [ds]
  (:resources (dataset ds)))

(def ^:dynamic refineria-dir "/Users/nex/mirrors/")
(def ^:dynamic gh-org "mxabierto-mirror")

(defn emap [& args]
  (doall (apply map args)))

(defn resource-name [resource]
  (str/join (take 100 (standard-name (:name resource)))))

(defn mirror-dir [resource]
  (let [eldir (str refineria-dir (resource-name resource))]
    (sh "mkdir" eldir)
    eldir))

(defn clone-mirror [resource]
  (let [le-name (resource-name resource)]
    (clone (str gh-org "/" le-name)
           (str refineria-dir le-name))))

(defn repo-mirror [resource]
  (let [le-name (resource-name resource)]
    (create-org-repo gh-org
                     le-name
                     {:auth (env :gh)
                      :description (:description resource)})))

(defn repo [resource]
  (try (clone-mirror resource)
       (catch Exception e
         (if (= (:status (ex-data e)) :non-existant)
           (do
             (repo-mirror resource)
             (clone-mirror resource))
           (do
             (checkout (mirror-dir resource) "master")
             (pull (mirror-dir resource) "origin" (branch)))))))

(defn resource-file [resource]
  (str (mirror-dir resource)
       "/"
       (last (re-seq #"[^/]+" (:url resource)))))

(defn copy-resource [resource]
  (copy (:url resource)
       (resource-file resource)))

(defn copy-resources [] (map copy-resource (db :resources)))
(defn refina-csv [file]
  (println "Digitalizando: " file)
  (csv file (digitalize (csv file))))

(defn refina-zip [dir file]
  (println "Descomprimiendo: " file)
  (shs "unzip" file "-d" dir)
  (shs "rm" file))

(defn re-filter [re coll]
  (filter #(re-find re %) coll))

(defn filter-files [dir regex]
  (re-filter regex (ls& dir)))

(defn uncompress [dir]
  (if-let [zips (seq (filter-files dir #"zip"))]
    (do (doall (map #(refina-zip dir %)
                    zips))
        (uncompress dir))))

(defn refina-tsv [file]
  (csv (str file ".csv")
       (digitalize (tsv file)))
  (rm file))

(defn refina-tsvs [files]
  (let [victims (filter #(re-find #"tsv" %) files)]
    (doall (map refina-tsv
                victims))))

(defn refina-psv [file]
  (csv (str file ".csv")
       (digitalize (psv file)))
  (rm file))

(defn refina-psvs [files]
  (let [victims (filter #(and (or (re-find #"txt" %)
                                  (re-find #"psv" %))
                              (> 15 (count (re-find #"|" (slurp %))))) files)]
    (doall (map refina-psv
                victims))))

(defn refina [dir]
  (println "refining: " (str/join ", " (ls dir)))
  (uncompress dir)
  (let [files (ls& dir)]
    (doall (map refina-csv
                (re-filter #"csv" files)))
    (refina-tsvs files)
    (refina-psvs files)))

(defmacro in-buda [& body]
  `(binding [*db* (:db (mg/connect-via-uri "mongodb://localhost:27027/buda"))]
     ~@body))

(defn ls-buda []
  (binding [*db* (:db (mg/connect-via-uri "mongodb://localhost:27027/buda"))]
      (db)))

(defn update-buda [resource-name file]
  (let [batches (partition-all 100 (csv file))]
    (binding [*db* (:db (mg/connect-via-uri "mongodb://localhost:27027/buda"))]
      (db-delete resource-name)
      (emap #(db-insert resource-name %) batches))))

(defn apify
  [resource]
  (let [files (filter-files (mirror-dir resource) #"csv")]
    (map #(update-buda (resource-name resource) %)
         files)))

(defn mirror
  ([] (doall (map mirror (mirrored-resources))));later pmap
  ([resource]
   (let [dir (mirror-dir resource)]
     (try
       (println "mirroring: " (:name resource))
       (repo resource)
       (try
         (copy-resource resource)
         (catch Exception e (println "unable to download " (:name resource))
                (spit "log.log" (json {:name (:name resource) :e (str e) :en 1}))))
       (adda dir)
       (commit (str "Generado por la refinería en: " (t/now)) dir)
       (push dir "origin" "master")
       (checkout-B dir "refineria")
       (git-merge)
       (refina dir)
       (adda dir)
       (commit (str "Generado por la refinería en: " (t/now)) dir)
       (push-force dir "origin" "refineria")
       (apify resource)
       (catch Exception e (println e "\nCould not finish on: " (:name resource))
              (spit "log.log" (json {:name (:name resource) :e (str e) :en 2})))))))
