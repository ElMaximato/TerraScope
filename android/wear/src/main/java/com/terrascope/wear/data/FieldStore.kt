package com.terrascope.wear.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** Etiquetas permitidas por el backend (FIELD_TAGS) que se ofrecen en el reloj. */
val FieldTags = listOf(
    "land_potential" to "Terreno potencial",
    "commercial_space" to "Local comercial",
    "competitor" to "Competidor",
    "road_access" to "Acceso vial",
    "observed_risk" to "Riesgo",
    "needs_research" to "Investigar"
)

/** Etiqueta por defecto: el punto se guarda al instante y se clasifica (opcionalmente) después. */
const val DefaultTag = "needs_research"

data class WatchObservation(
    val id: String,
    val tag: String,
    val note: String?,
    val lat: Double,
    val lon: Double,
    val observedAt: String,   // ISO 8601 UTC
    val synced: Boolean
)

/**
 * Almacén local del reloj (SharedPreferences + JSON). Cada punto se guarda aquí primero, con UUID propio,
 * para que funcione sin teléfono cerca y la sincronización posterior sea idempotente.
 */
class FieldStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("terrascope_field", Context.MODE_PRIVATE)

    /** Identificador estable de este reloj (máx. 40 caracteres para el backend). */
    val deviceId: String
        get() = prefs.getString("device", null) ?: ("wear-" + UUID.randomUUID().toString().take(8)).also {
            prefs.edit().putString("device", it).apply()
        }

    @Synchronized
    fun observations(): List<WatchObservation> = readArray().map {
        WatchObservation(
            id = it.getString("id"),
            tag = it.getString("tag"),
            note = if (it.isNull("note")) null else it.getString("note"),
            lat = it.getDouble("lat"),
            lon = it.getDouble("lon"),
            observedAt = it.getString("observedAt"),
            synced = it.optBoolean("synced", false)
        )
    }

    fun pendingCount(): Int = observations().count { !it.synced }

    fun totalCount(): Int = observations().size

    @Synchronized
    fun addObservation(lat: Double, lon: Double, tag: String = DefaultTag): WatchObservation {
        val o = WatchObservation(UUID.randomUUID().toString(), tag, null, lat, lon, Instant.now().toString(), false)
        write(observations() + o)
        return o
    }

    /** Cambia la etiqueta de un punto ya guardado; vuelve a quedar pendiente de enviar. */
    @Synchronized
    fun updateTag(id: String, tag: String) {
        write(observations().map { if (it.id == id) it.copy(tag = tag, synced = false) else it })
    }

    /** Marca como sincronizados los puntos confirmados por el teléfono (se usa en la etapa 8B). */
    @Synchronized
    fun markSynced(ids: Set<String>) {
        write(observations().map { if (it.id in ids) it.copy(synced = true) else it })
    }

    private fun toJson(o: WatchObservation) = JSONObject()
        .put("id", o.id).put("tag", o.tag).put("note", o.note ?: JSONObject.NULL)
        .put("lat", o.lat).put("lon", o.lon).put("observedAt", o.observedAt).put("synced", o.synced)

    private fun readArray(): List<JSONObject> {
        val raw = prefs.getString("points", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun write(items: List<WatchObservation>) {
        val arr = JSONArray()
        items.forEach { arr.put(toJson(it)) }
        prefs.edit().putString("points", arr.toString()).apply()
    }
}
