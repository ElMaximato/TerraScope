package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.data.model.ZoneHighlight
import com.terrascope.app.data.model.ZoneIndicator
import com.terrascope.app.data.model.ZoneScore
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.DetailState
import com.terrascope.app.ui.viewmodel.ZoneDetailViewModel

private val ReliabilityHigh = Color(0xFF2E8B57)
private val ReliabilityMedium = Color(0xFFC98A1B)
private val ReliabilityLow = Color(0xFFC4473A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneDetailScreen(
    viewModel: ZoneDetailViewModel,
    zoneId: Int,
    token: String,
    onBack: () -> Unit,
    onReport: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val actionMsg by viewModel.actionMsg.collectAsState()

    LaunchedEffect(zoneId, token) { viewModel.load(zoneId, token) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMsg) {
        actionMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMsg()
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BrandPrimary
                        )
                    }
                },
                actions = {
                    val s = state
                    if (s is DetailState.Success) {
                        IconButton(onClick = { viewModel.toggleWatchlist(zoneId, token) }) {
                            Icon(
                                imageVector = if (s.inWatchlist) Icons.Filled.Bookmark
                                else Icons.Filled.BookmarkBorder,
                                contentDescription = "Guardar zona",
                                tint = BrandPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is DetailState.Loading, DetailState.Idle -> CenteredLoader()

                is DetailState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }

                is DetailState.Success -> {
                    val zone = s.zone
                    val isPlace = zone.level == "place"
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandBackground),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Encabezado: tipo de zona, nombre descriptivo y número del Census
                        item {
                            val fr = zone.friendly
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isPlace) BrandAccent.copy(alpha = 0.2f) else BrandPrimarySoft)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = (if (isPlace) "Ciudad" else (fr?.kind ?: levelLabel(zone.level))).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlace) Color(0xFF8B651B) else BrandPrimary
                                    )
                                }
                                Text(
                                    text = zoneTitle(zone),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandText,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    text = fr?.codeLabel?.takeIf { it.isNotBlank() } ?: "Zona censal ${shortZoneName(zone.name)}",
                                    fontSize = 12.sp,
                                    color = BrandTextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // En pocas palabras
                        zone.friendly?.description?.takeIf { it.isNotBlank() }?.let { d ->
                            item {
                                Surface(shape = RoundedCornerShape(22.dp), color = BrandPrimarySoft) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("En pocas palabras", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandPrimary)
                                        Text(d, fontSize = 14.sp, color = BrandText, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }

                        item {
                            if (s.inWatchlist) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleWatchlist(zoneId, token) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                                ) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardada · toca para quitar", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleWatchlist(zoneId, token) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                                ) {
                                    Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardar zona", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        s.score?.let { sc -> item { ScoreCard(sc) } }

                        item {
                            OutlinedButton(
                                onClick = { onReport(zoneId) },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                            ) { Text("Ver reporte", fontWeight = FontWeight.SemiBold) }
                        }

                        val highlights = zone.friendly?.highlights.orEmpty()
                        if (highlights.isNotEmpty()) {
                            item {
                                Text("Así es esta zona", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BrandText, modifier = Modifier.padding(top = 6.dp))
                            }
                            items(highlights) { h -> HighlightCard(h) }
                        }

                        item {
                            var open by remember { mutableStateOf(highlights.isEmpty()) }
                            Column {
                                TextButton(onClick = { open = !open }, contentPadding = PaddingValues(0.dp)) {
                                    Text(
                                        if (open) "Ocultar datos técnicos" else "Ver datos técnicos del Census",
                                        color = BrandPrimary, fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (open) {
                                    Text("GEOID ${zone.code}", fontSize = 11.sp, color = BrandTextMuted, modifier = Modifier.padding(bottom = 8.dp))
                                    if (zone.indicators.isEmpty()) {
                                        EmptyHint("Sin indicadores disponibles para esta zona.")
                                    } else {
                                        zone.indicators.chunked(2).forEach { pair ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                                pair.forEach { ind -> IndicatorCard(ind, Modifier.weight(1f)) }
                                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Fuente: U.S. Census Bureau, ACS 5-Year Estimates. Los márgenes de error (±) indican la incertidumbre de la muestra.",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(h: ZoneHighlight) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = BrandSurface, shadowElevation = 2.dp) {
        Column(Modifier.padding(16.dp)) {
            Text(h.title, fontSize = 12.sp, color = BrandTextMuted, fontWeight = FontWeight.Medium)
            Text(h.value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, modifier = Modifier.padding(top = 2.dp))
            h.comparison?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandText, modifier = Modifier.padding(top = 2.dp)) }
            Text(h.help, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun IndicatorCard(ind: ZoneIndicator, modifier: Modifier = Modifier) {
    val reliability = reliabilityOf(ind)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ind.name,
                fontSize = 12.sp,
                color = BrandTextMuted,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatIndicatorValue(ind.code, ind.value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
            if (!ind.unit.isNullOrBlank() && ind.code != "median_income") {
                Text(text = ind.unit, fontSize = 11.sp, color = BrandTextMuted)
            }
            Spacer(Modifier.height(6.dp))
            val margin = ind.marginOfError
            if (margin != null) {
                Text("Margen ${formatMargin(ind.code, margin)}", fontSize = 11.sp, color = BrandTextMuted)
            } else {
                Text("Indicador derivado", fontSize = 11.sp, color = BrandTextMuted)
            }
            if (reliability != null) {
                val color = when (reliability) {
                    Reliability.HIGH -> ReliabilityHigh
                    Reliability.MEDIUM -> ReliabilityMedium
                    Reliability.LOW -> ReliabilityLow
                }
                Text(
                    text = reliability.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            ind.referencePeriod?.let {
                Text(text = it, fontSize = 10.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ScoreCard(sc: ZoneScore) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = BrandPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("Potencial de oportunidad · ${sc.profileLabel}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            if (sc.status == "ok" && sc.score != null) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = sc.score.toString(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandAccent
                    )
                    Text(
                        text = " / 100",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "Comparado con ${sc.comparedWith} zonas de Somerton",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(14.dp))
                sc.factors.forEach { f ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(f.label, fontSize = 12.sp, color = Color.White)
                        Text("${"%.0f".format(f.points)} / ${f.weight}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(f.normalized.toFloat().coerceIn(0f, 1f))
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(BrandAccent)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                val confidenceText = when (sc.confidence) {
                    "high" -> "Confianza de los datos: alta"
                    "medium" -> "Confianza de los datos: media (algún indicador tiene margen de error amplio)"
                    "low" -> "Confianza de los datos: baja (márgenes de error muy amplios)"
                    else -> null
                }
                confidenceText?.let { Text(it, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)) }
                Text(
                    text = (sc.note ?: "") + " Puntaje relativo: 0 es la zona más baja y 100 la más alta. Cambia el perfil en la pestaña Invertir.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Text(
                    text = if (sc.status == "insufficient") "Datos insuficientes" else "No aplica",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandAccent,
                    modifier = Modifier.padding(top = 4.dp)
                )
                sc.reason?.let {
                    Text(it, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
