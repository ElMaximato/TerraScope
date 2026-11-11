# Estado actual de TerraScope — 8 noviembre 2026

## Resumen

TerraScope ya tiene implementado el mapa real de Somerton, datos oficiales TIGERweb/ACS, el ranking de inversión y el rediseño principal de Android. El backend, las pruebas y las compilaciones Android/Wear están verificadas en este equipo.

## Estado verificable hoy

- Backend: `npm run check` pasa; sintaxis correcta en 44 archivos.
- Backend: `npm test` pasa; 96 pruebas, 0 fallos.
- Android/Wear: `gradlew assembleDebug` pasa para los módulos `app` y `wear`.
- Importación oficial: existía una ejecución real verificada y quedan seis zonas oficiales en MySQL:
  - `0468080` — Somerton city
  - `04027011501`
  - `04027011503`
  - `04027011505`
  - `04027011506`
  - `04027011802`
- Nombres oficiales vigentes:
  - `Somerton city`
  - `Census Tract 115.01`
  - `Census Tract 115.03`
  - `Census Tract 115.05`
  - `Census Tract 115.06`
  - `Census Tract 118.02`
- Las zonas sintéticas `04027011502` y `04027011403` no deben volver a aparecer.
- El período ACS usado es `ACS 5-year 2020–2024`.
- El período TIGER usado es `TIGERweb ACS 2024`.
- `population_density` sí se calcula, escribe y usa en el ranking. `overlap_pct` no forma parte del lote actual ni se escribe.

## Implementado

### Backend

- Express, autenticación JWT, MySQL 8 geoespacial y migraciones hasta `005_overlap_indicator.sql`.
- Importador oficial [backend/scripts/import-somerton-official-data.js](../backend/scripts/import-somerton-official-data.js) con dry-run, transacción real, hash de lote opcional, respaldos, confirmación `IMPORTAR`, rollback y verificación posterior.
- Endpoints de zonas, indicadores, Watchlist, propiedades, campo y scoring.
- Scoring v2 con cuatro perfiles, min-max entre los five tracts, confianza y texto libre con palabras clave.
- El motor de scoring usa población, ingreso mediano, hogares y densidad poblacional. No usa `age` ni datos no demográficos.

### Android

- Jetpack Compose con cinco pestañas: Inicio, Mapa, Invertir, Guardados y Campo.
- Mapa Leaflet local dentro de un WebView, mosaico base CARTO y geometrías oficiales.
- La integración Watchlist usa `POST /watchlist` con `{ zoneId }` y lee `{ items }`.
- La ficha de zona muestra indicadores, margen, periodo y puntaje según el perfil compartido.
- Se corrigió el WebView en blanco y el tamaño fue verificado con `MATCH_PARENT`, `onPageFinished`, `setSize` e `invalidateSize`.

### Wear OS

- El módulo compila, pero no se considera cerrado hasta probar sincronización, sesiones y observaciones en un reloj/emulador real.

## Lo que falta

1. Corregir el estado documental: parte de la documentación y dos constantes históricas del importador todavía mencionan los tracts falsos `115.02`/`114.03` y una lista antigua de cuatro tracts.
2. Corregir la documentación de MapLibre: la implementación vigente es Leaflet WebView + CARTO.
3. Actualizar OpenAPI con los endpoints nuevos: perfiles, match, ranking y puntaje por zona.
4. Probar en dispositivo: Watchlist, rediseño de todas las pantallas, ranking/Invertir, perfil compartido y mapa real.
5. Probar Wear OS y la sincronización de campo de extremo a extremo.
6. Validar y cerrar la estrategia CARTO: el certificado/clave necesita rotación si se consideró expuesta, y una APK todavía hace visible cualquier token incluido en assets.
7. Revisar la clave de API del importador Census: no debe quedar documentada ni almacenarse en archivos bajo control de versión.
8. Continuar solo después de la verificación manual: valor oficial visible, selección de zona, Watchlist, ranking y mapa sin “API KEY REQUIRED”.

## Próxima tarea recomendada

La siguiente etapa segura es preparar una revisión final del código/documentación sin volver a ejecutar importaciones:

- corregir las constantes históricas del importador;
- sustituir los nombres obsoletos del lote y OpenAPI;
- retirar o marcar como histórico `docs/REAL_DATA_IMPORT_PLAN.md` y las secciones sintéticas;
- documentar exactamente los endpoints actuales;
- probar el backend contra MySQL y las cuatro rutas de scoring;
- hacer una prueba Android del mapa, Watchlist y ranking.
