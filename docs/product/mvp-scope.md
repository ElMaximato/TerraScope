# Propuesta de alcance del MVP (borrador para aprobación)

Fecha: 2026-11-06 · Basado en la investigación de la Etapa 0 (`docs/research/`).

## Perfil inicial
**Inversión comercial / site selection** en Somerton, AZ.

## Unidad de análisis (por decidir)
- **A:** census block groups. Verificado con datos reales: hay unos 165 en el condado de Yuma, con márgenes de error muy altos (p. ej. ingreso $162,250 ±$134,118) y varios sin ingreso. **Ya no se recomienda como unidad principal.**
- **B (ahora recomendada):** census tracts. Más estables y con menos ruido. Pendiente: repetir la consulta de tracts y obtener geometrías (TIGER) para saber cuáles cubren Somerton.
- **C:** cuadrícula propia (p. ej. hexágonos) con datos asignados proporcionalmente. Más flexible, pero compleja y propensa a dar falsa precisión.

## Incluido en el MVP (condicionado a la Etapa 4)
1. Mapa de Somerton con zonas (unidad A o B).
2. Ficha de zona: población, hogares, ingreso mediano, edad mediana (ACS, con margen de error y vintage) + densidad derivada.
3. Capa de negocios/POI de OSM con atribución y aviso de cobertura.
4. Distancias a vías principales/POI.
5. Jurisdicción de la zona (si la fuente se confirma).
6. Opportunity Score comercial básico **solo si** la cobertura OSM es suficiente; si no, indicadores sin score.
7. Watchlist, propiedades manuales, observaciones del reloj y sincronización (según spec).

**Requisito de la actividad:** Wear OS es obligatorio. Mínimo en el reloj: lista de zonas guardadas con evaluación rápida, sesión de campo con puntos (etiqueta + nota) y sincronización con el teléfono (ver `docs/design/wear-os-screens.md`).

## Fuera del MVP
Precios de mercado, comparables, rentas, zonificación por parcela, tráfico, ML, perfiles adicionales.

## Criterio de salida de la Etapa 0
Existe una lista realista de indicadores obtenibles legalmente (ver matriz). **Falta validar con llamadas reales** al Census API y a Overpass (Etapa 4, o antes en un mini-experimento si lo apruebas).
