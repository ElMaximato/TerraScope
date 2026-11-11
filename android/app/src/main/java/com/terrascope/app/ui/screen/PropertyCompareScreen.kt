package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
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

sealed class PropertyCompareState {
    object Loading : PropertyCompareState()
    data class Ready(
        val properties: List<PropertyItem>,
        val zones: Map<Int, RankedZone>,
        val profileLabel: String,
        val a: Int,
        val b: Int
    ) : PropertyCompareState()
    data class Error(val message: String) : PropertyCompareState()
}

class PropertyCompareViewModel : ViewModel() {
    private val _state = MutableStateFlow<PropertyCompareState>(PropertyCompareState.Loading)
    val state: StateFlow<PropertyCompareState> = _state

    fun load(token: String) {
        viewModelScope.launch {
            _state.value = PropertyCompareState.Loading
            _state.value = try {
                val props = TerraScopeApi.fetchProperties(token)
                if (props.size < 2) throw IllegalStateException("Necesitas al menos 2 propiedades registradas para compararlas.")
                val ranking = TerraScopeApi.fetchRanking(InvestSelection.profile.value, token)
                PropertyCompareState.Ready(props, ranking.items.associateBy { it.zoneId }, ranking.label, props[0].id, props[1].id)
            } catch (e: Exception) {
                PropertyCompareState.Error(e.message ?: "No se pudieron cargar las propiedades")
            }
        }
    }

    fun pick(slotA: Boolean, id: Int) {
        val s = _state.value as? PropertyCompareState.Ready ?: return
        _state.value = if (slotA) {
            if (id == s.b) s.copy(a = id, b = s.a) else s.copy(a = id)
        } else {
            if (id == s.a) s.copy(b = id, a = s.b) else s.copy(b = id)
        }
    }
}

/** Superficie en m² (o null si no hay dato). */
private fun areaM2(p: PropertyItem): Double? {
    val v = p.areaValue ?: return null
    return when (p.areaUnit) {
        "m2" -> v
        "sqft" -> v * 0.09290304
        "acre" -> v * 4046.8564224
        else -> null
    }
}

private fun pricePerM2(p: PropertyItem): Double? {
    val price = p.askingPrice ?: return null
    val area = areaM2(p) ?: return null
    return if (area > 0) price / area else null
}

private fun money(v: Double?) = v?.let { "\$" + "%,.0f".format(it) } ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyCompareScreen(viewModel: PropertyCompareViewModel, token: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(token) { viewModel.load(token) }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Comparar propiedades", fontWeight = FontWeight.Bold) },
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
                is PropertyCompareState.Loading -> CenteredLoader()
                is PropertyCompareState.Error -> Box(Modifier.padding(20.dp)) { ErrorChip(s.message) }
                is PropertyCompareState.Ready -> {
                    val pa = s.properties.first { it.id == s.a }
                    val pb = s.properties.first { it.id == s.b }
                    val za = pa.zoneId?.let { s.zones[it] }
                    val zb = pb.zoneId?.let { s.zones[it] }
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Picker("Propiedad A", s.properties, s.a) { viewModel.pick(true, it) }
                        Picker("Propiedad B", s.properties, s.b) { viewModel.pick(false, it) }

                        Surface(shape = RoundedCornerShape(24.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Spacer(Modifier.weight(1f))
                                    HeaderCell(pa.name, Modifier.weight(1.2f))
                                    HeaderCell(pb.name, Modifier.weight(1.2f))
                                }
                                HorizontalDivider(color = BrandLine, modifier = Modifier.padding(vertical = 10.dp))
                                Row2("Tipo", propertyTypeLabel(pa.propertyType), propertyTypeLabel(pb.propertyType))
                                Row2("Zona", za?.name?.let { shortZoneName(it) } ?: "Fuera de zonas", zb?.name?.let { shortZoneName(it) } ?: "Fuera de zonas")
                                Row2("Puntaje de zona", za?.score?.let { "$it / 100" } ?: "—", zb?.score?.let { "$it / 100" } ?: "—", highlightHigher = za?.score to zb?.score)
                                Row2("Precio", money(pa.askingPrice), money(pb.askingPrice), lowerBetter = pa.askingPrice to pb.askingPrice)
                                Row2("Superficie", areaM2(pa)?.let { "%,.0f m²".format(it) } ?: "—", areaM2(pb)?.let { "%,.0f m²".format(it) } ?: "—")
                                Row2("Precio por m²", pricePerM2(pa)?.let { "\$%,.2f".format(it) } ?: "—", pricePerM2(pb)?.let { "\$%,.2f".format(it) } ?: "—", lowerBetter = pricePerM2(pa) to pricePerM2(pb))
                            }
                        }

                        Surface(shape = RoundedCornerShape(24.dp), color = BrandPrimary, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) {
                                Text("En resumen · ${s.profileLabel}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                Spacer(Modifier.height(6.dp))
                                Text(verdictText(pa, pb, za, zb), color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                        Text(
                            "El puntaje es de la zona completa (demografía del Census), no del predio, y no incluye competencia ni precios de mercado. " +
                                "El precio por m² solo se calcula si la propiedad tiene precio y superficie. Verifica en campo antes de decidir.",
                            fontSize = 11.sp, color = BrandTextMuted
                        )
                    }
                }
            }
        }
    }
}

