package com.terrascope.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.ReportPdf
import com.terrascope.app.data.model.ReportFactor
import com.terrascope.app.data.model.ReportIndicator
import com.terrascope.app.data.model.ZoneReport
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.InvestSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReportState {
    object Loading : ReportState()
    data class Success(val report: ZoneReport) : ReportState()
    data class Error(val message: String) : ReportState()
}

class ReportViewModel : ViewModel() {
    private val _state = MutableStateFlow<ReportState>(ReportState.Loading)
    val state: StateFlow<ReportState> = _state
    private val _ai = MutableStateFlow<AiState>(AiState.Idle)
    val ai: StateFlow<AiState> = _ai

    fun load(zoneId: Int, token: String) {
        viewModelScope.launch {
            _state.value = ReportState.Loading
            _ai.value = AiState.Idle
            _state.value = try {
                ReportState.Success(TerraScopeApi.fetchZoneReport(zoneId, token, InvestSelection.profile.value))
            } catch (e: Exception) {
                ReportState.Error(e.message ?: "No se pudo cargar el reporte")
            }
        }
    }

    /** El resumen con IA se pide solo cuando el usuario toca "Reporte IA". */
    fun generateAi(zoneId: Int, token: String) {
        if (_ai.value is AiState.Loading) return
        viewModelScope.launch {
            _ai.value = AiState.Loading
            try {
                val r = TerraScopeApi.fetchZoneReport(zoneId, token, InvestSelection.profile.value, withAi = true)
                val text = r.aiSummary
                if (text != null) {
                    (_state.value as? ReportState.Success)?.let { _state.value = ReportState.Success(it.report.copy(aiSummary = text)) }
                    _ai.value = AiState.Done(text)
                } else {
                    _ai.value = AiState.Failed(aiFailureMessage(r.aiStatus))
                }
            } catch (e: Exception) {
                _ai.value = AiState.Failed(aiFailureMessage("error"))
            }
        }
    }
}

// ── Colores y textos amigables ────────────────────────────────────────────

private val Good = Color(0xFF2E8B57)
private val Mid = Color(0xFFC98A1B)
private val Bad = Color(0xFFC4473A)
private val GoodSoft = Color(0xFFE6F4EC)
private val MidSoft = Color(0xFFFBF1DC)

private fun scoreColor(score: Int) = when { score >= 67 -> Good; score >= 34 -> Mid; else -> Bad }
private fun levelOf(n: Double) = when { n >= 0.67 -> "Alto"; n <= 0.33 -> "Bajo"; else -> "Medio" }
private fun levelColor(n: Double) = when { n >= 0.67 -> Good; n <= 0.33 -> Bad; else -> Mid }
private fun confText(c: String?) = when (c) { "high" -> "Datos firmes"; "medium" -> "Datos aproximados"; "low" -> "Datos poco firmes"; else -> "Sin confianza calculada" }
private fun confColor(c: String?) = when (c) { "high" -> Good; "medium" -> Mid; "low" -> Bad; else -> BrandTextMuted }
private fun fmt(v: Double) = if (v >= 100) "%,.0f".format(v) else "%.1f".format(v)

private fun precision(variationPct: Double?) = when {
    variationPct == null -> null
    variationPct < 12 -> "Dato firme" to Good
    variationPct <= 40 -> "Dato aproximado" to Mid
    else -> "Dato poco firme" to Bad
}

