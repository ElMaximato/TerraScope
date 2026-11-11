package com.terrascope.app.data.model

// ── Zonas ──────────────────────────────────────────────────────────────────

data class ZoneItem(
    val id: Int,
    val code: String,
    val name: String,
    val level: String,
    // Nombre descriptivo calculado por el backend ("Residencial · norte"); null si no hay geometría
    val friendlyTitle: String? = null,
    val kind: String? = null
)

data class ZoneHighlight(
    val code: String,
    val title: String,
    val value: String,
    val help: String,
    val comparison: String?
)

data class ZoneFriendly(
    val title: String,
    val kind: String?,
    val codeLabel: String,
    val description: String,
    val highlights: List<ZoneHighlight>
)

data class ZoneIndicator(
    val code: String,
    val name: String,
    val value: Double,
    val unit: String?,
    val marginOfError: Double?,
    val referencePeriod: String?
)

data class ZoneDetail(
    val id: Int,
    val code: String,
    val name: String,
    val level: String,
    val indicators: List<ZoneIndicator>,
    // GeoJSON del polígono (puede ser null si el backend aún no lo devuelve en lista)
    val boundaryGeoJson: String? = null,
    val friendly: ZoneFriendly? = null
)

// ── Opportunity Finder (puntaje explicable) ───────────────────────────────

data class ScoreFactor(
    val code: String,
    val label: String,
    val weight: Int,
    val points: Double,
    val normalized: Double
)

data class ZoneScore(
    val status: String,          // "ok" | "insufficient" | "not_applicable"
    val score: Int?,
    val confidence: String?,     // "high" | "medium" | "low" | null
    val profile: String,
    val profileLabel: String,
    val note: String?,
    val comparedWith: Int,
    val reason: String?,
    val factors: List<ScoreFactor>
)

data class InvestProfile(val id: String, val label: String, val description: String)

data class ProfileMatch(val profile: String, val label: String, val matched: List<String>)

data class RankedZone(
    val zoneId: Int,
    val code: String,
    val name: String,
    val status: String,
    val score: Int?,
    val confidence: String?,
    val friendlyTitle: String? = null
)

data class Ranking(
    val profile: String,
    val label: String,
    val description: String,
    val note: String,
    val items: List<RankedZone>
)

// ── Propiedades ────────────────────────────────────────────────────────────

data class PropertyItem(
    val id: Int,
    val name: String,
    val propertyType: String,     // land | commercial | residential | other
    val address: String?,
    val lat: Double,
    val lon: Double,
    val areaValue: Double?,
    val areaUnit: String?,        // m2 | sqft | acre
    val askingPrice: Double?,
    val notes: String?,
    val zoneId: Int?,
    val createdAt: String
)

/** Datos que el usuario captura en el formulario de alta. */
data class PropertyDraft(
    val name: String,
    val propertyType: String,
    val lat: Double,
    val lon: Double,
    val askingPrice: Double?,
    val areaValue: Double?,
    val areaUnit: String?,
    val notes: String?
)

// ── Autenticación ──────────────────────────────────────────────────────────

data class AuthUser(
    val id: Int,
    val email: String,
    val displayName: String?
)

// ── Sesiones de campo ─────────────────────────────────────────────────────

/** Punto marcado desde el reloj. */
data class FieldPoint(
    val id: String,
    val tag: String,
    val lat: Double,
    val lon: Double,
    val observedAt: String,
    val note: String? = null
)

data class FieldSession(
    val id: String,       // UUID enviado desde el reloj
    val deviceId: String,
    val startedAt: String,
    val endedAt: String?,
    val observationCount: Int
)


// ── Análisis de ubicaciones propias ───────────────────────────────────────

data class AnalysisItem(
    val name: String, val kind: String, val zoneName: String?, val score: Int?, val confidence: String?, val status: String
)

data class LocationAnalysis(
    val profileLabel: String, val summary: String, val items: List<AnalysisItem>,
    val notes: List<String>, val aiText: String?, val aiStatus: String
)

// ── Reporte por zona (Etapa 9) ────────────────────────────────────────────

data class ReportIndicator(
    val name: String, val value: Double, val unit: String?, val marginOfError: Double?,
    val variationPct: Double?, val rankPosition: Int?, val rankOf: Int?, val period: String?
)

data class ReportItem(val ref: String?, val text: String)
data class ReportSource(val what: String, val name: String, val period: String?)

data class ReportFactor(val label: String, val weight: Int, val points: Double, val normalized: Double)

data class ZoneReport(
    val zoneName: String,
    val profileLabel: String,
    val generatedAt: String,
    val summary: String,
    val score: Int?,
    val confidence: String?,
    val indicators: List<ReportIndicator>,
    val strengths: List<ReportItem>,
    val warnings: List<ReportItem>,
    val missingData: List<String>,
    val verification: List<String>,
    val limitations: List<String>,
    val sources: List<ReportSource>,
    val methodology: String,
    val aiSummary: String? = null,
    val aiStatus: String = "none",
    val rankPosition: Int? = null,
    val rankOf: Int? = null,
    val factors: List<ReportFactor> = emptyList()
)
