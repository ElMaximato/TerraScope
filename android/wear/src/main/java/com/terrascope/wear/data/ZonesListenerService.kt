package com.terrascope.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Recibe las zonas guardadas que publica el teléfono. */
class ZonesListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == ZoneStore.PATH_ZONES) {
                DataMapItem.fromDataItem(e.dataItem).dataMap.getString("json")?.let { ZoneStore(this).saveJson(it) }
            }
        }
    }
}
