package com.terrascope.app.data.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Se despierta cuando el reloj publica puntos nuevos, aunque la app del teléfono esté cerrada. */
class FieldSyncService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == PhoneFieldSync.PATH_POINTS) {
                try {
                    PhoneFieldSync.processItem(this, event.dataItem)
                } catch (e: Exception) {
                    android.util.Log.w("TerraSync", "Error procesando puntos: ${e.message}")
                }
            }
        }
    }
}
