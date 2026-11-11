# Dry-run de descubrimiento oficial - Somerton 2024

Fecha de ejecución: 2026-11-07

## Resultado

- Año consultado: 2024.
- Vigencia coincidente para TIGERweb y ACS: sí.
- Modo: `--dry-run`, solo lectura.
- Validación de sintaxis: aprobada, 43 archivos.
- Pruebas: 82 aprobadas, 0 fallidas.
- No se escribió en MySQL.
- No se crearon archivos ni respaldos.
- No se ejecutaron migraciones.

## TIGERweb

Servicio usado para tracts:

- `tigerWMS_ACS2024`
- URL: `https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/tigerWMS_ACS2024/MapServer/8`
- Capa: `Census Tracts`
- Vintage: 2024
- HTTP: 200
- Features devueltos: 5

Servicio usado para el place:

- `tigerWMS_ACS2024`
- URL: `https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/tigerWMS_ACS2024/MapServer/28`
- Capa: `Incorporated Places`
- Vintage: 2024
- HTTP: 200
- Features devueltos: 1

Consulta aplicada a los tracts:

- Estado: `04`
- Condado de Yuma: `027`
- Geometría de place `0468080` como filtro.
- Relación espacial: `esriSpatialRelIntersects`.
- Sistema de entrada y salida: `4326`.
- Geometría devuelta: sí.

## Place oficial descubierto

| GEOID | Nombre | AREALAND |
|---|---|---:|
| 0468080 | Somerton city | 18,836,277 m² |

## Tracts descubiertos

| GEOID | Nombre | Porcentaje dentro del place | ACS disponible | Advertencia |
|---|---|---:|---|---|
| 04027011506 | Census Tract 115.06 | 100.00% | No | Ninguna |
| 04027011505 | Census Tract 115.05 | 26.05% | No | Ninguna |
| 04027011802 | Census Tract 118.02 | 9.55% | No | Casi fuera (<10%) |
| 04027011503 | Census Tract 115.03 | 95.14% | No | Ninguna |
| 04027011501 | Census Tract 115.01 | 10.76% | No | Ninguna |

## Metricas de traslape

Se reportan dos medidas distintas:

- Porcentaje del tract que cae dentro de Somerton.
- Porcentaje del area de Somerton aportado por cada tract.

| GEOID | Dentro del place | Aportacion al place |
|---|---:|---:|
| 04027011506 | 100.00% | 7.73% |
| 04027011505 | 26.05% | 9.88% |
| 04027011802 | 9.55% | 10.22% |
| 04027011503 | 95.14% | 16.51% |
| 04027011501 | 10.76% | 55.66% |

- Suma de aportaciones al place: `100.00%`.
- Diferencia contra el 100% del place: `0.00%`.

## Comparación con GEOID suministrados anteriormente

Los GEOID anteriores se conservaron únicamente como referencia histórica. No controlan el descubrimiento ni la selección final.

- Referencia anterior:
  - `04027011501`
  - `04027011502`
  - `04027011503`
  - `04027011403`
- Encontrados por TIGERweb:
  - `04027011506`
  - `04027011505`
  - `04027011802`
  - `04027011503`
  - `04027011501`
- No encontrados respecto a la referencia anterior:
  - `04027011502`
  - `04027011403`
- Adicionales respecto a la referencia anterior:
  - `04027011506`
  - `04027011505`
  - `04027011802`

## ACS

El proyecto es universitario y no sera implementado como servicio publico. No se solicita ni debe registrarse una clave nueva para este proyecto.

- `CENSUS_API_KEY` no estaba definida en la ultima ejecucion.
- GEOID ACS encontrados para Yuma County: 0.
- Tracts descubiertos sin datos ACS disponibles:
  - `04027011506`
  - `04027011505`
  - `04027011802`
  - `04027011503`
  - `04027011501`
- Place sin datos ACS disponibles: `0468080`.
- Poblacion, hogares, ingreso mediano y edad mediana quedaron como `null` en las cinco zonas tract y en el place.
- Sus margenes de error quedaron como `null`, porque no hay fila ACS que validar.
- No se sustituyen los valores por ceros ni se estiman datos.
- La suma poblacional de los cinco tracts es `null`; la poblacion del place tambien es `null`; por tanto no se calcula una diferencia.
- Los tracts pueden incluir poblacion fuera del limite municipal. Si se completa ACS, la diferencia tracts menos place debe reportarse sin forzarla ni corregirla.

## Validación espacial

Todas las geometrías reportadas fueron válidas con MySQL y la convención del proyecto:

- SRID: 4326.
- Orden de ejes: `axis-order=long-lat`.
- Método: `ST_IsValid`.

| GEOID | Resultado |
|---|---|
| 04027011506 | Válida |
| 04027011505 | Válida |
| 04027011802 | Válida |
| 04027011503 | Válida |
| 04027011501 | Válida |
| 0468080 | Válida |

## Geometria detallada

| GEOID | Tipo | Anillos | Vertices |
|---|---|---:|---:|
| 04027011506 | Polygon | 1 | 58 |
| 04027011505 | Polygon | 1 | 371 |
| 04027011802 | Polygon | 1 | 166 |
| 04027011503 | Polygon | 1 | 110 |
| 04027011501 | Polygon | 1 | 1026 |
| 0468080 | Polygon | 1 | 378 |

## Estado de la base actual

Consulta de solo lectura:

- Zonas existentes coincidentes: 3.
- Valores actuales de indicadores coincidentes: 15.
- Zonas no pertenecientes al lote descubierto:

| GEOID | ID | Watchlist items | Properties | Indicator values actuales |
|---|---:|---:|---:|---:|
| 04027011403 | 4 | 0 | 0 | 5 |
| 04027011502 | 2 | 0 | 0 | 5 |

No se eliminan ni se modifican estas zonas.

## Hash del lote

```text
dcb7108bef3c8c6abe654c68f38390d3159f4824d08a9ac3d76bd15cc8214265
```

Hashes por archivo descargado:

```text
TIGER tracts GeoJSON: 2251730a60595e54150602b90b026311bb2f1cd3d09d4ec15833370078f84d8b
TIGER place GeoJSON:  eeda50cdd9be9befe94a7505a06cb9d3b5f7b3f3c19de64baa2b5c13809e06d3
ACS tracts JSON:       no disponible
ACS place JSON:        no disponible
```

## Lista candidata, pendiente de aprobación

```text
0468080 Somerton city
04027011506 Census Tract 115.06
04027011505 Census Tract 115.05
04027011802 Census Tract 118.02
04027011503 Census Tract 115.03
04027011501 Census Tract 115.01
```

Esta lista no debe importarse hasta que se apruebe explicitamente. Aunque el uso no sera publico ni productivo, los datos oficiales deben revisarse antes de la importacion real.

## Indicadores derivados propuestos

Sin crear datos ni migraciones, el lote propone:

- `population_density = poblacion / AREALAND_tract_km2`.
- `overlap_pct = porcentaje del tract dentro del place`.
- Ambos tendrian `margin_of_error = null`.
- `population_density` usa el tipo `derived`; `overlap_pct` se registra como un indicador derivado de geometria.
- El registro usa las tablas existentes `indicators` y `indicator_values`, con un `data_source` derivado y `reference_period`. No se ejecuto ninguna migracion nueva.

Como ACS no estaba disponible, las densidades poblacionales quedaron `null`.

> **Actualización 2026-11-07:** este documento conserva el informe histórico del análisis. Posteriores ajustes aprobaron quitar `overlap_pct` del lote y de la escritura real; `population_density` sí se conserva como indicador derivado.
