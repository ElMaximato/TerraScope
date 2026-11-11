# Arquitectura (estado al cierre de la Etapa 2)

Solo se describe lo que existe hoy. Lo demás está marcado como "por construir".

## Componentes
```
Android (módulo app)  <--- Data Layer (Etapa 8) --->  Wear OS (módulo wear)
        |                                                    |
        +------------------ HTTP (REST) ---------------------+
                                  |
                       Backend Node + Express
                                  |
                              MySQL 8.0
```

| Componente | Tecnología | Estado |
|---|---|---|
| Teléfono (`android/app`) | Kotlin + Jetpack Compose | Plantilla compila y corre |
| Reloj (`android/wear`) | Kotlin + Wear Compose | Plantilla compila y corre |
| Backend (`backend/`) | Node + Express + mysql2 | `GET /api/health` funcionando |
| Base de datos | MySQL 8.0.46 | Tablas `data_sources`, `zones`, `places`; funciones espaciales verificadas |
| Mapas | Leaflet.js WebView + CARTO/OpenStreetMap | Implementado y cargando en el teléfono; diagnóstico y coropleta activos |
| Comunicación reloj-teléfono | Wearable Data Layer | Por construir (Etapa 8); `applicationId` ya compartido |

## Decisiones respecto a la especificación base
La especificación original proponía FastAPI, PostgreSQL/PostGIS, Docker Compose y Google Maps o Mapbox. Se cambió por decisión del usuario o por restricción real:
- **MySQL en lugar de PostgreSQL/PostGIS:** preferencia del usuario; MySQL 8 cubre lo espacial necesario (verificado).
- **Node + Express en lugar de FastAPI:** el usuario delegó el stack y pidió mantener la solución simple.
- **Sin Docker Compose:** MySQL ya está instalado localmente.
- **Leaflet WebView/CARTO en lugar de Google Maps:** se descartó Google Maps por la facturación y se implementó una solución sin clave, con Leaflet y mosaicos CARTO derivados de OSM (ver ADR-001).

## Datos espaciales
Coordenadas en SRID 4326. Al insertar usar `ST_GeomFromText('POINT(<lon> <lat>)', 4326, 'axis-order=long-lat')`. Verificado con `npm run db:verify`.

## Por construir
Autenticación y autorización, modelo de datos completo, importación de datos reales, pantallas, scoring, sincronización idempotente reloj-teléfono.
