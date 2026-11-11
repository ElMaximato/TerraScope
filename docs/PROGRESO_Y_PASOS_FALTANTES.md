# TerraScope — Resumen de Avance del Proyecto y Pasos Faltantes

**Proyecto:** TerraScope — Plataforma de Inteligencia Territorial e Inmobiliaria  
**Área Piloto:** Somerton, Arizona (Condado de Yuma, EE. UU.)  
**Pila Tecnológica:** Android (Kotlin + Jetpack Compose) + Wear OS (Wear Compose) + Backend (Node.js + Express + MySQL 8 Geoespacial)  
**Fecha de actualización:** Noviembre 2026  

> **Nota de auditoría (2026-11-08):** Este documento contiene secciones históricas que todavía describen cuatro tracts sintéticos, nombres inventados y `db:load-somerton`. Para el estado vigente, consultar primero `ESTADO_ACTUAL_PARA_CLAUDE.md` y `PROJECT_STATUS.md`. No usar las secciones antiguas de datos como fuente de implementación actual.

---

## 1. Visión y Objetivos del Proyecto

TerraScope es una plataforma móvil diseñada para que inversionistas, desarrolladores y emprendedores evalúen oportunidades territoriales en **Somerton, Arizona**. A diferencia de un portal inmobiliario convencional (como Zillow), TerraScope analiza el **contexto socioeconómico y espacial** de cada zona (población, ingreso mediano, edad, dinamismo territorial) combinando:
1. **Aplicación Móvil Android:** Centro analítico principal con mapas temáticos, fichas de zona, indicadores demográficos reales del U.S. Census Bureau y gestión de cartera/watchlist.
2. **Aplicación para Smartwatch (Wear OS):** Asistente de campo para recorridos físicos, permitiendo registrar observaciones in situ y consultar información contextual rápida.
3. **Backend Geoespacial REST:** Motor de datos en Node.js/Express con MySQL 8 espacial (SRID 4326), validación rigurosa, autenticación segura y sincronización idempotente.

---

## 2. Lo que se ha realizado hasta el momento (Etapas 0 a 5)

El desarrollo se ha ejecutado bajo una metodología incremental estricta con validaciones técnicas en cada paso.

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Etapa 0    │ ──> │   Etapa 1    │ ──> │   Etapa 2    │ ──> │   Etapa 3    │ ──> │   Etapa 4    │ ──> │   Etapa 5    │
│ Viabilidad   │     │  Diseño UX   │     │ Arquitectura │     │ Backend Base │     │ Datos Reales │     │ App Android  │
│  [Cerrada]   │     │  [Cerrada]   │     │  [Cerrada]   │     │  [Cerrada]   │     │  [Cerrada]   │     │  [En Código] │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

### ✅ Etapa 0 — Descubrimiento y Viabilidad
- **Investigación de fuentes oficiales:** Estudio exhaustivo de APIs y portales de datos abiertos (U.S. Census ACS 5-Year, Census TIGER/Line, OpenStreetMap, Yuma County GIS y City of Somerton).
- **Delimitación geográfica:** Identificación de las 4 subdivisiones censales (*Census Tracts*: `115.01`, `115.02`, `115.03` y `114.03`) y el polígono municipal incorporado (*Census Designated Place* `0468080`).
- **Entregables documentales:**
  - `docs/research/data-sources.md` (Inventario de fuentes y licencias).
  - `docs/research/data-availability-matrix.md` (Matriz de factibilidad de indicadores).
  - `docs/product/mvp-scope.md` (Alcance blindado para entrega escolar/profesional).

### ✅ Etapa 1 — Definición Funcional y Sistema de Diseño (UX/UI)
- **Flujos de usuario:** Definición de navegación en teléfono y reloj en `docs/product/user-flows.md`.
- **Sistema de diseño:** Paleta institucional sobria y premium inspirada en territorio (Verde Pino `#0F4D3A`, Acento Dorado `#E3AE55`, Fondo Limpio `#F6F8F7`, Tipografía moderna y sin emojis) en `docs/design/design-system.md`.
- **Especificación de pantallas:**
  - 12 pantallas Android definidas (A1 a A12) en `docs/design/android-screens.md`.
  - 6 pantallas Wear OS definidas (W1 a W6) en `docs/design/wear-os-screens.md`.

