package com.terrascope.wear.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray

/** Recibe del teléfono la confirmación de los puntos que ya están en el servidor. */
class AckListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchSync.PATH_ACK) return
        try {
            val arr = JSONArray(String(event.data, Charsets.UTF_8))
            val ids = (0 until arr.length()).map { arr.getString(it) }.toSet()
            FieldStore(this).markSynced(ids)
            SyncEvents.tick.value = SyncEvents.tick.value + 1
        } catch (e: Exception) {
            // Mensaje mal formado: se ignora; los puntos seguirán pendientes.
        }
    }
}
