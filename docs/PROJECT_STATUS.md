# TerraScope - Estado del proyecto

**Última actualización:** 2026-11-07
**Etapa actual:** 5 - Android: Experiencia principal. Mapa y Watchlist funcionando con datos oficiales; rediseño minimalista aplicado a todas las pantallas principales.

## Etapa 7A — Comparador de zonas — 2026-11-08
- Pantalla `CompareScreen.kt` (ruta `compare`, botón "Comparar dos zonas" en la pestaña Invertir): se eligen dos zonas censales (A y B; si se elige la misma en ambos lados se intercambian) y se ve una tabla lado a lado con puntaje, población, hogares, ingreso mediano, edad mediana y densidad. El valor mayor se resalta, salvo en edad mediana, que no tiene un "mejor".
- Tarjeta de veredicto: "Conviene más: Zona X" con los puntos de cada una, según el **perfil elegido en Invertir** (compartido con `InvestSelection`). Si falta algún dato, dice "Datos insuficientes para puntuar" y empate cuando los puntos son iguales.
- No requiere cambios de backend: reutiliza `GET /api/zones`, `GET /api/zones/:id` y `GET /api/zones/:id/score?profile=`.
- Sin probar en el teléfono (probado por el usuario hasta la pestaña Invertir).
- Probado por el usuario: el comparador funciona.

## Etapa 7B — Property Explorer — 2026-11-08
- Pestaña Guardados → Propiedades → botón **Registrar propiedad** (ruta `property/new`, `PropertyFormScreen.kt`): nombre, tipo (terreno / comercial / residencial / otro), ubicación, precio de venta opcional, superficie con unidad (m², ft², acres) y notas.
- La ubicación se elige **tocando el mapa** (`assets/map/picker.html` dentro de un diálogo a pantalla completa, limitado al área de Somerton, con la misma clave de CARTO de `config.js`). No se usa GPS: no hacen falta permisos y funciona aunque el usuario no esté físicamente en Somerton.
- Al guardar, el backend (`POST /api/properties`, ya existente) ubica la propiedad en su zona con `ST_Contains` y la app abre la ficha de esa zona (contexto territorial: indicadores y puntaje). Si queda fuera de las zonas, la tarjeta dice "Fuera de las zonas cubiertas".
- Lista de propiedades: tarjeta con tipo, coordenadas, superficie y precio; tocar abre la zona; icono para eliminar (`DELETE /api/properties/:id`).
- **Error corregido:** `fetchProperties` leía `label`, `areaM2` y `priceUsd`, pero el backend responde `name`, `areaValue` y `askingPrice`; la pestaña Propiedades nunca habría cargado. `PropertyItem` y el parser se alinearon con el contrato real.
- Sin cambios de backend. Sin probar en el teléfono.
## Etapa 8A — App del reloj (Wear OS): marcar un punto con un toque — 2026-11-08
- **Decisión de diseño (cambia `docs/design/wear-os-screens.md`, W4–W7):** se eliminó el "recorrido" como paso obligatorio. Caso de uso real: el inversionista maneja, ve un letrero de renta o un terreno y quiere guardarlo con **un solo toque**. El backend ya acepta puntos sin sesión (`sessionId` opcional), así que no se pierde nada.
- Plan de la Etapa 8: **8A** app del reloj con guardado local (esta); **8B** sincronización reloj → teléfono (Data Layer) → backend `POST /api/field/sync`, y la pestaña Campo del teléfono pasa a listar **puntos** (`GET /api/field/observations`) con la opción de convertir un punto en propiedad para ver el veredicto de su zona; **8C** zonas guardadas en el reloj (teléfono → reloj) y pulido.
- Flujo del reloj (módulo `android/wear`, Wear Compose Material 3): Inicio con botón grande **"Marcar aquí"** → "Ubicando…" → el punto se guarda al instante (ubicación + hora + UUID) con vibración de confirmación y la etiqueta por defecto `needs_research` ("Investigar") → pantalla "Punto guardado" con 6 etiquetas opcionales (terreno potencial, local comercial, competidor, acceso vial, riesgo, investigar); si no se toca nada, vuelve sola al inicio a los 8 s. La etiqueta no puede quedar vacía porque el backend la exige.
- Seguridad: pensado para usarse manejando solo con el toque; la clasificación se hace después.
- Datos locales (`data/FieldStore.kt`, SharedPreferences + JSON): puntos con UUID, fecha ISO 8601 UTC, etiqueta, coordenadas y bandera `synced`; el id del reloj (`wear-xxxxxxxx`) cabe en los 40 caracteres del backend.
- Ubicación (`data/LocationHelper.kt`): una lectura puntual por toque (fusionado/GPS/red con límite de 8 s y último valor conocido como respaldo); el permiso se pide en el primer toque. Si no hay ubicación **no se guarda** el punto y se avisa (el backend exige coordenadas válidas y no se inventan).
- Probar en el emulador de reloj: Extended controls → Location → fijar (32.5975, -114.7100) y "Send"; aceptar el permiso la primera vez.
- Sin probar. Todavía no se envía nada al teléfono (8B).