private val AiSteps = listOf(
    "Leyendo los datos de la zona…",
    "Comparando con las otras zonas de Somerton…",
    "Revisando fortalezas y cuidados…",
    "Redactando el resumen en lenguaje sencillo…"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel, zoneId: Int, token: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val ai by viewModel.ai.collectAsState()
    val context = LocalContext.current
    var pdfError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(zoneId, token) { viewModel.load(zoneId, token) }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Reporte de zona", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = BrandPrimary)
                    }
                },
                actions = {
                    val s = state
                    if (s is ReportState.Success) {
                        TextButton(onClick = {
                            try { ReportPdf.share(context, s.report); pdfError = null }
                            catch (e: Exception) { pdfError = "No se pudo generar el PDF" }
                        }) { Text("PDF", color = BrandPrimary, fontWeight = FontWeight.SemiBold) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is ReportState.Loading -> CenteredLoader()
                is ReportState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }
                is ReportState.Success -> {
                    val r = s.report
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text(shortZoneName(r.zoneName), fontWeight = FontWeight.Bold, fontSize = 26.sp, color = BrandText)
                                Spacer(Modifier.height(6.dp))
                                Pill(r.profileLabel, BrandPrimarySoft, BrandPrimary)
                            }
                        }
                        item { HeroCard(r) }
                        pdfError?.let { item { ErrorChip(it) } }

                        if (r.score != null) {
                            item {
                                AiReportSection(state = ai, loadingSteps = AiSteps, onGenerate = { viewModel.generateAi(zoneId, token) })
                            }
                        }

                        if (r.factors.isNotEmpty()) {
                            item {
                                Section("Cómo le va frente a las demás zonas", "Cada barra compara esta zona con las otras de Somerton (más llena = mejor).") {
                                    r.factors.forEach { FactorBar(it) }
                                }
                            }
                        }

                        item {
                            Section("Los datos detrás del puntaje", "Cifras del Census. Los puntos muestran en qué lugar queda entre las zonas.") {
                                r.indicators.forEachIndexed { i, ind ->
                                    if (i > 0) HorizontalDivider(color = BrandLine, modifier = Modifier.padding(vertical = 10.dp))
                                    IndicatorRow(ind)
                                }
                            }
                        }

                        if (r.strengths.isNotEmpty()) {
                            item { Callout("Lo que juega a su favor", r.strengths.map { it.text }, GoodSoft, Good, "+") }
                        }
                        if (r.warnings.isNotEmpty()) {
                            item { Callout("Lo que hay que cuidar", r.warnings.map { it.text }, MidSoft, Mid, "!") }
                        }
                        if (r.missingData.isNotEmpty()) {
                            item { Callout("Datos que faltan", r.missingData, MidSoft, Mid, "?") }
                        }

                        item {
                            Section("Antes de decidir, revisa en campo", "Cosas que este reporte no puede saber.") {
                                r.verification.forEach { CheckItem(it) }
                            }
                        }

                        item { HowToRead(r) }
                    }
                }
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun HeroCard(r: ZoneReport) {
    Surface(shape = RoundedCornerShape(28.dp), color = BrandSurface, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (r.score != null) {
                Gauge(r.score)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.rankPosition?.let { Pill("${it}.º de ${r.rankOf} zonas", BrandPrimarySoft, BrandPrimary) }
                    Pill(confText(r.confidence), confColor(r.confidence).copy(alpha = 0.14f), confColor(r.confidence))
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(r.summary, fontSize = 14.sp, color = BrandText, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun Gauge(score: Int) {
    val progress by animateFloatAsState(score / 100f, tween(900), label = "gauge")
    val color = scoreColor(score)
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(BrandLine, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = color)
            Text("de 100", fontSize = 12.sp, color = BrandTextMuted)
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandText)
            subtitle?.let { Text(it, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 2.dp)) }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun FactorBar(f: ReportFactor) {
    val progress by animateFloatAsState(f.normalized.toFloat().coerceIn(0f, 1f), tween(800), label = "bar")
    val color = levelColor(f.normalized)
    Column(Modifier.padding(bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(f.label, fontSize = 13.sp, color = BrandText, fontWeight = FontWeight.Medium)
            Text(levelOf(f.normalized), fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BrandLine)) {
            Box(Modifier.fillMaxWidth(progress.coerceAtLeast(0.03f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color))
        }
        Text("Aporta ${"%.0f".format(f.points)} de ${f.weight} puntos", fontSize = 11.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun IndicatorRow(i: ReportIndicator) {
    Column(Modifier.fillMaxWidth()) {
        Text(i.name, fontSize = 12.sp, color = BrandTextMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt(i.value), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandText)
            Text(" ${i.unit ?: ""}", fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(bottom = 3.dp))
            Spacer(Modifier.weight(1f))
            precision(i.variationPct)?.let { (t, c) -> Pill(t, c.copy(alpha = 0.14f), c) }
        }
        if (i.rankPosition != null && i.rankOf != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (n in 1..i.rankOf) {
                    Box(
                        Modifier.padding(end = 6.dp).size(if (n == i.rankPosition) 14.dp else 10.dp).clip(CircleShape)
                            .background(if (n == i.rankPosition) BrandPrimary else BrandLine)
                    )
                }
                Text("${i.rankPosition}.º de ${i.rankOf}", fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun Callout(title: String, items: List<String>, bg: Color, accent: Color, mark: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
            Spacer(Modifier.height(10.dp))
            items.forEach {
                Row(Modifier.padding(bottom = 8.dp)) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                        Text(mark, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(it, fontSize = 13.sp, color = BrandText, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CheckItem(text: String) {
    Row(Modifier.padding(bottom = 10.dp)) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(BrandPrimarySoft), contentAlignment = Alignment.Center) {
            Text("✓", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = BrandText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HowToRead(r: ZoneReport) {
    var open by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(24.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cómo leer este reporte", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandText, modifier = Modifier.weight(1f))
                Text(if (open) "Ocultar" else "Ver", color = BrandPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Limitaciones", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandText)
                    r.limitations.forEach { Text("• $it", fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 4.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text("Fuentes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandText)
                    r.sources.forEach {
                        Text("• ${it.what}: ${it.name}${it.period?.let { p -> " ($p)" } ?: ""}", fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Cómo se calcula", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandText)
                    Text(r.methodology, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
