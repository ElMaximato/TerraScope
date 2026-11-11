# Plan de integración de datos reales: TIGERweb y Census ACS

**Fecha de revisión:** 2026-11-07
**Estado:** Plan aprobado para revisión técnica; pendiente de implementación
**Alcance:** Solo datos geográficos y demográficos del backend
**Fuera de alcance actual:** Interfaz del mapa, WebView, mapa base, leyenda y colores por indicador

---

## 1. Contexto y objetivo

TerraScope es una aplicación Android y Wear OS para el análisis territorial de Somerton, Arizona. El proyecto utiliza:

- Android nativo con Kotlin y Jetpack Compose.
- Wear OS con el mismo `applicationId` para la Data Layer.
- Backend Node.js con Express y MySQL 8.
- Tipos espaciales SRID 4326.
- U.S. Census, TIGERweb, Census ACS y OpenStreetMap como fuentes oficiales o comunitarias.

La aplicación ya compila y consume un contrato estable de API. Sin embargo, los datos y geometrías actuales de Somerton no pueden considerarse reales ni verificables de extremo a extremo.

El objetivo de esta etapa es sustituir los datos de relleno por:

- Límites oficiales de Census Tracts obtenidos de TIGERweb.
- Limite municipal oficial de Somerton obtenido de TIGERweb.
- Indicadores ACS 5-Year obtenidos del U.S. Census Bureau.
- Nombres oficiales, sin nombres comerciales ni descriptivos inventados.
- Trazabilidad de fuente, licencia, vintage, periodo y fecha de descarga.

## 2. Problemas detectados

### Geometrías

Los archivos actuales contienen límites rectangulares:

- `data/somerton_tracts_tiger.json`.
- `data/somerton_place_tiger.json`.

No representan límites oficiales irregulares de TIGER.

Los datos locales de cuatro tracts actualmente incluyen rectángulos sintéticos para:

- GEOID `04027011501`.
- GEOID `04027011502`.
- GEOID `04027011503`.
- GEOID `04027011403`.

También existe el límite municipal `0468080`, pero actualmente se presenta como un rectángulo sintético.

### Nombres

El script `backend/scripts/load-somerton-data.js` agrega nombres ficticios y comerciales, por ejemplo:

- `Somerton Norte / Oportunidad`.
- `Somerton Centro / Sur`.
- `Somerton Oeste`.
- `Valle de Somerton / Agrícola`.

Estos nombres deben eliminarse.

Los nombres permitidos deben provenir del Census o ser construidos exclusivamente como:

- `Census Tract 115.01`.
- `Census Tract 115.02`.
- `Census Tract 115.03`.
- `Census Tract 114.03`.
- `Somerton city`.

### Indicadores

Los indicadores existentes están respaldados por el archivo local:

- `data/somerton_acs_data.json`.

No hay una trazabilidad completa de URL exacta, fecha de consulta ni descarga reproducible asociada. Algunos años y periodos pueden haber sido combinados sin un registro claro.

Los datos reales deberán declararse con un único año ACS 5-Year que sea coherente en todo el lote importado.

## 3. Estado de implementación

### Cerrado

- Etapas 0 y 1: descubrimiento, diseño y UX.
- Etapa 2: arquitectura, Android, Wear OS base, backend Express y MySQL.
- Etapa 3: modelo de datos, autenticación, autorización, CRUD, watchlist y sincronización de campo.
- Etapa 4: carga inicial de geometrías e indicadores, migraciones y pruebas automatizadas.
- Backend con 74 pruebas aprobadas: 37 unitarias y 37 de integración.
- Android compila con `:app:assembleDebug` y `:wear:assembleDebug`.
- El mapa actual carga y selecciona las cinco zonas existentes.

### Pendiente verificado

- Sustituir las geometrías sintéticas por TIGERweb real.
- Eliminar nombres comerciales o descriptivos inventados.
- Ejecutar una descarga reproducible y trazable desde Census/ACS.
- Validar todos los GEOID.
- Actualizar el registro de fuentes.
- Confirmar que los valores reales corresponden al mismo vintage.
- No modificar la interfaz de mapa hasta terminar esta fase.

## 4. Esquema actual sin cambios requeridos

### Migración `004_indicators.sql`

La migración `004_indicators.sql` define:

- `indicators`: catálogo de variables.
- `indicator_values`: valor, margen de error, fuente, periodo y zona.

La clave única es:

```text
(zone_id, indicator_id, reference_period)
```