### ✅ Etapa 2 — Arquitectura Base y Configuración del Entorno
- **Configuración del monorepositorio:** Estructura modular conteniendo `android/` (módulos `app` y `wear`) y `backend/`.
- **Android Studio & Gradle:** Configuración de Gradle Kotlin DSL (AGP 9.4.1, Kotlin 2.2.10, SDK 37, Compose BOM 2026.02.01).
- **Configuración de emuladores:** Emparejamiento exitoso verificado entre teléfono (*Medium Phone API 37*) y reloj (*Wear OS Large Round* con Android 17).
- **Servidor Backend inicial:** Express + Node.js con endpoint de verificación `GET /api/health` y conexión a MySQL 8.
- **Verificación espacial en BD:** Verificación de funciones geométricas `ST_Contains`, `ST_Distance_Sphere` e índices `SPATIAL` en MySQL (`backend/scripts/verify-spatial.js`).

### ✅ Etapa 3 — Modelo de Datos, Autenticación y Backend Base
- **Migraciones SQL versionadas:**
  - `001_base_geo.sql`: Tablas de zonas (`zones`), lugares (`places`) y fuentes (`data_sources`) con tipos `GEOMETRY` SRID 4326.
  - `002_users.sql`: Usuarios con contraseñas hasheadas mediante `scrypt` criptográfico y tokens JWT.
  - `003_user_data.sql`: Propiedades de usuario (`properties`), listas de seguimiento (`watchlist_items`), sesiones de campo (`field_sessions`) y observaciones (`field_observations`).
- **Servicios e endpoints REST construidos:**
  - Autenticación: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`.
  - Propiedades: CRUD completo con detección automática de zona mediante intersección espacial `ST_Contains`.
  - Watchlist: Guardar y eliminar zonas de interés con aislamiento por usuario.
  - Sincronización de campo: Ingestión por lotes idempotente para sesiones y observaciones provenientes de Wear OS.
- **Control de calidad:** **68 pruebas automatizadas aprobadas** (34 unitarias + 34 de integración contra MySQL local).

### ✅ Etapa 4 — Primer Conjunto de Datos Reales de Somerton
- **Geometrías reales importadas:** Conversión de polígonos GeoJSON a WKT para los 4 Census Tracts y el Place de Somerton (`data/somerton_tracts_tiger.json` y `data/somerton_place_tiger.json`).
- **Métricas demográficas oficiales (ACS 2022/2024):** Ingestión de población total, número de hogares, ingreso mediano del hogar y edad mediana con sus respectivos márgenes de error (`data/somerton_acs_data.json`).
- **Migración `004_indicators.sql`:** Creación de catálogo de indicadores y tabla de valores normalizados.
- **Script de automatización:** `npm run db:load-somerton` para poblar la base de datos de manera reproducible.
- **Suite de pruebas:** **74/74 pruebas exitosas al 100%** (37 unitarias y 37 de integración).

### ✅ Etapa 5 — Android: Experiencia Principal (Código Completado)
Se ha implementado toda la lógica, modelos, arquitectura MVVM y pantallas nativas en Jetpack Compose:
1. **Gestión de Sesión y Token Persistente:** `TokenStore.kt` utilizando Jetpack DataStore Preferences para recordar el JWT entre reinicios de la aplicación.
2. **Cliente HTTP Tipado:** `TerraScopeApi.kt` implementando manejo de excepciones tipadas (`ApiException`) y consumo de todos los endpoints del backend (`http://10.0.2.2:3000`).
3. **Flujo de Autenticación Completo:** `AuthScreen.kt` y `AuthViewModel.kt` con validación de formularios, alternancia entre Login y Registro, estados de carga y retroalimentación de error.
4. **Navegación por Pestañas:** `MainNavigation.kt` con barra de navegación inferior (`NavigationBar`) que conecta las 4 secciones principales:
   - **Inicio (`HomeScreen.kt`):** Tarjeta destacada de Somerton (14,574 hab, $74,015 ingreso mediano), acceso rápido a Watchlist y catálogo de zonas.
   - **Mapa de Inversión (`MapScreen.kt`):** Mapa interactivo embebido vía Leaflet.js y CARTO/OpenStreetMap que dibuja las zonas disponibles, aplica coropletas por indicador, etiquetas, leyenda, selección y navegación a la ficha técnica. Se corrigió el WebView en blanco con `MATCH_PARENT`, `invalidateSize`, carga por `onPageFinished` y diagnóstico de mosaicos.
   - **Ficha de Zona (`ZoneDetailScreen.kt`):** Visualización exhaustiva de indicadores sociodemográficos con márgenes de error, fuente oficial y botón de guardado a Watchlist con alertas Snackbar.
   - **Guardados (`SavedScreen.kt`):** Pestañas internas para consultar zonas en seguimiento y propiedades dadas de alta.
   - **Campo (`FieldScreen.kt`):** Visualización de sesiones de recorrido sincronizadas desde el reloj inteligente.
   - **Perfil (`ProfileScreen.kt`):** Datos del usuario, información de compilación y cierre de sesión seguro.