## Etapa 7C — Veredicto de la propiedad y pines en el mapa — 2026-11-08
- Objetivo del módulo: alguien ve una oferta (terreno, local…) en Somerton, la registra con su ubicación y la app le dice qué tal está la zona donde cae, según su perfil de inversión.
- `PropertyVerdictScreen.kt` (ruta `property/{propertyId}`): al guardar una propiedad (o al tocarla en la lista o en el mapa) se muestra el veredicto: puntaje 0–100 de su zona, lugar en el ranking ("#1 de 5 para Restaurante"), cuál es la mejor zona si no es esa, confianza de los datos, y los datos de la oferta (precio, superficie y **precio por unidad de superficie**). Casos cubiertos: fuera de las zonas cubiertas, dentro de la ciudad pero sin tract puntuado, y datos insuficientes. Botones a la ficha de la zona y al comparador.
- Mapa: cada propiedad del usuario aparece como un pin dorado con borde verde (`TerraMap.setProperties` en `map.html`, capa propia sobre los polígonos), con el nombre al tocarlo, y al tocar el pin se abre su veredicto. Los pines solo muestran las propiedades del usuario (el backend filtra por `user_id`) y se recargan al volver a la pestaña Mapa.
- Lista Guardados → Propiedades: tocar una propiedad abre su veredicto.
- Sin cambios de backend. Sin probar en el teléfono.
- Pendiente (opcional): editar una propiedad existente (el backend ya tiene `PUT`), comparar propiedades entre sí y considerar competencia/precios del entorno (requiere datos que hoy no tenemos).

## Etapa 6 (ampliada) — Perfiles de inversión y ranking — 2026-11-07
- El inversionista elige en qué quiere invertir y la app ordena las zonas de Somerton según ese perfil. Todo es explicable: los perfiles usan los mismos 4 indicadores del Census y solo cambian los pesos (siempre suman 100).

| Perfil | Población | Ingreso | Hogares | Densidad |
|---|---|---|---|---|
| Bienes raíces (vivienda) | 20 | 40 | 30 | 10 |
| Restaurante y comida | 35 | 20 | 15 | 30 |
| Comercio minorista | 30 | 30 | 20 | 20 |
| Servicios locales | 25 | 20 | 40 | 15 |

- Método (`backend/src/lib/score.js`, v2): normalización min-max entre los tracts × peso = puntos; el puntaje es la suma. Sin datos completos o con menos de 3 zonas comparables → "Datos insuficientes". La confianza sale del peor coeficiente de variación (<12 % alta, 12–40 % media, >40 % baja). La edad mediana no entra. El place no se puntúa.
- Descripción libre: `GET /api/zones/profiles/match?q=...` sugiere un perfil con una regla de palabras clave (no es IA) y la app muestra qué entendió para que el usuario lo corrija.
- Endpoints nuevos (requieren sesión): `GET /api/zones/profiles`, `GET /api/zones/profiles/match?q=`, `GET /api/zones/ranking?profile=`, `GET /api/zones/:id/score?profile=`.
- Android: pestaña nueva **Invertir** (descripción libre, selector de perfil, "mejor zona", ranking con barras y confianza). El perfil elegido se comparte con la ficha de zona, cuya tarjeta de puntaje muestra el perfil activo.
- Resultado con los datos oficiales actuales (cálculo local con los valores del dry-run): vivienda y servicios → 115.05 primera; restaurante → 115.06 primera; minorista → 115.05 primera; 115.01 queda en 0 en todos (es relativo).
- Limitación declarada en pantalla: solo demografía del Census; no considera competencia, precios ni tráfico. Para ir más allá harían falta datos de comercios existentes (OSM, cobertura escasa).
- Probado: 11 pruebas unitarias en `tests/unit/score.test.js` y `npm run check`. Sin probar: endpoints contra MySQL y la pestaña nueva en el teléfono. Falta documentar los endpoints en `docs/api/openapi.yaml`.

## Resumen de la sesión del 2026-11-07 (qué falló y cómo se resolvió)

| # | Problema | Causa | Solución | Estado |
|---|---|---|---|---|
| 1 | Mapa gris, aviso "El mapa tiene tamaño 0 (360x0)" | El WebView se creaba sin `layoutParams` explícitos dentro de Compose y la página recibía viewport de alto 0; `height:100%` resolvía a 0 | `MapScreen.kt`: WebView con `MATCH_PARENT`; la app mide el `AndroidView` en dp y llama `TerraMap.setSize(w,h)` antes de `setData`. `map.html`: `#map` en `position:absolute` con `top/left/right/bottom:0` y función `setSize` | Probado por el usuario: el mapa dibuja zonas y etiquetas |
| 2 | Marca de agua "API KEY REQUIRED" sobre el mapa | Desde 25-sep-2026 CARTO exige clave gratuita en sus mosaicos raster (no depende del zoom) | `map.html`: constante `CARTO_KEY`, URL `https://basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}.png?key=...`; clave gratuita en carto.com/basemaps/apikey (uso no comercial hasta 5 M de mosaicos/mes). Atribución OSM + CARTO siempre visible | Probado por el usuario: la marca desapareció |
| 3 | Se podía alejar el mapa de Somerton | `minZoom` fijo en 9 y sin límites | `map.html`: `lockToCity()` limita el zoom mínimo a 1 nivel menos que el encuadre de la ciudad y fija `maxBounds` alrededor del place | Implementado, sin probar |
| 4 | "Error al actualizar la lista" al guardar una zona | Android y backend no coincidían: Android hacía `PUT /watchlist/:id` (no existe, 404) y leía `zones`; el backend espera `POST /watchlist` con `{zoneId}` y responde `items[{zoneId, zone:{code,name,level}}]` | `TerraScopeApi.kt`: `addToWatchlist` usa `POST /watchlist`; `fetchWatchlist` lee `items`/`zoneId`/`zone.*`. Esto también arregla las pestañas Guardados e Inicio, que siempre veían la lista vacía | Implementado, sin probar |
| 5 | Datos sintéticos / zonas falsas | Lote inicial con geometrías rectangulares y nombres inventados | Importación oficial TIGERweb + ACS 2020–2024 | Ver sección siguiente |

