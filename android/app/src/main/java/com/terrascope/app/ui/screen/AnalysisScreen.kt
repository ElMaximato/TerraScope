package com.terrascope.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.model.AnalysisItem
import com.terrascope.app.data.model.LocationAnalysis
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.InvestSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AnalysisState {
    object Loading : AnalysisState()
    data class Success(val analysis: LocationAnalysis) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class AnalysisViewModel : ViewModel() {
    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Loading)
    val state: StateFlow<AnalysisState> = _state
    private val _ai = MutableStateFlow<AiState>(AiState.Idle)
    val ai: StateFlow<AiState> = _ai

    fun load(token: String) {
        viewModelScope.launch {
            _state.value = AnalysisState.Loading
            _ai.value = AiState.Idle
            _state.value = try {
                AnalysisState.Success(TerraScopeApi.fetchAnalysis(token, InvestSelection.profile.value))
            } catch (e: Exception) {
                AnalysisState.Error(e.message ?: "No se pudo generar el análisis")
            }
        }
    }

    /** El texto con IA se pide solo cuando el usuario toca "Reporte IA". */
    fun generateAi(token: String) {
        if (_ai.value is AiState.Loading) return
        viewModelScope.launch {
            _ai.value = AiState.Loading
            _ai.value = try {
                val a = TerraScopeApi.fetchAnalysis(token, InvestSelection.profile.value, withAi = true)
                a.aiText?.let { AiState.Done(it) } ?: AiState.Failed(aiFailureMessage(a.aiStatus))
            } catch (e: Exception) {
                AiState.Failed(aiFailureMessage("error"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel, token: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val ai by viewModel.ai.collectAsState()
    LaunchedEffect(token) { viewModel.load(token) }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mis ubicaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = BrandPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is AnalysisState.Loading -> CenteredLoader()
                is AnalysisState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }
                is AnalysisState.Success -> {
                    val a = s.analysis
                    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item {
                            Text("Perfil: ${a.profileLabel}", fontSize = 12.sp, color = BrandTextMuted)
                        }
                        item {
                            Surface(shape = RoundedCornerShape(24.dp), color = BrandPrimary, modifier = Modifier.fillMaxWidth()) {
                                Text(a.summary, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
                            }
                        }
                        if (a.items.any { it.score != null }) {
                            item {
                                AiReportSection(
                                    state = ai,
                                    buttonLabel = "Reporte IA de mis ubicaciones",
                                    loadingSteps = listOf(
                                        "Ubicando tus puntos en las zonas…",
                                        "Comparando tus ubicaciones entre sí…",
                                        "Pensando qué conviene más y qué menos…",
                                        "Redactando el resumen…"
                                    ),
                                    onGenerate = { viewModel.generateAi(token) }
                                )
                            }
                        }
                        if (a.items.isEmpty()) item { EmptyHint("Aún no tienes ubicaciones. Marca puntos con el reloj o registra propiedades.") }
                        items(a.items) { LocationRow(it) }
                        item {
                            AnalysisCard("Ten en cuenta") {
                                a.notes.forEach { Text("• $it", fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(bottom = 4.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(i: AnalysisItem) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = BrandSurface, shadowElevation = 2.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(i.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrandText)
                Text(
                    (if (i.kind == "property") "Propiedad" else "Punto del reloj") + " · " +
                        (i.zoneName?.let { shortZoneName(it) } ?: "fuera de las zonas"),
                    fontSize = 12.sp, color = BrandTextMuted
                )
            }
            Text(
                i.score?.let { "$it" } ?: "—", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                color = if (i.score != null) BrandPrimary else BrandTextMuted
            )
        }
    }
}

@Composable
private fun AnalysisCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = BrandSurface, shadowElevation = 2.dp) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrandText)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
