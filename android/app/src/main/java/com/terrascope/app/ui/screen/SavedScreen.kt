package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.data.model.PropertyItem
import com.terrascope.app.data.model.ZoneItem
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.ListState
import com.terrascope.app.ui.viewmodel.SavedViewModel

@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    token: String,
    onZoneClick: (ZoneItem) -> Unit,
    onAddProperty: () -> Unit,
    onPropertyZone: (Int) -> Unit,
    onCompareProperties: () -> Unit = {}
) {
    val watchlistState by viewModel.watchlist.collectAsState()
    val propertiesState by viewModel.properties.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(token) { viewModel.load(token) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(top = 28.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Guardados",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = BrandText
            )
            Spacer(Modifier.height(16.dp))
            SegmentedTabs(
                selected = selectedTab,
                labels = listOf("Zonas", "Propiedades"),
                onSelect = { selectedTab = it }
            )
        }

        when (selectedTab) {
            0 -> WatchlistTab(
                state = watchlistState,
                onZoneClick = onZoneClick,
                onRemove = { zone -> viewModel.removeFromWatchlist(zone.id, token) }
            )
            1 -> PropertiesTab(
                state = propertiesState,
                onAdd = onAddProperty,
                onCompare = onCompareProperties,
                onOpenZone = onPropertyZone,
                onDelete = { viewModel.deleteProperty(it.id, token) }
            )
        }
    }
}

@Composable
private fun SegmentedTabs(selected: Int, labels: List<String>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(BrandPrimarySoft)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) BrandSurface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) BrandPrimary else BrandTextMuted
                )
            }
        }
    }
}

@Composable
private fun WatchlistTab(
    state: ListState<ZoneItem>,
    onZoneClick: (ZoneItem) -> Unit,
    onRemove: (ZoneItem) -> Unit
) {
    when (state) {
        is ListState.Loading -> CenteredLoader()
        is ListState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(state.message) }
        is ListState.Success -> {
            if (state.data.isEmpty()) {
                EmptyHint("No tienes zonas guardadas.\nAbre una zona y toca \"Guardar zona\".")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { zone ->
                        WatchlistItem(
                            zone = zone,
                            onClick = { onZoneClick(zone) },
                            onRemove = { onRemove(zone) }
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun WatchlistItem(
    zone: ZoneItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
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
                .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zoneTitle(zone),
                    fontWeight = FontWeight.SemiBold,
                    color = BrandText,
                    fontSize = 15.sp
                )
                Text(if (zone.level == "place") "Ciudad" else "Zona censal ${shortZoneName(zone.name)}", fontSize = 11.sp, color = BrandTextMuted)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar de guardados",
                    tint = BrandTextMuted
                )
            }
        }
    }
}

@Composable
private fun PropertiesTab(
    state: ListState<PropertyItem>,
    onAdd: () -> Unit,
    onCompare: () -> Unit,
    onOpenZone: (Int) -> Unit,
    onDelete: (PropertyItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) { Text("Registrar propiedad", fontWeight = FontWeight.SemiBold) }
        if (state is ListState.Success && state.data.size >= 2) {
            OutlinedButton(
                onClick = onCompare,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).height(44.dp),
                shape = RoundedCornerShape(22.dp)
            ) { Text("Comparar propiedades", fontWeight = FontWeight.SemiBold, color = BrandPrimary) }
        }

        when (state) {
            is ListState.Loading -> CenteredLoader()
            is ListState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(state.message) }
            is ListState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyHint("No tienes propiedades registradas.\nToca \"Registrar propiedad\" para agregar un terreno o local.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.data, key = { it.id }) { prop ->
                            PropertyCard(prop, onOpenZone = onOpenZone, onDelete = { onDelete(prop) })
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

fun propertyTypeLabel(type: String): String = when (type) {
    "land" -> "Terreno"
    "commercial" -> "Comercial"
    "residential" -> "Residencial"
    else -> "Otro"
}

fun areaUnitLabel(unit: String?): String = when (unit) {
    "m2" -> "m²"
    "sqft" -> "ft²"
    "acre" -> "acres"
    else -> ""
}

@Composable
private fun PropertyCard(prop: PropertyItem, onOpenZone: (Int) -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenZone(prop.id) },
        shape = RoundedCornerShape(22.dp),
        color = BrandSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(prop.name, fontWeight = FontWeight.SemiBold, color = BrandText, fontSize = 15.sp)
                Text(
                    "${propertyTypeLabel(prop.propertyType)} · %.5f, %.5f".format(prop.lat, prop.lon),
                    fontSize = 11.sp,
                    color = BrandTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (prop.areaValue != null) {
                    Text(
                        "Superficie: ${"%,.0f".format(prop.areaValue)} ${areaUnitLabel(prop.areaUnit)}",
                        fontSize = 12.sp,
                        color = BrandTextMuted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (prop.askingPrice != null) {
                    Text(
                        "Precio: \$${"%,.0f".format(prop.askingPrice)}",
                        fontSize = 14.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (prop.zoneId != null) "Toca para ver el veredicto de su zona" else "Fuera de las zonas cubiertas",
                    fontSize = 10.sp,
                    color = BrandTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar propiedad", tint = BrandTextMuted)
            }
        }
    }
}
