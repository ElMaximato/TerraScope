# Fuentes de datos candidatas para TerraScope (Somerton, AZ)

## Estado de importación oficial
- Límites y geografía: TIGERweb Census, vintage `TIGERweb ACS 2024`.
- Indicadores: ACS 5-Year, periodo `ACS 5-year 2020–2024`.
- Descubrimiento real por intersección del place `0468080` con tracts del county de Yuma; el lote y su hash requieren aprobación explícita.
- La clave `CENSUS_API_KEY` solo se usa desde el entorno; nunca se guarda en `data_sources.url`, logs, manifest, reportes ni respaldos.
- Los respaldos oficiales estarán bajo `backend/data/raw/2024/`; los JSON crudos se ignoran y el `manifest.json` sí se versiona.
- Estado al 8 de noviembre de 2026: la importación real ya fue ejecutada y verificada con seis zonas oficiales. El lote vigente contiene Somerton place y cinco tracts:
  `0468080`, `04027011501`, `04027011503`, `04027011505`, `04027011506` y `04027011802`.
- No conservar los GEOID sintéticos `04027011502` ni `04027011403` como datos activos.

**Etapa:** 0 - Descubrimiento y viabilidad
**Fecha de la investigación:** 2026-11-06
**Método:** búsqueda web de documentación y páginas públicas. **No se ejecutó ninguna llamada real a las APIs** (el entorno de investigación no tenía acceso a census.gov). "Verificado" significa "confirmado en documentación o páginas consultadas", no "probado con una petición real".

Leyenda: **Verificado** (confirmado en una fuente consultada) · **Hipótesis** (razonable, falta comprobar) · **Pendiente** (no investigado o sin respuesta clara).

---

## 1. U.S. Census Bureau (Decennial + ACS + TIGER/Line)

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Población Somerton (censo 2020) | 14,197 habitantes; 4,149 unidades de vivienda (4.6 % vacías); área total aprox. 7.28 mi² | Verificado (Wikipedia citando Census; QuickFacts de census.gov confirma 14,197) |
| Geografías ACS 5 años | Estimaciones a nivel place, **tract y block group**. Tracts y block groups existen solo en ACS de 5 años | Verificado (presentación del ACS Office) |
| Bloques (blocks) | Solo para datos decenales de formulario corto; no para ACS | Verificado (documentación de la librería census_area) |
| Acceso programático | Census API con clave gratuita (api.census.gov/data/key_signup.html); GEOID como llave para unir tablas con geometrías | Verificado con llamada real: **la clave es obligatoria** (sin clave responde "Missing Key") |
| Límites geográficos | TIGER/Line: shapefiles sin copyright; "TIGER/Line" y "Census TIGER" son marcas registradas del Census Bureau | Verificado (metadatos AZMAG) |
| Margen de error | ACS son estimaciones con margen de error; a nivel block group suelen ser amplios | Verificado con llamada real: place ingreso ±$11,292; block groups con márgenes muy altos |
| Licencia | Datos del gobierno federal, en general dominio público; confirmar aviso de atribución | Hipótesis |

**Verificado con llamada real (2026-11-06, ACS 2024):** Somerton city tiene 14,574 habitantes (±19), ingreso mediano $74,015 (±$11,292), edad mediana 31.3 y 4,964 hogares. Las cifras que daban los agregadores diferían porque eran de otros años (ACS 2021, 2023) o mezclaban ZIP con ciudad; las de 2024 sí coinciden. **Conclusión: obtener todo del Census API y guardar vintage, geografía y margen de error.** Ver `validation-results.md`.

## 2. OpenStreetMap (negocios, vías, POI)

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Licencia | ODbL 1.0. Uso comercial permitido con atribución "© OpenStreetMap contributors"; bases derivadas publicadas tienen obligación de share-alike | Verificado |
| Overpass API pública | Infraestructura comunitaria sin SLA; las políticas piden no usarla como backend sostenido. Para uso sostenido: instancia propia o extractos regionales | Verificado |
| Cobertura en Somerton | **No evaluada** (falta contar negocios/POI y etiquetas) | Pendiente |
| Calidad/actualidad | Mapeo comunitario: pueden faltar nombres, horarios o negocios | Verificado (advertencia general) |

**Implicación:** importar un extracto una vez (script reproducible) a la base de datos en lugar de consultar Overpass desde la app. Mostrar atribución en el mapa y en reportes.

## 3. Yuma County GIS

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Existencia | División GIS que mantiene mapas maestros de zonificación y uso de suelo y el GIS empresarial | Verificado (página oficial About Us - GIS) |
| Visor | Visor público de parcelas y mapas de zonificación en yumacountyaz.gov | Verificado (referenciado por guía de asessor; no abierto directamente) |
| Descarga masiva / API | Su FAQ pregunta si hay mapas CAD/GIS disponibles, pero **no se pudo leer la respuesta** | Pendiente |
| Parcelas en servicio estatal | Arizona Department of Water Resources publica un MapServer de parcelas con datos de Yuma (carpeta noviembre 2023) | Verificado (existencia); licencia y vigencia: **Pendiente** |
| Parcelas comerciales | Vendedores (Regrid, Land Records, USLandGrid) ofrecen parcelas de Yuma con zonificación y valores, **de pago** | Verificado (existencia); precios y licencia de redistribución: **Pendiente** |

## 4. City of Somerton

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Código de zonificación | Zoning Ordinance de Somerton (adoptado 2015) publicado como PDF | Verificado |
| Mapa de zonificación digital | Agregadores comerciales (Zoneomics, ZoningPoint) muestran un mapa. **No se confirmó una capa GIS oficial descargable** | Pendiente |
| Jurisdicción tribal | La Cocopah Indian Tribe tiene su propio Planning Department en Somerton y su propio proceso de desarrollo | Verificado (documento del Cocopah); impacto en el mapa: **Hipótesis** |
| Zonas de oportunidad | Un listado comercial las menciona; sin verificar en fuente oficial | Pendiente (baja confianza) |

**Hipótesis:** parte del territorio alrededor de Somerton puede estar bajo jurisdicción tribal o del condado, con reglas distintas. TerraScope debe mostrar la jurisdicción antes de afirmar nada de zonificación.

## 5. Data USA
Solo se vio una página de perfil con datos de 2021 (ingreso mediano $53,741; edad mediana 30.9), distinta de otras cifras del mismo año en otros sitios. Es un agregador de datos del Census.
- Estado: **Hipótesis** de que sirve solo para exploración. Licencia y metodología: **Pendiente**.
- Recomendación: no integrarlo; ir a la fuente original.

## 6. Datos de mercado inmobiliario

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Zillow API pública | Retirada el 30-sep-2021. Hoy solo hay vías con aprobación (Bridge para miembros de MLS/corredores; programas de rentas/leads). Individuos y la mayoría de startups no califican | Verificado (varias fuentes de 2026) |
| Scraping de Zillow | Sus términos de uso prohíben el acceso automatizado | Verificado |
| Zillow Research | CSV de métricas agregadas de descarga libre | Verificado (existencia); cobertura para Somerton: **Pendiente** |
| Alternativa MVP | Captura manual de propiedades por el usuario (spec 10.4), marcada "aportada por usuario" | Decisión de diseño |

## 7. No recomendadas para el MVP
- Agregadores de terceros como fuente de datos (inconsistentes, sin trazabilidad).
- Scraping de portales inmobiliarios (riesgo contractual/legal).