5. **Orquestador Principal:** `MainActivity.kt` que evalúa reactivamente el estado de autenticación (Cargando → Login → Dashboard Principal).

---

## 3. Estado Actual del Proyecto

| Componente | Estado | Detalle |
|---|---|---|
| **Base de Datos (MySQL 8)** | ✅ Operativa | Esquema completo con 4 migraciones. Los datos geográficos actuales requieren sustitución oficial. |
| **Backend REST (Express)** | ✅ 100% Funcional | 74/74 pruebas pasando. Endpoints de Auth, Zonas, Watchlist, Propiedades y Sync activos. |
| **Código Android (Etapa 5)** | ✅ Escrito al 100% | Código Compose, ViewModels y navegación implementados. |
| **Compilación Android (Etapa 5)** | ✅ Confirmada | El usuario reportó `BUILD SUCCESSFUL` para `:app:assembleDebug` y `:wear:assembleDebug`. |
| **Mapa Leaflet/CARTO** | ✅ Cargado y confirmado visualmente | WebView, Leaflet local, coropleta, leyenda, etiquetas, controles y diagnóstico corregidos. |
| **Datos geográficos oficiales** | ⚠️ Requiere corrección | Los JSON locales contienen límites sintéticos rectangulares y nombres comerciales inventados. |
| **Importador oficial TIGERweb + ACS** | ⚠️ Descubrimiento TIGERweb cerrado | El importador y `--dry-run` estan implementados. ACS queda pendiente sin clave nueva porque el proyecto es universitario y no sera publico. |

---

## 4. Pasos Faltantes para Completar el Proyecto (Roadmap Restante)

Para culminar la especificación general del producto TerraScope, restan las siguientes etapas:

```
┌─────────────────────────┐
│ Validar Compilación     │  <-- Paso inmediato actual
│      (Etapa 5)          │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        Etapa 6          │
│   Opportunity Finder    │  Scoring territorial explicable y versionado
│   (Motor de Puntuación) │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        Etapa 7          │
│    Property Explorer    │  CRUD móvil de propiedades y comparador de zonas
│      y Comparador       │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        Etapa 8          │
│   Wear OS (Obligatorio) │  App de reloj para recorridos físicos y Data Layer
│     Investor Scout      │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        Etapa 9          │
│  Reportes y Asistencia  │  Generación de reportes ejecutivos con soporte IA
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        Etapa 10         │
│   Calidad y Portafolio  │  Pruebas E2E, demo escolar y cierre de entrega
└─────────────────────────┘
```

---

### Paso Inmediato: Sustituir los datos sintéticos por datos oficiales

- **Estado:** Android ya compila y el mapa ya carga, pero los límites y nombres presentes en MySQL todavía no fueron sustituidos por el lote oficial.
- **Acción:** Completar el cruce ACS cuando existan datos oficiales disponibles; no solicitar ni configurar una clave nueva.
- **Primera ejecución requerida:** modo `--dry-run`, sin aplicar migraciones ni escribir en MySQL.
- **Criterio previo:** validar los GEOID descubiertos por geometría, el place municipal, geometrías TIGERweb, indicadores ACS, nombres oficiales, periodos y trazabilidad; no imponer por adelantado una lista fija de tracts.
- **Criterio de cierre:** obtención y aprobación explícita del usuario antes de ejecutar la importación real.
- **Cambio ya realizado:** el mapa profesional sí quedó implementado antes de la sustitución oficial para resolver la experiencia en blanco.

---

### Etapa 6 — Opportunity Finder y Metodología de Scoring Territorial
**Objetivo:** Crear un algoritmo transparente que evalúe qué tan atractiva es una zona según un perfil de inversión (ej. comercio minorista / servicios locales).
1. **Definición de variables disponibles:** Población cercana, nivel de ingreso mediano, concentración de hogares y densidad.
2. **Metodología de scoring transparente:** Fórmula matemática documentada (ponderaciones de 0 a 100) que evite "cajas negras".
3. **Indicador de confianza y datos faltantes:** Si faltan datos en una zona, el sistema debe alertar "Datos insuficientes" en vez de arrojar una calificación engañosa.
4. **Implementación en Backend:** Endpoint `GET /api/zones/:id/score` o cálculo dentro de la consulta de zona.
5. **Desglose en la interfaz Android:** Gráficos de barras o barras de progreso en la ficha de zona mostrando qué factores suman o restan a la calificación.

