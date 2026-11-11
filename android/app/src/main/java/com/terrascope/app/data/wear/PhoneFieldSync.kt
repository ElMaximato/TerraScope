package com.terrascope.app.data.wear

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.terrascope.app.data.local.TokenStore
import com.terrascope.app.data.network.TerraScopeApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Teléfono: recibe los puntos del reloj (Data Layer), los sube al backend (`POST /api/field/sync`, idempotente)
 * y le confirma al reloj cuáles ya están guardados. Todas las funciones son bloqueantes: usarlas fuera del hilo principal.
 */
object PhoneFieldSync {
    private const val TAG = "TerraSync"
    const val PATH_POINTS = "/field/points"
    private const val PATH_ACK = "/field/ack"

    /** Procesa los puntos pendientes que el reloj ya dejó publicados. Devuelve cuántos se confirmaron. */
    fun processPending(context: Context): Int {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH_POINTS).build()
        val buffer = Tasks.await(Wearable.getDataClient(context).getDataItems(uri))
        var total = 0
        try {
            for (item in buffer) total += processItem(context, item)
        } finally {
            buffer.release()
        }
        return total
    }

    fun processItem(context: Context, item: DataItem): Int {
        val token = runBlocking { TokenStore.tokenFlow(context).first() }
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Sin sesión en el teléfono: los puntos del reloj quedan pendientes")
            return 0
        }
        val json = DataMapItem.fromDataItem(item).dataMap.getString("json") ?: return 0
        val arr = JSONArray(json)
        val points = (0 until arr.length()).map { arr.getJSONObject(it) }
        if (points.isEmpty()) return 0

        val confirmed = try {
            runBlocking { TerraScopeApi.syncObservations(points, token) }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo subir al servidor: ${e.message}")
            return 0
        }
        if (confirmed.isEmpty()) return 0
        // El DataItem es una cola de pendientes: si ya quedó todo en el servidor se elimina, para que no se
        // vuelva a subir (y pise ediciones o borrados hechos en el teléfono). El reloj republica lo que cambie.
        if (points.all { it.optString("id") in confirmed }) {
            try { Tasks.await(Wearable.getDataClient(context).deleteDataItems(item.uri)) }
            catch (e: Exception) { Log.w(TAG, "No se pudo limpiar el DataItem: ${e.message}") }
        }

        val nodeId = item.uri.host
        if (!nodeId.isNullOrEmpty()) {
            try {
                val ack = JSONArray(confirmed.toList()).toString().toByteArray(Charsets.UTF_8)
                Tasks.await(Wearable.getMessageClient(context).sendMessage(nodeId, PATH_ACK, ack))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo avisar al reloj: ${e.message}")
            }
        }
        return confirmed.size
    }
}
