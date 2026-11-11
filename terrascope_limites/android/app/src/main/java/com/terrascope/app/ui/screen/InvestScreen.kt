package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.data.model.RankedZone
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.InvestViewModel
import com.terrascope.app.ui.viewmodel.RankingState

private const val MAX_INVEST_CHARS = 100

@Composable
fun InvestScreen(
    viewModel: InvestViewModel,
    token: String,
    onZoneClick: (Int) -> Unit,
    onCompare: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val rankingState by viewModel.ranking.collectAsState()
    val matchMsg by viewModel.matchMsg.collectAsState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(token) { viewModel.load(token) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Invertir", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BrandText)
                Text(
                    "Elige en qué quieres invertir y te decimos qué zona de Somerton conviene más.",
                    fontSize = 13.sp,
                    color = BrandTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Descripción libre
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = BrandSurface, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(MAX_INVEST_CHARS) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Ej. abrir una taquería, comprar un terreno…", fontSize = 13.sp) },
                        supportingText = {
                            Text(
                                "${text.length}/$MAX_INVEST_CHARS",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.describe(text, token) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) { Text("Sugerir perfil", fontWeight = FontWeight.SemiBold) }
                    matchMsg?.let {
                        Text(it, fontSize = 12.sp, color = BrandPrimary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        // Selector de perfil
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { p ->
                    val isSel = p.id == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSel) BrandPrimary else BrandSurface)
                            .clickable { viewModel.select(p.id, token) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            p.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSel) Color.White else BrandTextMuted
                        )
                    }
                }
            }
        }

        when (val rs = rankingState) {
            is RankingState.Loading -> item { CenteredLoader() }
            is RankingState.Error -> item { ErrorChip(rs.message + "\n¿Está corriendo el backend?") }
            is RankingState.Success -> {
                val r = rs.ranking
                item {
                    Text(r.description, fontSize = 12.sp, color = BrandTextMuted)
                }
                val best = r.items.firstOrNull { it.score != null }
                if (best != null) {
                    item { BestZoneCard(best, r.label, onClick = { onZoneClick(best.zoneId) }) }
                }
                item {
                    OutlinedButton(
                        onClick = onCompare,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                    ) { Text("Comparar dos zonas", fontWeight = FontWeight.SemiBold) }
                }
                item {
                    Text(
                        "Ranking de zonas",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(r.items.withIndex().toList(), key = { it.value.zoneId }) { (index, zone) ->
                    RankRow(position = index + 1, zone = zone, onClick = { onZoneClick(zone.zoneId) })
                }
                item {
                    Text(r.note, fontSize = 11.sp, color = BrandTextMuted)
                }
            }
        }
    }
}

@Composable
private fun BestZoneCard(zone: RankedZone, profileLabel: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = BrandPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mejor zona para $profileLabel", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text(
                    zone.friendlyTitle ?: "Zona ${shortZoneName(zone.name)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "Zona censal ${shortZoneName(zone.name)} · ${confidenceLabel(zone.confidence)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Text("${zone.score}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = BrandAccent)
        }
    }
}

@Composable
private fun RankRow(position: Int, zone: RankedZone, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$position", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        zone.friendlyTitle ?: "Zona ${shortZoneName(zone.name)}",
                        fontWeight = FontWeight.SemiBold,
                        color = BrandText,
                        fontSize = 15.sp
                    )
                    Text(
                        "Zona censal ${shortZoneName(zone.name)} · " + (if (zone.score != null) confidenceLabel(zone.confidence) else "Datos insuficientes"),
                        fontSize = 11.sp,
                        color = BrandTextMuted
                    )
                }
                Text(
                    zone.score?.toString() ?: "–",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
            if (zone.score != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BrandPrimarySoft)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((zone.score / 100f).coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BrandPrimary)
                    )
                }
            }
        }
    }
}

private fun confidenceLabel(confidence: String?): String = when (confidence) {
    "high" -> "Confianza alta"
    "medium" -> "Confianza media"
    "low" -> "Confianza baja"
    else -> "Sin margen de error"
}
