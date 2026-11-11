package com.terrascope.app.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.terrascope.app.data.model.ZoneDetail
import com.terrascope.app.data.model.ZoneIndicator
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToLong

/** Indicadores que el mapa puede pintar (coropleta). */
enum class MapIndicator(val code: String, val chipLabel: String, val legendTitle: String) {
    POPULATION("population", "Población", "Población total (personas)"),
    INCOME("median_income", "Ingreso", "Ingreso mediano del hogar (USD)"),
    AGE("median_age", "Edad", "Edad mediana (años)"),
    DENSITY("population_density", "Densidad", "Densidad poblacional (hab/km²)")
}

/** Rampa secuencial de la marca: claro -> verde pino. La usan el mapa y la leyenda. */
val MapRamp: List<Color> = listOf(
    Color(0xFFDCEFE6),
    Color(0xFF5FAE8F),
    Color(0xFF0F4D3A)
)

fun rampColor(fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f) * (MapRamp.size - 1)
    val i = t.toInt().coerceAtMost(MapRamp.size - 2)
    return lerp(MapRamp[i], MapRamp[i + 1], t - i)
}

private fun Color.toHex(): String = String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)

data class IndicatorRange(val min: Double, val max: Double)

/** Mínimo y máximo de cada indicador entre los tracts (la ciudad no entra en la escala). */
fun computeRanges(zones: List<ZoneDetail>): Map<String, IndicatorRange> {
    val result = mutableMapOf<String, IndicatorRange>()
    for (indicator in MapIndicator.values()) {
        val values = zones.filter { it.level == "tract" }
            .mapNotNull { z -> z.indicators.firstOrNull { it.code == indicator.code }?.value }
        if (values.isNotEmpty()) result[indicator.code] = IndicatorRange(values.min(), values.max())
    }
    return result
}

/**
 * Construye el GeoJSON que recibe map.html. Cada zona lleva los colores ya calculados
 * por indicador, de modo que la leyenda de la app y el mapa usan exactamente la misma escala.
 */
fun buildMapGeoJson(zones: List<ZoneDetail>): String {
    val ranges = computeRanges(zones)
    val features = JSONArray()
    for (z in zones) {
        val boundary = z.boundaryGeoJson ?: continue
        val colors = JSONObject()
        if (z.level == "tract") {
            for (indicator in MapIndicator.values()) {
                val value = z.indicators.firstOrNull { it.code == indicator.code }?.value ?: continue
                val range = ranges[indicator.code] ?: continue
                val fraction = if (range.max == range.min) 0.5f
                else ((value - range.min) / (range.max - range.min)).toFloat()
                colors.put(indicator.code, rampColor(fraction).toHex())
            }
        }
        val props = JSONObject()
            .put("id", z.id)
            .put("code", z.code)
            .put("name", z.name)
            .put("level", z.level)
            .put("colors", colors)
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("geometry", JSONObject(boundary))
                .put("properties", props)
        )
    }
    return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
}

// ── Formato de valores ──────────────────────────────────────────────────

fun formatIndicatorValue(code: String, value: Double): String = when (code) {
    "median_income" -> "$" + String.format(Locale.US, "%,d", value.roundToLong())
    "median_age" -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%,d", value.roundToLong())
}

fun formatMargin(code: String, margin: Double): String = when (code) {
    "median_age" -> "±" + String.format(Locale.US, "%.1f", margin)
    "median_income" -> "±$" + String.format(Locale.US, "%,d", margin.roundToLong())
    else -> "±" + String.format(Locale.US, "%,d", margin.roundToLong())
}

// ── Confiabilidad de la estimación ACS ──────────────────────────────────

enum class Reliability(val label: String) { HIGH("Confiabilidad alta"), MEDIUM("Confiabilidad media"), LOW("Confiabilidad baja") }

/**
 * Coeficiente de variación a partir del margen de error al 90 % (ACS): CV = (MOE / 1.645) / estimado.
 * Convención habitual: menos de 12 % alta, de 12 % a 40 % media, más de 40 % baja.
 * Devuelve null cuando no hay margen de error (por ejemplo, los indicadores derivados).
 */
fun reliabilityOf(indicator: ZoneIndicator): Reliability? {
    val moe = indicator.marginOfError ?: return null
    if (indicator.value <= 0.0) return null
    val cv = (moe / 1.645) / indicator.value
    return when {
        cv < 0.12 -> Reliability.HIGH
        cv <= 0.40 -> Reliability.MEDIUM
        else -> Reliability.LOW
    }
}