Indicadores base:

- `population`.
- `households`.
- `median_income`.
- `median_age`.
- `population_density`.

`population_density` es un indicador derivado. Debe calcularse con población dividida entre el área terrestre TIGER informada por la geometría oficial.

### Tabla `zones`

La tabla mantiene el siguiente contrato:

```text
id
code
name
level
boundary
source_id
```

Campos:

- `code`: GEOID oficial.
- `name`: nombre oficial.
- `level`: `place`, `tract` o `block_group`.
- `boundary`: GEOMETRY SRID 4326.
- `source_id`: referencia a `data_sources`.

## 5. Contrato JSON exacto para Android

No se necesitan cambios para la sustitución de datos porque el contrato actual ya consume GeoJSON.

### Lista de zonas

`GET /api/zones`

```json
{
  "zones": [
    {
      "id": 1,
      "code": "04027011501",
      "name": "Census Tract 115.01",
      "level": "tract"
    }
  ]
}
```

`ZoneItem` en Android exige:

- `id`: entero.
- `code`: texto.
- `name`: texto.
- `level`: texto.

La lista no devuelve geometría.

### Detalle de zona

`GET /api/zones/:id`

```json
{
  "zone": {
    "id": 1,
    "code": "04027011501",
    "name": "Census Tract 115.01",
    "level": "tract",
    "boundary": {
      "type": "Polygon",
      "coordinates": []
    },
    "source": {},
    "indicators": [
      {
        "code": "population",
        "name": "Población total",
        "value": 2163,
        "marginOfError": 485,
        "referencePeriod": "ACS 5-yr 2023"
      }
    ]
  }
}
```

`boundary` puede ser:

- `Polygon`.
- `MultiPolygon`.

Android actualmente solicita el detalle de cada zona para construir el mapa:

- [TerraScopeApi.kt](/C:/Users/ferba/Documents/ProyectosUT/inversiones_somerton%20-%20Copy/android/app/src/main/java/com/terrascope/app/data/network/TerraScopeApi.kt)
- [MapScreen.kt](/C:/Users/ferba/Documents/ProyectosUT/inversiones_somerton%20-%20Copy/android/app/src/main/java/com/terrascope/app/ui/screen/MapScreen.kt)

## 6. Importador oficial propuesto

Crear un script nuevo, reproducible e independiente:

```text
backend/scripts/import-somerton-official-data.js
```

Posibles comandos:

```powershell
npm run db:import-somerton -- --dry-run
npm run db:import-somerton
npm run db:import-somerton -- --year 2024
```

El script debe:

- Usar un único periodo oficial.
- No aplicar migraciones en modo `--dry-run`.
- No escribir en MySQL en modo `--dry-run`.
- Validar antes de iniciar una transacción.
- Confirmar de forma explícita antes de modificar la base.
- Hacer rollback ante cualquier error.

## 7. Descarga TIGERweb

Descargar directamente desde TIGERweb:

- Los cuatro GEOID de los tracts de Somerton.
- El límite municipal de Somerton `0468080`.
- Geometrías en EPSG:4326.
- Campos oficiales:
  - `GEOID`.
  - `NAME`.
  - `BASENAME`.
  - `AREALAND`.
  - `AREAWATR`.
  - Geometría.

Validaciones obligatorias:

- GEOID exacto.
- Tipo `Polygon` o `MultiPolygon`.
- Anillos cerrados.
- Coordenadas dentro de los límites válidos de longitud y latitud.
- Geometría dentro de la extensión esperada de Somerton y Yuma County.
- Solo una geometría por GEOID.
- Ningún resultado vacío, truncado o sintético.
- Nombres oficiales, sin texto comercial.

Los archivos descargados deberán conservar hashes o metadatos mínimos de procedencia.

## 8. Descarga Census ACS

Consultar una única versión ACS 5-Year, por ejemplo 2024 cuando esté disponible y validada.

Variables:

- `B01003_001E/M`: población y margen de error.
- `B11001_001E/M`: hogares y margen de error.
- `B19013_001E/M`: ingreso mediano y margen de error.
- `B01002_001E/M`: edad mediana y margen de error.

Descargas necesarias:

- Los cuatro tracts por Yuma County.
- El lugar `68080` para Somerton city.

Si falta una variable:

- Registrar `null` para el valor o margen.
- No sustituir el dato con cero.
- No inventar un valor estimado.
- Mostrar los datos faltantes en la UI o marcarlos como insuficientes.