## Importación oficial ejecutada y verificada — 2026-11-07
- La importación real **ya se ejecutó** con `CENSUS_API_KEY` definida en la terminal. Los respaldos están en `backend/data/raw/2024/` (hash del lote `844d03c4d4c6feb92c00a10f3426cbc3a1dd1c6662ebfe583808b866d704f9a7`).
- Verificado en MySQL con `SELECT id, code, name, level FROM terrascope.zones`: 6 zonas, `0468080` Somerton city (place) y los tracts `04027011501`, `04027011503`, `04027011505`, `04027011506`, `04027011802`. Las zonas falsas `04027011502` y `04027011403` ya no existen.
- Un `--dry-run` posterior confirmó: 6 zonas coincidentes, 30 valores de indicadores coincidentes, ninguna zona falsa, ACS 67 tracts + 1 place sin faltantes. **No se debe volver a importar.**
- Datos ACS de Somerton city: 14,574 personas, 4,964 hogares, ingreso mediano $74,015, edad mediana 31.3.
- Observaciones sin bloquear: el tract 118.02 solo tiene 9.55 % dentro del place; la suma de poblaciones de los 5 tracts (19,076) supera la del place (14,574) porque los tracts salen del límite urbano; la comparación `ST_Intersection` reporta 44 % de diferencia contra el área oficial, pero la suma por porcentajes da ~19 km² (casi igual a AREALAND), por lo que se atribuye a la operación espacial y no a los datos (hipótesis sin comprobar). En el dry-run, la densidad aparece con etiqueta `undefined` solo al imprimir.
- `CENSUS_API_KEY` puede guardarse en `backend/.env` (el importador carga `dotenv`); `.env` está en `.gitignore`. Los scripts de `scripts/` no cargan `.env`.

## Rediseño minimalista del mapa — 2026-11-07
- Referencia: tarjetas blancas flotantes, esquinas muy redondeadas, jerarquía tipográfica clara y poco ruido.
- `MapScreen.kt`: encabezado compacto (título "Somerton", chips en píldora sin borde, leyenda fina de 6 dp), controles de zoom de 40 dp, ficha inferior con valor grande, margen y confiabilidad, fila de indicadores secundarios sobre fondo suave y botón en píldora. Atribución más discreta.
- `map.html`: coropleta más translúcida (0.6), bordes finos, etiquetas más pequeñas y límite de ciudad más sutil.
- Probado por el usuario en el teléfono: funciona.

## Rediseño minimalista del resto de pantallas — 2026-11-07
- Mismo lenguaje visual que el mapa: títulos grandes sobre fondo claro, tarjetas blancas con esquinas de 22–28 dp y sombra suave, sin emojis.
- `HomeScreen.kt`: saludo + "Explora Somerton", tarjeta verde con población, ingreso y número de zonas (ya dinámico: antes decía 4 fijo), carrusel de zonas guardadas y lista de zonas con chevron.
- `SavedScreen.kt`: control segmentado Zonas / Propiedades en lugar de `TabRow`.
- `FieldScreen.kt`: tarjetas de sesión con estado en píldora.
- `ZoneDetailScreen.kt`: encabezado con nivel, nombre y GEOID; botón principal Guardar/Guardada; indicadores en cuadrícula de 2 columnas con margen de error, confiabilidad y periodo.
- Funciones compartidas nuevas en `HomeScreen.kt`: `shortZoneName` y `levelLabel`.
- Implementado, sin compilar ni probar en teléfono.

## Mapa profesional cargado — 2026-11-07
- La pantalla de teléfono ya usa Leaflet.js dentro de un `WebView` de Android y carga correctamente el mapa base.
- El proveedor implementado es CARTO **`light_all`**, derivado de OpenStreetMap. Se usa un mosaico claro adecuado para la coropleta:
  `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png`.
- El WebView carga `android/app/src/main/assets/map/map.html`, con Leaflet local, CSS local y JavaScript local.
- Se corrigieron las causas que dejaban el mapa en blanco:
  - La clase de puente JavaScript quedó protegida con `@Keep`.
  - La página se considera lista tanto por el puente como por `onPageFinished`.
  - Se fija un layout `MATCH_PARENT` para evitar un viewport de alto cero.
  - Se recalcula el tamaño real en Kotlin y en JavaScript con `invalidateSize()` y `setSize()`.
  - Se cambió el proveedor OSM directo por CARTO porque `tile.openstreetmap.org` bloqueaba peticiones desde `file://` o fallaba por la política del proveedor.
  - Se añadieron diagnósticos con Logcat y una advertencia visual si no hay zonas, el viewport mide cero o falla el mapa base.
- La interfaz ya muestra selector de población, ingreso, edad y densidad, leyenda, etiquetas Census, controles de zoom y botón de centrar.
- Se confirmó visualmente que el mapa carga y muestra los polígonos/etiquetas correspondientes a los datos presentes en MySQL.
- La visualización todavía debe validarse nuevamente después de reemplazar los datos sintéticos por el lote oficial aprobado.

