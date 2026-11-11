package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.model.PropertyItem
import com.terrascope.app.data.model.RankedZone
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.InvestSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────

sealed class VerdictState {
    object Loading : VerdictState()
    data class Ready(
        val property: PropertyItem,
        val profileLabel: String,
        val zone: RankedZone?,      // null si la propiedad cae fuera de los tracts puntuados
        val position: Int?,         // lugar en el ranking (1 = mejor)
        val total: Int,
        val best: RankedZone?
    ) : VerdictState()
    data class Error(val message: String) : VerdictState()
}

class PropertyVerdictViewModel : ViewModel() {
    private val _state = MutableStateFlow<VerdictState>(VerdictState.Loading)
    val state: StateFlow<VerdictState> = _state

    fun load(propertyId: Int, token: String) {
        viewModelScope.launch {
            _state.value = VerdictState.Loading
            _state.value = try {
                val property = TerraScopeApi.fetchProperties(token).firstOrNull { it.id == propertyId }
                    ?: throw IllegalStateException("No se encontró la propiedad")
                val ranking = TerraScopeApi.fetchRanking(InvestSelection.profile.value, token)
                val idx = ranking.items.indexOfFirst { it.zoneId == property.zoneId }
                VerdictState.Ready(
                    property = property,
                    profileLabel = ranking.label,
                    zone = if (idx >= 0) ranking.items[idx] else null,
                    position = if (idx >= 0 && ranking.items[idx].score != null) idx + 1 else null,
                    total = ranking.items.size,
                    best = ranking.items.firstOrNull { it.score != null }
                )
            } catch (e: Exception) {
                VerdictState.Error(e.message ?: "No se pudo cargar la propiedad")
            }
        }
    }
}

// ── Pantalla ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyVerdictScreen(
    viewModel: PropertyVerdictViewModel,
    propertyId: Int,
    token: String,
    onBack: () -> Unit,
    onOpenZone: (Int) -> Unit,
    onCompare: () -> Unit,
    onEdit: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(propertyId, token) { viewModel.load(propertyId, token) }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = BrandPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { onEdit(propertyId) }) { Text("Editar", color = BrandPrimary, fontWeight = FontWeight.SemiBold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is VerdictState.Loading -> CenteredLoader()
                is VerdictState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }
                is VerdictState.Ready -> VerdictContent(s, onOpenZone, onCompare)
            }
        }
    }
}

@Composable
private fun VerdictContent(s: VerdictState.Ready, onOpenZone: (Int) -> Unit, onCompare: () -> Unit) {
    val p = s.property
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(propertyTypeLabel(p.propertyType).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            Text(p.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BrandText, modifier = Modifier.padding(top = 4.dp))
            Text("%.5f, %.5f".format(p.lat, p.lon), fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 2.dp))
        }

        // Veredicto
        Surface(shape = RoundedCornerShape(28.dp), color = BrandPrimary, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text("Veredicto de la zona · ${s.profileLabel}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                when {
                    p.zoneId == null -> {
                        VerdictTitle("Fuera de las zonas cubiertas")
                        VerdictText("Esta ubicación no cae dentro de los límites de Somerton que tiene TerraScope, así que no hay datos para evaluarla.")
                    }
                    s.zone == null -> {
                        VerdictTitle("Sin puntaje para esta ubicación")
                        VerdictText("Está dentro de la ciudad, pero no en una de las zonas censales que se puntúan.")
                    }
                    s.zone.score == null || s.position == null -> {
                        VerdictTitle("${s.zone.friendlyTitle ?: "Zona ${shortZoneName(s.zone.name)}"}: datos insuficientes")
                        VerdictText("A esta zona le faltan indicadores para calcular un puntaje confiable.")
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                            Text("${s.zone.score}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = BrandAccent)
                            Text(" / 100", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                        }
                        VerdictText("${s.zone.friendlyTitle ?: "Zona ${shortZoneName(s.zone.name)}"} (zona censal ${shortZoneName(s.zone.name)}) · #${s.position} de ${s.total} para ${s.profileLabel}")
                        Spacer(Modifier.height(8.dp))
                        val best = s.best
                        VerdictText(
                            if (s.position == 1) "Es la mejor zona de Somerton para este tipo de inversión."
                            else if (best != null) "La mejor zona para este perfil es ${best.friendlyTitle ?: "la ${shortZoneName(best.name)}"} (${best.score} puntos)."
                            else ""
                        )
                        VerdictText(
                            when (s.zone.confidence) {
                                "high" -> "Confianza de los datos: alta."
                                "medium" -> "Confianza de los datos: media (algún indicador tiene margen de error amplio)."
                                "low" -> "Confianza de los datos: baja."
                                else -> ""
                            }
                        )
                    }
                }
                Text(
                    "Estimación basada solo en demografía del Census. No considera competencia, precios ni tráfico. Cambia el perfil en la pestaña Invertir.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        // Datos de la oferta
        Surface(shape = RoundedCornerShape(22.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("La oferta", fontWeight = FontWeight.Bold, color = BrandText, fontSize = 15.sp)
                p.askingPrice?.let { DataLine("Precio", "\$${"%,.0f".format(it)}") }
                if (p.areaValue != null) {
                    DataLine("Superficie", "${"%,.0f".format(p.areaValue)} ${areaUnitLabel(p.areaUnit)}")
                    if (p.askingPrice != null && p.areaValue > 0) {
                        DataLine("Precio por ${areaUnitLabel(p.areaUnit)}", "\$${"%,.2f".format(p.askingPrice / p.areaValue)}")
                    }
                }
                if (p.askingPrice == null && p.areaValue == null) {
                    Text("No registraste precio ni superficie.", fontSize = 12.sp, color = BrandTextMuted)
                }
                p.notes?.let { Text(it, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 4.dp)) }
            }
        }

        p.zoneId?.let { zoneId ->
            Button(
                onClick = { onOpenZone(zoneId) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) { Text("Ver datos de la zona", fontWeight = FontWeight.SemiBold) }
        }
        OutlinedButton(
            onClick = onCompare,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
        ) { Text("Comparar zonas", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun VerdictTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandAccent, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun VerdictText(text: String) {
    if (text.isNotBlank()) Text(text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun DataLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = BrandTextMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandText)
    }
}
