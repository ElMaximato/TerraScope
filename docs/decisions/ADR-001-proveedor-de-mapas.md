# ADR-001: Proveedor de mapas

**Fecha:** 2026-11-06
**Estado actual:** Leaflet.js + CARTO implementados. Historial de decisiones aprobado por el usuario incluido al final.

## Contexto
La app Android necesita un mapa de Somerton con marcadores y zonas (polígonos). Proyecto escolar: debe ser simple y no depender de cuentas de pago.

## Opciones consideradas
| Opción | A favor | En contra |
|---|---|---|
| Google Maps SDK + maps-compose | Estable (maps-compose 8.4.0, minSdk 23), bien documentada | Exige clave de API y facturación en Google Cloud, que pide datos fiscales (RFC desde México). **Descartada: el usuario no puede activar la facturación.** |
| **MapLibre Compose + mosaicos OSM** | Sin clave, tarjeta ni RFC | API declarada experimental ("expect breaking changes between minor releases"); servidor de mosaicos OSM sin SLA |

## Decisión
**Implementación inicial aprobada:** Leaflet.js en Android WebView con mapa base claro de CARTO derivado de OpenStreetMap.
MapLibre Compose quedó como opción de diseño y no fue la implementación aplicada.

## Consecuencias
- Implementación actual: Leaflet 1.9.4 + recursos locales en `android/app/src/main/assets/map/`.
- Tiles base: `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png`, subdominios `abcd`.
- La decisión de usar CARTO resuelve el bloqueo observado de tiles OSM directos desde una página WebView cargada con `file://`; conserva la atribución de OpenStreetMap y CARTO.
- Cumplir la política de mosaicos de OSM: URL HTTPS configurable, User-Agent propio y estable (no el de la librería), atribución "© OpenStreetMap contributors" visible en el mapa, respetar la caché, sin descargas masivas ni modo sin conexión.
- Si OSM bloqueara el acceso, se podrá cambiar la URL de mosaicos a otro proveedor sin tocar el resto de la app.

## Historial
- 2026-11-06: aprobada primero Google Maps; el usuario no tiene RFC para activar la facturación; se cambia a MapLibre + OSM.
- 2026-11-07/08: se implementó Leaflet.js dentro de WebView. Los mosaicos directos de OSM quedaron reemplazados por CARTO y se agregó una clave CARTO de uso gratuito al entorno Android.

## Fuentes (consultadas 2026-11-06)
- https://developers.google.com/maps/documentation/android-sdk/usage-and-billing
- https://developers.google.com/maps/documentation/android-sdk/maps-compose
- https://maplibre.org/maplibre-compose/
- https://operations.osmfoundation.org/policies/tiles/