## Importación oficial preparada — 2026-11-07
- Implementado el importador real de `backend/scripts/import-somerton-official-data.js`, protegido por `--expect-hash`, confirmación `IMPORTAR` y transacción única.
- Añadida la migración `005_overlap_indicator.sql` sin modificar 001–004.
- Periodos fijados: `ACS 5-year 2020–2024` y `TIGERweb ACS 2024`.
- `population_density` se registra derivada para tracts y place.
- (Histórico) Esta ejecución real ya se corrió: ver "Importación oficial ejecutada y verificada".

## Ajustes mínimos posteriores — 2026-11-07
- Se eliminó `overlap_pct` del lote y de la escritura de indicadores. Conserva la migración `005_overlap_indicator.sql` sin modificaciones.
- `population_density` continúa como indicador derivado.
- `--expect-hash` pasó a ser opcional:
  - Sin `--expect-hash`, la ejecución real imprime el hash del lote y los cuatro hashes de archivos descargados antes de solicitar escribir `IMPORTAR`.
  - Con `--expect-hash`, el hash recalculado debe coincidir exactamente.
- Se conservaron la transacción, el rollback y las verificaciones posteriores.
- Eliminados:
  - `backend/scripts/load-somerton-data.js`
  - El script `db:load-somerton` de `backend/package.json`
  - `data/somerton_tracts_tiger.json`
  - `data/somerton_place_tiger.json`
  - `data/somerton_acs_data.json`
- `npm run check` aprobó con sintaxis correcta en 42 archivos.
- `npm test` aprobó con 85 pruebas y 0 fallos.
- No se ejecutaron el dry-run ni la importación real.

## Resultado de implementación y dry-run — 2026-11-07

- `npm run check` pasó correctamente: sintaxis válida en 43 archivos.
- `npm test` pasó con 85 pruebas aprobadas y 0 fallidas.
- Se completó el dry-run sin escribir en MySQL ni crear archivos ni respaldos.
- TIGERweb ACS 2024 respondió correctamente:
  - Place `0468080` / Somerton city.
  - Tracts descubiertos por intersección espacial: `04027011506`, `04027011505`, `04027011802`, `04027011503` y `04027011501`.
- Todas las seis geometrías fueron evaluadas como válidas con `ST_IsValid`, SRID 4326 y `axis-order=long-lat`.
- El lote actual calculó el hash SHA-256 `16f9b494c1e6a7e72cfb6a86ac9b7ae2913c5a8bb4bc632c8e4f0dc2751d79da`.
- El hash aprobado previamente sigue registrado como `53476dd07993c4435611f8f73cba1e438cf98aef5e01500f4b17313b40c81458`; no se debe ejecutar la importación real hasta resolver formalmente la diferencia de hash.
- ACS no pudo completarse en el dry-run porque `CENSUS_API_KEY` no está definida en la sesión de ejecución:
  - Los cinco tracts quedaron sin población, hogares, ingreso mediano ni edad mediana.
  - No hubo datos ACS de Yuma para cruzar con los tracts descubiertos.
- La importación real quedó protegida para abortar antes de confirmar, respaldar o escribir si faltan datos ACS.
- En la base actual se detectaron las dos zonas falsas sin dependencias nuevas:
  - `id=2`, `04027011502`: 0 watchlist_items, 0 properties, 5 indicator_values.
  - `id=4`, `04027011403`: 0 watchlist_items, 0 properties, 5 indicator_values.
- Aún no se eliminan esas zonas ni se ejecuta la limpieza sintética.
- El código de importación real acepta `--expect-hash` opcionalmente. Sin él imprime el hash del lote y los cuatro archivos descargados; siempre solicita escribir exactamente `IMPORTAR`, salvo `--yes` únicamente en terminal sin entrada interactiva.
- Comando preparado, sin ejecución real:

```powershell
cd backend
npm run db:import-somerton -- --expect-hash <hash-aprobado>
```

## Completado

- Lectura de la especificación y del diseño base de Figma (React web; solo referencia visual).
- Investigación de fuentes: Census/ACS/TIGER, OSM, Yuma County GIS, City of Somerton, Data USA, mercado inmobiliario.
- Documentos: `docs/research/data-sources.md`, `docs/research/data-availability-matrix.md`, `docs/product/mvp-scope.md`, `docs/product/risks-and-decisions.md`.
- Etapa 1: `docs/product/user-flows.md`, `docs/design/design-system.md`, `docs/design/android-screens.md`, `docs/design/wear-os-screens.md`.

## Inspección y corrección de datos reales — 2026-11-07

- [x] Revisados `backend/db/migrations/004_indicators.sql`, las rutas `/api/zones`, el repositorio de zonas y el importador actual.
- [x] Confirmado el contrato JSON usado por Android:
  - `/api/zones` devuelve `id`, `code`, `name` y `level`.
  - `/api/zones/:id` devuelve `boundary` como objeto GeoJSON.
  - `boundary` acepta `Polygon` o `MultiPolygon`.
  - Android espera indicadores como `code`, `name`, `value`, `unit`, `marginOfError` y `referencePeriod`.
