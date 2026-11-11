# TerraScope

## Especificación general del producto, requisitos y plan de desarrollo

**Versión:** 1.0\
**Estado:** Documento base de definición y planificación\
**Plataforma:** Android + Wear OS\
**Área piloto:** Somerton, Arizona, Estados Unidos\
**Tipo de producto:** Plataforma móvil de inteligencia territorial e
inmobiliaria\
**Idioma inicial:** Español técnico en documentación; interfaz preparada
para internacionalización\
**Audiencia:** Equipo de desarrollo, docentes, colaboradores técnicos y
asistentes de IA

------------------------------------------------------------------------

# 1. Resumen ejecutivo

**TerraScope** es una plataforma de inteligencia territorial e
inmobiliaria que ayuda a inversionistas, desarrolladores y emprendedores
a explorar zonas, comparar ubicaciones y evaluar oportunidades
inmobiliarias o comerciales utilizando información geográfica,
demográfica, económica y territorial.

El producto no pretende ser otro catálogo de propiedades ni reemplazar a
plataformas como Zillow. Su objetivo es analizar el contexto de una
ubicación y ayudar a responder preguntas como:

-   ¿Qué zonas de Somerton podrían ser interesantes para abrir un
    negocio?
-   ¿Qué áreas presentan características favorables para desarrollo
    residencial?
-   ¿Cómo se compara una zona con otra?
-   ¿Qué factores favorecen o perjudican una ubicación?
-   ¿Qué información está disponible, de qué fuente proviene y qué tan
    reciente es?
-   ¿Qué aspectos deben verificarse físicamente antes de tomar una
    decisión?

La aplicación Android será el centro principal de análisis. La
aplicación Wear OS será un acompañante de campo para recorridos físicos:
mostrará información contextual, alertas de proximidad y permitirá
registrar observaciones durante la exploración.

El sistema contará con un backend y una base de datos geoespacial. Los
cálculos y análisis deberán estar respaldados por datos trazables. La
inteligencia artificial podrá ayudar a explicar resultados y redactar
reportes, pero no deberá inventar datos ni sustituir cálculos
verificables.

**Principio central del producto:** TerraScope debe explicar por qué una
ubicación puede ser interesante, qué evidencia respalda esa conclusión,
qué riesgos existen y qué información falta.

------------------------------------------------------------------------

# 2. Visión, objetivos y propuesta de valor

## 2.1 Visión

Construir una herramienta profesional de inteligencia territorial que
transforme datos geográficos, demográficos, económicos y comerciales en
información útil para evaluar ubicaciones y oportunidades de inversión.

## 2.2 Objetivos del producto

1.  Centralizar información relevante de una zona geográfica.
2.  Visualizar indicadores mediante mapas y capas temáticas.
3.  Comparar zonas con criterios consistentes.
4.  Calcular indicadores de oportunidad configurables según el objetivo
    de análisis.
5.  Evaluar propiedades o terrenos concretos sin confundir su valor con
    el potencial de la zona.
6.  Acompañar recorridos físicos mediante Wear OS.
7.  Permitir guardar ubicaciones, observaciones y oportunidades.
8.  Generar reportes claros, trazables y comprensibles.
9.  Diseñar una arquitectura que pueda extenderse posteriormente a otras
    ciudades y regiones.
10. Construir un producto demostrable y mantenible, adecuado para un
    portafolio profesional.

## 2.3 Propuesta de valor

TerraScope no debe limitarse a mostrar puntos en un mapa. Debe
relacionar los datos disponibles, explicar los factores relevantes y
mostrar sus limitaciones.

Ejemplo de resultado esperado:

> Esta zona presenta condiciones potencialmente favorables para cierto
> tipo de inversión debido a su población cercana, accesibilidad y baja
> concentración de competidores identificados. Sin embargo, la
> información de precios de suelo es limitada y se recomienda verificar
> zonificación, disponibilidad de servicios y condiciones del inmueble.

Este ejemplo es ilustrativo. La aplicación solo podrá generar
conclusiones reales cuando disponga de fuentes y datos verificables.

## 2.4 Usuarios objetivo

-   Inversionistas inmobiliarios.
-   Emprendedores que buscan ubicaciones para nuevos negocios.
-   Desarrolladores de proyectos residenciales o comerciales.
-   Profesionales que realizan análisis preliminares de terrenos y
    zonas.
-   Usuarios que necesitan organizar recorridos y observaciones de
    campo.

La primera versión será un producto piloto centrado en Somerton,
Arizona. La arquitectura deberá evitar dependencias innecesarias de una
sola ciudad.

------------------------------------------------------------------------

# 3. Alcance del producto

## 3.1 Plataformas incluidas

  -----------------------------------------------------------------------
  Componente                          Propósito
  ----------------------------------- -----------------------------------
  Aplicación Android                  Experiencia principal de análisis,
                                      mapas, comparación, propiedades y
                                      reportes

  Aplicación Wear OS                  Asistente de exploración física y
                                      consulta rápida de información

  Backend REST                        Integración de fuentes,
                                      procesamiento, autenticación,
                                      análisis y sincronización

  Base de datos                       Persistencia relacional y
                                      almacenamiento de información
                                      geoespacial
  -----------------------------------------------------------------------

## 3.2 Fuera del alcance inicial

No se debe desarrollar en el MVP:

-   Aplicación web para usuarios finales.
-   Aplicación de escritorio.
-   Aplicación para televisores.
-   Marketplace o portal de compraventa de inmuebles.
-   Publicación de anuncios inmobiliarios.
-   Procesamiento de pagos.
-   Firma de contratos.
-   Tasaciones oficiales.
-   Predicción garantizada de rentabilidad.
-   Recomendaciones financieras personalizadas.
-   Rastreo GPS permanente en segundo plano.
-   Red social o mensajería entre inversionistas.

El backend sí tendrá endpoints REST y herramientas internas de
administración técnica cuando sean necesarias, pero eso no implica
construir un frontend web para clientes.

## 3.3 Delimitación geográfica

**Primera etapa geográfica:** Somerton, Arizona.

La arquitectura debe permitir incorporar después otras ciudades del
condado de Yuma y, eventualmente, otras regiones. No se debe ampliar el
alcance geográfico hasta que la experiencia piloto funcione
correctamente.

------------------------------------------------------------------------

# 4. Perfiles de inversión y lógica de análisis

Una zona no es buena o mala de manera universal. Su conveniencia depende
del objetivo del usuario.

TerraScope deberá separar al menos estos perfiles:

1.  **Inversión comercial:** identificar ubicaciones potenciales para
    negocios.
2.  **Desarrollo residencial:** estudiar características territoriales y
    demográficas relevantes para proyectos de vivienda.
3.  **Propiedades para renta:** analizar indicadores de contexto y datos
    de mercado disponibles.
4.  **Compra de terrenos:** estudiar ubicación, accesibilidad,
    zonificación y características conocidas del terreno.

Los criterios, variables y ponderaciones deberán ser diferentes según el
perfil. No se deberá reutilizar una puntuación única para todos los
casos.

## 4.1 Opportunity Score

El Opportunity Score será un indicador interno de apoyo a la
exploración, expresado de 0 a 100 cuando existan datos suficientes.

Requisitos del indicador:

-   Tener una configuración distinta por perfil de inversión.
-   Mostrar los factores que influyeron en el resultado.
-   Mostrar ponderaciones y metodología.
-   Identificar los datos faltantes.
-   Mostrar fuente y fecha de actualización de cada indicador.
-   Mostrar un nivel de confianza o calidad de datos separado del score.
-   Evitar resultados aparentemente precisos cuando la información sea
    insuficiente.
-   Permitir revisar los factores positivos y negativos.
-   Mantener un historial de la versión de la metodología usada.

**No se debe presentar el Opportunity Score como probabilidad de éxito,
rendimiento esperado ni garantía de rentabilidad.** Debe describirse
como una herramienta de análisis preliminar.

