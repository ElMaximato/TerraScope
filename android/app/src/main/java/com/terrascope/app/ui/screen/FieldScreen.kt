package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.terrascope.app.data.model.FieldPoint
import com.terrascope.app.data.wear.PhoneFieldSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.FieldViewModel
import com.terrascope.app.ui.viewmodel.ListState

fun fieldTagLabel(tag: String): String = when (tag) {
    "land_potential" -> "Terreno potencial"
    "commercial_space" -> "Local comercial"
    "competitor" -> "Competidor"
    "road_access" -> "Acceso vial"
    "observed_risk" -> "Riesgo"
    "construction" -> "Construcción"
    "infrastructure" -> "Infraestructura"
    else -> "Investigar"
}

@Composable
fun FieldScreen(
    viewModel: FieldViewModel,
    token: String,
    onEvaluate: (lat: Double, lon: Double, name: String) -> Unit,
    onAnalyze: () -> Unit = {}
) {
    val context = LocalContext.current
    val pointsState by viewModel.points.collectAsState()
    var syncing by remember { mutableStateOf(false) }
    var syncedNow by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<FieldPoint?>(null) }
    var deleting by remember { mutableStateOf<FieldPoint?>(null) }
    val actionError by viewModel.actionError.collectAsState()

    // Al abrir (o al pulsar "Sincronizar"): sube lo que el reloj haya dejado pendiente y recarga la lista.
    LaunchedEffect(token, reload) {
        syncing = true
        syncedNow = try {
            withContext(Dispatchers.IO) { PhoneFieldSync.processPending(context) }
        } catch (e: Exception) { 0 }
        syncing = false
        viewModel.load(token)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Campo", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = BrandText)
                Text(
                    "Lugares que marcaste desde tu reloj",
                    fontSize = 13.sp,
                    color = BrandTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            TextButton(onClick = { reload++ }, enabled = !syncing) {
                Text(if (syncing) "Sincronizando…" else "Sincronizar", color = BrandPrimary)
            }
        }
        OutlinedButton(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(44.dp),
            shape = RoundedCornerShape(22.dp)
        ) { Text("Analizar mis ubicaciones", fontWeight = FontWeight.SemiBold, color = BrandPrimary) }
        if (syncedNow > 0) {
            Text(
                "Se recibieron $syncedNow ${if (syncedNow == 1) "punto nuevo" else "puntos nuevos"} del reloj.",
                fontSize = 12.sp,
                color = BrandPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
        }
        actionError?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp))
        }
        Spacer(Modifier.height(8.dp))

        when (val s = pointsState) {
            is ListState.Loading -> CenteredLoader()
            is ListState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }
            is ListState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyHint("Aún no hay puntos.\nToca \"Marcar aquí\" en tu reloj Wear OS cuando veas un terreno o local.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.data, key = { it.id }) { point ->
                            FieldPointCard(
                                point,
                                onEvaluate = {
                                    onEvaluate(point.lat, point.lon, point.note ?: "Punto del reloj · ${fieldTagLabel(point.tag)}")
                                },
                                onEdit = { editing = point },
                                onDelete = { deleting = point }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    FieldDialogs(
        editing = editing, deleting = deleting,
        onSave = { p, name, tag -> viewModel.edit(p.id, name, tag, token); editing = null },
        onConfirmDelete = { p -> viewModel.delete(p.id, token); deleting = null },
        onDismiss = { editing = null; deleting = null }
    )
}

@Composable
private fun FieldDialogs(
    editing: FieldPoint?, deleting: FieldPoint?,
    onSave: (FieldPoint, String, String) -> Unit, onConfirmDelete: (FieldPoint) -> Unit,
    onDismiss: () -> Unit
) {
    editing?.let { p ->
        var name by remember(p.id) { mutableStateOf((p.note ?: "").take(80)) }
        var tag by remember(p.id) { mutableStateOf(p.tag) }
        val tags = listOf("land_potential", "commercial_space", "construction", "competitor",
            "infrastructure", "road_access", "observed_risk", "needs_research")
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar punto") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(80) },
                        label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("${name.length}/80", fontSize = 11.sp, color = BrandTextMuted,
                                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Tipo", fontSize = 12.sp, color = BrandTextMuted)
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(tags) { t ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = tag == t, onClick = { tag = t })
                                Text(fieldTagLabel(t), fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSave(p, name, tag) }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("¿Borrar este punto?") },
            text = { Text(p.note ?: fieldTagLabel(p.tag)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(p) }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FieldPointCard(point: FieldPoint, onEvaluate: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(point.note ?: fieldTagLabel(point.tag), fontWeight = FontWeight.SemiBold, color = BrandText, fontSize = 15.sp)
                    if (point.note != null) Text(fieldTagLabel(point.tag), fontSize = 12.sp, color = BrandPrimary)
                }
                Text(formatSessionDate(point.observedAt), fontSize = 11.sp, color = BrandTextMuted)
            }
            Text(
                "%.5f, %.5f".format(point.lat, point.lon),
                fontSize = 11.sp,
                color = BrandTextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEvaluate,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
            ) { Text("Evaluar este punto", fontWeight = FontWeight.SemiBold) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("Editar", color = BrandPrimary) }
                TextButton(onClick = onDelete) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/** Convierte ISO 8601 a formato legible (básico sin librerías extra). */
fun formatSessionDate(isoStr: String): String {
    return try {
        // "2026-11-06T10:30:00.000Z" -> "06 Nov 2026, 10:30"
        val parts = isoStr.substringBefore('Z').substringBefore('+').split('T')
        val dateParts = parts[0].split('-')
        val timeParts = parts.getOrNull(1)?.split(':') ?: emptyList()
        val months = listOf("","Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
        val month = dateParts.getOrNull(1)?.toIntOrNull()?.let { months.getOrElse(it) { it.toString() } } ?: ""
        val day = dateParts.getOrNull(2) ?: ""
        val year = dateParts.getOrNull(0) ?: ""
        val time = "${timeParts.getOrElse(0){"00"}}:${timeParts.getOrElse(1){"00"}}"
        "$day $month $year, $time"
    } catch (e: Exception) {
        isoStr
    }
}

