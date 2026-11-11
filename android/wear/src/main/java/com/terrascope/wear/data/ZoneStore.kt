package com.terrascope.wear.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray

data class WatchZone(
    val id: Int, val name: String, val level: String,
    val status: String, val score: Int?, val confidence: String?, val profile: String?
)

/** Zonas guardadas que el teléfono publica ("/zones/saved"); se guardan localmente para verlas sin conexión. */
class ZoneStore(context: Context) {
    private val ctx = context.applicationContext
    private val prefs = ctx.getSharedPreferences("terrascope_zones", Context.MODE_PRIVATE)

    fun zones(): List<WatchZone> {
        val raw = prefs.getString("zones", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                WatchZone(
                    id = o.getInt("id"), name = o.optString("name"), level = o.optString("level"),
                    status = o.optString("status"),
                    score = if (o.has("score")) o.getInt("score") else null,
                    confidence = if (o.has("confidence")) o.getString("confidence") else null,
                    profile = if (o.has("profile")) o.getString("profile") else null
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun saveJson(json: String) {
        prefs.edit().putString("zones", json).apply()
        SyncEvents.tick.value = SyncEvents.tick.value + 1
    }

    /** Lee lo último que publicó el teléfono (por si el servicio no alcanzó a recibirlo). Bloqueante. */
    fun loadFromDataLayer() {
        try {
            val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH_ZONES).build()
            val buffer = Tasks.await(Wearable.getDataClient(ctx).getDataItems(uri))
            try {
                for (item in buffer) DataMapItem.fromDataItem(item).dataMap.getString("json")?.let { saveJson(it) }
            } finally { buffer.release() }
        } catch (e: Exception) { /* sin conexión: se queda lo guardado */ }
    }

    companion object { const val PATH_ZONES = "/zones/saved" }
}