- [x] Confirmado que `004_indicators.sql` puede conservarse sin cambios.
- [x] Confirmado que las geometrías actuales son rectangulares y sintéticas en `data/somerton_tracts_tiger.json` y `data/somerton_place_tiger.json`.
- [x] Confirmado que existen nombres inventados en `backend/scripts/load-somerton-data.js`.
- [x] Creado el plan técnico completo en `docs/REAL_DATA_IMPORT_PLAN.md`.
- [x] Implementar importador oficial TIGERweb con descubrimiento de tracts.
- [x] Ejecutar comprobacion `--dry-run` sin escribir en MySQL ni crear archivos.
- [x] ACS completado con `CENSUS_API_KEY` del usuario.
- [x] Geometrías y nombres sintéticos reemplazados en MySQL (verificado).

### Regla de priorización

Los datos reales siguen siendo la prioridad funcional, pero el mapa profesional ya quedó implementado en Android.
Antes de la importación real deben aprobarse datos ACS/TIGERweb y reconciliarse el hash. La importación debe preservar IDs, dependencias y realizar rollback.
Después de la importación real se debe volver a validar el mapa contra nombres, límites y indicadores oficiales.

## Etapa 2 - progreso

- [x] Paso 1: entorno del usuario (Node 26.7.0, npm 11.19.0, Java 17.0.12, Git 2.55.0; MySQL instalado pero fuera del PATH).
- [x] Paso 2: estructura del repo, `.gitignore`, README y backend Express con `GET /api/health`. **Implementado y probado por el usuario**: `status: ok`, MySQL 8.0.46, base `terrascope` creada.
- [x] Paso 3: proyecto Android Studio con módulos `app` y `wear` (Kotlin + Compose, AGP 9.4.1). **Probado por el usuario**: sync correcto y ambos módulos corriendo a la vez en emulador (capturas del usuario). `applicationId` del reloj = `com.terrascope.app`. Versiones en `docs/architecture/versions.md`.
- [x] Paso 4: `backend/db/schema.sql` (data_sources, zones, places con índices espaciales) y scripts `npm run db:setup` / `npm run db:verify`. **Probado por el usuario**: 6 verificaciones OK en MySQL 8.0.46 (ejes, distancia esférica vs haversine, ST_Contains, ST_Area, índices SPATIAL).
- [x] Paso 5: mapa implementado con **Leaflet.js en WebView y mosaicos CARTO/OpenStreetMap** (ADR-001 actualizado). No requiere clave propia de mapas ni dependencias nativas adicionales. Se confirmó visualmente que carga en teléfono/emulador, muestra polígonos, leyenda, etiquetas y controles.
- [x] Paso 6: guía `docs/setup/emulator-pairing.md`. **Probado por el usuario** (captura de *Paired Devices*): *Medium Phone API 37.0* y *Wear OS Large Round* (Android 17) emparejados, estado Online.
- [x] Paso 7: README completo. **Probado por el usuario**: `.\gradlew.bat :app:assembleDebug :wear:assembleDebug` -> BUILD SUCCESSFUL (Gradle 9.6.0).
- [x] Paso 8: informe de cierre y `docs/architecture/architecture.md`.

## Etapa 3 - progreso

- [x] Entrega A: migraciones (users, properties, watchlist_items, field_sessions, field_observations), migrador, autenticación (register/login/me, scrypt + JWT), validación, errores con formato único, rate limit. ADR-002 y `docs/architecture/data-model.md`. **Probado por el usuario**: `db:setup` aplicó 3 migraciones y `npm test` dio 25/25 (10 de integración contra MySQL + 15 unitarias).
- [x] Entrega B: propiedades (CRUD con asociación automática a zona), zonas (lista), Watchlist, sincronización de campo por lotes e idempotente (sesiones y observaciones), autorización por dueño, conexión y sesión MySQL en UTC, OpenAPI (`docs/api/openapi.yaml`). **Probado por el usuario**: `npm test` con 68/68 pruebas pasando (34 integración contra MySQL + 34 unitarias). Etapa 3 cerrada con éxito.

## Etapa 4 - Cerrada ✅

- [x] Crear el flujo de carga y trazabilidad de geometrías e indicadores.
- [x] Extraer y estructurar el conjunto inicial de indicadores del Census ACS.
- [x] Migración `004_indicators.sql` con tablas `indicators` e `indicator_values`.
- [x] Conversión espacial `polygonGeoJsonToWkt` en `src/lib/geo.js` con soporte para Polygon y MultiPolygon.
- [x] Endpoints y repositorio actualizados (`GET /api/zones`, `GET /api/zones/indicators`, `GET /api/zones/:id`).
- [x] Especificación OpenAPI actualizada.
- [x] Script de carga automática `backend/scripts/load-somerton-data.js`.
- [x] **74/74 pruebas pasando (37 unitarias + 37 de integración contra MySQL)**.
- [ ] **Revisión posterior obligatoria:** reemplazar geometrías sintéticas y nombres inventados por límites TIGERweb y nombres oficiales.

## Etapa 5 - Android: Experiencia principal

- [x] **Paso A — Dependencias** (`libs.versions.toml` + `app/build.gradle.kts`):
  - Navigation Compose 2.8.5
  - ViewModel + Lifecycle-runtime-compose 2.8.7
  - DataStore Preferences 1.1.1
  - Kotlinx Coroutines Android 1.9.0
  - Activity Compose 1.9.3