## 4.2 Separación entre zona y propiedad

El sistema debe distinguir dos evaluaciones:

-   **Inteligencia de zona:** estudia el contexto territorial,
    demográfico, comercial y de accesibilidad.
-   **Evaluación de propiedad:** estudia un inmueble o terreno
    específico, sus características, precio solicitado, superficie,
    condiciones conocidas y relación con el contexto de la zona.

Una zona atractiva no convierte automáticamente a cualquier propiedad
dentro de ella en una buena compra. La evaluación de un inmueble debe
señalar los datos que todavía necesitan validación.

------------------------------------------------------------------------

# 5. Aplicación Android: módulos y funcionalidades

La aplicación Android será el producto principal y tendrá una interfaz
profesional, consistente y centrada en mapas y análisis.

## 5.1 Autenticación y cuenta

Funciones:

-   Registro de usuario.
-   Inicio y cierre de sesión.
-   Recuperación de acceso, si se implementa un proveedor compatible.
-   Gestión básica del perfil.
-   Persistencia segura de la sesión.
-   Cierre de sesión en dispositivos.
-   Sincronización de datos asociados a la cuenta.

No almacenar contraseñas en texto plano. La autenticación deberá
resolverse en el backend o mediante un proveedor de identidad
correctamente configurado.

## 5.2 Dashboard

Debe mostrar un resumen útil, no una pantalla llena de métricas
decorativas.

Elementos posibles:

-   Resumen de zonas guardadas.
-   Oportunidades consultadas recientemente.
-   Indicadores destacados de Somerton.
-   Acceso rápido al mapa.
-   Actividad reciente.
-   Alertas relevantes.
-   Estado y fecha de actualización de datos.

El dashboard debe adaptarse a la disponibilidad real de datos. No
mostrar cifras ficticias como si fueran reales.

## 5.3 Investment Map

Será una de las funciones centrales del producto.

Funciones:

-   Mostrar el mapa de Somerton.
-   Permitir desplazamiento, zoom y selección de ubicaciones.
-   Mostrar zonas y puntos de interés disponibles.
-   Aplicar filtros por perfil de inversión.
-   Activar y desactivar capas temáticas.
-   Mostrar leyendas comprensibles.
-   Consultar detalles de una zona.
-   Mostrar la procedencia y fecha de actualización de los datos.
-   Diferenciar visualmente datos confirmados, estimaciones y datos
    aportados por usuarios.
-   Gestionar estados de carga, error y ausencia de información.

Capas posibles, sujetas a disponibilidad y licencia:

-   Densidad de población.
-   Indicadores demográficos.
-   Ingreso familiar mediano.
-   Hogares.
-   Negocios y puntos de interés.
-   Competidores por categoría.
-   Vías y accesibilidad.
-   Zonificación y uso de suelo, si existen datos oficiales utilizables.
-   Infraestructura y servicios, si la fuente permite su uso.

No todas las capas estarán disponibles desde el inicio. Cada una deberá
tener una fuente identificada y una definición geográfica clara.

## 5.4 Area Intelligence

Al seleccionar una zona, el usuario podrá consultar una ficha de
inteligencia territorial.

Indicadores candidatos:

-   Población.
-   Densidad poblacional.
-   Número de hogares.
-   Ingreso familiar mediano.
-   Composición y características demográficas agregadas.
-   Actividad comercial identificada.
-   Competidores cercanos.
-   Accesibilidad vial.
-   Distancias relevantes.
-   Uso de suelo o zonificación, cuando exista información oficial.
-   Infraestructura disponible, cuando la fuente sea confiable.

Cada indicador deberá incluir:

-   Valor.
-   Unidad de medida.
-   Área geográfica a la que corresponde.
-   Fuente.
-   Fecha o periodo de referencia.
-   Fecha de consulta o actualización del sistema.
-   Tipo de dato: oficial, estimación, derivado o aportado por usuario.
-   Advertencias metodológicas cuando sean necesarias.

No se debe representar un dato censal de un área grande como si
describiera con precisión una calle o un terreno individual.

## 5.5 Opportunity Finder

Permitirá encontrar zonas que coincidan con un perfil de análisis.

Funciones:

-   Seleccionar el objetivo de inversión.
-   Definir filtros disponibles.
-   Consultar zonas candidatas.
-   Ordenar resultados por score o por indicadores concretos.
-   Mostrar explicación del resultado.
-   Mostrar riesgos, limitaciones y datos faltantes.
-   Abrir la zona en el mapa.
-   Guardar una zona en la lista de seguimiento.

La búsqueda debe permitir que el usuario entienda por qué una zona
aparece en los resultados. No deberá mostrar una clasificación opaca.

## 5.6 Property Explorer

Permitirá registrar y evaluar propiedades o terrenos de interés.

Campos iniciales:

-   Nombre o identificador.
-   Ubicación geográfica.
-   Dirección, si se conoce.
-   Tipo de propiedad.
-   Superficie y unidad.
-   Precio de compra o precio solicitado.
-   Precio de renta, si aplica.
-   Descripción y notas.
-   Fotografías, si se habilita almacenamiento de imágenes.
-   Fuente del dato o forma en que se obtuvo.
-   Fecha de registro.
-   Estado de revisión.

Funciones:

-   Crear, consultar, editar y eliminar registros propios.
-   Asociar una propiedad con una zona.
-   Consultar indicadores contextuales de esa zona.
-   Comparar propiedades registradas.
-   Marcar datos pendientes de verificación.
-   Mantener claro qué datos fueron introducidos por el usuario y cuáles
    provienen de fuentes externas.

La primera versión no debe afirmar que calcula una tasación oficial.

## 5.7 Comparison

Permitirá comparar dos o más zonas o propiedades, según el tipo de
comparación.

Debe:

-   Comparar únicamente indicadores equivalentes.
-   Mostrar unidades y periodos.
-   Advertir cuando los datos tengan diferente antigüedad o
    granularidad.
-   Mostrar diferencias de forma visual y textual.
-   Evitar comparar valores incompatibles o incompletos como si fueran
    equivalentes.

## 5.8 Watchlist

Funciones:

-   Guardar zonas y propiedades favoritas.
-   Agregar etiquetas y notas.
-   Consultar elementos guardados.
-   Recibir alertas cuando exista un cambio relevante y verificable.
-   Sincronizar la lista entre Android y Wear OS cuando corresponda.

No generar alertas de cambios si la fuente no permite detectar o
confirmar dichos cambios.

## 5.9 Reports

Generación de reportes de análisis para una zona o propiedad.

Contenido esperado:

-   Identificación de la ubicación.
-   Perfil de inversión analizado.
-   Resumen de indicadores.
-   Factores favorables.
-   Factores desfavorables y riesgos.
-   Datos faltantes.
-   Opportunity Score, si aplica.
-   Nivel de confianza/calidad de datos.
-   Fuentes y fechas.
-   Metodología o versión del análisis.
-   Aviso de que el análisis es orientativo y no garantiza resultados.

El formato inicial podrá ser una vista de reporte dentro de Android. La
exportación a PDF puede incorporarse cuando la generación de datos y el
diseño del reporte sean estables.

## 5.10 Preferencias

-   Unidades de medida.
-   Idioma, cuando se implemente internacionalización.
-   Preferencias de notificación.
-   Preferencias de privacidad y ubicación.
-   Configuración de exploración.
-   Gestión de sesión.

------------------------------------------------------------------------

# 6. Aplicación Wear OS: Investor Scout

La aplicación del reloj tendrá un propósito específico: asistir al
usuario mientras recorre físicamente una zona.

No será una copia reducida de Android. Su interfaz debe ser breve,
contextual, legible y optimizada para pantalla pequeña.

## 6.1 Zone Alerts

-   Detectar proximidad a zonas guardadas cuando los permisos y las
    condiciones del dispositivo lo permitan.
