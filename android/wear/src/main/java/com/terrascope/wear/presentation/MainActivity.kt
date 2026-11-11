package com.terrascope.wear.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.terrascope.wear.data.FieldStore
import com.terrascope.wear.data.SyncEvents
import com.terrascope.wear.data.WatchSync
import com.terrascope.wear.data.FieldTags
import com.terrascope.wear.data.ZoneStore
import com.terrascope.wear.data.WatchZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.terrascope.wear.data.hasLocationPermission
import com.terrascope.wear.data.readLocationOnce
import com.terrascope.wear.presentation.theme.TerraScopeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

private sealed class WearScreen {
    object Home : WearScreen()
    object Locating : WearScreen()
    data class Saved(val observationId: String) : WearScreen()
    object Zones : WearScreen()
    data class ZoneDetail(val zoneId: Int) : WearScreen()
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val store = remember { FieldStore(context) }
    val zoneStore = remember { ZoneStore(context) }
    var screen by remember { mutableStateOf<WearScreen>(WearScreen.Home) }
    var message by remember { mutableStateOf<String?>(null) }
    // Se incrementa al cambiar los datos locales para refrescar contadores.
    var localVersion by remember { mutableIntStateOf(0) }
    val syncTick by SyncEvents.tick.collectAsState()
    val version = localVersion + syncTick

    // Al abrir la app se reintenta enviar lo que haya quedado pendiente.
    LaunchedEffect(Unit) {
        WatchSync.push(context, store)
        withContext(Dispatchers.IO) { zoneStore.loadFromDataLayer() }
    }

    // Un toque: lee la ubicación y guarda el punto al instante con la etiqueta por defecto.
    fun mark() {
        scope.launch {
            message = null
            screen = WearScreen.Locating
            val loc = readLocationOnce(context)
            if (loc == null) {
                message = "Sin ubicación. Activa el GPS y reintenta."
                screen = WearScreen.Home
            } else {
                val saved = store.addObservation(loc.latitude, loc.longitude)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                WatchSync.push(context, store)
                localVersion++
                screen = WearScreen.Saved(saved.id)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) mark() else message = "Se necesita el permiso de ubicación."
    }

    TerraScopeTheme {
        AppScaffold {
            when (val current = screen) {
                is WearScreen.Home -> HomeScreen(
                    pending = remember(version) { store.pendingCount() },
                    total = remember(version) { store.totalCount() },
                    message = message,
                    zoneCount = remember(version) { zoneStore.zones().size },
                    onZones = { screen = WearScreen.Zones },
                    onMark = {
                        if (hasLocationPermission(context)) mark()
                        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )

                is WearScreen.Locating -> LocatingScreen()

                is WearScreen.Zones -> ZonesScreen(
                    zones = remember(version) { zoneStore.zones() },
                    onOpen = { screen = WearScreen.ZoneDetail(it.id) },
                    onBack = { screen = WearScreen.Home }
                )

                is WearScreen.ZoneDetail -> {
                    val zone = remember(version, current.zoneId) { zoneStore.zones().firstOrNull { it.id == current.zoneId } }
                    ZoneDetailScreen(zone, onBack = { screen = WearScreen.Zones })
                }

                is WearScreen.Saved -> SavedScreen(
                    onTag = { tag ->
                        store.updateTag(current.observationId, tag)
                        WatchSync.push(context, store)
                        localVersion++
                        screen = WearScreen.Home
                    },
                    onDone = { screen = WearScreen.Home }
                )
            }
        }
    }
}

// ── Inicio: un solo botón grande ──────────────────────────────────────────

@Composable
private fun HomeScreen(
    pending: Int, total: Int, message: String?, zoneCount: Int, onZones: () -> Unit, onMark: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text("TerraScope") }
            }
            item {
                Button(
                    onClick = onMark,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text("Marcar aquí") }
            }
            item {
                Button(
                    onClick = onZones,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text(if (zoneCount == 0) "Zonas guardadas" else "Zonas guardadas ($zoneCount)") }
            }
            message?.let { msg ->
                item { Text(msg, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            }
            item {
                Text(
                    text = when {
                        total == 0 -> "Toca para guardar este lugar"
                        pending == 0 -> "$total ${if (total == 1) "punto" else "puntos"} · todo enviado"
                        else -> "$total ${if (total == 1) "punto" else "puntos"} · $pending por enviar"
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LocatingScreen() {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text("Ubicando…") }
            }
            item { Text("Obteniendo ubicación", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

// ── Punto guardado: etiqueta opcional; si no se toca nada, vuelve solo al inicio ──

@Composable
private fun SavedScreen(onTag: (String) -> Unit, onDone: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    LaunchedEffect(Unit) {
        delay(8000)
        onDone()
    }
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { EdgeButton(onClick = onDone) { Text("Listo") } }
    ) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text("Punto guardado") }
            }
            item { Text("¿Qué es? (opcional)", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            FieldTags.forEach { (tag, label) ->
                item {
                    Button(
                        onClick = { onTag(tag) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec)
                    ) { Text(label) }
                }
            }
        }
    }
}

// ── Zonas guardadas (enviadas por el teléfono) ───────────────────────────

private fun zoneName(z: WatchZone) = z.name.replace(Regex("^Census Tract\\s+", RegexOption.IGNORE_CASE), "")

private fun confidenceLabel(c: String?) = when (c) {
    "high" -> "Confianza alta"
    "medium" -> "Confianza media"
    "low" -> "Confianza baja"
    else -> null
}

@Composable
private fun ZonesScreen(zones: List<WatchZone>, onOpen: (WatchZone) -> Unit, onBack: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { EdgeButton(onClick = onBack) { Text("Volver") } }
    ) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text("Zonas guardadas") }
            }
            if (zones.isEmpty()) {
                item {
                    Text(
                        "Aún no hay zonas. Guarda una en la app del teléfono (con la app abierta y sesión iniciada).",
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            zones.forEach { z ->
                item {
                    Button(
                        onClick = { onOpen(z) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec)
                    ) { Text(zoneName(z) + (z.score?.let { " · $it" } ?: "")) }
                }
            }
        }
    }
}

@Composable
private fun ZoneDetailScreen(zone: WatchZone?, onBack: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { EdgeButton(onClick = onBack) { Text("Volver") } }
    ) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) { Text(zone?.let { zoneName(it) } ?: "Zona") }
            }
            if (zone == null) {
                item { Text("Esta zona ya no está guardada.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else if (zone.score != null) {
                item { Text("Puntaje ${zone.score}/100", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                zone.profile?.let { item { Text(it, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } }
                confidenceLabel(zone.confidence)?.let { item { Text(it, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } }
                item { Text("Puntaje relativo, solo demográfico.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else {
                item { Text("Sin puntaje para esta zona (datos insuficientes o no aplica).", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun DefaultPreview() {
    TerraScopeTheme {
        AppScaffold { HomeScreen(pending = 2, total = 5, message = null, zoneCount = 3, onZones = {}, onMark = {}) }
    }
}