private fun verdictText(pa: PropertyItem, pb: PropertyItem, za: RankedZone?, zb: RankedZone?): String {
    val parts = mutableListOf<String>()
    val sa = za?.score; val sb = zb?.score
    if (sa != null && sb != null) {
        parts += when {
            sa > sb -> "\"${pa.name}\" está en una zona mejor puntuada ($sa frente a $sb)."
            sb > sa -> "\"${pb.name}\" está en una zona mejor puntuada ($sb frente a $sa)."
            else -> "Las dos están en zonas con el mismo puntaje ($sa)."
        }
    } else parts += "No se pueden comparar las zonas: al menos una propiedad no tiene puntaje."
    val ppa = pricePerM2(pa); val ppb = pricePerM2(pb)
    if (ppa != null && ppb != null && ppa != ppb) {
        val (cheap, dear) = if (ppa < ppb) pa to pb else pb to pa
        val pct = (Math.abs(ppa - ppb) / maxOf(ppa, ppb) * 100).toInt()
        parts += "\"${cheap.name}\" sale $pct % más barata por m² que \"${dear.name}\"."
    } else if (ppa == null || ppb == null) parts += "Falta precio o superficie en alguna propiedad para comparar el precio por m²."
    return parts.joinToString(" ")
}

@Composable
private fun Picker(label: String, props: List<PropertyItem>, selected: Int, onPick: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(bottom = 6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            props.forEach { p ->
                val sel = p.id == selected
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .background(if (sel) BrandPrimary else BrandSurface)
                        .clickable { onPick(p.id) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(p.name, fontSize = 13.sp, color = if (sel) Color.White else BrandText, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandText, modifier = modifier.padding(horizontal = 4.dp))
}

@Composable
private fun Row2(
    label: String, a: String, b: String,
    highlightHigher: Pair<Int?, Int?>? = null,
    lowerBetter: Pair<Double?, Double?>? = null
) {
    val aWins = when {
        highlightHigher?.first != null && highlightHigher.second != null -> highlightHigher.first!! > highlightHigher.second!!
        lowerBetter?.first != null && lowerBetter.second != null -> lowerBetter.first!! < lowerBetter.second!!
        else -> false
    }
    val bWins = when {
        highlightHigher?.first != null && highlightHigher.second != null -> highlightHigher.second!! > highlightHigher.first!!
        lowerBetter?.first != null && lowerBetter.second != null -> lowerBetter.second!! < lowerBetter.first!!
        else -> false
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.weight(1f))
        Text(a, fontSize = 13.sp, color = if (aWins) Color(0xFF2E8B57) else BrandText, fontWeight = if (aWins) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp))
        Text(b, fontSize = 13.sp, color = if (bWins) Color(0xFF2E8B57) else BrandText, fontWeight = if (bWins) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp))
    }
}