-   Notificar al usuario al entrar en una zona relevante.
-   Mostrar nombre o identificador de la zona.
-   Mostrar Opportunity Score disponible.
-   Mostrar uno o dos indicadores clave.
-   Permitir abrir un resumen rápido.
-   Respetar las preferencias de notificación y frecuencia.

La geocerca no debe depender de rastreo GPS continuo. Se deberán evaluar
geofencing del sistema, ubicación del teléfono y ubicación del reloj
según el escenario y las capacidades disponibles.

## 6.2 Field Explorer

Modo para realizar recorridos.

Funciones:

-   Iniciar y finalizar una sesión de exploración.
-   Registrar ubicaciones durante el recorrido.
-   Guardar un punto de interés.
-   Añadir una nota breve o dictada por voz, si el dispositivo y los
    permisos lo permiten.
-   Añadir etiquetas predefinidas.
-   Sincronizar registros con Android.
-   Mostrar estado de sincronización.

Etiquetas iniciales posibles:

-   Terreno potencial.
-   Local comercial.
-   Construcción.
-   Competidor.
-   Infraestructura.
-   Acceso vial.
-   Riesgo observado.
-   Requiere investigación.

## 6.3 Quick Evaluation

Al consultar una zona:

-   Nombre o referencia.
-   Score disponible.
-   Indicadores clave.
-   Resumen de factores favorables.
-   Resumen de riesgos.
-   Fecha de actualización.
-   Estado de conexión o disponibilidad de datos.

No mostrar grandes tablas ni intentar reproducir el dashboard del
teléfono.

## 6.4 Watchlist

-   Consultar zonas guardadas.
-   Abrir una ficha rápida.
-   Recibir alertas configuradas.
-   Marcar una ubicación como visitada, si se implementa ese flujo.

## 6.5 Exploration Mode

-   Mostrar estado de la sesión.
-   Registrar puntos.
-   Consultar un resumen del recorrido.
-   Finalizar la sesión.
-   Sincronizar los datos pendientes.

La aplicación debe funcionar de forma tolerante a conexiones
intermitentes. Los registros locales pendientes deberán sincronizarse
cuando sea posible.

## 6.6 Restricciones de Wear OS

-   No asumir que todos los relojes tienen cámara.
-   No asumir conexión permanente con el teléfono.
-   No mantener GPS activo de forma innecesaria.
-   Solicitar permisos en el momento en que se necesiten.
-   Diseñar para pantallas pequeñas y sesiones cortas.
-   Considerar batería, latencia y conectividad.
-   Implementar estados sin conexión y sincronización pendiente.

------------------------------------------------------------------------

# 7. Requisitos funcionales

Los siguientes requisitos identifican comportamientos que el sistema
debe ofrecer. Cada requisito deberá convertirse en tareas técnicas y
pruebas durante su etapa de implementación.

  -----------------------------------------------------------------------
  ID                      Requisito               Prioridad
  ----------------------- ----------------------- -----------------------
  RF-001                  El sistema permitirá    Alta
                          registrar una cuenta e  
                          iniciar/cerrar sesión.  

  RF-002                  El sistema protegerá    Alta
                          los recursos privados   
                          asociados a cada        
                          usuario.                

  RF-003                  Android mostrará un     Alta
                          dashboard con           
                          información disponible  
                          y accesos principales.  

  RF-004                  Android mostrará un     Alta
                          mapa interactivo        
                          centrado inicialmente   
                          en Somerton.            

  RF-005                  El usuario podrá        Alta
                          seleccionar una zona y  
                          consultar su ficha de   
                          inteligencia.           

  RF-006                  El usuario podrá        Alta
                          activar filtros y capas 
                          geográficas             
                          disponibles.            

  RF-007                  Cada indicador mostrará Alta
                          su fuente, periodo y    
                          granularidad cuando     
                          esos metadatos existan. 

  RF-008                  El usuario podrá        Alta
                          seleccionar un perfil   
                          de inversión.           

  RF-009                  El sistema calculará el Alta
                          Opportunity Score según 
                          la metodología          
                          versionada del perfil.  

  RF-010                  El sistema explicará    Alta
                          los factores que        
                          contribuyeron al score. 

  RF-011                  El sistema identificará Alta
                          datos faltantes o       
                          insuficientes.          

  RF-012                  El usuario podrá        Alta
                          guardar y quitar zonas  
                          de su Watchlist.        

  RF-013                  El usuario podrá        Alta
                          registrar, editar,      
                          consultar y eliminar    
                          propiedades propias.    

  RF-014                  El usuario podrá        Media
                          asociar una propiedad   
                          con su zona geográfica. 

  RF-015                  El usuario podrá        Media
                          comparar zonas y        
                          propiedades con         
                          indicadores             
                          compatibles.            

  RF-016                  El usuario podrá        Media
                          consultar reportes de   
                          análisis.               

  RF-017                  El reloj podrá          Alta
                          consultar zonas         
                          guardadas y resúmenes   
                          disponibles.            

  RF-018                  El reloj podrá          Alta
                          registrar puntos de     
                          interés durante una     
                          exploración.            

  RF-019                  El reloj podrá iniciar  Alta
                          y finalizar una sesión  
                          de exploración.         

  RF-020                  El sistema sincronizará Alta
                          los registros del reloj 
                          con Android/backend     
                          cuando exista           
                          conectividad.           

  RF-021                  El reloj podrá mostrar  Media
                          alertas de proximidad   
                          configuradas.           

  RF-022                  El usuario podrá        Alta
                          controlar permisos y    
                          preferencias de         
                          ubicación y             
                          notificaciones.         

  RF-023                  El sistema distinguirá  Alta
                          datos oficiales,        
                          estimados, derivados y  
                          aportados por usuarios. 

  RF-024                  El sistema conservará   Alta
                          trazabilidad de fuentes 
                          y metodología de        
                          análisis.               

  RF-025                  El usuario podrá        Alta
                          consultar cuándo se     
                          actualizó cada conjunto 
                          de datos.               
  -----------------------------------------------------------------------

Las prioridades pueden cambiar al validar la viabilidad de las fuentes.
Un requisito no se considerará terminado solo porque exista una
pantalla: deberá funcionar con datos, estados y pruebas adecuados.

------------------------------------------------------------------------

# 8. Requisitos no funcionales

  -----------------------------------------------------------------------
  ID                      Requisito               Criterio esperado
  ----------------------- ----------------------- -----------------------
  RNF-001                 Seguridad               Comunicación cifrada
                                                  mediante HTTPS en
                                                  entornos desplegados.

  RNF-002                 Protección de           No almacenar
                          credenciales            contraseñas en texto
                                                  plano ni incluir
                                                  secretos en el
                                                  repositorio.

  RNF-003                 Autorización            El backend validará
                                                  permisos en cada
                                                  operación protegida; no
                                                  confiará solo en la
                                                  interfaz.

  RNF-004                 Privacidad              Solicitar ubicación
                                                  únicamente cuando sea
                                                  necesaria y explicar su
                                                  propósito.

  RNF-005                 Rendimiento             Las pantallas
                                                  principales deben
                                                  responder con fluidez y
                                                  mostrar estados de
                                                  carga; definir
                                                  presupuestos medibles
                                                  durante el MVP.

  RNF-006                 Disponibilidad parcial  El usuario podrá
                                                  consultar datos locales
                                                  o registros pendientes
                                                  cuando no haya
                                                  conexión, dentro de los
                                                  límites definidos.

  RNF-007                 Consistencia            Las operaciones de
                                                  sincronización deberán
                                                  evitar duplicados y
                                                  manejar reintentos.

  RNF-008                 Mantenibilidad          Código modular,
                                                  responsabilidades
                                                  separadas y
                                                  convenciones
                                                  documentadas.

  RNF-009                 Escalabilidad           La arquitectura
                                                  permitirá incorporar
                                                  nuevas ciudades y
                                                  conjuntos de datos sin
                                                  reescribir el núcleo.

  RNF-010                 Accesibilidad           Contraste adecuado,
                                                  tamaños legibles,
                                                  etiquetas accesibles y
                                                  objetivos táctiles
                                                  apropiados.

  RNF-011                 Usabilidad              Los flujos principales
                                                  deberán ser
                                                  comprensibles sin
                                                  depender de
                                                  documentación externa.

  RNF-012                 Observabilidad          Backend con logs
                                                  estructurados, manejo
                                                  consistente de errores
                                                  y métricas básicas.

  RNF-013                 Calidad                 Pruebas unitarias,
                                                  integración y pruebas
                                                  de interfaz en los
                                                  módulos críticos.

  RNF-014                 Portabilidad            Versiones y
                                                  dependencias
                                                  documentadas;
                                                  configuración
                                                  reproducible.

  RNF-015                 Trazabilidad            Todo dato externo
                                                  relevante conservará
                                                  fuente, fecha, periodo
                                                  y método de
                                                  transformación.

  RNF-016                 Eficiencia energética   Wear OS evitará
                                                  ubicación y
                                                  sincronización
                                                  intensivas cuando no
                                                  sean necesarias.

  RNF-017                 Integridad geoespacial  Coordenadas, sistemas
                                                  de referencia, unidades
                                                  y límites geográficos
                                                  deberán validarse.

  RNF-018                 Transparencia           El sistema no
                                                  presentará estimaciones
                                                  como hechos oficiales
                                                  ni scores como
                                                  garantías.
  -----------------------------------------------------------------------

