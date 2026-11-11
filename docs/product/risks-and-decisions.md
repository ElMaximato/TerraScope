# Riesgos y decisiones pendientes (Etapa 0)

## Riesgos detectados
| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| R1 | Márgenes de error ACS altos en block groups pequeños | Alto | Mostrar margen de error; considerar tracts |
| R2 | Cobertura OSM insuficiente en Somerton | Alto | Medir antes de construir el score; permitir captura manual |
| R3 | Sin capa GIS oficial de zonificación descargable | Medio/Alto | Mostrar solo jurisdicción; enlazar al visor oficial; no afirmar zonificación |
| R4 | Jurisdicciones mixtas (ciudad, condado, tribal) | Medio | Capa de jurisdicción y advertencias |
| R5 | Sin datos de mercado inmobiliario | Alto | Captura manual identificada como "aportada por usuario" |
| R6 | Datos contradictorios en agregadores | Medio | Usar solo fuentes primarias |
| R7 | Fair Housing / uso responsable de demografía | Alto | Contexto agregado; revisión legal antes de uso comercial |
| R8 | Costos/licencia de mapas (Google Maps vs Mapbox) | Medio | Comparar en etapas 1-2 |

## Decisiones ya indicadas por el usuario
- **DECIDIDO (2026-11-06): MySQL con MySQL Workbench** (el usuario eligió la opción B de abajo). Se usarán tipos y funciones espaciales de MySQL 8; verificar en la Etapa 2.
- **Base de datos:** PostgreSQL + PostGIS y, si es posible, gestionarla con MySQL Workbench.
  **Hallazgo:** MySQL Workbench es un cliente para MySQL/MariaDB y no se conecta a PostgreSQL. Opciones:
  - **A (recomendada):** PostgreSQL + PostGIS administrada con **DBeaver** (interfaz parecida a Workbench) o pgAdmin. Mejor soporte geoespacial, núcleo del producto.
  - **B:** MySQL 8 con funciones espaciales y Workbench. Más familiar; para distancias y punto-en-polígono del MVP puede alcanzar, pero, hasta donde sé, tiene menos funciones espaciales y menos herramientas de importación geoespacial que PostGIS (**por verificar en la Etapa 2**). Obliga a adaptar el backend previsto (FastAPI/SQLAlchemy).
- **Diseño visual:** minimalista, claro, premium, poca información importante (referencia: app inmobiliaria clara con verde oscuro y dorado). Se aplica en la Etapa 1. La spec pide el modo oscuro como principal; habrá que decidir si el tema claro será el principal.
- **Entrega:** zips solo con archivos nuevos/modificados por paso.

## Decisiones pendientes (spec, sección 22)
1. Google Maps SDK o Mapbox.
2. Autenticación (propia o proveedor).
3. Hosting del backend y base de datos.
4. Almacenamiento de imágenes.
5. Unidad de análisis (ver `mvp-scope.md`).
6. Fuente de datos de mercado (hoy: ninguna viable).
7. Indicadores del perfil comercial (propuesta en la matriz).
8. Método y ponderaciones del score.
9. Geofencing y sincronización en Wear OS.
10. Proveedor de IA (opcional).
11. ~~Motor de base de datos~~ - resuelto: MySQL.

## Pendientes de investigación (mini-paso opcional)
- Probar el Census API (place/tract/block group de Somerton) y contar unidades.
- Extraer POI de OSM de Somerton y medir cobertura.
- Confirmar con Yuma County y Somerton si hay capas GIS descargables y su licencia.
