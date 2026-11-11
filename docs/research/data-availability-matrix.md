# Matriz de disponibilidad de datos y viabilidad (Somerton, AZ)

Fecha: 2026-11-06 · Estado: borrador para revisión.
**Importante:** ningún indicador está probado de extremo a extremo; la Etapa 4 lo hará con un dataset real.

Viabilidad MVP: **Alta** (fuente, licencia y método claros) · **Media** (falta verificar un punto) · **Baja/No** (sin fuente confirmada).

## A. Indicadores de zona (perfil comercial / site selection)

| # | Indicador | Fuente | Granularidad | Tipo de dato | Estado | Viabilidad MVP | Notas |
|---|---|---|---|---|---|---|---|
| 1 | Población total | Census (decenal 2020 / ACS 5 años) | place, tract, block group | Oficial / estimación | Verificado con llamada real (ACS 2024) | **Alta** | Place: 14,574 ±19 |
| 2 | Hogares y tamaño de hogar | ACS 5 años | tract, block group | Estimación | Verificado con llamada real | **Alta** | Place: 4,964 hogares |
| 3 | Densidad poblacional | Derivado: población / área (TIGER) | tract, block group | Derivado | Hipótesis | **Alta** | Validar CRS y unidades |
| 4 | Ingreso familiar mediano | ACS 5 años | tract, block group | Estimación | Verificado con llamada real | **Media** | Place: $74,015 ±$11,292; block groups con márgenes enormes y varios sin dato |
| 5 | Edad mediana | ACS 5 años | tract, block group | Estimación | Verificado con llamada real | **Alta** | Place: 31.3 años; block groups con valores extremos |
| 6 | Vivienda: valor/alquiler mediano | ACS 5 años | tract, block group | Estimación | Hipótesis | **Media** | Contexto, no precio de mercado actual |
| 7 | Negocios por categoría | OpenStreetMap | Punto | Comunitario | Medido: 68 POI (40 con nombre) en el área | **Baja/Media** | Escaso; complementar con captura manual o datos de demostración marcados |
| 8 | Competidores cercanos (conteo en radio) | Derivado de OSM | Punto/radio | Derivado | Hipótesis | **Baja** | Cobertura OSM escasa; mostrar advertencia |
| 9 | Accesibilidad vial (distancia a arteriales) | OSM / TIGER roads | Línea | Derivado | Verificado: 53 vías principales con nombre | **Alta** | Sin tráfico real |
| 10 | Distancias a puntos relevantes | OSM + TIGER | Punto | Derivado | Hipótesis | **Alta** | Cálculo espacial |
| 11 | Jurisdicción (ciudad / condado / tribal) | TIGER / GIS | Polígono | Oficial | Pendiente | **Media** | Necesaria antes de hablar de zonificación |
| 12 | Zonificación / uso de suelo | Somerton / Yuma County GIS | Polígono / parcela | Oficial | Pendiente | **Baja (por ahora)** | PDF del código existe; capa GIS no confirmada |
| 13 | Parcelas y superficie | Yuma County / ADWR / comerciales | Parcela | Oficial / pago | Existencia confirmada, licencia pendiente | **Baja (por ahora)** | Validar licencia |
| 14 | Infraestructura / servicios | Municipal / condado | - | - | Sin evidencia | **No** | Posponer |
| 15 | Flujo peatonal / vehicular | - | - | - | Sin fuente | **No** | Sin fuente abierta confirmada |

## B. Indicadores de propiedad

| # | Indicador | Fuente | Estado | Viabilidad MVP |
|---|---|---|---|---|
| 16 | Precio solicitado, superficie, notas | Captura manual | Diseño | **Alta** (marcado "aportado por usuario") |
| 17 | Precio de mercado / comparables | Sin fuente abierta confirmada | Verificado (ausencia) | **No** en MVP |
| 18 | Asociar propiedad con zona | Cálculo espacial | Hipótesis | **Alta** |

## C. Lo que NO se podrá ofrecer inicialmente
- Precios de suelo/ventas comparables y rentas de mercado.
- Zestimates o cualquier valuación.
- Flujo de tráfico o peatones.
- Zonificación oficial por parcela (hasta confirmar capa GIS y licencia).
- Predicciones o tendencias de precios.

## D. Resumen
Con fuentes gratuitas **sí es viable** un MVP de ficha de zona demográfica + contexto comercial (OSM) + distancias, mostrando siempre que el ACS son estimaciones con margen de error y que la cobertura OSM es incompleta. Un Opportunity Score solo sería defendible con los indicadores 1-5 y 7-10; deberá indicar baja confianza si OSM resulta escaso.