Los objetivos cuantitativos de rendimiento, disponibilidad y capacidad
deberán definirse después de conocer el volumen de datos, el entorno de
despliegue y los dispositivos de prueba. No se deben inventar
compromisos de SLA para un prototipo académico.

------------------------------------------------------------------------

# 9. Arquitectura técnica

## 9.1 Arquitectura general

``` text
┌─────────────────────────┐       ┌─────────────────────────┐
│ Android App             │       │ Wear OS App             │
│ Kotlin + Compose        │◄─────►│ Kotlin + Wear Compose   │
│                         │       │                         │
│ - Mapas                 │       │ - Zone Alerts           │
│ - Area Intelligence     │       │ - Field Explorer        │
│ - Opportunity Finder    │       │ - Quick Evaluation      │
│ - Properties            │       │ - Watchlist             │
│ - Reports               │       │ - Exploration Mode      │
└────────────┬────────────┘       └────────────┬────────────┘
             │                                 │
             │ REST API                        │ Wearable Data Layer
             │                                 │
             ▼                                 ▼
┌──────────────────────────────────────────────────────────┐
│ Backend API - Python + FastAPI                            │
│                                                          │
│ Auth / Users / Zones / Properties / Watchlist             │
│ Data ingestion / Geospatial queries / Analytics           │
│ Opportunity scoring / Reports / Sync                      │
└────────────────────────────┬─────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────┐
│ PostgreSQL + PostGIS                                      │
│ Users / Properties / Zones / Observations / Source data   │
│ Geometries / Indicators / Score versions / Sync records   │
└──────────────────────────────────────────────────────────┘
                             ▲
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
  Census / ACS          OpenStreetMap        GIS municipal /
  Data USA              Overpass u otras     Yuma County
                        fuentes OSM          cuando esté disponible
```

## 9.2 Aplicación Android

Tecnologías:

-   **Kotlin:** lenguaje principal.
-   **Jetpack Compose:** interfaz declarativa.
-   **Android Studio:** IDE.
-   **Google Maps SDK o Mapbox:** mapa interactivo; elegir uno después
    de revisar costos, cuotas, licencias y necesidades.
-   **Retrofit + OkHttp:** comunicación HTTP con backend.
-   **Kotlin Coroutines y Flow:** operaciones asíncronas y flujos de
    datos.
-   **Room:** persistencia local.
-   **Hilt:** inyección de dependencias.
-   **DataStore:** preferencias locales.
-   **Android Keystore:** protección de material sensible cuando
    aplique.
-   **JUnit, MockK y herramientas AndroidX:** pruebas según
    compatibilidad.

Arquitectura recomendada: separación por capas y módulos de
responsabilidad, inspirada en Clean Architecture y las recomendaciones
oficiales de Android. Evitar complejidad ceremonial: la arquitectura
debe facilitar pruebas y mantenimiento, no crear capas vacías.

Capas lógicas sugeridas:

-   `presentation`: pantallas, componentes Compose y ViewModels.
-   `domain`: casos de uso y modelos de negocio.
-   `data`: repositorios, fuentes remotas y locales.
-   `core`: utilidades, configuración y componentes compartidos.

## 9.3 Aplicación Wear OS

Tecnologías:

-   **Kotlin.**
-   **Wear Compose.**
-   **Wearable Data Layer API:** comunicación y sincronización con el
    teléfono.
-   **Fused Location Provider:** ubicación cuando corresponda.
-   **Geofencing API:** alertas de proximidad cuando sea viable.
-   **Haptic feedback:** respuestas hápticas.
-   **Room/DataStore:** almacenamiento local según las necesidades.
-   **WorkManager:** tareas diferibles de sincronización, cuando
    corresponda.

No utilizar Health Services salvo que una funcionalidad futura requiera
explícitamente métricas de actividad física. No es una dependencia
central de TerraScope.

## 9.4 Backend

Tecnologías:

-   **Python:** lenguaje.
-   **FastAPI:** API REST.
-   **Pydantic:** validación y esquemas.
-   **SQLAlchemy:** ORM y acceso a datos.
-   **Alembic:** migraciones.
-   **PostgreSQL:** base de datos relacional.
-   **PostGIS:** extensiones y consultas geoespaciales.
-   **Pytest:** pruebas.
-   **HTTPX:** pruebas y clientes HTTP cuando corresponda.
-   **Docker:** entorno reproducible y despliegue.
-   **OpenAPI:** documentación automática de endpoints.

El backend será responsable de:

-   Autenticación y autorización.
-   Validación de datos.
-   Integración y normalización de fuentes.
-   Consultas geoespaciales.
-   Cálculos del Opportunity Score.
-   Gestión de propiedades y listas guardadas.
-   Registro de observaciones de campo.
-   Sincronización.
-   Trazabilidad de fuentes.
-   Generación o preparación de reportes.

## 9.5 Base de datos

PostgreSQL + PostGIS es la opción recomendada porque el producto depende
de relaciones espaciales, distancias, áreas, puntos y polígonos.

Entidades iniciales candidatas:

-   `users`
-   `user_devices`
-   `zones`
-   `zone_boundaries`
-   `data_sources`
-   `dataset_imports`
-   `indicators`
-   `indicator_values`
-   `business_places`
-   `investment_profiles`
-   `scoring_models`
-   `scoring_model_versions`
-   `opportunity_scores`
-   `properties`
-   `watchlist_items`
-   `field_sessions`
-   `field_observations`
-   `sync_events`
-   `report_records`

El diseño definitivo debe derivarse de los casos de uso y fuentes
reales. No crear todas las tablas desde el primer día sin validar su
necesidad.

## 9.6 Integración móvil-reloj

Usar Wearable Data Layer API para los datos que deban compartirse entre
dispositivos compatibles.

Definir mensajes/eventos versionados, por ejemplo:

-   `watch.session.started`
-   `watch.session.ended`
-   `watch.observation.created`
-   `watch.observation.updated`
-   `watch.sync.requested`
-   `phone.watchlist.updated`
-   `phone.zone_summary.updated`

Cada evento debe tener identificador único, fecha, versión de esquema y
estado de sincronización. La sincronización deberá ser idempotente:
recibir el mismo evento más de una vez no debe crear registros
duplicados.

El backend seguirá siendo la fuente central de datos sincronizados entre
sesiones y cuentas. La Data Layer no sustituye al backend ni garantiza
conectividad permanente.

------------------------------------------------------------------------

# 10. Datos, fuentes y estrategia de integración

