package com.terrascope.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Avisos internos para refrescar la pantalla cuando llega una confirmación del teléfono. */
object SyncEvents {
    val tick = kotlinx.coroutines.flow.MutableStateFlow(0)
}

/**
 * Reloj -> teléfono por la Data Layer. Se publica la lista completa de puntos pendientes en un DataItem
 * ("/field/points"); el sistema lo entrega al teléfono en cuanto haya conexión (aunque se haya marcado sin él).
 * El teléfono responde con un mensaje "/field/ack" con los ids que el servidor ya recibió.
 */
object WatchSync {
    private const val TAG = "TerraWear"
    const val PATH_POINTS = "/field/points"
    const val PATH_ACK = "/field/ack"

    fun push(context: Context, store: FieldStore) {
        val pending = store.observations().filter { !it.synced }
        if (pending.isEmpty()) return
        val arr = JSONArray()
        pending.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("tag", it.tag).put("lat", it.lat).put("lon", it.lon)
                    .put("observedAt", it.observedAt)
            )
        }
        try {
            val request = PutDataMapRequest.create(PATH_POINTS).apply {
                dataMap.putString("json", arr.toString())
                dataMap.putString("device", store.deviceId)
                dataMap.putLong("sentAt", System.currentTimeMillis())   // fuerza el cambio aunque la lista sea igual
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context.applicationContext).putDataItem(request)
                .addOnFailureListener { Log.w(TAG, "No se pudo publicar los puntos: ${it.message}") }
        } catch (e: Exception) {
            Log.w(TAG, "Data Layer no disponible: ${e.message}")
        }
    }
}
