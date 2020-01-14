(ns dora.refineria
  (:require [clj-time.core :as t]
            [clojure.java.shell :refer :all]
            [clojure.string :as str]
            [digitalize.core :refer :all]
            [digitalize.strings :refer :all]
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
   "SNEDH-f1c66f48-5160-45d6-851e-8f3ecc2b05ce" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_gpos.csv"
,"SNEDH-07cdb67f-7303-462d-8ace-faa7bf947c16" "https://amilcar.pm/datos/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_ef.csv"
,"SNEDH-9309b49e-3517-4790-aeee-a9f430a69edf" "https://amilcar.pm/datos/AaR03_porcentaje_de_poblacion_por_debajo_del_nivel_minimo_de_consumo_de_energia_alimentaria.csv"
,"SNEDH-01319d5d-dbd8-4cd6-aa60-d5fb9d7e82a7" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_gpos.csv"
,"SNEDH-f49f596b-f20b-40ad-bca7-467b841cbf48" "https://amilcar.pm/datos/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_ef.csv"
,"SNEDH-a20cc804-8e5b-4474-a716-a9c77a92a19e" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_gpos.csv"
,"SNEDH-f05015d3-1383-4e2d-8165-3d11d68b1cf6" "https://amilcar.pm/datos/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_ef.csv"
,"SNEDH-97245744-cb5c-47af-9004-c8ce054f19fc" "https://amilcar.pm/datos/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_ef.csv"
,"SNEDH-270d33ad-b3f9-4ca6-815b-9809c61ed13b" "https://amilcar.pm/datos/MfE02ProyectosAMEXCIDdedicadosProteccionAmbiental.csv"
,"SNEDH-b1d50492-7fd4-4c0b-9ac4-9367a792c85c" "https://amilcar.pm/datos/CfP04bBecasExtranjerosOtorgadasPorLaAMEXCID.csv"
,"SNEDH-074bea77-3dfa-4908-9930-65bef361b893" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv"
,"SNEDH-4924142d-9be8-47c1-8c78-379d941cd018" "https://www.conapred.org.mx/datosabiertos/Derechos_Culturales_2011_2016.csv"
,"SNEDH-427bd185-4131-4b4f-aa04-58e54addae2b" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv"
,"SNEDH-e6718fdc-21c5-4448-810d-10e48f03d0b6" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv"
,"SNEDH-f6043a61-8a39-4633-afc9-930145c62814" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02b-Porcentaje-juicios-resueltos-favorablemente.csv"
,"SNEDH-1e4f9637-45ee-41e5-9d92-04dbc6a73120" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv"
,"SNEDH-d9127fa7-7bcb-4483-8397-018aecf2da67" "https://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndProdEneFueRenAltProtSanSalvador-MFR02.csv"
,"SNEDH-be06cc1e-40fe-4604-8e87-8c019726567a" "https://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndConEneRenProtSanSalvador-MFR03.csv"
,"SNEDH-e4376f0c-d337-4993-8766-41b7d5345fc0" "https://amilcar.pm/datos/Datos_AbiertosJFCA.csv"
,"SNEDH-4280ec21-ef75-48c3-b73c-9a587816fad9" "https://www.conapred.org.mx/datosabiertos/Derechos_Culturales_2011_2016.csv"
,"SNEDH-ea84d132-3cac-44f5-8e38-a844c3423238" "https://www.conapred.org.mx/datosabiertos/Derecho_al_Trabajo_2011_2016.csv"
,"SNEDH-d15750e4-2cad-4c43-a888-047e315fc0dd" "https://www.conapred.org.mx/datosabiertos/Acceso_a_la_Justicia_2011_2016.csv"
,"SNEDH-d0cdd1cc-2b5c-4e90-bcaf-c638812f21de" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv"
,"SNEDH-81368820-18fa-4df0-a9f4-9fa56d296633" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02a-Porcentaje-conflictos-resueltos-favorables-conciliaci%C3%B3n.csv"
,"SNEDH-3a89c399-ae7c-4c92-b5cb-0a2350e45b60" "http://www.profedet.gob.mx/profedet/datosabiertos/servicios/IndicadorTjP02b-Porcentaje-juicios-resueltos-favorablemente.csv"
,"SNEDH-13964da9-392a-4816-a727-7f2e3a296851" "http://amilcar.pm/user/pages/04.datos/01.SEP/01.INEA/Cobertura_de_Alfabetizacion.csv"
,"SNEDH-9204d917-d0f5-432b-9da0-5bbf51ad5aee" "http://amilcar.pm/user/pages/04.datos/01.SEP/01.INEA/Cobertura_de_Primaria.csv"
,"SNEDH-efe911dc-a79e-4b81-83fe-4cd0d221de71" "http://amilcar.pm/user/pages/04.datos/01.SEP/01.INEA/Cobertura_de_Secundaria.csv"
,"SNEDH-5d19bcfa-88c2-4435-8f1f-22149c9a41cc" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02a.csv"
,"SNEDH-59f33460-af6d-44c6-bf70-cfb04215d614" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02b.csv"
,"SNEDH-fd8465e5-4231-48f8-b8b0-59be67654edd" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE02.csv"
,"SNEDH-84cf0b68-c6d8-4d39-8aeb-10b21a25e199" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE03.csv"
,"SNEDH-abad5c4d-2ed5-4539-8a5e-1061efb9a391" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfP01.csv"
,"SNEDH-4f5c2305-f7a5-4446-80bd-3772c236eade" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfR03.csv"
,"SNEDH-6b1a843f-c744-49ff-9ace-d06047a15692" "http://amilcar.pm/user/pages/04.datos/16.SHCP/MfE01.csv"
,"SNEDH-ff674a7f-502d-4fe0-8bf2-d92cc922f52f" "http://amilcar.pm/user/pages/04.datos/16.SHCP/MfP01.csv"
,"SNEDH-23b19966-84bd-4b1b-8db0-03c11f144ccd" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE01.csv"
,"SNEDH-950d0dd2-3795-47da-8f47-e0792857a68e" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE02.csv"
,"SNEDH-c59d2b3a-6785-47c2-a891-611d961190f6" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP01.csv"
,"SNEDH-67a137f2-d914-46c2-9a2c-024f134715d0" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP02.csv"
,"SNEDH-2492d96f-2c2f-4d5f-911a-71841b31e175" "https://amilcar.pm/datos/UfE01.csv"
,"SNEDH-10f94230-9164-4f14-add3-1a0ee9ea9b5f" "https://amilcar.pm/datos/UfP01.csv"
,"SNEDH-b2e2098a-6e9d-4e04-8a62-8b3579e965a2" "http://www.cdi.gob.mx/datosabiertos/2018/Cultura/SNEDH.csv"
,"SNEDH-fd9f40e7-31f8-4435-8cb3-d1b9b398f8d1" "https://amilcar.pm/datos/adr03_ingresos_corr_der_trab.csv"
,"SNEDH-b147861d-9f5a-446d-a2ae-22eb56e5665b" "https://amilcar.pm/datos/car06_hogares_computadora.csv"
,"SNEDH-5b8eb95e-a3d7-4ace-ab7a-ca4f13188e34" "https://amilcar.pm/datos/car07_hogares_conexion_internet.csv"
,"SNEDH-3743a98e-1699-435b-9b39-39675583d463" "https://amilcar.pm/datos/cdr02_var_hogar_gast_nec_bas.csv"
,"SNEDH-4dce3d6e-297a-4aed-b883-030acb2d1e2a" "https://amilcar.pm/datos/cfr04_gasto_hog_art_esp.csv"
,"SNEDH-634e91e3-4768-45ae-9a8e-b0bdce053a81" "https://amilcar.pm/datos/mar14_num_vehiculos_automotores.csv"
,"SNEDH-794d12d2-7413-43d1-bd1f-56f6943e1a34" "https://amilcar.pm/datos/tar08b_muj_fun_pub.csv"
,"SNEDH-e1277e2f-78cb-4f59-9dcf-b9108c4daf48" "https://amilcar.pm/datos/tdr01d_brecha_tasa_sin_contrato.csv"
,"SNEDH-c2635f4e-635d-48fe-a17a-7f71cd6fe798" "https://amilcar.pm/datos/tdr02_ing_lab_per_cap.csv"
,"SNEDH-ffa8f093-75c1-4f17-9486-5f9281744ef8" "https://amilcar.pm/datos/udr01_sindicalizacion_gpos_pob.csv"
,"SNEDH-4a82ab47-ece4-40fc-bf82-4a9fa23c2364" "http://datosdgai.cultura.gob.mx/pss/CaR03.csv"
,"SNEDH-604d4d60-77b3-4bb3-a1ed-9f45025f20e4" "http://datosdgai.cultura.gob.mx/pss/CaR04.csv"
,"SNEDH-5e4ba28a-6244-4cd1-8222-5f870ec5bce6" "http://datosdgai.cultura.gob.mx/pss/CaR05.csv"
,"SNEDH-55dab076-19b0-4f6a-ae9b-2105b78366a6" "https://amilcar.pm/datos/CaR15b.csv"
,"SNEDH-306487e5-8085-40ea-b249-6390095ce03d" "http://datosdgai.cultura.gob.mx/pss/CcP01a.csv"
,"SNEDH-8a373549-1508-48da-99db-2505a085b7f4" "http://datosdgai.cultura.gob.mx/pss/CcP01b.csv"
,"SNEDH-d5ca0c04-9710-4af1-a3f4-c2923fbec6f1" "http://datosdgai.cultura.gob.mx/pss/CcP01c.csv"
,"SNEDH-bbf35e62-e1dd-41c7-8825-a8f35a41fded" "http://datosdgai.cultura.gob.mx/pss/CcP01d.csv"
,"SNEDH-5613a682-1db4-4ab1-aba6-ab8a98992e68" "http://datosdgai.cultura.gob.mx/pss/CcP03a.csv"
,"SNEDH-39b6c71b-b12c-47b9-8bf5-bf160fa55b1e" "http://datosdgai.cultura.gob.mx/pss/CcP03b.csv"
,"SNEDH-e6301c90-f613-4a16-ac25-910ae64251fc" "http://datosdgai.cultura.gob.mx/pss/CcR02.csv"
,"SNEDH-dd9b242c-95f7-42cf-adc5-b3d0442e304d" "http://datosdgai.cultura.gob.mx/pss/CcR03a.csv"
,"SNEDH-e2ba76f5-82f4-44dd-bea8-20f84488d344" "http://datosdgai.cultura.gob.mx/pss/CcR03b.csv"
,"SNEDH-2aea684f-9869-4bc2-9fcc-572bf8e5f712" "https://amilcar.pm/datos/CiR01.csv"
,"SNEDH-75ab4c8d-0b1f-4697-ac6d-e682f6d40c94" "https://amilcar.pm/datos/CiR04.csv"
,"SNEDH-fc561b8e-c9b6-4a1d-a845-e82c04d9c036" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/TasadeFuncionariosEspecializadosDelitosAmbientales.csv"
,"SNEDH-5090591a-58b0-49e2-9f36-e865e8aa4b36" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/MjR02a.csv"
,"SNEDH-4a28f010-cefc-4e53-8304-49ccad242a46" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/ExplotacionSexualdeMenoresApCi.csv"
,"SNEDH-c4b3ad8e-ffaf-4f2f-a9e8-0faa6b8a9256" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/HostigamientoSexualApCi.csv"
,"SNEDH-3fefcd7b-e058-4360-b673-108b154aa468" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/PorcentajedeDeterminacionesTratadePersonas.csv "
,"SNEDH-2cd860c4-6789-4d8e-8dfb-81c927c235de" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_mujeres_en_la_dirigencia_sindical_UdR02.csv"
,"SNEDH-f01384e9-2405-4904-8e63-80f3a2d76b6f" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Registro_de_nuevos_sindicatos_anualmente_UcR04.csv"
,"SNEDH-cc72a9c2-8962-4c2b-82d3-8d82ec1cb330" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_los_sindicatos_con_un_numero_de_afiliados_UfR01.csv"
,"SNEDH-45b750e8-8ddb-46cf-b822-358b120e10d0" "http://amilcar.pm/user/pages/04.datos/04.STPS/UaP09b2.csv"
,"SNEDH-35e80c9f-cf4c-41d8-876a-3a8c87d1bb49" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Num_solicitudes_inscripcion_sindicatos_rechazadas_Ent_Fed_UaP09a2.csv"
,"SNEDH-ba7e71db-5089-4bc1-b0c7-7cd802b3c098" "https://drive.google.com/uc?export=download&id=1hHsrSOFwY25hY3Ffs6hZJnpC38HJeXm3"
,"SNEDH-71626ea2-8bbd-4ef3-888d-70f1d8613904" "https://drive.google.com/uc?export=download&id=1uR7lyueZ2jREaMVKtfIffSrI5lEE5zeb"
,"SNEDH-9bbeb41f-ac51-4628-a01e-33ab8bb09357" "https://drive.google.com/uc?export=download&id=1DLQBKXao0xuA1FCeVjCsX9ythSteeNqF"
,"SNEDH-9cb4002c-f00d-4860-a6e7-6ad42ecd217d" "http://datosdgai.cultura.gob.mx/pss/CiP01.csv"
,"SNEDH-9e0cb023-3e34-4768-95a4-9d8512998033" "http://sep.gob.mx/dgticDatos/dgaigypt/ecP01.csv"
,"SNEDH-c1ec6909-5149-4052-8a22-1c74f2a367b1" "http://sep.gob.mx/dgticDatos/dgaigypt/eaP05.csv"
,"SNEDH-prueba-SEP" "http://sep.gob.mx/dgticDatos/dgaigypt/2019/EdP06.csv"
,"SNEDH-48c1d767-cb71-48f0-a36a-2d937cf15c45" "https://www.inegi.org.mx/contenidos/datosgob/mcr07_eliminacion_excretas.csv"
,"SNEDH-23884293-8595-4b16-a7cf-09a269334a9c" "https://www.inegi.org.mx/contenidos/datosgob/mdr02_saneamiento_mejorado.csv"
,"SNEDH-a0fb015b-efd1-40a9-bc4e-e90c286383da" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_gpos.csv"
,"SNEDH-d7e63341-8129-410a-9e7f-b2ffeca2ea30" "https://www.inegi.org.mx/contenidos/datosgob/aar04_pob_acceso_serv_san_mej.csv"
,"SNEDH-29b54f48-62cb-4b78-a025-4fe28550c070" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_gpos.csv"
,"SNEDH-eca541e4-418c-46e2-9f5b-e3cb48895ec7" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_gpos.csv"
,"SNEDH-0f035aa6-587e-462e-b26a-7863e15d5405" "https://www.inegi.org.mx/contenidos/datosgob/car16_pob_lee_libros.csv"
,"SNEDH-9ff82c39-1797-492d-bd8f-131eaceea778" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR05.csv"
,"SNEDH-8884dad3-6da2-4b49-8a9e-e33f8c023273" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR06_sexo.csv"
,"SNEDH-c40a578f-881a-4fe0-bbf7-613306fa69c0" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR06_tipo_de_sost.csv"
,"SNEDH-b5f44720-25c2-436f-95df-5115e02da1c4" "https://www.inegi.org.mx/contenidos/datosgob/mar02_pob_acceso_serv.csv"
,"SNEDH-8d9e07bd-7c3a-4968-86eb-866ee728c64e" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_gpos.csv"
,"SNEDH-8dbb8db5-d502-4910-9aef-4189c32ed92a" "http://amilcar.pm/user/pages/04.datos/15.SEGOB/DH_Defensores_SS.csv"
,"SNEDH-768367cf-7ba1-45e3-8ed8-8a843912b79f" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/ScR01_Porcentaje_de_poblacion_con_carencia_por_acceso_a_los_servicios_de_salud_gpos.csv"
,"SNEDH-86bc785d-56b8-42d5-9f39-1b7de2ab1f56" "https://www.inegi.org.mx/contenidos/datosgob/ssap02_pob_der_habiente.csv"
,"SNEDH-6587acf0-6d26-4de6-9c50-646861ed171a" "https://www.inegi.org.mx/contenidos/datosgob/ssap03_sistema_no_con_salud.csv"
,"SNEDH-25a2d46e-3ea2-4a14-9087-6d4cb7d68917" "https://www.inegi.org.mx/contenidos/datosgob/ssar01_tasa_part_eco.csv"
,"SNEDH-a23b00ec-c1db-4027-8023-7d53e614b9b5" "https://www.inegi.org.mx/contenidos/datosgob/ssar02_pea_afil_seg_social.csv"
,"SNEDH-a96a8916-bf7e-4020-a29f-f30261c6afd5" "https://www.inegi.org.mx/contenidos/datosgob/ssar03_pob_cot_reg_con.csv"
,"SNEDH-3a628c42-aef5-4e1d-bc21-3475eea1b3c9" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/SScR01_Porcentaje_de_la_poblacion_con_carencia_por_acceso_a_la_seguridad_social_gpos.csv"
,"SNEDH-56439491-7fb0-4764-b375-020c1f283383" "https://www.inegi.org.mx/contenidos/datosgob/sscr03a_tasa_ries_trab.csv"
,"SNEDH-e6aa87f9-a7f1-48d9-972e-5e9e551b5dcd" "https://www.inegi.org.mx/contenidos/datosgob/sscr03b_tasa_inc_trab.csv"
,"SNEDH-85a7dd34-15e0-43e3-a970-52793a7e2a7e" "https://www.inegi.org.mx/contenidos/datosgob/ssdp02a_por_trab_dom_cui.csv"
,"SNEDH-e365abfa-7906-4fc2-ad35-a9e21b8c9b9a" "https://www.inegi.org.mx/contenidos/datosgob/ssdp02b_men_asis_guar_pub.csv"
,"SNEDH-27e043ea-3531-465b-ad2c-be56ad881c2e" "https://www.inegi.org.mx/contenidos/datosgob/ssdr01_pob_65_pen_dire.csv"
,"SNEDH-af29c2af-a72d-4bc2-af00-cf4d7cbab142" "https://www.inegi.org.mx/contenidos/datosgob/ssdr02a_dere_rec_pen.csv"
,"SNEDH-98b89f24-685f-4c46-94cb-ef47304faa25" "https://www.inegi.org.mx/contenidos/datosgob/ssdr03a_pob_ocu_afil_seg_social.csv"
,"SNEDH-fa7d6fe9-b73e-4a02-ac4a-24c4306f62b1" "https://www.inegi.org.mx/contenidos/datosgob/ssdr02b_dere_rec_pen_rango.csv"
,"SNEDH-dc81f7b3-ab94-4dde-9e92-08b44c7f168b" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR02_porcentaje_de_personas_con_carencia_de_acceso_a_la_alimentacion_ef.csv"
,"SNEDH-42f53e5c-fa41-47ce-9e42-1354253e462d" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR03_porcentaje_de_poblacion_por_debajo_del_nivel_minimo_de_consumo_de_energia_alimentaria.csv"
,"SNEDH-2e97976c-b0c8-4bb1-be53-eab36fc7a149" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05a_porcentaje_de_poblacion_con_ingreso_inferior_a_la_linea_de_bienestar_minimo_ef.csv"
,"SNEDH-c9ef3a43-70d9-4815-975e-69394e4ae2a6" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/AaR05b_porcentaje_de_poblacion_en_situacion_de_pobreza_extrema_ef.csv"
,"SNEDH-RECURSO-INSP-AaR06a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR06a%20Prevalencia_de_desnutrici%C3%B3n_infantil_en_menores_de_5_a%C3%B1os_Desnutrici%C3%B3n_Cr%C3%B3nica.csv"
,"SNEDH-RECURSO-INSP-AaR06b" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR06b%20Prevalencia_de_desnutrici%C3%B3n_infantil_en_menores_de_5_a%C3%B1os_Desnutrici%C3%B3n_Aguda.csv"
,"SNEDH-d4d4b3d8-b46a-4d4b-88a2-c3ef28152d7a" "https://www.inegi.org.mx/contenidos/datosgob/adr02_gasto_hogar_alimentos.csv"
,"SNEDH-67fa72ff-4095-424b-9939-96074a87922e" "https://www.inegi.org.mx/contenidos/datosgob/adr03_ingresos_corr_der_trab.csv"
,"SNEDH-2210a0cc-4671-4d83-b330-5ae02f7bedb7" "https://www.inegi.org.mx/contenidos/datosgob/afp01_indice_rur_ent_fed.csv"
,"SNEDH-b71d40fe-f527-4226-bd9a-32c15d7701ef" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02a.csv"
,"SNEDH-0d0b09d5-0e68-4778-9f5c-74c200919bb5" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/AfP02b.csv"
,"SNEDH-62b72b68-7684-487f-b886-92ef041effc2" "https://www.inegi.org.mx/contenidos/datosgob/afr01_sector_primario_pib.csv"
,"SNEDH-aad64856-e526-4922-aa52-62bb2aee8695" "http://amilcar.pm/user/pages/04.datos/03.TSA/AjP07a_V02.csv"
,"SNEDH-6a38fb33-5e25-45e1-8752-1ff15e41c684" "http://amilcar.pm/user/pages/04.datos/03.TSA/AjP07b_V02.csv"
,"SNEDH-534ee9e9-a868-4f86-893e-1d33df640895" "https://www.inegi.org.mx/contenidos/datosgob/ajp04a_hec_der_hum_alimen.csv"
,"SNEDH-3f00cc4f-376b-4118-a04e-c178f2927ea2" "https://www.inegi.org.mx/contenidos/datosgob/ajp04b_vic_der_hum_alimen.csv"
,"SNEDH-73f6bfff-ca8d-48c3-92e1-7a82229ac019" "https://www.inegi.org.mx/contenidos/datosgob/ajr01_conflictos_alimentacion.csv"
,"SNEDH-569689e0-c87f-4ae9-8b11-4eb257275382" "https://www.inegi.org.mx/contenidos/datosgob/car01_tasa_alfabetismo.csv"
,"SNEDH-cddfd8cd-6520-482b-908f-bcbdb5911b8b" "https://www.inegi.org.mx/contenidos/datosgob/car02_tasa_alfab_pob_ind.csv"
,"SNEDH-ab591ef6-f3eb-46cc-a5f0-ed90ea980283" "http://datosdgai.cultura.gob.mx/pss/CaR03.csv"
,"SNEDH-f09f20f4-2e5d-48d0-a8bd-2c2e21100a39" "http://datosdgai.cultura.gob.mx/pss/CaR04.csv"
,"SNEDH-0fee4e6d-7b17-46b7-944a-4e32a118a8bc" "http://datosdgai.cultura.gob.mx/pss/CaR05.csv"
,"SNEDH-a712fadd-6673-4521-ba17-8c2d475092b4" "https://www.inegi.org.mx/contenidos/datosgob/car06_hogares_computadora.csv"
,"SNEDH-f90f9728-8aa3-43e2-90bc-addd081386ce" "https://www.inegi.org.mx/contenidos/datosgob/car07_hogares_conexion_internet.csv"
,"SNEDH-6ebc309d-d084-44cd-b336-b6ee8603f042" "https://www.inegi.org.mx/contenidos/datosgob/car08_per_uso_der_cult.csv"
,"SNEDH-61dddc93-8a1d-4678-8505-198b0b4045be" "https://www.inegi.org.mx/contenidos/datosgob/car09_eventos_culturales.csv"
,"SNEDH-5e262519-0f86-4c2e-bdca-9c0480cf7719" "https://www.inegi.org.mx/contenidos/datosgob/car10_hrs_pob_even_culturales.csv"
,"SNEDH-c69fd3e9-8f52-4fac-8eb7-49d79d42707a" "https://indesol.cloud/datos-abiertos/CaR11a.csv"
,"SNEDH-4dc2c62a-717f-48c2-8bce-659b264a0bcc" "https://indesol.cloud/datos-abiertos/CaR11b.csv"
,"SNEDH-2712ff22-a018-46e3-ab0b-00e55f773e2d" "http://amilcar.pm/user/pages/04.datos/14.INEGI/car14b_pob_afro.csv"
,"SNEDH-46da8a46-c631-4316-ade5-ab98c058bfbf" "http://amilcar.pm/user/pages/04.datos/01.SEP/CaR15a.csv"
,"SNEDH-3cea39e0-5dfc-472d-b28b-a44fe03e49d1" "http://datosdgai.cultura.gob.mx/pss/CaR15b.csv"
,"SNEDH-637337b5-9797-4c9b-ae0a-c0b7d18ca3cc" "http://datosdgai.cultura.gob.mx/pss/CcP01a.csv"
,"SNEDH-f112a1ea-1523-4e8b-9b86-2f4fad116d90" "http://datosdgai.cultura.gob.mx/pss/CcP01b.csv"
,"SNEDH-8b3c53b2-30a1-4878-8c2d-c1240777f200" "http://datosdgai.cultura.gob.mx/pss/CcP01c.csv"
,"SNEDH-1c023dae-a59a-4076-8aa4-6d535a995b37" "http://datosdgai.cultura.gob.mx/pss/CcP01d.csv"
,"SNEDH-b8b68210-e92b-4033-9af1-bbac59e0e046" "http://datosdgai.cultura.gob.mx/pss/CcP03a.csv"
,"SNEDH-cce35260-0bd4-4920-a763-507dd42a2795" "http://datosdgai.cultura.gob.mx/pss/CcP03b.csv"
,"SNEDH-ac0fe470-5ef9-47ec-9930-ddf8a90f0bac" "https://datosabiertos.impi.gob.mx/Documents/patenteshab.csv"
,"SNEDH-f4423ed8-ccb6-4b0b-901b-55c0fb3ee10b" "http://datosdgai.cultura.gob.mx/pss/CcR02.csv"
,"SNEDH-72c77f52-fcb3-47d7-bd34-21aa0a814fbc" "https://www.inegi.org.mx/contenidos/datosgob/cdr04_crec_pob_leg_ind.csv"
,"SNEDH-dc6277fc-89b8-4eef-be4d-eedb864e72bf" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE02.csv"
,"SNEDH-288ea00b-2354-427b-a789-dffd0a00e320" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE05.csv"
,"SNEDH-eeb84545-c4ef-44b1-a6f6-fe387e5f321a" "http://amilcar.pm/user/pages/04.datos/13.Conacyt/Indicador-CfP02-2018.csv"
,"SNEDH-021973e0-7528-463b-88e5-9774834c457f" "http://amilcar.pm/user/pages/04.datos/13.Conacyt/Indicador-CfP03-2018.csv"
,"SNEDH-0dd60e20-97a1-4644-8302-dade0bba59d4" "http://amilcar.pm/user/pages/04.datos/10.amexcid/CfP04b%20Becas%20a%20extranjeros%20otorgadas%20por%20la%20AMEXCID.csv"
,"SNEDH-b948e80d-11fe-44bd-b98f-92ecd2bf4b74" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfP05.csv"
,"SNEDH-94a0cadf-23d2-40e6-b9d4-0f1cae339832" "https://www.inegi.org.mx/contenidos/datosgob/cfr01_bien_serv_cult.csv"
,"SNEDH-a3307fb8-a55d-4285-beb8-a049c6d6ac49" "http://amilcar.pm/user/pages/04.datos/13.Conacyt/Indicador-CfR02-2018.csv"
,"SNEDH-f2d8ee67-e8be-486d-b712-ccba33b4733a" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfR03.csv"
,"SNEDH-9631840f-3004-4575-9ff7-dac0bfdb7dfe" "http://datosdgai.cultura.gob.mx/pss/CiP01.csv"
,"SNEDH-bcd12f0a-108c-4b2b-8eee-7b866a2198b7" "http://datosdgai.cultura.gob.mx/pss/CiR02.csv"
,"SNEDH-282e9813-0280-49d9-9235-1752fd9623dc" "http://datosdgai.cultura.gob.mx/pss/CiR04.csv"
,"SNEDH-2f33e321-12eb-430a-9e7f-df3c01cfeb7c" "https://www.inegi.org.mx/contenidos/datosgob/cjp03a_hec_der_hum_cultu.csv"
,"SNEDH-07bb861e-563f-4a29-abc5-42ab46a977cf" "https://www.inegi.org.mx/contenidos/datosgob/cjp03b_vic_der_hum_cultu.csv"
,"SNEDH-inee-eap01a" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01a.csv"
,"SNEDH-inee-eap01b" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01b.csv"
,"SNEDH-inee-eap01c" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01c.csv"
,"SNEDH-inee-eap01d-l3sec" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01d_L3sec.csv"
,"SNEDH-inee-eap01d-l6prim" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01d_L6prim.csv"
,"SNEDH-inee-eap01d-m3sec" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01d_M3sec.csv"
,"SNEDH-inee-eap01d-m6prim" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP01d_M6prim.csv"
,"SNEDH-inee-eap02-12-14" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP02_12_14.csv"
,"SNEDH-inee-eap02-15-17" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP02_15_17.csv"
,"SNEDH-inee-eap02-18-24" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP02_18_24.csv"
,"SNEDH-inee-eap02-3-5" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP02_3_5.csv"
,"SNEDH-inee-eap02-6-11" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP02_6_11.csv"
,"SNEDH-inee-ear03-ems-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_EMS_Entidad.csv"
,"SNEDH-inee-ear03-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_Primaria_entidad.csv"
,"SNEDH-inee-ear03-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_Secundaria_entidad.csv"
,"SNEDH-inee-ear04-ems-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_EMS_Entidad.csv"
,"SNEDH-inee-ear04-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_Primaria_Entidad.csv"
,"SNEDH-inee-ear04-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_Secundaria_Entidad.csv"
,"SNEDH-inee-ear05-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR05_Primaria_Entidad.csv"
,"SNEDH-inee-ear05-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR05_Secundaria_Entidad.csv"
,"SNEDH-inee-ear06b-12a14-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_12a14_Entidad.csv"
,"SNEDH-inee-ear06b-15a17-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_15a17_Entidad.csv"
,"SNEDH-inee-ear06b-6a11-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_6a11_Entidad.csv"
,"SNEDH-3465f777-ecac-487f-a53c-85cc9c159e80" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcP01.csv"
,"SNEDH-ba4afebc-6260-44f1-b761-0986d176a274" "http://amilcar.pm/user/pages/04.datos/01.SEP/Ecp05.csv"
,"SNEDH-inee-ecr01" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EcR01.csv"
,"SNEDH-30d00676-d7b1-45d7-94f0-2a7393d18b98" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR02.csv"
,"SNEDH-d2dd3d77-b3db-4283-9c4c-e9f4496af232" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR04.csv"
,"SNEDH-46705461-b263-43bd-86cd-9b6eced61b02" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcR06_entidad.csv"
,"SNEDH-eee78257-4aec-4a6a-9b33-3e631529825a" "http://amilcar.pm/user/pages/04.datos/EdP01.csv"
,"SNEDH-97bc0fc0-a5bb-4964-b224-1a67431b29d5" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdP04.csv"
,"SNEDH-f4ff3572-15ba-427b-9c8e-7eb933b0662a" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdP05.csv"
,"SNEDH-de888cbb-9407-45ef-b3de-05aa7eed7091" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdP06.csv"
,"SNEDH-inee-edp07-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EdP07_primaria_entidad.csv"
,"SNEDH-inee-edp07-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EdP07_secundaria_entidad.csv"
,"SNEDH-3e118eb9-01b9-48d7-8497-5e041798bb92" "https://www.inegi.org.mx/contenidos/datosgob/edr02_paridad_genero_alfabet_juv.csv"
,"SNEDH-d5e43708-75e8-4738-8cc8-ee6f056e183a" "https://www.inegi.org.mx/contenidos/datosgob/edr03_pob_indigena_asiste_escuela.csv"
,"SNEDH-64497775-7595-48c3-80ed-8ec882349494" "https://www.inegi.org.mx/contenidos/datosgob/edr04a_pob_indigena_educ_primaria.csv"
,"SNEDH-905043b5-226d-46ed-951e-9865dae91a1a" "https://www.inegi.org.mx/contenidos/datosgob/edr04b_pob_indigena_edu_secundaria.csv"
,"SNEDH-7fb93fe9-e79e-4e78-b97e-8b92da0bf87f" "https://www.inegi.org.mx/contenidos/datosgob/edr04c_pob_indigena_edu_msuperior.csv"
,"SNEDH-45f9490f-82b8-4a66-bba1-2978f528cf5d" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdR05a.csv"
,"SNEDH-11b2aae4-dfc9-439f-9d40-ab2013bb0873" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdR05b.csv"
,"SNEDH-eac3fe8c-e49f-49db-9a6e-d7fec807e835" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdR05c.csv"
,"SNEDH-8262a2a3-780e-4bb6-837b-4f5c3d95f71a" "https://www.inegi.org.mx/contenidos/datosgob/edr06_pob_indigena_edu_superior.csv"
,"SNEDH-d2eebae9-3dcc-4e9b-bdd8-22ee9c3cf618" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/EfP01a.csv"
,"SNEDH-inee-efp01b" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfP01b.csv"
,"SNEDH-5d92cca7-ff0b-40c1-ac9b-cb5be5d178b8" "http://amilcar.pm/user/pages/04.datos/01.SEP/EfP05.csv"
,"SNEDH-inee-efp06" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfP06.csv"
,"SNEDH-8db13bd9-4351-4d59-a08a-c4224f065f15" "http://amilcar.pm/user/pages/04.datos/01.SEP/EfP07TotalBasica.csv"
,"SNEDH-inee-efr01a-preescolar-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_preescolar_entidad.csv"
,"SNEDH-inee-efr01a-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_primaria_entidad.csv"
,"SNEDH-inee-efr01a-telesecundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_telesecundaria_entidad.csv"
,"SNEDH-9ece2ca6-021e-4384-8f7b-6fab3cc9f126" "https://www.inegi.org.mx/contenidos/datosgob/efr02_gas_hog_educ.csv"
,"SNEDH-a53f7d9c-fa43-4edc-9684-ceacb78ec1a7" "http://amilcar.pm/user/pages/04.datos/01.SEP/EiP03.csv"
,"SNEDH-36562e6c-c62d-42e2-b2f6-9f73ae2c3937" "https://www.inegi.org.mx/contenidos/datosgob/ejp01a_hec_der_hum_educa.csv"
,"SNEDH-3bfda88c-0d70-4ac5-aca8-23bd2dac7912" "https://www.inegi.org.mx/contenidos/datosgob/ejp01b_vic_der_hum_educa.csv"
,"SNEDH-2e813296-2c9e-484a-b6dd-3680c896fe05" "https://www.inegi.org.mx/contenidos/datosgob/mar01_pob_acceso_agua.csv"
,"SNEDH-9909413e-a36c-4424-95a0-1654f8cef517" "https://www.inegi.org.mx/contenidos/datosgob/mar05_veg_nat_rem_eco.csv"
,"SNEDH-4c1cbbb8-7f0f-4acb-96d6-5d28fa8d3415" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/MaR09_porcentaje_de_poblacion_en_viviendas_sin_chimenea_que_usan_lena_o_carbon_para_cocinar_ef.csv"
,"SNEDH-20f59c90-e2b3-4c40-9f71-a4c50c9b7ddf" "http://amilcar.pm/user/pages/04.datos/14.INEGI/mar10_pob_acceso_gas.csv"
,"SNEDH-8eb08b2f-de3c-43ba-9781-02f62bbdc86e" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mortalidad_por_enfermedades_respiratorias_agudas_en_menores_de_5_anios.csv"
,"SNEDH-3125d4a6-1d4b-40ca-b6b7-f1e1eb012fa3" "https://www.inegi.org.mx/contenidos/datosgob/mar14_num_vehiculos_automotores.csv"
,"SNEDH-beeaa654-06c9-46ef-a4e9-c869ad5b2243" "https://www.inegi.org.mx/contenidos/datosgob/mcr01_agua_entubada_hogar.csv"
,"SNEDH-dfefce03-331a-4343-8a34-3326c702a38b" "https://www.inegi.org.mx/contenidos/datosgob/mcr02_energia_elect_hogar.csv"
,"SNEDH-55b53fab-7db8-4083-bf73-c42db40a252e" "https://www.inegi.org.mx/contenidos/datosgob/mcr03_recole_basura.csv"
,"SNEDH-14cc7874-212b-42ef-9fdf-c5eaf33ea981" "https://www.inegi.org.mx/contenidos/datosgob/mcr05_desechos_reciclados.csv"
,"SNEDH-00092f9b-a542-47ea-80b9-f07d8c755494" "https://www.inegi.org.mx/contenidos/datosgob/mcr07_eliminacion_excretas.csv"
,"SNEDH-e2a89f2e-a6fd-49ff-b8dc-c020b3eadcea" "https://www.inegi.org.mx/contenidos/datosgob/mdr02_saneamiento_mejorado.csv"
,"SNEDH-efc39b54-d960-405e-8e3c-5baa5f09bb17" "http://amilcar.pm/user/pages/04.datos/10.amexcid/MfE02%20Proyectos%20de%20la%20AMEXCID%20dedicados%20a%20la%20protecci%C3%B3n%20ambiental.csv"
,"SNEDH-4d2ed40d-cd53-4b4e-8d6d-57634c25842e" "http://amilcar.pm/user/pages/04.datos/16.SHCP/MfP01.csv"
,"SNEDH-c4928a41-2cfc-44ad-bbe9-0313379266ac" "https://www.inegi.org.mx/contenidos/datosgob/mfr01_agot_degr_amb.csv"
,"SNEDH-22649bec-ea62-4e4b-a3a2-f625ef0722af" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/TasadeFuncionariosEspecializadosDelitosAmbientales.csv"
,"SNEDH-dc1b1d6f-d6b8-4811-ae86-d88768c8129d" "https://www.inegi.org.mx/contenidos/datosgob/mjp08a_hec_der_hum_med_amb.csv"
,"SNEDH-2a0ccab2-b9b9-4b15-831f-948e8d620694" "https://www.inegi.org.mx/contenidos/datosgob/mjp08b_vic_der_hum_med_amb.csv"
,"SNEDH-885d31bc-da6a-429b-893b-0fd056c7ea8c" "https://www.scjn.gob.mx/sites/default/files/documentos/indicadores_cumplimiento_dh/MjR01_1.csv"
,"SNEDH-e039e75a-b73c-4da0-907b-d564397954c2" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/MjR02a.csv"
,"SNEDH-dd886422-3c96-49f6-ad31-f06e75e06736" "https://www.inegi.org.mx/contenidos/datosgob/sap01a_pob_afiliada_serv_salud.csv"
,"SNEDH-a4b4cc38-6754-4b3e-a31c-93710d30f1fd" "https://www.inegi.org.mx/contenidos/datosgob/sap01b_pob_afiliada_seg_popular.csv"
,"SNEDH-6297a81d-b10f-4105-bba3-3a970166964f" "https://www.inegi.org.mx/contenidos/datosgob/sap03a_pob_adulta_afiliada_serv_salud.csv"
,"SNEDH-fea39977-3942-4cf2-a591-929fd205a322" "https://www.inegi.org.mx/contenidos/datosgob/sap03b_pob_adulta_afiliada_seg_popular.csv"
,"SNEDH-e66e3c86-2cd2-4885-8685-112cce18e089" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Razon_de_mortalidad_materna.csv"
,"SNEDH-d8fddcad-9e76-4f53-b46f-a065b9c35ff7" "http://amilcar.pm/user/pages/04.datos/06.SALUD/01.DGIS-DEDSS/SaR03a%20Tasa_de_mortalidad_por_homicidios_por_entidad.csv"
,"SNEDH-3cf53f7c-bab9-40fc-8a28-82e1e7e2a844" "http://amilcar.pm/user/pages/04.datos/06.SALUD/01.DGIS-DEDSS/SaR03b%20Tasa_de_mortalidad_por_suicidios_por_entidad.csv"
,"SNEDH-e2db6607-0917-4e61-b6df-ff2aa9f8e734" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mortalidad_infantil.csv"
,"SNEDH-c7ddf55c-2d66-4cd2-95d0-c03207fd81ab" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mortalidad_de_menores_de_5_axos.csv"
,"SNEDH-69b9ffe1-6894-4b9a-8a9e-a1282433718a" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mxdicos_generales_por_mil_habitantes.csv"
,"SNEDH-10d5bdd0-5654-4df8-bd46-db8cd09429eb" "https://www.inegi.org.mx/contenidos/datosgob/ScPo4b_tasa_enfermeras.csv"
,"SNEDH-49173871-d212-48c3-bfbe-77dc49bc4d96" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Porcentaje_de_partos_antendidos_por_personal_calificado.csv"
,"SNEDH-bdde4055-cf15-41f6-ad22-82c0becdb51d" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/ScR01_Porcentaje_de_poblacion_con_carencia_por_acceso_a_los_servicios_de_salud_ef.csv"
,"SNEDH-4752d36e-ff00-496a-87e5-c02d95e9baf5" "https://www.inegi.org.mx/contenidos/datosgob/scr03_pob_serv_salud_tipo.csv"
,"SNEDH-8db34f2b-32a9-41c9-874b-d8b55c101e03" "https://www.inegi.org.mx/contenidos/datosgob/scr04_hog_gas_segsalud.csv"
,"SNEDH-49352768-1412-4a76-89e1-db064c0be675" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Prevalencia_de_uso_de_mxtodos_anticonceptivos_en_mujeres_de_15_a_24_axos.csv"
,"SNEDH-d713dd63-57f6-4da6-afaa-de3c71628897" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Prevalencia_de_uso_de_mxtodos_anticonceptivos_de_15_a_49_axos.csv"
,"SNEDH-84198174-fe0e-4d36-a720-cf982bf5d644" "https://www.inegi.org.mx/contenidos/datosgob/sdp03_hog_gas_salud_alternativo.csv"
,"SNEDH-3d778e8c-dd6f-4894-9583-6e35881f82bd" "http://amilcar.pm/user/pages/04.datos/17.prospera/Men_5a%C3%B1os_ctrl_nut_PROSPERA.CSV"
,"SNEDH-1252b421-f5d0-48de-a8a9-8cb3279a9681" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Nxmero_de_camas_de_psiquiatrxa_por_1x000_hab.csv"
,"SNEDH-22b63ca0-9694-4cc8-91da-792f26f5697d" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Nxmero_de_consultorios_de_psicologxa_por_1x000_hab.csv"
,"SNEDH-5060b80a-9b6a-4a42-9ec4-d2311ebab529" "http://amilcar.pm/user/pages/04.datos/06.SALUD/01.DGIS-DEDSS/SdP10c%20Nxmero_de_psicologos_por_1x000_hab.csv"
,"SNEDH-3cc05998-87e8-4602-9172-ec899f0dba00" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Nxmero_de_psiquiatras_por_1x000_hab.csv"
,"SNEDH-5e63faae-db7b-42f3-ada9-5d0077618f1d" "https://www.inegi.org.mx/contenidos/datosgob/sdr03_per_disc_acc_salud.csv"
,"SNEDH-0d8fb72f-c20a-4ada-ad88-c922ac22bff6" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mortalidad_por_cxncer_de_mama.csv"
,"SNEDH-4d75bae3-888a-4b00-8932-5c340080ee6e" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Tasa_de_mortalidad_por_cxncer_cxrvico_uterino.csv"
,"SNEDH-7310ee0c-ed42-4daf-bea9-faebf32f7108" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Porcentaje_de_embarazadas_atendidas_durante_el_primer_trimestre_de_embarazo.csv"
,"SNEDH-ba4f9aed-3e1c-46f0-8501-b7ab0466fece" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Porcentaje_de_cobertura_con_esquema_bxsico_completo_de_vacunacixn_en_nixos_menores_de_un_axo.csv"
,"SNEDH-8ff6acbb-4cce-405d-9316-41b092824f1a" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Porcentaje_del_gasto_publico_destinado_al_sector_salud_respecto_al_PIB_2.csv"
,"SNEDH-b447f18f-abc9-410d-8bda-ac47c1130c67" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Gasto_pxblico_en_salud_como_porcentaje_del_gasto_pxblico_total.csv"
,"SNEDH-5e6783af-f456-49cf-bb1c-76d7eb9ff3ae" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Gasto_pxblico_en_salud_per_cxpita_para_poblacixn_asegurada.csv"
,"SNEDH-37231dd0-00f7-4f5a-948f-e5337630221f" "http://www.dged.salud.gob.mx/contenidos/dedss/descargas/protocolo/Gasto_pxblico_en_salud_per_cxpita_para_poblacixn_no_asegurada.csv"
,"SNEDH-dce38849-0993-43d9-8e09-2fad1d78481b" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/SfP03b.csv"
,"SNEDH-ea12171e-60f1-42b4-af50-7fb63220fb5a" "https://www.inegi.org.mx/contenidos/datosgob/sfr01a_gasto_en_salud.csv"
,"SNEDH-e891dfe7-1e14-4433-913e-a606117f8d97" "https://www.inegi.org.mx/contenidos/datosgob/sjp01a_hec_der_hum_salud.csv"
,"SNEDH-191d8454-2ba0-4462-b68f-575988632654" "https://www.inegi.org.mx/contenidos/datosgob/sjp01b_vic_der_hum_salud.csv"
,"SNEDH-c3cd374a-9266-4e98-bbc2-cd5ee79c2f71" "http://www.bienestar.gob.mx/work/models/SEDESOL/Transparencia/Indicador_SSaP05.csv"
,"SNEDH-a1c8ea2b-8356-451d-b144-39f84d412253" "https://www.coneval.org.mx/Medicion/Documents/Datos_Abiertos/DESC_SNEDH/SScR01_Porcentaje_de_la_poblacion_con_carencia_por_acceso_a_la_seguridad_social_ef.csv"
,"SNEDH-12ea63ce-5c5e-452c-bd03-b55bd31c8892" "https://www.inegi.org.mx/contenidos/datosgob/ssdr03b_cobertura_seg_social.csv"
,"SNEDH-8502569f-dfb8-4c8f-9aa5-2ffb00b30bf8" "http://www.consar.gob.mx/gobmx/datosabiertos/PORCENTAJE_DE_POBLACION_ECONOMICAMENTE_ACTIVA_EN_AFORE.csv"
,"SNEDH-9bc18699-a817-410d-8f20-d1a6e984022a" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/SSfP01a.csv"
,"SNEDH-2337582d-d637-425b-bda7-e01fa7773e0b" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/SSfP01b.csv"
,"SNEDH-afe00db9-c657-4d9c-a202-406612557d80" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/SSfP01c.csv"
,"SNEDH-0ef53126-81ad-427f-8223-4861de004c2d" "https://www.inegi.org.mx/contenidos/datosgob/ssjp01a_hec_der_hum_seg_soc.csv"
,"SNEDH-8461c257-a601-48b9-a676-2afb630a746a" "https://www.inegi.org.mx/contenidos/datosgob/ssjp01b_vic_der_hum_seg_soc.csv"
,"SNEDH-4c603f0d-cf90-4a01-94d4-5d6579cccf24" "http://datosabiertos.stps.gob.mx/Datos/Casos_de_pensiones_tramitados.csv"
,"SNEDH-7595af52-eca8-4dff-a09d-29de2dbba66e" "http://datosabiertos.stps.gob.mx/Datos/Casos_promovidos_por_la_PROFEDET.csv"
,"SNEDH-4d6ecc15-5a87-4d74-994a-be99a662c793" "https://www.inegi.org.mx/contenidos/datosgob/tar01_tasa_ocup_infantil.csv"
,"SNEDH-3e002929-37ac-48ef-8a1b-a4f0966f8bdb" "https://www.inegi.org.mx/contenidos/datosgob/tar02a_tasa_desocupacion.csv"
,"SNEDH-16ca73f9-ad5b-4ffa-be83-51665c928e4a" "https://www.inegi.org.mx/contenidos/datosgob/tar03_trab_asala_ocup.csv"
,"SNEDH-35f286eb-5c3e-4db6-b491-c109f41c25d6" "https://www.inegi.org.mx/contenidos/datosgob/tar04_tasa_infor_lab.csv"
,"SNEDH-f082decb-06f4-43db-a2bd-1ae1680f3173" "https://www.inegi.org.mx/contenidos/datosgob/tar05a_trab_ingr_inf_sal_min.csv"
,"SNEDH-1aa7047c-44b7-4969-b068-d6643d33bb35" "https://www.inegi.org.mx/contenidos/datosgob/tar05b_tasa_cond_crit.csv"
,"SNEDH-d7300dc3-c421-4e4d-8eff-1ebe8c7ae1e2" "https://www.inegi.org.mx/contenidos/datosgob/tar06_muj_tot_asal_no_agro.csv"
,"SNEDH-5ffb6763-9279-4c27-95eb-96057d676a56" "https://www.inegi.org.mx/contenidos/datosgob/tar08b_muj_fun_pub.csv"
,"SNEDH-12c6f694-a632-4f35-9d30-cb9384383557" "https://www.inegi.org.mx/contenidos/datosgob/tar09_tasa_part_per_dis.csv"
,"SNEDH-Temporal-STPS-TcP03" "http://datosabiertos.stps.gob.mx/datos/CNDHProtocolo/TcP03Tasadeinspeccionlaboral.csv"
,"SNEDH-Temporal-STPS-TcR01" "http://amilcar.pm/user/pages/04.datos/04.STPS/SNETotalAteCol.csv"
,"SNEDH-5828c689-66bd-4c9e-9827-20d0336d11e0" "https://www.inegi.org.mx/contenidos/datosgob/tcr03_desocu_larga_dur.csv"
,"SNEDH-Temporal-STPS-TcR04" "http://datosabiertos.stps.gob.mx/datos/CNDHProtocolo/NodeContratosColectivosTcR04.csv"
,"SNEDH-fcaec197-d30a-407a-bbea-9009a73db43b" "https://www.inegi.org.mx/contenidos/datosgob/tcr05_pob_ocu.csv"
,"SNEDH-1f9b7912-9a15-4bd7-b40d-efc03aeb8402" "https://www.inegi.org.mx/contenidos/datosgob/tdp03_inst_salud_pob.csv"
,"SNEDH-754cd7b3-cbf9-4442-8c47-b3e77fffa3cd" "http://amilcar.pm/user/pages/04.datos/14.INEGI/tdr01a.csv"
,"SNEDH-82fae4df-9a30-4d18-bed8-5210b21637b3" "http://amilcar.pm/user/pages/04.datos/14.INEGI/tdr01b.csv"
,"SNEDH-c35b7eca-b967-4f4c-81c0-82a6842f93e9" "http://amilcar.pm/user/pages/04.datos/14.INEGI/tdr01c.csv"
,"SNEDH-da9a2b1e-38cb-402c-ad06-45bd4f4463d0" "http://amilcar.pm/user/pages/04.datos/14.INEGI/tdr01d.csv"
,"SNEDH-440cc94b-aa53-4bcb-9ed5-6695f0728696" "https://www.inegi.org.mx/contenidos/datosgob/tdr02_ing_lab_per_cap.csv"
,"SNEDH-23a9a0f8-743e-4a5a-b0eb-47e6e17ac267" "https://www.inegi.org.mx/contenidos/datosgob/tdr04_homb_ocup.csv"
,"SNEDH-cd6f6ffd-8088-46ba-8ecd-9d843be05bb8" "https://www.inegi.org.mx/contenidos/datosgob/tdr05_brecha_hom_muj.csv"
,"SNEDH-f23b9a9c-c1c5-4beb-a8fe-db54ff75745d" "https://www.inegi.org.mx/contenidos/datosgob/tfr01_masa_salarial_pib.csv"
,"SNEDH-a2ef3c71-57a9-46aa-a52f-0a661c3127a4" "https://www.inegi.org.mx/contenidos/datosgob/tip02_solic_anuales_inf_inegi.csv"
,"SNEDH-8003095d-fd07-419e-9c44-c78a36317047" "https://www.inegi.org.mx/contenidos/datosgob/tir01_usu_anuales_portal_inegi.csv"
,"SNEDH-f23a37f2-4c70-4ac1-988a-d6139ca3b91c" "http://amilcar.pm/user/pages/04.datos/04.STPS/01.JFCA/JFCA%20TjPO1a.csv"
,"SNEDH-584fb3eb-f19d-4f5d-81cc-63dd76f6d4c8" "http://amilcar.pm/user/pages/04.datos/04.STPS/01.JFCA/JFCA%20TjPO1b.csv"
,"SNEDH-7cdfad7f-a701-4bdf-84d9-5951c577707e" "https://www.inegi.org.mx/contenidos/datosgob/tjp04a_hec_der_hum_trab.csv"
,"SNEDH-f8dd0d1b-865c-401b-b4b4-94de024e1559" "https://www.inegi.org.mx/contenidos/datosgob/tjp04b_vic_der_hum_trab.csv"
,"SNEDH-18002f17-a69a-4286-85da-3d2217293749" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/ExplotacionSexualdeMenoresApCi.csv"
,"SNEDH-75ce45b4-c5c7-4bac-9025-d6dbc24792c6" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/HostigamientoSexualApCi.csv"
,"SNEDH-c186b6fc-6a03-4e71-aa4f-86eacb8ff9fe" "http://datosabiertospgr.blob.core.windows.net/sansalvador-pgr/PorcentajedeDeterminacionesTratadePersonas.csv"
,"SNEDH-b0bd7891-cc76-4974-b9fc-2f2d4d6a42a5" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Num_solicitudes_inscripcion_sindicatos_rechazadas_Ent_Fed_UaP09a2.csv"
,"SNEDH-e18f63d0-4bd8-40aa-b05c-dbd9562969c3" "http://amilcar.pm/user/pages/04.datos/04.STPS/UaP09b2.csv"
,"SNEDH-47d0eb92-4817-4233-845f-37b21d5a48d3" "https://www.inegi.org.mx/contenidos/datosgob/uar01_tasa_sindica.csv"
,"SNEDH-Temporal-STPS-UaR04" "http://datosabiertos.stps.gob.mx/datos/CNDHProtocolo/NodediasnolaboradosporhuelgaUaR04.csv"
,"SNEDH-f23ba25f-9e4f-4757-b360-ae3effdad70a" "http://amilcar.pm/user/pages/04.datos/04.STPS/01.JFCA/JFCA%20UcPO2a.csv"
,"SNEDH-c457398c-2671-4d43-9228-6129235f2423" "http://amilcar.pm/user/pages/04.datos/04.STPS/01.JFCA/JFCA%20UcPOb.csv"
,"SNEDH-8c4eb948-54ae-4e7e-9e11-4874b99e43ae" "https://www.inegi.org.mx/contenidos/datosgob/ucr02_sindicalizacion_ent_fed.csv"
,"SNEDH-92434c5e-514f-4f73-931a-a17b272e8534" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Registro_de_nuevos_sindicatos_anualmente_UcR04.csv"
,"SNEDH-Temporal-STPS-UcR05" "http://amilcar.pm/user/pages/04.datos/04.STPS/NodeProcesosdeNeg_ColectivasinemplazamientoUcR05.csv"
,"SNEDH-ad01e529-964c-461a-94dc-446f5b3e2b73" "http://amilcar.pm/user/pages/04.datos/14.INEGI/udr01_sindicalizacion_gpos_pob.csv"
,"SNEDH-13513000-1f17-4c5a-a2da-b7c91e3c182c" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_mujeres_en_la_dirigencia_sindical_UdR02.csv"
,"SNEDH-6403dbdf-e836-4aaa-a843-afc5efe60a6c" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/UfE01.csv"
,"SNEDH-098b3974-5ca0-4779-b426-a2347bb2740e" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Porcentaje_de_los_sindicatos_con_un_numero_de_afiliados_UfR01.csv"
,"SNEDH-ca47ffec-3524-4e1c-9c48-444ac1fc8e5a" "https://www.inegi.org.mx/contenidos/datosgob/ujp02a_hec_der_hum_sind.csv"
,"SNEDH-3e315307-b8c2-4d93-8fb4-57ee5809b4b7" "https://www.inegi.org.mx/contenidos/datosgob/ujp02b_vic_der_hum_sind.csv"
,"SNEDH-Temporal-SALUD-SaR07a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/SaR07a.csv"
,"SNEDH-Temporal-SALUD-SaR07b" "http://amilcar.pm/user/pages/04.datos/06.SALUD/SaR07b.csv"
,"SNEDH-Temporal-SALUD-SdR07a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/SdR07a.csv"
,"SNEDH-Temporal-CONAPO-SdR04-Entidad" "http://amilcar.pm/user/pages/04.datos/07.CONAPO/SdR04-Entidad.csv"
,"SNEDH-Temporal-IMSS-SScP01a" "http://amilcar.pm/user/pages/04.datos/08.IMSS/SScP01a%20Tasa%20Invalidez%202010-2018_.csv"
,"SNEDH-Temporal-IMSS-SScR03c" "http://amilcar.pm/user/pages/04.datos/08.IMSS/SScR03cTasa%20de%20defunciones%20por%20riesgos%20de%20trabajo%202010-2018_.csv"
,"SNEDH-Temporal-ISSSTE-SSaR04b" "http://amilcar.pm/user/pages/04.datos/09.ISSSTE/SSaR04b.csv"
,"SNEDH-Temporal-ISSSTE-SScR03d" "http://amilcar.pm/user/pages/04.datos/09.ISSSTE/SSdR03%20-%20por%20entidad.csv"
,"SNEDH-c29f98d1-970d-442a-b1a4-a37ee91ae3c8" "http://datosdgai.cultura.gob.mx/pss/CcR03a.csv"
,"SNEDH-595ec00c-4c70-41b5-8403-faf8ef0b52b4" "http://datosdgai.cultura.gob.mx/pss/CcR03b.csv"
,"SNEDH-54c1db65-e5d7-4378-93c9-316f84310a2f" "http://amilcar.pm/user/pages/04.datos/CiR01.csv"
,"SNEDH-inee-eap03-ems-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_ems_entidad.csv"
,"SNEDH-inee-eap03-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_primaria_entidad.csv"
,"SNEDH-inee-eap03-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_secundaria_entidad.csv"
,"SNEDH-inee-ear01-ems-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_ems_entidad.csv"
,"SNEDH-inee-ear01-preescolar-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_preescolar_entidad.csv"
,"SNEDH-inee-ear01-primaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_primaria_entidad.csv"
,"SNEDH-inee-ear01-secundaria-entidad" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_secundaria_entidad.csv"
,"SNEDH-b0919a90-92e3-4762-b37b-2e264f6beb65" "http://amilcar.pm/user/pages/04.datos/01.SEP/EfP07EntidadPreescolar.csv"
,"SNEDH-3d1bde64-5cfa-4618-ac91-9dc307409c34" "http://amilcar.pm/user/pages/04.datos/01.SEP/EfP07EntidadPrimaria.csv"
,"SNEDH-91b06917-bc9e-4c2d-a837-7f540f90a695" "http://www.conapo.gob.mx/work/models/CONAPO/Datos_Abiertos/Proyecciones2018/prot_san_sal_ev_proyecciones.csv"
,"SNEDH-af37f18e-bd0a-42c2-a2eb-b3f11e2f7773" "https://www.inegi.org.mx/contenidos/datosgob/tdr03_muj_ocup_prest_lab.csv"
,"SNEDH-72ed99f0-173d-43c9-b2b2-afe219076e20" "https://www.dgepj.cjf.gob.mx/DatosAbiertos/tje01.csv"
,"SNEDH-9b3194fa-4481-4456-97f8-26c0ea4183e9" "https://www.dgepj.cjf.gob.mx/DatosAbiertos/tje02.csv"
,"SNEDH-d3e99768-2fd7-4ae5-974c-803b64410a2a" "https://datosabiertos.stps.gob.mx/Datos/DGRA/Num_solicitudes_inscripcion_sindicatos_rechazadas_Ent_Fed_UaP09a2.csv"
,"SNEDH-df32595f-2295-4eef-b8ed-da8560a58cfc" "https://www.inegi.org.mx/contenidos/datosgob/tcr02_duracion_desocupacion.csv"
,"SNEDH-RECURSO-INSP-AaR07b" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR07b%20Prevalencia%20de%20desnutrici%C3%B3n%20global%20para%20ni%C3%B1os.csv"
,"SNEDH-RECURSO-INSP-AaR10a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR10a%20Prevalencia_de_sobrepeso_en_ni%C3%B1os_adolescentes_y_adultos.csv"
,"SNEDH-RECURSO-INSP-AaR10b-Total" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR10b%20Prevalencia_de_obesidad_en_ni%C3%B1os_adolescentes_y_adultos.csv"
,"SNEDH-1d99718a-5c52-4937-b12f-e2728ba76fbc" "http://amilcar.pm/user/pages/04.datos/02.Conapred/CdP05_2011_2018.csv"
,"SNEDH-577c3d02-a15e-4f9e-b165-6f4b01a96d9e" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfE03.csv"
,"SNEDH-137184e1-1135-4538-a361-67eabdf8362d" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/CfP01.csv"
,"SNEDH-badc715d-37b1-4b55-a3ae-87c9d5422db0" "http://amilcar.pm/user/pages/04.datos/01.SEP/EaP05b.csv"
,"SNEDH-inee-ear03-ems-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_EMS_Tipo_Sexo.csv"
,"SNEDH-inee-ear03-primaria-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_Primaria_Tipo_Sexo.csv"
,"SNEDH-inee-ear03-secundaria-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR03_Secundaria_tipo_sexo.csv"
,"SNEDH-inee-ear04-ems-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_EMS_Tipo_Sexo.csv"
,"SNEDH-inee-ear04-primaria-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_Primaria_Tipo_Sexo.csv"
,"SNEDH-inee-ear04-secundaria-tipo-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR04_Secundaria_Tipo_Sexo.csv"
,"SNEDH-29f65f01-b187-42e0-91a1-ccd2d33b96fa" "http://amilcar.pm/user/pages/04.datos/01.SEP/EdR01.csv"
,"SNEDH-inee-efp04" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfP04.csv"
,"SNEDH-e6c8ebba-0402-45c0-b555-077770e4b7ab" "http://amilcar.pm/user/pages/04.datos/01.SEP/EfP07TotalPorNivel.csv"
,"SNEDH-inee-efr01a-preescolar-tipo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_preescolar_tipo.csv"
,"SNEDH-inee-efr01a-primaria-tipo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_primaria_tipo.csv"
,"SNEDH-7b1698eb-331d-4e51-900d-c2ac01d410dd" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/MfE01.csv"
,"SNEDH-942070a4-4e15-405c-8660-6843ecbf56d8" "http://amilcar.pm/user/pages/04.datos/02.Conapred/TdP01_2011_2018.csv"
,"SNEDH-c1309f1b-7fdd-441c-91b8-8724d90ea480" "http://amilcar.pm/user/pages/04.datos/16.SHCP/UfP01.csv"
,"SNEDH-Temporal-CONAPO-SdR04-Grupos" "http://amilcar.pm/user/pages/04.datos/07.CONAPO/SdR04-Grupo.csv"
,"SNEDH-RECURSO-INSP-AaR07a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR07a%20Prevalencia_de_desnutrici%C3%B3n_global_para_adultos_Bajo_peso.csv"
,"SNEDH-RECURSO-INSP-AaR10b-Localidad" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR10b%20Prevalencia_de_obesidad_por_tipo_localidad.csv"
,"SNEDH-inee-eap03-ems-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EAP03_ems_sexo.csv"
,"SNEDH-inee-eap03-primaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_primaria_sexo.csv"
,"SNEDH-inee-eap03-secundaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_secundaria_sexo.csv"
,"SNEDH-inee-ear01-ems-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_ems_sexo.csv"
,"SNEDH-inee-ear01-preescolar-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_preescolar_sexo.csv"
,"SNEDH-inee-ear01-primaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_primaria_sexo.csv"
,"SNEDH-inee-ear01-secundaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_secundaria_sexo.csv"
,"SNEDH-inee-ear05-primaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR05_Primaria_Sexo.csv"
,"SNEDH-inee-ear05-secundaria-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR05_Secundaria_Sexo.csv"
,"SNEDH-inee-ear06b-12a14-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_12a14_Sexo.csv"
,"SNEDH-inee-ear06b-15a17-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_15a17_Sexo.csv"
,"SNEDH-inee-ear06b-6a11-sexo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR06b_6a11_Sexo.csv"
,"SNEDH-inee-efr01a-telesecundaria-tipo" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EfR01a_telesecundaria_tipo.csv"
,"SNEDH-56e1c10f-cb92-4b43-9471-06d5450dfce5" "http://www.conapo.gob.mx/work/models/CONAPO/Datos_Abiertos/Proyecciones2018/prot_san_sal_ev_sex_proyecciones.csv"
,"SNEDH-3cf53f7c-bab9-40fc-8a28-82e1e7e2sexo" "http://amilcar.pm/user/pages/04.datos/06.SALUD/01.DGIS-DEDSS/SaR03b%20Tasa_de_mortalidad_por_suicidios_por_sexo.csv"
,"SNEDH-Temporal-SALUD-SjP01d" "http://amilcar.pm/user/pages/04.datos/06.SALUD/SjP01d.csv"
,"SNEDH-69adc323-ec49-4008-bc88-d9ccff0faccd" "https://www.inegi.org.mx/contenidos/datosgob/cdr02_var_hogar_gast_nec_bas.csv"
,"SNEDH-f2b343b1-c6fd-4364-943b-a691debf0d30" "https://www.inegi.org.mx/contenidos/datosgob/cfr04_gasto_hog_art_esp.csv"
,"SNEDH-inee-eap03-ems-sostenimiento" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_ems_sostenimiento.csv"
,"SNEDH-inee-eap03-nacional-grado" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_nacional_grado.csv"
,"SNEDH-inee-eap03-primaria-tip-serv" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_primaria_tip_serv.csv"
,"SNEDH-inee-eap03-secundaria-tip-serv" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaP03_secundaria_tip_serv.csv"
,"SNEDH-inee-ear01-nacional-nivel" "https://www.inee.edu.mx/wp-content/uploads/2019/06/EaR01_nacional_nivel.csv"
,"SNEDH-Temporal-IMSS-SSiP02-Actividad" "http://amilcar.pm/user/pages/04.datos/08.IMSS/SSiP02_ActEc.csv"
,"SNEDH-7b2ae874-9260-4526-8a88-a3bbcdbb8943" "http://amilcar.pm/user/pages/04.datos/13.Conacyt/Indicador-EcR03-2018.csv"
,"SNEDH-99cf58be-c0c6-40eb-8afa-e08286b29adc" "http://amilcar.pm/user/pages/04.datos/11.sagarpa/AcP01.csv"
,"SNEDH-fe6af64e-e908-4553-a771-fca36172287d" "http://amilcar.pm/user/pages/04.datos/14.INEGI/car14a_tasa_crec_pob_ind.csv"
,"SNEDH-a237389b-c144-49bb-8616-04f85195263e" "http://archivos.diputados.gob.mx/adela/CcE04.csv"
,"SNEDH-a569461e-6948-4d2a-b907-232cb84453ea" "http://amilcar.pm/user/pages/04.datos/05.CDI/CdR06.csv"
,"SNEDH-fc1949a8-a9fd-47bb-8e9b-7d1816d3d76b" "https://www.dgepj.cjf.gob.mx/DatosAbiertos/CjR01.csv"
,"SNEDH-ebd33bb7-1e00-42a3-a130-6c375cc34a55" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Porcentaje_de_la_superficie_cubierta_por_bosques_y_selvas.csv"
,"SNEDH-001f6b0f-bb9e-4b81-956d-ceab7f87b13c" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Porcentaje_de_areas_afectadas_por_degradacion_ambiental.csv"
,"SNEDH-330f622e-bb02-472f-94f9-4c59910427cc" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Emisiones_de_dioxido_de_carbono_per_capita.csv"
,"SNEDH-4a87a2a8-d845-412c-b04d-e4af5f56ca9d" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Consumo_de_clorofluorocarburos_que_agotan_la_capa_de_ozono.csv"
,"SNEDH-45647f1e-3789-4231-be3f-b946908da1be" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/Emisiones_de_Gases_Efecto_Invernadero_(GEI).csv"
,"SNEDH-66fc7949-0be1-4c9c-8843-39451d7dda84" "http://dsiappsdev.semarnat.gob.mx/datos/SNEDH/Concentracion_promedio_de_particulas_PM10.csv"
,"SNEDH-5d21cce6-cd3d-4735-9752-c6e13ff6d0a1" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/Generacion_de_residuos_solidos_per_capita.csv"
,"SNEDH-48d6069b-d30b-41e4-9e07-8c4d20a0d4c3" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/Generacion_de_residuos_peligrosos_por_empresas.csv"
,"SNEDH-ef303077-3172-4bb8-a6bc-5f1ffb1fb723" "https://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndProdEneFueRenAltProtSanSalvador-MFR02.csv"
,"SNEDH-ecbcf0ca-00dd-4057-8639-9f77e2ad47a7" "https://base.energia.gob.mx/dgaic/DA/P/DGPlaneacionInformacionEnergeticas/BalanceNacionalEnergia/SENER_05_IndConEneRenProtSanSalvador-MFR03.csv"
,"SNEDH-31989aa7-2072-4b2c-9571-e24f239aaab4" "http://amilcar.pm/user/pages/04.datos/15.SEGOB/DH_Expedientes_SS.csv"
,"SNEDH-75fcb0a5-5590-43f8-a276-6015135348ac" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE01.csv"
,"SNEDH-bb9fad87-8c02-4f4a-8d38-98b30162c7dd" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfE02.csv"
,"SNEDH-6ae98a5f-7924-4c63-9cf3-312621674270" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP01.csv"
,"SNEDH-bc210836-cb91-4a33-969a-5a4f382f8149" "https://www.secciones.hacienda.gob.mx/work/models/asuntos_internacionales/datos_abiertos/TfP02.csv"
,"SNEDH-8f1ffc7e-9098-415f-be80-59c655dacd18" "http://datosabiertos.stps.gob.mx/Datos/Porcentaje_conciliaciones_resoluci%C3%B3n_favorable.csv"
,"SNEDH-d5c4f74b-18de-4b67-8c06-333dddbb38de" "http://datosabiertos.stps.gob.mx/Datos/Porcentaje_juicios_promovidos_favorables.csv"
,"SNEDH-04b6b55c-b6be-4023-84e7-d405d8e98cac" "http://amilcar.pm/user/pages/04.datos/02.Conapred/TjR03b_2011_2018.csv"
,"SNEDH-RECURSO-INSP-AaR08" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR08%20Porcentaje_de_mujeres_embarazadas_con_anemia_nutricional.csv"
,"SNEDH-RECURSO-INSP-AaR09" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR09%20Porcentaje%20de%20ni%C3%B1os%20de%2012%20a%2059%20meses%20con%20anemia%20nutricional.csv"
,"SNEDH-RECURSO-INSP-AcR03a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AcR03a%20Porcentaje_de_hogares_cubiertos_por_un_programa_p%C3%BAblico_de_ayuda_alimentaria.csv"
,"SNEDH-97de02a0-611c-4be9-b7da-cbfe9407da7e" "https://www.inegi.org.mx/contenidos/datosgob/ccp05_trab_pub_sec_cult.csv"
,"SNEDH-Temporal-SALUD-SjP01c" "http://amilcar.pm/user/pages/04.datos/06.SALUD/SjP01c.csv"
,"SNEDH-93096006-604e-47a1-8a37-5ad56310433a" "http://amilcar.pm/user/pages/04.datos/06.SALUD/02.Conadic/SiP03%20atencion%20preventiva%20contra%20el%20consumo%20de%20drogas.csv"
,"SNEDH-85c7a03a-66c7-409a-8303-fef9b3ffcc54" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/ScP02%20Porcentaje%20de%20la%20poblaci%C3%B3n%20con%20acceso%20a%20medicamentos_2014.csv"
,"SNEDH-7035fb31-eef3-49bf-b4fa-2698e67ad7e3" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/SdP04b%20Gasto%20en%20salud%20sexual%20y%20reprod.csv"
,"SNEDH-d7e3a58d-7fda-4d22-9dbd-af67b56273ea" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/SdP09a%20Porcentaje%20infantes%20menores%206%20meses.csv"
,"SNEDH-105d3fd1-e5c9-43a7-8309-2e45285ed136" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/SdP09b%20Porcentaje%20de%20infantes%20de%200%20a%206%20meses.csv"
,"SNEDH-6c6f46ec-6c3c-4640-8b20-43582fe0f637" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR11%20Prevalencia_de_diabetes_en_adolescentes_y_adultos.csv"
,"SNEDH-bae52ade-9285-4ac3-a853-0ce9e715e664" "http://amilcar.pm/user/pages/04.datos/06.SALUD/insp/AaR12%20Prevalencia_de_hipertensi%C3%B3n_arterial_en_adolescentes_y_adultos_por_dia.csv"
,"SNEDH-Temporal-SEP-EcP02-Entidad" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcP02-BE_Entidades.csv"
,"SNEDH-Temporal-SEP-EcP02-Nivel" "http://amilcar.pm/user/pages/04.datos/01.SEP/EcP02-BE_Nivel.csv"
,"SNEDH-Temporal-SEMARNAT-Mdp01a-entidad" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MdP01a_entidad.csv"
,"SNEDH-Temporal-SEMARNAT-Mdp01a-tipo-actividad" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MdP01a_tipo_act.csv"
,"SNEDH-Temporal-SEMARNAT-Mdp01a-tipo-est" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MdP01a_tipo_est.csv"
,"SNEDH-Temporal-SEMARNAT-Mje03b" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MjE03b.csv"
,"SNEDH-Temporal-SEMARNAT-MjP02-entidad" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MjP02_entidad.csv"
,"SNEDH-Temporal-SEMARNAT-MjP02-materia" "http://amilcar.pm/user/pages/04.datos/12.SEMARNAT/MjP02_materia.csv"
,"SNEDH-Temporal-IMSS-TaR07" "http://amilcar.pm/user/pages/04.datos/08.IMSS/TaR07.csv"
,"SNEDH-Coneval-EaR06a-Entidad" "https://www.coneval.org.mx/Informes/Pobreza/Datos_abiertos/ODS/EaR06a_Porcentaje_de_la_poblacion_con_carencia_por_rezago_educativo_ef.csv"
,"SNEDH-Coneval-EaR06a-Grupos" "https://www.coneval.org.mx/Informes/Pobreza/Datos_abiertos/ODS/EaR06a_Porcentaje_de_la_poblacion_con_carencia_por_rezago_educativo_gpos.csv"})

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