La viabilidad del producto depende de la disponibilidad, calidad,
granularidad, licencia y actualización de los datos.

## 10.1 Fuentes candidatas

  -----------------------------------------------------------------------
  Fuente                  Uso potencial           Validación necesaria
  ----------------------- ----------------------- -----------------------
  U.S. Census Bureau      Población, hogares y    Geografía disponible,
                          datos censales          periodo, límites y API

  American Community      Ingresos y              Margen de error,
  Survey (ACS)            características         periodo y nivel
                          demográficas estimadas  geográfico

  Data USA                Exploración y contexto  Fuente original,
                          de datos públicos       metodología y
                                                  condiciones de uso

  OpenStreetMap           Calles, puntos de       Licencia ODbL,
                          interés y negocios      atribución, cobertura y
                          mapeados                actualización

  Yuma County GIS         Capas geográficas y     Disponibilidad,
                          datos territoriales     licencia, formato y
                                                  cobertura

  City of Somerton        Planeación,             Confirmar que los
                          zonificación, permisos  datasets existan y
                          y datos municipales     puedan reutilizarse

  Fuentes de mercado      Precios, ventas y       Acceso, licencia,
  inmobiliario            rentas                  cobertura,
                                                  actualización y uso
                                                  comercial
  -----------------------------------------------------------------------

Estas son fuentes candidatas, no integraciones garantizadas. Antes de
programar cada integración, se debe realizar una investigación
documentada y comprobar que los datos pueden obtenerse y utilizarse
legalmente.

## 10.2 Registro de fuentes

Cada conjunto de datos debe registrar como mínimo:

-   Nombre de la fuente.
-   Organización responsable.
-   URL o referencia documental.
-   Licencia o condiciones de uso.
-   Fecha de consulta.
-   Periodo de referencia.
-   Fecha de publicación, si existe.
-   Cobertura geográfica.
-   Granularidad espacial.
-   Campos importados.
-   Método de transformación.
-   Frecuencia de actualización.
-   Limitaciones conocidas.

## 10.3 Reglas de calidad de datos

-   No inventar datos para completar visualizaciones.
-   No asumir que un dato está disponible solo porque una fuente lo
    publica para otra ciudad o escala.
-   No confundir fecha de descarga con periodo de referencia.
-   No presentar datos estimados como datos censales exactos.
-   No mezclar unidades sin normalización.
-   No combinar zonas geográficas incompatibles sin advertirlo.
-   Registrar errores y valores faltantes.
-   Mostrar mensajes adecuados cuando no haya información suficiente.
-   Mantener atribución y cumplir licencias.

## 10.4 Datos inmobiliarios

No asumir que Zillow u otra plataforma ofrece una API pública gratuita
ni que sus datos pueden reutilizarse comercialmente. La investigación
deberá comprobar términos, acceso, cobertura y licencias antes de elegir
un proveedor.

Si no existe una fuente de mercado viable para el MVP, se podrá permitir
que el usuario registre precios observados o datos de propiedades
manualmente, identificándolos claramente como datos aportados por el
usuario.

------------------------------------------------------------------------

# 11. Inteligencia artificial

La IA es una capacidad complementaria, no el núcleo de la verdad del
sistema.

## 11.1 Usos permitidos

-   Explicar indicadores y resultados.
-   Resumir factores favorables y desfavorables.
-   Redactar borradores de reportes a partir de datos proporcionados por
    el backend.
-   Ayudar a formular consultas de análisis.
-   Resumir notas de campo, si el usuario lo solicita.

## 11.2 Restricciones

-   No inventar cifras, fuentes, tendencias ni negocios.
-   No calcular indicadores críticos mediante texto libre de un LLM
    cuando exista una fórmula determinista.
-   No generar una recomendación sin mostrar los datos que la sustentan.
-   No presentar contenido generado como asesoría financiera, legal o de
    tasación.
-   No ocultar incertidumbre.
-   No permitir que la IA cambie la metodología del score sin control de
    versiones y pruebas.

## 11.3 Diseño recomendado

El backend calcula los indicadores y el score. Después puede enviar a un
modelo de IA un paquete estructurado con valores, metadatos, fuentes,
limitaciones y resultados. La IA solo redacta una explicación basada en
ese paquete.

Si no hay una integración de IA disponible o configurada, las funciones
centrales deben seguir funcionando.

------------------------------------------------------------------------

# 12. Seguridad, privacidad y cumplimiento

Requisitos esenciales:

-   HTTPS en entornos desplegados.
-   Secretos fuera del código fuente.
-   Archivo `.env` local excluido de Git y archivo `.env.example` sin
    secretos.
-   Autenticación segura y autorización en backend.
-   Validación de entrada en cliente y servidor.
-   Protección frente a acceso a recursos de otros usuarios.
-   Límites de solicitudes y controles básicos contra abuso.
-   Logs sin contraseñas, tokens ni datos sensibles innecesarios.
-   Gestión de permisos de ubicación y notificaciones.
-   Opción para desactivar alertas y exploración.
-   Política de retención y eliminación de datos personales.
-   Copias de seguridad de la base de datos en entornos persistentes.
-   Dependencias revisadas y actualizadas.

## 12.1 Uso responsable de información demográfica

Los indicadores demográficos deben utilizarse para entender el contexto
agregado de un área, no para discriminar personas o comunidades.

TerraScope no debe:

-   Recomendar excluir grupos protegidos.
-   Clasificar individuos por características sensibles.
-   Inferir atributos sensibles de personas.
-   Utilizar datos demográficos como mecanismo automático de exclusión.
-   Presentar una zona como indeseable por la presencia de una
    comunidad.

Los análisis deben centrarse en características del territorio,
accesibilidad, actividad comercial, infraestructura, disponibilidad de
servicios y otros factores legítimos y explicables. Deben revisarse las
obligaciones aplicables de Fair Housing y otras normas relevantes en
Estados Unidos antes de cualquier uso comercial.

------------------------------------------------------------------------

# 13. Experiencia de usuario y diseño visual

## 13.1 Dirección visual

-   Estética profesional y sobria.
-   Interfaz moderna orientada a análisis.
-   Modo oscuro como opción visual principal, con contraste suficiente.
-   Jerarquía tipográfica clara.
-   Mapas y datos como elementos protagonistas.
-   Tarjetas y gráficas con propósito funcional.
-   Uso moderado del color para comunicar categorías y estados.
-   Iconografía consistente.
-   Sin emojis como sustitutos de iconos de interfaz.
-   Evitar pantallas saturadas de métricas.

## 13.2 Principios de UX

-   Mostrar primero la información necesaria para decidir qué explorar.
-   Permitir profundizar en detalles bajo demanda.
-   Explicar términos y unidades.
-   No ocultar la fecha o procedencia de datos.
-   Mantener consistencia entre Android y Wear OS sin forzar diseños
    idénticos.
-   Diseñar estados vacíos, errores, carga y funcionamiento sin
    conexión.
-   Solicitar permisos justo antes de la función que los necesita.
-   En el reloj, priorizar acciones rápidas y contenido breve.

------------------------------------------------------------------------

# 14. Plan de desarrollo por etapas

**Regla fundamental para cualquier asistente de IA que participe en el
proyecto: no desarrollar todo de una sola vez.** Se trabajará por etapas
pequeñas, con entregables verificables y aprobación explícita antes de
avanzar.

Cada etapa debe terminar con:

1.  Resumen de lo realizado.
2.  Archivos creados o modificados.
3.  Decisiones técnicas tomadas.
4.  Pruebas ejecutadas y resultados.
5.  Problemas conocidos.
6.  Riesgos o decisiones pendientes.
7.  Instrucciones exactas para ejecutar o validar.
8.  Confirmación de que los criterios de aceptación se cumplen o lista
    de pendientes.

No iniciar la etapa siguiente hasta que la etapa actual esté revisada y
aprobada.

## Etapa 0 --- Descubrimiento y viabilidad

