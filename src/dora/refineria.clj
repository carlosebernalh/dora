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
   "SNEDH-f1c66f48-5160-45d6-851e-8f3ecc2b05ce" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_gpos.csv",
   "SNEDH-07cdb67f-7303-462d-8ace-faa7bf947c16" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_ef.csv",
   "SNEDH-9309b49e-3517-4790-aeee-a9f430a69edf" "https://amilcar.pm/datos/AaR03_porcentaje_de_poblacion_por_debajo_del_nivel_minimo_de_consumo_de_energia_alimentaria.csv",
   "SNEDH-01319d5d-dbd8-4cd6-aa60-d5fb9d7e82a7" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_gpos.csv",
   "SNEDH-f49f596b-f20b-40ad-bca7-467b841cbf48" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_ef.csv",
   "SNEDH-a20cc804-8e5b-4474-a716-a9c77a92a19e" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_gpos.csv",
   "SNEDH-f05015d3-1383-4e2d-8165-3d11d68b1cf6" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_ef.csv",
   "SNEDH-97245744-cb5c-47af-9004-c8ce054f19fc" "https://amilcar.pm/datos/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_ef.csv",
   "SNEDH-270d33ad-b3f9-4ca6-815b-9809c61ed13b" "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/MfE02ProyectosAMEXCIDdedicadosProteccionAmbiental.csv",
   "SNEDH-b1d50492-7fd4-4c0b-9ac4-9367a792c85c" "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/CfP04bBecasExtranjerosOtorgadasPorLaAMEXCID.csv",
   "SNEDH-074bea77-3dfa-4908-9930-65bef361b893" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv",
   "SNEDH-4924142d-9be8-47c1-8c78-379d941cd018" "https://www.conapred.org.mx/datosabiertos/Derechos_Culturales_2011_2016.csv",
   "SNEDH-427bd185-4131-4b4f-aa04-58e54addae2b" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv",
   "SNEDH-e6718fdc-21c5-4448-810d-10e48f03d0b6" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv",
   "SNEDH-f6043a61-8a39-4633-afc9-930145c62814" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02b-Porcentaje-juicios-resueltos-favorablemente.csv",
   "SNEDH-1e4f9637-45ee-41e5-9d92-04dbc6a73120" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv",
   "SNEDH-d9127fa7-7bcb-4483-8397-018aecf2da67" "http://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndProdEneFueRenAltProtSanSalvador-MFR02.csv",
   "SNEDH-be06cc1e-40fe-4604-8e87-8c019726567a" "http://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndConEneRenProtSanSalvador-MFR03.csv",
   "SNEDH-e4376f0c-d337-4993-8766-41b7d5345fc0" "https://amilcar.pm/datos/Datos_AbiertosJFCA.csv",
   "SNEDH-a0fb015b-efd1-40a9-bc4e-e90c286383da" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_gpos.csv",
   "SNEDH-dc81f7b3-ab94-4dde-9e92-08b44c7f168b" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_ef.csv",
   "SNEDH-42f53e5c-fa41-47ce-9e42-1354253e462d" "https://amilcar.pm/datos/AaR03_porcentaje_de_poblacion_por_debajo_del_nivel_minimo_de_consumo_de_energia_alimentaria.csv",
   "SNEDH-29b54f48-62cb-4b78-a025-4fe28550c070" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_gpos.csv",
   "SNEDH-2e97976c-b0c8-4bb1-be53-eab36fc7a149" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_ef.csv",
   "SNEDH-eca541e4-418c-46e2-9f5b-e3cb48895ec7" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_gpos.csv",
   "SNEDH-c9ef3a43-70d9-4815-975e-69394e4ae2a6" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_ef.csv",
   "SNEDH-4c1cbbb8-7f0f-4acb-96d6-5d28fa8d3415" "https://amilcar.pm/datos/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_ef.csv",
   "SNEDH-8d9e07bd-7c3a-4968-86eb-866ee728c64e" "https://amilcar.pm/datos/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_gpos.csv",
   "SNEDH-a237389b-c144-49bb-8616-04f85195263e" "http://archivos.diputados.gob.mx/adela/CcE04.csv",
   "SNEDH-4280ec21-ef75-48c3-b73c-9a587816fad9" "https://www.conapred.org.mx/datosabiertos/Derechos_Culturales_2011_2016.csv",
   "SNEDH-ea84d132-3cac-44f5-8e38-a844c3423238" "https://www.conapred.org.mx/datosabiertos/Derecho_al_Trabajo_2011_2016.csv",
   "SNEDH-d15750e4-2cad-4c43-a888-047e315fc0dd" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv",
   "SNEDH-efc39b54-d960-405e-8e3c-5baa5f09bb17" "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/MfE02ProyectosAMEXCIDdedicadosProteccionAmbiental.csv",
   "SNEDH-d0cdd1cc-2b5c-4e90-bcaf-c638812f21de" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv",
   "SNEDH-81368820-18fa-4df0-a9f4-9fa56d296633" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02a-Porcentaje-conflictos-resueltos-favorables-conciliaci%C3%B3n.csv",
   "SNEDH-3a89c399-ae7c-4c92-b5cb-0a2350e45b60" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02b-Porcentaje-juicios-resueltos-favorablemente.csv",
   "SNEDH-e18b9b93-cd18-41bb-a8a2-8a943aca6076" "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Alfabetizacion.csv",
   "SNEDH-671c7b28-a12d-412a-b8e4-672c1fbb36a1" "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Secundaria.csv",
   "SNEDH-7c38adfb-a5bb-4824-9f93-4a2419a2058c" "http://www.inea.gob.mx/images/documentos/datos/Cobertura_de_Primaria.csv",
   "SNEDH-ebd33bb7-1e00-42a3-a130-6c375cc34a55" "https://amilcar.pm/datos/Porcentaje_de_la_superficie_cubierta_por_bosques_y_selvas.csv",
   "SNEDH-001f6b0f-bb9e-4b81-956d-ceab7f87b13c" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Porcentaje_de_areas_afectadas_por_degradacion_ambiental.csv",
   "SNEDH-330f622e-bb02-472f-94f9-4c59910427cc" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Emisiones_de_dioxido_de_carbono_per_capita.csv",
   "SNEDH-4a87a2a8-d845-412c-b04d-e4af5f56ca9d" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Consumo_de_clorofluorocarburos_que_agotan_la_capa_de_ozono.csv",
   "SNEDH-45647f1e-3789-4231-be3f-b946908da1be" "https://amilcar.pm/datos/Emisiones_de_Gases_Efecto_Invernadero_(GEI).csv",
   "SNEDH-66fc7949-0be1-4c9c-8843-39451d7dda84" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Concentracion_promedio_de_particulas_PM10.csv",
   "SNEDH-5d21cce6-cd3d-4735-9752-c6e13ff6d0a1" "https://amilcar.pm/datos/Generacion_de_residuos_solidos_per_capita.csv",
   "SNEDH-48d6069b-d30b-41e4-9e07-8c4d20a0d4c3" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Generacion_de_residuos_peligrosos_por_empresas.csv",
   "SNEDH-5d19bcfa-88c2-4435-8f1f-22149c9a41cc" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02a.csv",
   "SNEDH-59f33460-af6d-44c6-bf70-cfb04215d614" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02b.csv",
   "SNEDH-fd8465e5-4231-48f8-b8b0-59be67654edd" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE02.csv",
   "SNEDH-84cf0b68-c6d8-4d39-8aeb-10b21a25e199" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE03.csv",
   "SNEDH-abad5c4d-2ed5-4539-8a5e-1061efb9a391" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfP01.csv",
   "SNEDH-4f5c2305-f7a5-4446-80bd-3772c236eade" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfR03.csv",
   "SNEDH-6b1a843f-c744-49ff-9ace-d06047a15692" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/MfE01.csv",
   "SNEDH-ff674a7f-502d-4fe0-8bf2-d92cc922f52f" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/MfP01.csv",
   "SNEDH-23b19966-84bd-4b1b-8db0-03c11f144ccd" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE01.csv",
   "SNEDH-950d0dd2-3795-47da-8f47-e0792857a68e" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE02.csv",
   "SNEDH-c59d2b3a-6785-47c2-a891-611d961190f6" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP01.csv",
   "SNEDH-67a137f2-d914-46c2-9a2c-024f134715d0" "http://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP02.csv",
   "SNEDH-2492d96f-2c2f-4d5f-911a-71841b31e175" "https://amilcar.pm/datos/UfE01.csv",
   "SNEDH-10f94230-9164-4f14-add3-1a0ee9ea9b5f" "https://amilcar.pm/datos/UfP01.csv",
   "SNEDH-99cf58be-c0c6-40eb-8afa-e08286b29adc" "https://www.sagarpa.gob.mx/quienesomos/datosabiertos/sagarpa/Documents/AcP01_.csv",
   "SNEDH-0dd60e20-97a1-4644-8302-dade0bba59d4" "https://transparencia.sre.gob.mx/datos_abiertos/AMEXCID/CfP04bBecasExtranjerosOtorgadasPorLaAMEXCID.csv",
   "SNEDH-b2e2098a-6e9d-4e04-8a62-8b3579e965a2" "http://www.cdi.gob.mx/datosabiertos/2018/Cultura/SNEDH.csv",
   "SNEDH-72ed99f0-173d-43c9-b2b2-afe219076e20" "http://www.dgepj.cjf.gob.mx/DatosAbiertos/tje01.csv",
   "SNEDH-9b3194fa-4481-4456-97f8-26c0ea4183e9" "http://www.dgepj.cjf.gob.mx/DatosAbiertos/tje02.csv",
   "SNEDH-a2e5bb0e-9c7f-4687-b25a-b60a92b27dbd" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/aar04_pob_acceso_serv_san_mej.csv",
   "SNEDH-b35c6d80-dc98-43e0-82f8-62af1da9bdc0" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/adr02_gasto_hogar_alimentos.csv",
   "SNEDH-fd9f40e7-31f8-4435-8cb3-d1b9b398f8d1" "https://amilcar.pm/datos/adr03_ingresos_corr_der_trab.csv",
   "SNEDH-9ca64f8a-8120-4ac6-b2ac-a52da57f837f" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/afp01_indice_rur_ent_fed.csv",
   "SNEDH-1215991b-8bbf-4188-bf77-a0469ec2617e" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car01_tasa_alfabetismo.csv",
   "SNEDH-b2e808d3-9ce6-4689-a74b-a2f37a834bc5" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car02_tasa_alfab_pob_ind.csv",
   "SNEDH-b147861d-9f5a-446d-a2ae-22eb56e5665b" "https://amilcar.pm/datos/car06_hogares_computadora.csv",
   "SNEDH-5b8eb95e-a3d7-4ace-ab7a-ca4f13188e34" "https://amilcar.pm/datos/car07_hogares_conexion_internet.csv",
   "SNEDH-8faed149-d432-4fef-8664-8eb2cf17b732" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car09_eventos_culturales.csv",
   "SNEDH-bed1d521-e171-4563-8ccb-b659021fa8e6" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car10_hrs_pob_even_culturales.csv",
   "SNEDH-4feed196-b153-461b-ba49-8a192031edb7" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car14a_tasa_crec_pob_ind.csv",
   "SNEDH-6f6e2809-9fed-4774-becd-dc53b5061cad" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car14b_pob_afro.csv",
   "SNEDH-784df23d-71f4-47dd-b3bf-9f8cb12b1cf0" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/car16_pob_lee_libros.csv",
   "SNEDH-3743a98e-1699-435b-9b39-39675583d463" "https://amilcar.pm/datos/cdr02_var_hogar_gast_nec_bas.csv",
   "SNEDH-95a3fe13-a35e-48c3-8692-98317a4b67e1" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/cdr04_crec_pob_leg_ind.csv",
   "SNEDH-4dce3d6e-297a-4aed-b883-030acb2d1e2a" "https://amilcar.pm/datos/cfr04_gasto_hog_art_esp.csv",
   "SNEDH-5dc1b006-f7fa-44ac-912e-d2aadba63945" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar01_pob_acceso_agua.csv",
   "SNEDH-e530e4c8-df0e-420e-a02f-18c9cbfad95e" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar02_pob_acceso_serv.csv",
   "SNEDH-6291a1e2-3d72-431a-b6b6-32d03773b33a" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar05_veg_nat_rem_eco.csv",
   "SNEDH-cfb813bc-4d62-49b9-a12b-80a2c3c4adcb" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mar10_pob_acceso_gas.csv",
   "SNEDH-634e91e3-4768-45ae-9a8e-b0bdce053a81" "https://amilcar.pm/datos/mar14_num_vehiculos_automotores.csv",
   "SNEDH-eb645747-7a29-4204-bb6c-fda187e5a78e" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr01_agua_entubada_hogar.csv",
   "SNEDH-1c83fb04-1cdc-4ae3-b7a4-21d05f8c4065" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr02_energia_elect_hogar.csv",
   "SNEDH-720b19c0-a12e-4f79-a329-eae19fabfe5f" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr03_recole_basura.csv",
   "SNEDH-2aa92fe0-89fa-4bca-b813-fe4ead2a37b3" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr05_tasa_reciclaje.csv",
   "SNEDH-48c1d767-cb71-48f0-a36a-2d937cf15c45" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mcr07_eliminacion_excretas.csv",
   "SNEDH-23884293-8595-4b16-a7cf-09a269334a9c" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/mdr02_saneamiento_mejorado.csv",
   "SNEDH-9a52a2ad-6778-4894-b20f-844845126061" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar01_tasa_ocup_infantil.csv",
   "SNEDH-f48a05ab-8a69-47e5-831e-118de04bdf38" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar02a_tasa_desocupacion.csv",
   "SNEDH-3cf060be-94ed-4dd8-be3d-0c4e0fe3d2e4" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar03_trab_asala_ocup.csv",
   "SNEDH-45a64e4e-8932-45e0-8f35-f7046954c29f" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar04_tasa_infor_lab.csv",
   "SNEDH-23444004-ab72-42d4-9025-630a410c1f3c" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar05a_trab_ingr_inf_sal_min.csv",
   "SNEDH-43519ebc-3d7b-4826-9a88-d3dff2837934" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar05b_tasa_cond_crit.csv",
   "SNEDH-873dd425-4c45-478a-a712-40ecb636d6c1" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar06_muj_tot_asal_no_agro.csv",
   "SNEDH-794d12d2-7413-43d1-bd1f-56f6943e1a34" "https://amilcar.pm/datos/tar08b_muj_fun_pub.csv",
   "SNEDH-27d1ac47-dc05-4a14-8335-733a5bf783e0" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tar09_tasa_part_per_dis.csv",
   "SNEDH-ae0798a4-6669-45d3-8933-81ac4a7640af" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr02_duracion_desocupacion.csv",
   "SNEDH-9cb7a2a2-aff6-4796-a458-cc7aa47241da" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr03_desocu_larga_dur.csv",
   "SNEDH-201a6671-7287-44b4-911b-96b6d691708e" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tcr05_pob_ocu.csv",
   "SNEDH-d494d2a6-7eae-418f-90f6-3e290c15cc02" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdp03_inst_salud_pob.csv",
   "SNEDH-fca62e4e-97b3-4a83-9718-0fbb64e940f2" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01a_brecha_tasa_part_lab.csv",
   "SNEDH-9e393603-469e-4e4a-9782-d703e56ed86b" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01b_brecha_tasa_deso_lab.csv",
   "SNEDH-f6abd8de-dbc4-4c76-8fee-a738082031f4" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr01c_brecha_tasa_infor_lab.csv",
   "SNEDH-e1277e2f-78cb-4f59-9dcf-b9108c4daf48" "https://amilcar.pm/datos/tdr01d_brecha_tasa_sin_contrato.csv",
   "SNEDH-c2635f4e-635d-48fe-a17a-7f71cd6fe798" "https://amilcar.pm/datos/tdr02_ing_lab_per_cap.csv",
   "SNEDH-d2301822-4afb-4c1b-b89b-6fdbeeddef7b" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr03_muj_ocup_prest_lab.csv",
   "SNEDH-7a3592ee-7294-4960-93b4-9a2a07061daa" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr04_homb_ocup.csv",
   "SNEDH-866ec23c-b9b4-4f4b-88ce-fda80aabd600" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tdr05_discr_sal_muj_homb.csv",
   "SNEDH-57ea9a79-4253-412c-ae21-ac8f9b55a825" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tip02_solic_anuales_inf_inegi.csv",
   "SNEDH-53ed0dbb-4a46-468f-9442-7636b7a6d628" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/tir01_usu_anuales_portal_inegi.csv",
   "SNEDH-10084945-caa3-4ad2-b9e5-2927fedfa0f5" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/uar01_tasa_sindica.csv",
   "SNEDH-c103a6a4-b9f3-42e5-a347-8e0abe9af7c2" "http://www.beta.inegi.org.mx/contenidos/proyectos/pss/datosabiertos/ucr02_sindicalizacion_ent_fed.csv",
   "SNEDH-ffa8f093-75c1-4f17-9486-5f9281744ef8" "https://amilcar.pm/datos/udr01_sindicalizacion_gpos_pob.csv",
   "SNEDH-4a82ab47-ece4-40fc-bf82-4a9fa23c2364" "http://datosdgai.cultura.gob.mx/pss/CaR03.csv",
   "SNEDH-604d4d60-77b3-4bb3-a1ed-9f45025f20e4" "http://datosdgai.cultura.gob.mx/pss/CaR04.csv",
   "SNEDH-5e4ba28a-6244-4cd1-8222-5f870ec5bce6" "http://datosdgai.cultura.gob.mx/pss/CaR05.csv",
   "SNEDH-55dab076-19b0-4f6a-ae9b-2105b78366a6" "https://amilcar.pm/datos/CaR15b.csv",
   "SNEDH-306487e5-8085-40ea-b249-6390095ce03d" "http://datosdgai.cultura.gob.mx/pss/CcP01a.csv",
   "SNEDH-8a373549-1508-48da-99db-2505a085b7f4" "http://datosdgai.cultura.gob.mx/pss/CcP01b.csv",
   "SNEDH-d5ca0c04-9710-4af1-a3f4-c2923fbec6f1" "http://datosdgai.cultura.gob.mx/pss/CcP01c.csv",
   "SNEDH-bbf35e62-e1dd-41c7-8825-a8f35a41fded" "http://datosdgai.cultura.gob.mx/pss/CcP01d.csv",
   "SNEDH-5613a682-1db4-4ab1-aba6-ab8a98992e68" "http://datosdgai.cultura.gob.mx/pss/CcP03a.csv",
   "SNEDH-39b6c71b-b12c-47b9-8bf5-bf160fa55b1e" "http://datosdgai.cultura.gob.mx/pss/CcP03b.csv",
   "SNEDH-e6301c90-f613-4a16-ac25-910ae64251fc" "http://datosdgai.cultura.gob.mx/pss/CcR02.csv",
   "SNEDH-dd9b242c-95f7-42cf-adc5-b3d0442e304d" "http://datosdgai.cultura.gob.mx/pss/CcR03a.csv",
   "SNEDH-e2ba76f5-82f4-44dd-bea8-20f84488d344" "http://datosdgai.cultura.gob.mx/pss/CcR03b.csv",
   "SNEDH-2aea684f-9869-4bc2-9fcc-572bf8e5f712" "http://datosdgai.cultura.gob.mx/pss/CiR01.csv",
   "SNEDH-75ab4c8d-0b1f-4697-ac6d-e682f6d40c94" "https://amilcar.pm/datos/CiR04.csv",
   "SNEDH-fc1949a8-a9fd-47bb-8e9b-7d1816d3d76b" "http://www.dgepj.cjf.gob.mx/DatosAbiertos/CjR01.csv",
   "SNEDH-4a28f010-cefc-4e53-8304-49ccad242a46" "http://datosabiertospgr.blob.core.windows.net/sansalvador/ExplotacionSexualdeMenoresApCi.csv",
   "SNEDH-3fefcd7b-e058-4360-b673-108b154aa468" "http://datosabiertospgr.blob.core.windows.net/sansalvador/PorcentajedeDeterminacionesTratadePersonas.csv",
   "SNEDH-5090591a-58b0-49e2-9f36-e865e8aa4b36" "http://datosabiertospgr.blob.core.windows.net/sansalvador/MjR02a.csv",
   "SNEDH-fc561b8e-c9b6-4a1d-a845-e82c04d9c036" "http://datosabiertospgr.blob.core.windows.net/sansalvador/MjE03a.csv",
   "SNEDH-c4b3ad8e-ffaf-4f2f-a9e8-0faa6b8a9256" "http://datosabiertospgr.blob.core.windows.net/sansalvador/HostigamientoSexualApCi.csv",
   "SNEDH-2cd860c4-6789-4d8e-8dfb-81c927c235de" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_mujeres_en_la_dirigencia_sindical_UdR02.csv",
   "SNEDH-f01384e9-2405-4904-8e63-80f3a2d76b6f" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Registro_de_nuevos_sindicatos_anualmente_UcR04.csv",
   "SNEDH-cc72a9c2-8962-4c2b-82d3-8d82ec1cb330" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_los_sindicatos_con_un_numero_de_afiliados_UfR01.csv",
   "SNEDH-45b750e8-8ddb-46cf-b822-358b120e10d0" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Numero_de_trabajadores_incluidos_en_solicitudes_de_inscripción_de_sindicatos_Ent_Fed_UaP09b2.csv",
   "SNEDH-35e80c9f-cf4c-41d8-876a-3a8c87d1bb49" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Num_solicitudes_inscripcion_sindicatos_rechazadas_Ent_Fed_UaP09a2.csv"
   ,"SNEDH-ba7e71db-5089-4bc1-b0c7-7cd802b3c098" "https://drive.google.com/uc?export=download&id=1hHsrSOFwY25hY3Ffs6hZJnpC38HJeXm3"
   ,"SNEDH-944c7e38-1d5a-4750-ae8b-e884f462ef17" "https://drive.google.com/uc?export=download&id=1aUkifUu86iqikKjil50Uku8SclUU5Z1Y"
   ,"SNEDH-31989aa7-2072-4b2c-9571-e24f239aaab4" "https://datosabiertos.segob.gob.mx/DatosAbiertos/UDDH_Expedientes"
   ,"SNEDH-8dbb8db5-d502-4910-9aef-4189c32ed92a" "https://datosabiertos.segob.gob.mx/DatosAbiertos/UDDH_Defensoras"
   ,"SNEDH-71626ea2-8bbd-4ef3-888d-70f1d8613904" "https://drive.google.com/uc?export=download&id=1uR7lyueZ2jREaMVKtfIffSrI5lEE5zeb"
   ,"SNEDH-1cd40277-c0e4-4248-b10f-6a49f79caa15" "https://drive.google.com/uc?export=download&id=1uR7lyueZ2jREaMVKtfIffSrI5lEE5zeb"
   ,"SNEDH-678bb367-ca3c-44d4-9365-5e821be00c9d" "https://drive.google.com/uc?export=download&id=1tlSOH4OCH1Ki7WTtyPYen7gdPqy5RQD0"
   ,"SNEDH-9bbeb41f-ac51-4628-a01e-33ab8bb09357" "https://drive.google.com/uc?export=download&id=1DLQBKXao0xuA1FCeVjCsX9ythSteeNqF"
   ,"SNEDH-9cb4002c-f00d-4860-a6e7-6ad42ecd217d" "http://datosdgai.cultura.gob.mx/pss/CiP01.csv"})

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