## 9. Validaciones del importador

### Antes de escribir

- Validar esquema JSON.
- Validar año y periodo.
- Validar cada GEOID.
- Validar geometría y coordenadas.
- Comparar unidades.
- Detectar duplicados.
- Detectar zonas adicionales o faltantes.
- Validar que los valores no sean valores de rechazo ACS como `-666666666`, `-888888888` o `-999999999`.
- No publicar valores negativos como indicadores reales.

### Dentro de la transacción

- Registrar o recuperar fuente TIGER.
- Registrar o recuperar fuente ACS.
- Insertar o actualizar zonas.
- Insertar o actualizar indicadores.
- Insertar o actualizar valores.
- Ejecutar consultas espaciales de comprobación.

### Después

- `SELECT` de zonas reales.
- Conteo esperado de tracts y place.
- Comprobación de geometrías exportadas desde MySQL.
- Comprobación de que el GeoJSON es válido.
- Comprobación de que Android puede parsearlo.
- Resumen de nombres, GEOID, indicadores y valores.
- Confirmar que los nombres ficticios no quedan en la base.

## 10. Comportamiento de `--dry-run`

El modo seco debe:

- Descargar los datos oficiales.
- Validar el lote completo.
- No llamar a `migrate()`.
- No crear o modificar registros de MySQL.
- No modificar datos locales existentes.
- Mostrar:
  - Año ACS seleccionado.
  - Vintage TIGER.
  - GEOID encontrados.
  - GEOID faltantes.
  - GEOID adicionales.
  - Tipo y número de vértices por geometría.
  - Indicadores disponibles y faltantes.
  - Valores y márgenes de error.
  - Nombres oficiales.
  - Hashes de datos descargados.
  - Diferencias respecto de la base actual.
  - Advertencias o errores.

El usuario decide si autoriza la importación después de revisar ese informe.

## 11. Reglas de trazabilidad

Cada zona debe enlazar a su fuente TIGER.

Cada valor de indicador debe enlazar a su fuente ACS.

Debe registrarse:

- Organización.
- Nombre de fuente.
- URL exacta o endpoint utilizado.
- Licencia.
- Periodo de referencia.
- Fecha de descarga.
- Tipo de dato:
  - `official`.
  - `estimate`.
  - `derived`.

Ejemplo:

- TIGER: tipo `official`.
- ACS: tipo `estimate`.
- Densidad poblacional: tipo `derived`.

## 12. Seguridad e integridad

No guardar claves Census dentro del código.

El proyecto es universitario y no sera publicado ni implementado como servicio publico. Por este motivo, no se solicita ni se configura una clave Census nueva. Si una ejecucion futura recibe una clave temporal autorizada por el usuario, solo puede recibirse desde una variable de entorno; nunca se escribira en codigo, logs, URLs, Markdown, respaldos ni archivos descargados.

No incluirla en:

- Commits.
- Archivos `.md`.
- Capturas.
- Logs.
- Respuestas de API.

Usar usuario HTTPS de Census únicamente cuando sea posible y no compartir datos personales.

## 13. Protección contra modificaciones accidentales

Al reemplazar datos:

- No modificar ni sobrescribir las migraciones 001–004.
- No añadir migraciones solo para renombrar datos.
- No cambiar las rutas actuales de `/api/zones`.
- No cambiar el formato actual esperado por Android.
- No incluir datos de demostración sin marcarlos claramente.
- No mezclar periodos ACS antiguos con nuevos.
- No mezclar geometrías antiguas con geometrías nuevas.
- Mantener la transacción y hacer rollback completo ante un error.

## 14. Orden de trabajo aprobado

1. Inspeccionar el backend y confirmar el contrato.
2. Corregir y reemplazar los datos reales.
3. Agregar el modo `--dry-run`.
4. Validar los datos descargados y el hash del lote.
5. Obtener aprobación antes de escribir en MySQL.
6. Ejecutar la importación real.
7. Validar las zonas, indicadores y GeoJSON desde la API.
8. Solo después, modificar el frontend del mapa.

## 15. Fuera de alcance de esta fase

No se implementarán todavía:

- Mapa base profesional.
- OpenStreetMap o MapLibre en WebView.
- Leyenda.
- Color por indicador.
- Ficha desplegable desde el mapa.
- Opportunity Score.
- POI comerciales.
- Zonificación oficial.
- IA.
- Reportes.

Estos elementos se ejecutarán únicamente después de verificar los límites y indicadores oficiales.