**Objetivo:** comprobar que el producto puede construirse con datos y
tecnologías disponibles.

Pasos:

1.  Investigar datos de Census/ACS para Somerton.
2.  Identificar geografías disponibles y sus límites.
3.  Investigar cobertura de OpenStreetMap en la zona.
4.  Investigar fuentes oficiales de GIS, zonificación y planeación.
5.  Revisar licencias y términos de uso.
6.  Identificar datos inmobiliarios realmente accesibles.
7.  Documentar qué indicadores son viables para el MVP.
8.  Identificar datos que no se podrán ofrecer inicialmente.
9.  Definir un primer perfil de inversión para el MVP, preferentemente
    inversión comercial/site selection.
10. Crear una matriz de fuentes, riesgos y decisiones.

**Entregables:** - `docs/research/data-sources.md` -
`docs/research/data-availability-matrix.md` -
`docs/product/mvp-scope.md` - Lista de riesgos y decisiones pendientes.

**Criterio de salida:** existe una lista realista de indicadores que
pueden obtenerse legal y técnicamente para Somerton. No se continúa con
visualizaciones basadas en datos inventados.

## Etapa 1 --- Definición funcional y UX

**Objetivo:** concretar los flujos antes de implementar pantallas.

Pasos:

1.  Definir perfiles de usuario y casos de uso.
2.  Crear mapa de navegación Android.
3.  Crear mapa de navegación Wear OS.
4.  Diseñar flujos principales: mapa, ficha de zona, búsqueda de
    oportunidad, propiedad y exploración de campo.
5.  Definir estados de carga, error, vacío y sin conexión.
6.  Definir componentes visuales y sistema de diseño.
7.  Crear wireframes y prototipo visual.
8.  Revisar la usabilidad en tamaños reales de teléfono y reloj.

**Entregables:** - `docs/product/user-flows.md` -
`docs/design/design-system.md` - `docs/design/android-screens.md` -
`docs/design/wear-os-screens.md`

**Criterio de salida:** los flujos principales están definidos y no
existen pantallas sin una función o fuente de datos identificada.

## Etapa 2 --- Arquitectura y configuración del repositorio

**Objetivo:** crear una base técnica reproducible sin implementar aún
todo el producto.

Pasos:

1.  Definir estructura del monorepositorio.
2.  Crear proyecto Android Kotlin + Compose.
3.  Crear módulo Wear OS Kotlin + Wear Compose.
4.  Crear backend FastAPI.
5.  Configurar PostgreSQL + PostGIS mediante Docker Compose.
6.  Definir variables de entorno y `.env.example`.
7.  Configurar formato, lint y pruebas básicas.
8.  Configurar Git, `.gitignore` y README.
9.  Documentar versiones compatibles de JDK, Gradle, Kotlin, Android
    Gradle Plugin y SDK.
10. Ejecutar una compilación mínima de Android, Wear OS y backend.

**Entregables:** - Estructura inicial del repositorio. - `README.md` -
`docker-compose.yml` - `.env.example` - Aplicaciones mínimas
compilables. - Backend con endpoint de salud. -
`docs/architecture/architecture.md`

**Criterio de salida:** los tres componentes arrancan o compilan
siguiendo instrucciones documentadas en un entorno limpio.

## Etapa 3 --- Modelo de datos y backend base

**Objetivo:** construir la persistencia y API central.

Pasos:

1.  Definir entidades mínimas justificadas por casos de uso.
2.  Crear modelos y migraciones.
3.  Configurar conexión PostgreSQL/PostGIS.
4.  Implementar validación con Pydantic.
5.  Implementar estructura de errores y respuestas.
6.  Implementar autenticación y autorización.
7.  Implementar pruebas de repositorios y endpoints.
8.  Documentar endpoints mediante OpenAPI.

**Entregables:** - Modelos iniciales. - Migraciones Alembic. - Endpoints
base. - Pruebas automatizadas. - Documentación API.

**Criterio de salida:** el backend persiste y consulta datos de prueba
con validación, autorización y pruebas.

## Etapa 4 --- Primer conjunto de datos real

**Objetivo:** integrar una fuente real y visualizarla de extremo a
extremo.

Pasos:

1.  Elegir una fuente de datos viable de la etapa 0.
2.  Implementar un proceso de importación reproducible.
3.  Normalizar campos y unidades.
4.  Guardar fuente, periodo, cobertura y fecha de importación.
5.  Validar geometrías y sistemas de referencia.
6.  Crear consultas geoespaciales necesarias.
7.  Crear endpoint para consultar indicadores por zona.
8.  Mostrar esos datos en Android.
9.  Comparar algunos resultados con la fuente original.
10. Documentar limitaciones.

**Entregables:** - Primer dataset real. - Proceso de importación. -
Endpoint funcional. - Primera visualización Android con datos reales. -
Pruebas de integridad y documentación de la fuente.

**Criterio de salida:** el usuario puede consultar al menos un indicador
real en su contexto geográfico y rastrear su origen.

## Etapa 5 --- Android: experiencia principal

**Objetivo:** construir los flujos centrales de análisis.

Implementar en este orden:

1.  Navegación principal.
2.  Dashboard con estados reales.
3.  Investment Map.
4.  Selección de zona.
5.  Area Intelligence.
6.  Filtros y capas disponibles.
7.  Watchlist.
8.  Estados de error, carga y ausencia de datos.
9.  Persistencia local y caché cuando sea útil.
10. Pruebas de ViewModels, repositorios y UI crítica.

**Criterio de salida:** la experiencia principal permite explorar el
mapa, seleccionar una zona y consultar información real sin depender de
datos ficticios.

## Etapa 6 --- Opportunity Finder y metodología de scoring

**Objetivo:** implementar un análisis explicable y versionado.

Pasos:

1.  Elegir el perfil de inversión inicial.
2.  Definir variables que realmente estén disponibles.
3.  Definir normalización, ponderaciones y tratamiento de faltantes.
4.  Documentar por qué cada variable es relevante.
5.  Separar calidad de datos del score.
6.  Implementar la metodología en backend.
7.  Crear pruebas con casos conocidos y extremos.
8.  Mostrar desglose de factores en Android.
9.  Versionar la metodología.
10. Validar que los resultados no sean engañosos.

**Criterio de salida:** el score es reproducible, explicable, probado y
muestra sus limitaciones. Si faltan datos esenciales, el sistema debe
indicarlo en vez de producir una puntuación engañosa.

## Etapa 7 --- Property Explorer y comparación

**Objetivo:** gestionar inmuebles registrados por el usuario y
compararlos con contexto.

Pasos:

1.  Implementar CRUD de propiedades.
2.  Validar coordenadas, superficies, unidades y precios.
3.  Asociar propiedades con zonas.
4.  Mostrar contexto territorial de cada propiedad.
5.  Implementar comparación de propiedades.
6.  Implementar comparación de zonas.
7.  Añadir pruebas de permisos y propiedad de registros.

**Criterio de salida:** los registros están aislados por usuario y las
comparaciones muestran datos equivalentes con sus limitaciones.

## Etapa 8 --- Wear OS: Investor Scout

**Objetivo:** entregar una experiencia útil durante recorridos físicos.

Implementar en este orden:

1.  Proyecto Wear OS compilable.
2.  Navegación y diseño para pantalla pequeña.
3.  Comunicación inicial con Android mediante Data Layer.
4.  Consulta de Watchlist.
5.  Quick Evaluation.
6.  Registro local de observaciones.
7.  Inicio y cierre de sesiones de exploración.
8.  Sincronización idempotente.
9.  Permisos y ubicación.
10. Geofencing y alertas, después de validar los flujos básicos.
11. Pruebas en emulador y dispositivo físico compatible.

**Criterio de salida:** el reloj puede consultar información, registrar
observaciones y sincronizarlas sin duplicados; los fallos de conexión no
provocan pérdida silenciosa de registros.

## Etapa 9 --- Reportes e IA asistiva