- [x] **Paso B — Autenticación persistente**:
  - `data/local/TokenStore.kt` — DataStore: guarda/recupera JWT, email y displayName.
  - `data/network/TerraScopeApi.kt` — Refactorizado: login, register, fetchMe, fetchZones, fetchZoneDetail, fetchWatchlist, addToWatchlist, removeFromWatchlist, fetchProperties, fetchFieldSessions.
  - `ui/screen/AuthScreen.kt` — Pantalla login/registro con toggle, validación visual, estado de carga y errores.
  - `ui/viewmodel/AuthViewModel.kt` — Verifica token guardado al inicio, login, register, logout, clearError.
- [x] **Paso C — Navegación principal**:
  - `ui/navigation/MainNavigation.kt` — NavHost con 4 tabs (Inicio, Mapa, Guardados, Campo) + ruta `zone/{id}` + ruta `profile`.
- [x] **Paso D — Tab Inicio**:
  - `ui/screen/HomeScreen.kt` — Tarjeta resumen Somerton, watchlist guardada, lista completa de zonas. Toque → ficha de zona.
  - `ui/viewmodel/ViewModels.kt` → `HomeViewModel`
- [x] **Paso E — Tab Mapa**:
  - `ui/screen/MapScreen.kt` — WebView con Leaflet.js + OSM tiles. Polígonos de los 4 tracts con colores diferenciados. Popup con botón → navega a ficha de zona vía scheme `terrascope://zone/{id}`.
  - `ui/viewmodel/ViewModels.kt` → `MapViewModel`
- [x] **Paso F — Ficha de zona**:
  - `ui/screen/ZoneDetailScreen.kt` — Indicadores en tarjetas (valor grande, margen de error, período). Botón guardar/quitar de watchlist con Snackbar de confirmación.
  - `ui/viewmodel/ViewModels.kt` → `ZoneDetailViewModel`
- [x] **Paso G — Tab Guardados**:
  - `ui/screen/SavedScreen.kt` — Tabs internas: Zonas (watchlist con borrado) y Propiedades (lista de propiedades del usuario).
  - `ui/viewmodel/ViewModels.kt` → `SavedViewModel`
- [x] **Paso H — Tab Campo**:
  - `ui/screen/FieldScreen.kt` — Lista de sesiones sincronizadas desde Wear OS con estado (completada/en curso) y conteo de observaciones.
  - `ui/viewmodel/ViewModels.kt` → `FieldViewModel`
- [x] **Paso I — Perfil**:
  - `ui/screen/ProfileScreen.kt` — Avatar con inicial, email, info de versión/backend, botón "Cerrar sesión" con diálogo de confirmación.
- [x] **Paso J — MainActivity refactorizado**:
  - `MainActivity.kt` — Orquesta el flujo: Checking → AuthScreen / MainNavigation. AuthViewModel con `by viewModels()`.
- [x] **Paso K — Compilar y verificar**: confirmado por el usuario con `BUILD SUCCESSFUL` para `:app:assembleDebug :wear:assembleDebug`.

## Modelos de datos Android (actualizados)

`data/model/Models.kt`:
- `ZoneItem`, `ZoneIndicator`, `ZoneDetail` (con `boundaryGeoJson?`)
- `PropertyItem`, `AuthUser`, `FieldSession`

## Decisiones aprobadas por el usuario

- Trabajo por pasos; entregas en .zip solo con archivos nuevos/modificados.
- Carpeta del proyecto: `C:\Users\ferba\Documents\ProyectosUT\inversiones_somerton`.
- Base de datos: **MySQL** (con MySQL Workbench).
- El proyecto es para una actividad/examen escolar: alcance reducido.
- Stack propuesto (delegado al asistente, **pendiente de confirmar al cerrar la Etapa 1**): Android Studio con app móvil y Wear OS en un solo proyecto Kotlin + Compose; backend Node.js + Express + MySQL (corre aparte).
- Diseño: minimalista, claro, premium (aplicado en la Etapa 1; tema claro principal en teléfono, oscuro en reloj).
- **Wear OS es obligatorio**: la app móvil debe estar conectada con el reloj.
- **Mapas**: Leaflet.js + OSM en WebView (sin API key). ADR-001 actualizado.

## Problemas conocidos

- Sin probar en teléfono: rediseño de Inicio, Guardados, Campo y ficha de zona.
- Usar solo el Census como fuente demográfica (los agregadores mezclan años y geografías).
- Cobertura de negocios en OpenStreetMap escasa; complementar con captura manual o datos de demostración marcados.
- El mapa requiere Internet y la clave gratuita de CARTO (`CARTO_KEY` en `map.html`); cuidar el tope de 5 M de mosaicos/mes y no compartir la clave con otros proyectos.
- Densidad: se calcula contra el área total del tract, por lo que resulta baja en tracts extensos (ya se avisa en la leyenda).
- `docs/architecture` y ADR-001 pueden seguir mencionando MapLibre; la realidad es Leaflet en WebView.

## Etapa 8B — Sincronización reloj → teléfono → backend (sin probar)