---

### Etapa 7 — Property Explorer y Comparación de Zonas
**Objetivo:** Permitir al usuario registrar inmuebles o terrenos concretos y contrastar dos ubicaciones lado a lado.
1. **Formulario de alta de propiedad en Android:** Interfaz para registrar título, coordenadas (GPS o toque en mapa), precio en USD, superficie (m²/sqft) y notas.
2. **Contexto territorial automático:** Al guardar la propiedad, el backend la ubica dentro de un Census Tract de Somerton y muestra las estadísticas de esa zona.
3. **Módulo de comparación lado a lado:** Pantalla para seleccionar 2 Census Tracts y ver una tabla comparativa de sus indicadores (diferencia de población, delta de ingresos, puntuación).

---

### Etapa 8 — Wear OS: Investor Scout (Módulo Obligatorio de Reloj)
**Objetivo:** Brindar la experiencia de campo durante recorridos físicos por Somerton.
1. **Desarrollo en `android/wear` con Wear Compose:** Interfaz adaptada a pantallas circulares (optimizada para el emulador *Wear OS Large Round*).
2. **Integración con Wearable Data Layer API:** Sincronización local entre el reloj y el teléfono conectado.
3. **Funcionalidades del reloj:**
   - **Consulta de Watchlist:** Ver las zonas guardadas directamente en la muñeca.
   - **Registro rápido de observación:** Botón para marcar un punto geográfico con notas rápidas ("Local desocupado", "Zona con alto tráfico vehicular", "Terreno baldío").
   - **Sesión de recorrido:** Iniciar y finalizar recorrido de campo con conteo de puntos relevados.
4. **Sincronización tolerante a fallos:** Guardado local en el reloj (SQLite/Room) y sincronización idempotente hacia el backend cuando haya conectividad.

---

### Etapa 9 — Reportes Ejecutivos e IA Asistiva
**Objetivo:** Convertir los datos analizados en documentos comprensibles para toma de decisiones o presentación escolar.
1. **Generador de fichas ejecutivas:** Estructurar un reporte resumen por zona (indicadores, factores a favor, advertencias de riesgo y recomendaciones de verificación física).
2. **Capa de síntesis con IA (opcional y controlada):** Asistente que redacta un resumen cualitativo a partir de los datos exactos del Census, sin inventar cifras ni alucinar fuentes.
3. **Exportación o vista de impresión:** Formato limpio y exportable para sustentar la viabilidad del proyecto.

---

### Etapa 10 — Calidad, Seguridad, Demostración y Cierre de Portafolio
**Objetivo:** Dejar el proyecto listo para calificación escolar, examen o portafolio profesional.
1. **Pruebas integrales de extremo a extremo:** Flujo completo: Reloj registra punto en campo ➔ Teléfono sincroniza ➔ Backend procesa en MySQL ➔ Aparece en el mapa.
2. **Limpieza y datos de demostración predefinidos:** Asegurar que un usuario nuevo pueda iniciar la app y ver datos completos y realistas de Somerton inmediatamente.
3. **Documentación final de entrega:**
   - Manual de puesta en marcha paso a paso en `README.md`.
   - Guía de arquitectura y decisiones técnicas (ADRs).
   - Grabación o capturas del flujo funcionando simultáneamente en teléfono y reloj.

---

## 5. Resumen de Archivos Clave del Repositorio

- **Control de estado:** `docs/PROJECT_STATUS.md`
- **Especificación canónica:** `docs/TerraScope_Especificacion_y_Plan_de_Desarrollo.md`
- **Código Android (App):**
  - `android/app/src/main/java/com/terrascope/app/MainActivity.kt` (Punto de entrada)
  - `android/app/src/main/java/com/terrascope/app/ui/navigation/MainNavigation.kt` (Navegación y tabs)
  - `android/app/src/main/java/com/terrascope/app/ui/screen/` (Pantallas Compose: Auth, Home, Map, ZoneDetail, Saved, Field, Profile)
  - `android/app/src/main/java/com/terrascope/app/data/network/TerraScopeApi.kt` (API REST)
  - `android/app/src/main/java/com/terrascope/app/data/local/TokenStore.kt` (Persistencia DataStore)
- **Código Backend:**
  - `backend/src/server.js` y `backend/src/app.js` (Servidor Express)
  - `backend/src/routes/` y `backend/src/repositories/` (Lógica de negocio y consultas SQL)
  - `backend/db/migrations/` (Esquema relacional y geoespacial)
- **Código Wear OS:**
  - `android/wear/` (Módulo listo para implementar la Etapa 8)