**Objetivo:** presentar análisis comprensibles y trazables.

Pasos:

1.  Definir plantilla de reporte.
2.  Generar resumen desde datos estructurados.
3.  Añadir fuentes, fechas y metodología.
4.  Añadir factores favorables, riesgos y datos faltantes.
5.  Implementar exportación PDF si es viable.
6.  Evaluar integración de IA para redactar explicaciones.
7.  Validar que el texto generado no introduzca hechos no presentes.
8.  Probar funcionamiento sin proveedor de IA.

**Criterio de salida:** los reportes son reproducibles y toda afirmación
cuantitativa puede rastrearse a datos del sistema.

## Etapa 10 --- Calidad, seguridad y preparación de portafolio

**Objetivo:** convertir el prototipo funcional en un proyecto
demostrable y mantenible.

Pasos:

1.  Revisar seguridad de API y autorización.
2.  Revisar privacidad y permisos.
3.  Ejecutar pruebas unitarias e integración.
4.  Ejecutar pruebas de interfaz críticas.
5.  Probar funcionamiento con red inestable.
6.  Probar sincronización reloj-teléfono.
7.  Medir rendimiento en dispositivos objetivo.
8.  Revisar accesibilidad y diseño.
9.  Preparar datos de demostración claramente identificados.
10. Preparar capturas y video de demostración.
11. Documentar arquitectura y decisiones técnicas.
12. Documentar limitaciones y trabajo futuro.
13. Preparar instrucciones de instalación y ejecución.
14. Revisar licencias de datos y dependencias.

**Criterio de salida:** un tercero puede clonar el repositorio,
configurar el entorno siguiendo el README, ejecutar el proyecto y
comprender sus decisiones técnicas.

------------------------------------------------------------------------

# 15. Organización sugerida del repositorio

La estructura es una propuesta inicial y puede ajustarse en la etapa 2.

``` text
TerraScope/
├── android/
│   ├── app/
│   └── core/
│       ├── common/
│       ├── designsystem/
│       ├── network/
│       └── database/
├── wear/
│   └── app/
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   └── v1/
│   │   ├── core/
│   │   ├── models/
│   │   ├── schemas/
│   │   ├── repositories/
│   │   ├── services/
│   │   ├── analytics/
│   │   ├── integrations/
│   │   └── main.py
│   ├── migrations/
│   ├── tests/
│   └── requirements/
├── data/
│   ├── samples/
│   └── documentation/
├── docs/
│   ├── architecture/
│   ├── design/
│   ├── product/
│   ├── research/
│   └── decisions/
├── scripts/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

No incluir datasets con restricciones de redistribución ni secretos en
el repositorio. Los archivos de muestra deben ser pequeños y contar con
su procedencia.

------------------------------------------------------------------------

# 16. Estrategia de pruebas y aceptación

## 16.1 Backend

-   Pruebas unitarias de reglas de negocio.
-   Pruebas de integración con PostgreSQL/PostGIS.
-   Pruebas de autenticación y autorización.
-   Pruebas de validación de coordenadas y unidades.
-   Pruebas de importación de datos.
-   Pruebas de idempotencia de sincronización.
-   Pruebas de errores de fuentes externas.

## 16.2 Android

-   Pruebas de ViewModels y casos de uso.
-   Pruebas de repositorios.
-   Pruebas de navegación.
-   Pruebas de estados de carga/error/vacío.
-   Pruebas de mapas con datos de prueba controlados.
-   Pruebas de sesión y manejo de errores de API.

## 16.3 Wear OS

-   Pruebas de interfaz en distintos tamaños.
-   Pruebas de Data Layer.
-   Pruebas de sincronización repetida.
-   Pruebas sin conexión.
-   Pruebas de permisos de ubicación.
-   Pruebas de batería y frecuencia de ubicación.
-   Pruebas físicas de vibración y alertas cuando se disponga de reloj
    compatible.

## 16.4 Pruebas de aceptación del MVP

El MVP se considerará funcional cuando:

1.  Un usuario pueda iniciar sesión.
2.  Android muestre un mapa de Somerton.
3.  El usuario pueda seleccionar una zona.
4.  La ficha muestre al menos un conjunto de indicadores reales y
    trazables.
5.  El usuario pueda guardar una zona.
6.  El usuario pueda consultar el análisis de un perfil inicial, siempre
    que existan datos suficientes.
7.  El usuario pueda registrar una propiedad manualmente.
8.  El reloj pueda consultar la Watchlist o un resumen sincronizado.
9.  El reloj pueda registrar una observación de campo.
10. La observación pueda sincronizarse sin duplicarse.
11. Los estados sin conexión y los errores sean visibles y manejados.
12. El repositorio incluya documentación y pruebas de las funciones
    críticas.

------------------------------------------------------------------------

# 17. Gestión del trabajo con asistentes de IA

Este apartado es obligatorio para cualquier IA que trabaje en
TerraScope.

## 17.1 Regla de trabajo incremental

No intentes construir todo el proyecto de una sola vez. No generes
decenas de archivos sin explicar su función. No avances automáticamente
por todas las etapas.

Trabaja exclusivamente en la etapa y tarea solicitadas.

## 17.2 Antes de modificar código

1.  Inspecciona la estructura actual del repositorio.
2.  Lee el README y la documentación relevante.
3.  Identifica tecnologías y versiones existentes.
4.  Comprueba qué partes ya están implementadas.
5.  No sobrescribas trabajo existente sin justificarlo.
6.  Presenta un plan breve de archivos que modificarás.
7.  Señala riesgos y dependencias.

## 17.3 Durante la implementación

-   Realiza cambios pequeños y coherentes.
-   Mantén las convenciones del proyecto.
-   Evita duplicar lógica.
-   No añadas dependencias sin justificar su necesidad.
-   No uses versiones incompatibles o inventadas.
-   No inventes APIs, endpoints, propiedades de SDK o datos.
-   Consulta documentación oficial cuando una decisión dependa de
    versiones o APIs actuales.
-   No pongas secretos en el código.
-   No desactives validaciones o pruebas para ocultar errores.
-   No uses datos ficticios como si fueran datos reales.
-   Si necesitas datos simulados para una prueba, identifícalos
    claramente.
-   Implementa estados de carga, error y ausencia de datos.
-   Añade pruebas para reglas de negocio y flujos críticos.
-   Mantén actualizado el README y la documentación técnica.

## 17.4 Al terminar cada tarea

Entrega un informe con:

``` text
ETAPA:
TAREA:

