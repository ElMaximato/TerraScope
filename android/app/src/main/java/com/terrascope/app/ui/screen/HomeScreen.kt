package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.data.model.ZoneItem
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.HomeViewModel
import com.terrascope.app.ui.viewmodel.ListState

/** "Census Tract 115.01" -> "115.01" */
fun shortZoneName(name: String): String = name.replace(Regex("^Census Tract\\s+", RegexOption.IGNORE_CASE), "")

/** Nombre para mostrar: el descriptivo ("Residencial · norte") o, si no existe, "Zona 115.03". */
fun zoneTitle(zone: ZoneItem): String =
    if (zone.level == "place") zone.name else zone.friendlyTitle ?: "Zona ${shortZoneName(zone.name)}"

fun zoneTitle(zone: com.terrascope.app.data.model.ZoneDetail): String =
    if (zone.level == "place") zone.name else zone.friendly?.title ?: "Zona ${shortZoneName(zone.name)}"

fun levelLabel(level: String): String = when (level) {
    "place" -> "Ciudad"
    "tract" -> "Zona censal"
    else -> level
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    token: String,
    userEmail: String,
    onZoneClick: (ZoneItem) -> Unit
) {
    val zonesState by viewModel.zones.collectAsState()
    val watchlistState by viewModel.watchlist.collectAsState()

    LaunchedEffect(token) { viewModel.load(token) }

    val tractCount = when (val z = zonesState) {
        is ListState.Success -> z.data.count { it.level == "tract" }
        else -> null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Hola, ${userEmail.substringBefore('@')}", fontSize = 13.sp, color = BrandTextMuted)
                Text(
                    "Explora Somerton",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        item { SomertonSummaryCard(tractCount) }

        item { SectionTitle("Guardadas") }

        when (val ws = watchlistState) {
            is ListState.Loading -> item { CenteredLoader() }
            is ListState.Error -> item { ErrorChip(ws.message) }
            is ListState.Success -> {
                if (ws.data.isEmpty()) {
                    item { EmptyHint("Aún no tienes zonas guardadas.\nAbre una zona y toca \"Guardar zona\".") }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(ws.data, key = { it.id }) { zone ->
                                SavedMiniCard(zone = zone, onClick = { onZoneClick(zone) })
                            }
                        }
                    }
                }
            }
            else -> item { EmptyHint("Cargando…") }
        }

        item { SectionTitle("Zonas de Somerton") }

        when (val zs = zonesState) {
            is ListState.Loading -> item { CenteredLoader() }
            is ListState.Error -> item { ErrorChip(zs.message + "\n¿Está corriendo el backend?") }
            is ListState.Success -> {
                items(zs.data, key = { it.id }) { zone ->
                    ZoneListCard(zone = zone, onClick = { onZoneClick(zone) })
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = BrandText,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SomertonSummaryCard(tractCount: Int?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = BrandPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("Somerton, Arizona", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color.White)
            Text(
                "Ciudad incorporada · Condado de Yuma",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryStat("Población", "14,574")
                SummaryStat("Ingreso mediano", "\$74,015")
                SummaryStat("Zonas", tractCount?.toString() ?: "–")
            }
            Text(
                "U.S. Census Bureau · ACS 2020–2024",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandAccent)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun SavedMiniCard(zone: ZoneItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandAccent)
            )
            Spacer(Modifier.height(14.dp))
            Text(zoneTitle(zone), fontWeight = FontWeight.Bold, color = BrandText, fontSize = 14.sp, maxLines = 2)
            Text(levelLabel(zone.level), fontSize = 11.sp, color = BrandTextMuted)
        }
    }
}

@Composable
fun ZoneListCard(zone: ZoneItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (zone.level == "place") "C" else "Z",
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zoneTitle(zone),
                    fontWeight = FontWeight.SemiBold,
                    color = BrandText,
                    fontSize = 15.sp
                )
                Text(
                    text = if (zone.level == "place") "Toda la ciudad · referencia" else "Zona censal ${shortZoneName(zone.name)}",
                    fontSize = 11.sp,
                    color = BrandTextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandTextMuted
            )
        }
    }
}

// ── Componentes compartidos ───────────────────────────────────────────────

@Composable
fun CenteredLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BrandPrimary, strokeWidth = 2.dp)
    }
}

@Composable
fun ErrorChip(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFBECEB)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            fontSize = 13.sp,
            color = Color(0xFFC4473A)
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = BrandTextMuted, fontSize = 13.sp)
    }
}