- Reloj: un toque guarda el punto (GPS + etiqueta por defecto `needs_research`) y lo publica como DataItem `/field/points` (Data Layer).
- Teléfono: `FieldSyncService` recibe el DataItem, sube a `POST /api/field/sync` con el token y responde con mensaje `/field/ack`; el reloj marca los puntos como sincronizados.
- Pestaña Campo: lista los puntos (`GET /api/field/observations`), botón "Sincronizar" y "Evaluar este punto" (abre el formulario de propiedad con ubicación prellenada).
- Las sesiones de recorrido (`FieldSession`, `fetchFieldSessions`) quedan como legado sin uso.
- Editar/borrar puntos desde el teléfono: `PUT` y `DELETE /api/field/observations/:id` (solo nombre y etiqueta; la ubicación no se edita). El nombre se guarda en `note`.
- Fix: reenviar un punto con otra etiqueta ahora la actualiza (antes el backend lo ignoraba como duplicado). El teléfono borra el DataItem del reloj tras confirmarlo, para no re-subir puntos ya editados o borrados.
- Pendiente: probar con ambos emuladores emparejados; 8C (zonas guardadas en el reloj).

## Etapa 8C — Zonas guardadas en el reloj (sin probar)

- Teléfono -> reloj por Data Layer: `PhoneZoneSync` publica el DataItem `/zones/saved` con la watchlist y el puntaje de cada zona (perfil de inversión elegido). Se reenvía al iniciar sesión, al guardar/quitar una zona y al cambiar de perfil.
- Reloj: botón "Zonas guardadas" en el inicio; lista con puntaje y detalle (puntaje, perfil, confianza). Se guarda localmente (`ZoneStore`) para verlas sin conexión; `ZonesListenerService` recibe las actualizaciones.
- Pendiente: pulido final; edición/comparación de propiedades; documentar endpoints nuevos en `openapi.yaml`.

## Etapa 9A — Reporte por zona (backend)

- `GET /api/zones/:id/report?profile=retail`: reporte determinista (sin IA) con resumen, puntaje, indicadores con margen de error y posición entre las zonas, factores a favor, advertencias, datos faltantes, lista de verificación en campo según el perfil, limitaciones, fuentes y metodología. Cada afirmación cuantitativa lleva `ref` al indicador del que sale.
- Código en `backend/src/lib/report.js` (función pura) con tests en `backend/tests/unit/report.test.js` (6 pasan; el endpoint no se probó contra la base).
- Siguiente: 9B (pantalla de reporte en la app + PDF) y 9C (resumen con IA opcional, validado).

## Etapa 9B — Pantalla de reporte y PDF (sin probar)

- Ficha de zona: botón "Ver reporte" -> pantalla `report/{zoneId}` (`ReportScreen`) con resumen, indicadores, a favor, advertencias, verificación en campo, limitaciones, fuentes y metodología, según el perfil elegido en Invertir.
- Botón "PDF": `ReportPdf` genera el archivo con `PdfDocument` del sistema (sin librerías) y lo comparte con `FileProvider` (`res/xml/file_paths.xml`, proveedor en el manifest).
- Pendiente: reporte desde el veredicto de una propiedad; 9C (resumen con IA opcional).

## Etapa 9C — Resumen con IA (opcional)

- `GET /api/zones/:id/report?ai=1` añade `report.ai = {status, text}`. Solo se activa si el backend tiene `GEMINI_API_KEY` (Google AI Studio, nivel gratuito) o `ANTHROPIC_API_KEY` (variables de entorno del backend, no van en la app ni en el código); si hay ambas, se usa Gemini. Modelo de Gemini por defecto `gemini-3.8-flash`, cambiable con `GEMINI_MODEL` (copiar el nombre desde AI Studio); Anthropic: `claude-haiku-5-5` / `AI_MODEL`.
- Control: la IA recibe solo los datos del reporte; luego `isGrounded` descarta el texto si menciona una cifra que no está en los datos. Sin clave, error, tiempo agotado o texto inválido -> `text: null` y la app muestra solo el reporte de plantilla. Caché en memoria de 1 hora.
- La app y el PDF muestran el párrafo con la etiqueta "Resumen asistido por IA" solo si `status = ok`.
- Tests: `backend/tests/unit/aiSummary.test.js` (con proveedor simulado; no se probó contra la API real).
- Pendiente: pulido del reporte (gráficas, lenguaje más claro) en la etapa final.

## Etapa 9D — Análisis de mis ubicaciones (IA opcional)

- `GET /api/field/analysis?profile=retail&ai=1`: toma los puntos del reloj y las propiedades del usuario, asocia cada una a su zona censal (`findTractIdForPoint`), las ordena por el puntaje del perfil y arma un resumen; con IA añade un párrafo (qué conviene más, qué menos y qué verificar). Misma validación de cifras que el reporte de zona. Código: `backend/src/lib/analysis.js`, tests en `backend/tests/unit/analysis.test.js`.
- App: Campo -> "Analizar mis ubicaciones" (`AnalysisScreen`) con el ranking de ubicaciones y el texto de la IA.
- Backend: `loadTracts` pasó a `src/lib/tractData.js`. Los fallos de la IA ahora se registran en la terminal del backend con prefijo `[ai]`.
- Límite: el puntaje es de la zona, así que dos ubicaciones en la misma zona empatan.

## Rediseño de reportes (v2)

- Pantalla de reporte: medidor circular de puntaje, posición entre las zonas, barras por factor (Alto/Medio/Bajo), indicadores con puntos de posición e insignias de precisión, tarjetas "A favor" / "Hay que cuidar", lista de verificación y sección plegable "Cómo leer este reporte". Lenguaje más sencillo.
- Botón "Reporte IA" (también en "Mis ubicaciones") con animación de carga (destellos, degradado en movimiento, barra y mensajes por pasos), estado de error con reintento y tarjeta de resultado. La IA ya no se pide sola: solo al tocar el botón.
- Backend: `report.score.rank` (posición entre las zonas).
- Pendiente: gráficas en el PDF.