1. Objetivo
2. Implementación realizada
3. Archivos creados
4. Archivos modificados
5. Decisiones técnicas
6. Pruebas ejecutadas
7. Resultado de las pruebas
8. Problemas pendientes
9. Riesgos o limitaciones
10. Cómo ejecutar y validar
11. Siguiente tarea propuesta
```

No afirmes que algo funciona si no lo ejecutaste o probaste. Distingue
entre:

-   Implementado y probado.
-   Implementado pero no probado.
-   Diseñado, pendiente de implementar.
-   Bloqueado por una dependencia o fuente externa.

## 17.5 Control de avance

Mantén un archivo `docs/PROJECT_STATUS.md` con:

-   Etapa actual.
-   Tareas completadas.
-   Tareas pendientes.
-   Decisiones aprobadas.
-   Problemas conocidos.
-   Pruebas ejecutadas.
-   Próximo objetivo.

Al iniciar una nueva sesión, lee este archivo y el README antes de
proponer cambios.

## 17.6 Aprobación humana

Al terminar una etapa, detente y espera revisión. No continúes a la
siguiente etapa sin autorización explícita.

Si detectas que una decisión de arquitectura, fuente de datos o alcance
es incierta, presenta opciones con ventajas, desventajas, costo y
recomendación. No tomes decisiones importantes silenciosamente.

------------------------------------------------------------------------

# 18. Riesgos principales y mitigaciones

  -----------------------------------------------------------------------
  Riesgo                  Impacto                 Mitigación
  ----------------------- ----------------------- -----------------------
  Datos locales           Alto                    Validar fuentes en
  insuficientes                                   etapa 0 y reducir el
                                                  alcance del MVP

  Datos con granularidad  Alto                    Mostrar nivel
  inadecuada                                      geográfico y no
                                                  atribuir precisión
                                                  falsa

  Licencias restrictivas  Alto                    Revisar términos antes
                                                  de descargar o
                                                  redistribuir

  Costos de mapas o APIs  Medio/alto              Comparar proveedores,
                                                  cuotas y alternativas
                                                  antes de decidir

  Score engañoso          Alto                    Metodología explicable,
                                                  versionada y probada

  Desgaste de batería del Medio                   Geofencing, sesiones
  reloj                                           explícitas y ubicación
                                                  bajo demanda

  Sincronización          Medio                   IDs únicos,
  duplicada                                       idempotencia y cola
                                                  local

  Complejidad excesiva    Alto                    Implementar por etapas
  del MVP                                         y limitar perfiles
                                                  iniciales

  Dependencia de IA       Medio                   Mantener cálculos
  externa                                         centrales deterministas
                                                  y funcionamiento sin IA

  Riesgos de              Alto                    Revisar variables,
  discriminación                                  lenguaje, usos y
                                                  cumplimiento de Fair
                                                  Housing
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 19. Definición de MVP recomendada

El MVP debe demostrar el valor central del producto sin intentar
resolver todo el mercado inmobiliario.

## Incluido en MVP

-   Android nativo.
-   Wear OS nativo con un flujo de campo reducido.
-   Backend FastAPI.
-   PostgreSQL + PostGIS.
-   Área piloto Somerton.
-   Un perfil inicial de análisis: inversión comercial/site selection.
-   Mapa interactivo.
-   Al menos un conjunto real de indicadores trazables.
-   Ficha de inteligencia de zona.
-   Opportunity Score básico, explicable y condicionado a la
    disponibilidad de datos.
-   Watchlist.
-   Registro manual de propiedades.
-   Registro de observaciones desde Wear OS.
-   Sincronización reloj-teléfono/backend.
-   Autenticación y autorización.
-   Pruebas y documentación.

## Posponer

-   Todos los perfiles de inversión avanzados.
-   Predicciones de precios.
-   Modelos de machine learning.
-   Integraciones de datos inmobiliarios de pago.
-   Automatización avanzada de alertas de mercado.
-   Reportes ejecutivos complejos.
-   Expansión geográfica.
-   Funciones sociales o colaborativas.

**Recomendación:** primero construir un recorrido vertical completo:
dato real → backend → mapa Android → ficha de zona → explicación → zona
guardada → consulta rápida en reloj. Es preferible tener ese flujo
sólido a tener muchas pantallas sin datos funcionales.

------------------------------------------------------------------------

# 20. Criterios de calidad para portafolio profesional

TerraScope deberá demostrar no solo capacidad de crear interfaces, sino
criterio de ingeniería.

El repositorio final debería incluir:

-   README profesional con descripción, objetivos, capturas y guía de
    ejecución.
-   Diagrama de arquitectura.
-   Diagrama de base de datos.
-   Documentación de API.
-   Registro de decisiones arquitectónicas (ADR).
-   Matriz de fuentes y licencias.
-   Requisitos funcionales y no funcionales.
-   Instrucciones de configuración reproducibles.
-   Pruebas automatizadas y resultados.
-   Capturas reales de Android y Wear OS.
-   Video corto de demostración.
-   Ejemplos de análisis con datos identificados como reales o de
    demostración.
-   Limitaciones conocidas y roadmap futuro.
-   Historial de commits claro y coherente.

No se debe afirmar que el producto está listo para uso comercial solo
por funcionar en un emulador. Debe distinguirse entre prototipo, MVP
validado y producto desplegado.

------------------------------------------------------------------------

# 21. Primer encargo para Claude u otro asistente

Copia este texto junto con el documento cuando quieras iniciar el
trabajo:

> Actúa como arquitecto de software y desarrollador senior especializado
> en Android nativo, Wear OS, Kotlin, Jetpack Compose, Python, FastAPI y
> PostgreSQL/PostGIS.
>
> Este documento define TerraScope. Léelo completo y úsalo como fuente
> principal de contexto y requisitos.
>
> **No desarrolles el proyecto completo todavía.** Comienza
> exclusivamente con la **Etapa 0 --- Descubrimiento y viabilidad**.
>
> Primero analiza qué información real puede obtenerse para Somerton,
> Arizona, mediante Census/ACS, OpenStreetMap, Data USA, GIS del condado
> de Yuma y fuentes municipales. Comprueba cobertura, granularidad,
> licencias, condiciones de uso y limitaciones. No asumas que existen
> APIs gratuitas ni que los datos pueden reutilizarse comercialmente.
>
> Entrega una matriz de fuentes, indicadores viables, limitaciones,
> riesgos y una propuesta realista de MVP. Distingue claramente hechos
> verificados, hipótesis y puntos pendientes de investigación.
>
> No generes todavía las aplicaciones, el backend ni grandes cantidades
> de código. No inventes datos. Al terminar, presenta los entregables de
> la etapa y detente para revisión y aprobación antes de avanzar.
>
> En las etapas posteriores trabaja de forma incremental: inspecciona el
> repositorio antes de modificarlo, implementa una tarea a la vez,
> ejecuta pruebas, documenta resultados y espera aprobación humana entre
> etapas. Nunca afirmes que una función está probada si no ejecutaste
> las pruebas correspondientes.

------------------------------------------------------------------------

# 22. Decisiones técnicas pendientes de aprobación

Estas decisiones deben cerrarse durante las etapas correspondientes, no
asumirse sin investigación:

1.  Google Maps SDK o Mapbox.
2.  Proveedor de autenticación o autenticación propia con estándares
    seguros.
3.  Proveedor de hosting del backend y PostgreSQL.
4.  Estrategia de almacenamiento de imágenes.
5.  Fuente geográfica y nivel de agregación de las zonas de análisis.
6.  Fuente viable de datos de mercado inmobiliario.
7.  Primer conjunto de indicadores del perfil comercial.
8.  Método y ponderaciones iniciales del Opportunity Score.
9.  Estrategia exacta de geofencing y sincronización en Wear OS.
10. Proveedor de IA, si se incorpora.

Cada decisión importante deberá documentarse en un ADR con contexto,
opciones consideradas, decisión, consecuencias y fecha.

------------------------------------------------------------------------

# 23. Glosario

-   **Inteligencia territorial:** análisis de características
    geográficas, demográficas, económicas y de infraestructura de un
    área.
-   **Site selection:** proceso de evaluar y seleccionar ubicaciones
    para instalar un negocio o desarrollar un proyecto.
-   **Opportunity Score:** indicador comparativo de apoyo al análisis,
    no garantía de rentabilidad.
-   **Geofence:** límite geográfico virtual que puede activar un evento
    al entrar o salir de un área.
-   **PostGIS:** extensión de PostgreSQL para almacenar y consultar
    información geoespacial.
-   **Granularidad geográfica:** nivel espacial al que corresponde un
    dato, por ejemplo ciudad, tracto censal o bloque.
-   **ACS:** American Community Survey, encuesta del U.S. Census Bureau
    que publica estimaciones demográficas y socioeconómicas.
-   **Data Layer:** mecanismo de comunicación entre dispositivos Android
    y Wear OS compatibles.
-   **Idempotencia:** propiedad por la cual repetir una operación no
    genera efectos duplicados.
-   **ADR:** Architecture Decision Record, documento breve que registra
    una decisión técnica relevante.

------------------------------------------------------------------------

**Fin del documento.**\
Este documento es la especificación base. Debe evolucionar mediante
decisiones documentadas y revisiones controladas, sin perder
trazabilidad entre requisitos, implementación y pruebas.
