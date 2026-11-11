# Modelo de datos (MySQL 8)

Migraciones en `backend/db/migrations/` (se aplican con `npm run db:setup`; la tabla `schema_migrations` registra cuáles ya se aplicaron).
Coordenadas: SRID 4326. Insertar con `ST_GeomFromText('POINT(<lon> <lat>)', 4326, 'axis-order=long-lat')`.

| Tabla | Propósito | Notas |
|---|---|---|
| `data_sources` | Trazabilidad de cada fuente (organización, licencia, periodo, tipo de dato) | `data_type`: official / estimate / derived / user |
| `zones` | Zonas de análisis (tracts) | `boundary` GEOMETRY con índice espacial; `code` = GEOID. Vacía hasta la Etapa 4 |
| `places` | Negocios y lugares (OSM) | `location` POINT con índice espacial |
| `users` | Cuentas | `email` único; `password_hash` scrypt |
| `properties` | Propiedades registradas a mano | Pertenecen a un usuario; datos aportados por el usuario; `zone_id` opcional |
| `watchlist_items` | Zonas guardadas | Única por (usuario, zona) |
| `field_sessions` | Recorridos del reloj | `id` UUID generado por el cliente |
| `field_observations` | Puntos registrados en campo | `id` UUID del cliente = sincronización idempotente; `tag` con las 8 etiquetas de la especificación |
| `indicators` | Catálogo de métricas territoriales y demográficas | `code` único (population, median_income, etc.) |
| `indicator_values` | Valores cuantitativos por zona | `zone_id`, `indicator_id`, `value`, `margin_of_error`, `source_id`, `reference_period` |

## Importación oficial aprobada y indicadores
- Las zonas vigentes son Somerton city y los tracts oficiales `115.01`, `115.03`, `115.05`, `115.06` y `118.02`.
- `population_density` se deriva como población / AREALAND en km², con `margin_of_error = null`.
- `overlap_pct` se conserva en la migración 005, pero actualmente no forma parte del lote ni se escribe desde el importador real.
- La migración `005_overlap_indicator.sql` solo crea/actualiza el indicador `overlap_pct`; no modifica las migraciones 001–004.
- Los valores ACS rechazados o nulos no se convierten a cero: se omiten para evitar que Android los interprete como un valor real.
- Los periodos son exactos: `ACS 5-year 2020–2024` y `TIGERweb ACS 2024`.

## Relaciones
- `users` 1—N `properties`, `watchlist_items`, `field_sessions`, `field_observations` (borrado en cascada).
- `zones` 1—N `watchlist_items` y `properties` (si se borra la zona, la propiedad conserva sus datos y `zone_id` pasa a NULL).
- `zones` 1—N `indicator_values` (borrado en cascada).
- `indicators` 1—N `indicator_values` (borrado en cascada).
- `field_sessions` 1—N `field_observations` (si se borra la sesión, la observación se conserva).

## Qué NO se creó todavía (a propósito)
`scoring_models`, `opportunity_scores`, `report_records`, `sync_events`, `user_devices`: se crean en las etapas que los usan (6 y 9) cuando haya casos de uso y cálculo de scores reales.