## Etapa 10A — Seguridad y calidad

- Revisión completa documentada en `docs/SEGURIDAD_Y_PRIVACIDAD.md` (qué se revisó, qué se corrigió, riesgos aceptados).
- Backend: cabeceras de seguridad, CORS desactivado por defecto (`CORS_ORIGIN` para habilitarlo), límite por usuario en llamadas con IA.
- Android: respaldo en la nube desactivado y HTTP en claro permitido solo hacia `127.0.0.1`, `localhost` y `10.0.2.2` (si se usa otra IP de la red, hay que ajustar `network_security_config.xml`).
- Tests nuevos: `security.test.js` (requiere `npm install`) y `aiRateLimit.test.js`.
- Prueba manual pendiente: dos cuentas, confirmar que una no ve los datos de la otra.

## Pendientes cerrados + login con foto (sin probar)

- Login/registro rediseñado: foto del letrero de Somerton (`res/drawable-nodpi/somerton_sign.jpg`) con zoom lento, degradado a verde oscuro, tarjeta translúcida con interruptor "Entrar / Crear cuenta" y animación de entrada. La foto original es pequeña (452×573 px); en pantalla completa se ve suave, conviene una de mayor resolución.
- Editar propiedades: botón "Editar" en el veredicto -> formulario con los datos cargados (`property/edit/{id}`, usa `PUT /api/properties/:id`).
- Comparar propiedades: botón en Guardados > Propiedades (con 2 o más) -> pantalla lado a lado con zona, puntaje, precio, superficie en m², precio por m² y un resumen.

## Etapa 10B — Pruebas de extremo a extremo y datos de demostración (sin probar contra la base)

- `npm run db:seed-demo`: usuario demo con propiedades, zonas guardadas y puntos de campo marcados "[DEMO]", dentro de zonas reales (`backend/src/lib/geoPoints.js`, con tests).
- `npm run e2e`: prueba HTTP completa contra el backend en marcha (sincronización del reloj, reenvíos, edición, propiedades, reportes y aislamiento entre dos usuarios).
- Red inestable: el teléfono reintenta subir lo pendiente del reloj al abrir la app y cada minuto mientras está abierta.
- Lista de pruebas manuales y detalles en `docs/PRUEBAS_E2E.md`.

## Etapa 10C — Documentación final + endurecimiento del login

- Documentación: `README.md` reescrito (instalación, claves, IA, emparejado, API, licencias), `backend/.env.example` completo, `docs/ARQUITECTURA_Y_LIMITACIONES.md` y los endpoints nuevos en `docs/api/openapi.yaml` (v0.4.0). Las guías de capturas y video las graba el autor.
- Login/registro: el formulario sube con el teclado (la foto se encoge); contadores 80 (correo) y 20 (contraseña); al registrarse solo se aceptan correos de proveedores reconocidos (Gmail, Outlook, Hotmail, Yahoo, iCloud, etc., y `.edu`/`.edu.mx`) y contraseñas de 8 a 20 con una mayúscula y un número, con requisitos que se ponen en verde. Las mismas reglas se aplican en el backend (`credentialPolicy.js`, con tests). El login no las exige, para no dejar fuera cuentas existentes (p. ej. el usuario demo).
- Las pruebas de integración usan `EXTRA_EMAIL_DOMAINS=terrascope.invalid`; el E2E usa correos `e2e.*@gmail.com`.

## Etapa 10D — Nombres descriptivos de zonas y detalle más claro

- Backend (`lib/zoneLabels.js`): cada zona censal recibe un nombre calculado, no oficial: **Centro** (contiene el centro de la ciudad), **Residencial** (dentro de la ciudad y ≥1,000 hab/km²), **Urbana, baja densidad**, o **Afueras y campo** (su centro queda fuera de los límites de la ciudad), más el punto cardinal respecto al centro de Somerton. Si hay nombres repetidos se añade el número del Census. Sin geometría cae a "Zona N". Los umbrales son criterio nuestro (`DENSE_PER_KM2`), no una clasificación del Census.
- `GET /zones`, `GET /zones/{id}` y `GET /zones/ranking` devuelven `friendly` / `friendlyTitle`.
- App: listas, ranking, guardados, comparador y veredicto muestran el nombre descriptivo con el número del Census como dato secundario. El detalle muestra "En pocas palabras", tarjetas simples (personas, hogares, ingreso típico, gente por km²) con comparación contra el promedio de las zonas y explicación, y deja los datos técnicos del Census en una sección que se despliega.
- Pendiente: mini-mapa de ubicación en el detalle; validar las etiquetas contra la realidad de Somerton.

## Próximo objetivo

1. Probar en el teléfono el rediseño de Inicio, Guardados, Campo y ficha de zona.
2. Reiniciar el backend y probar la pestaña Invertir (perfiles, descripción libre, ranking) y la tarjeta de puntaje en la ficha.
3. Etapa 8B/8C: sincronización reloj → teléfono → backend y zonas guardadas en el reloj.
4. Probar Etapas 7B y 7C en el teléfono (registrar propiedad → veredicto → pin en el mapa).
