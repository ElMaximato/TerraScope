package com.terrascope.app.data.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.viewmodel.InvestSelection
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Se incrementa cuando cambia la lista de zonas guardadas, para que se reenvíe al reloj. */
object ZoneSyncTrigger {
    val tick = MutableStateFlow(0)
    fun bump() { tick.value = tick.value + 1 }
}

/**
 * Teléfono -> reloj: publica las zonas guardadas con su puntaje (perfil de inversión elegido) como DataItem
 * "/zones/saved". Bloqueante: usar fuera del hilo principal.
 */
object PhoneZoneSync {
    private const val TAG = "TerraSync"
    const val PATH_ZONES = "/zones/saved"

    suspend fun refresh(context: Context, token: String) {
        try {
            val profile = InvestSelection.profile.value
            val arr = JSONArray()
            for (z in TerraScopeApi.fetchWatchlist(token)) {
                val o = JSONObject().put("id", z.id).put("name", z.name).put("level", z.level)
                try {
                    val s = TerraScopeApi.fetchZoneScore(z.id, token, profile)
                    o.put("status", s.status).put("profile", s.profileLabel)
                    s.score?.let { o.put("score", it) }
                    s.confidence?.let { o.put("confidence", it) }
                } catch (e: Exception) {
                    o.put("status", "unavailable")
                }
                arr.put(o)
            }
            val request = PutDataMapRequest.create(PATH_ZONES).apply {
                dataMap.putString("json", arr.toString())
                dataMap.putLong("updatedAt", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Tasks.await(Wearable.getDataClient(context.applicationContext).putDataItem(request))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron enviar las zonas al reloj: ${e.message}")
        }
    }
}
