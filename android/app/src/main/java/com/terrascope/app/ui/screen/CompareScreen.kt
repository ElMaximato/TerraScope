package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.terrascope.app.data.model.ZoneDetail
import com.terrascope.app.data.model.ZoneItem
import com.terrascope.app.data.model.ZoneScore
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.InvestSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────

data class CompareSide(val zone: ZoneDetail, val score: ZoneScore?)

sealed class CompareState {
    object Loading : CompareState()
    data class Ready(val a: CompareSide, val b: CompareSide) : CompareState()
    data class Error(val message: String) : CompareState()
}

class CompareViewModel : ViewModel() {
    private val _zones = MutableStateFlow<List<ZoneItem>>(emptyList())
    val zones: StateFlow<List<ZoneItem>> = _zones

    private val _selA = MutableStateFlow<Int?>(null)
    val selA: StateFlow<Int?> = _selA
    private val _selB = MutableStateFlow<Int?>(null)
    val selB: StateFlow<Int?> = _selB

    private val _state = MutableStateFlow<CompareState>(CompareState.Loading)
    val state: StateFlow<CompareState> = _state

    fun load(token: String) {
        viewModelScope.launch {
            try {
                val tracts = TerraScopeApi.fetchZones(token).filter { it.level == "tract" }
                _zones.value = tracts
                if (tracts.size < 2) {
                    _state.value = CompareState.Error("Se necesitan al menos 2 zonas para comparar.")
                    return@launch
                }
                if (_selA.value == null || tracts.none { it.id == _selA.value }) _selA.value = tracts[0].id
                if (_selB.value == null || tracts.none { it.id == _selB.value } || _selB.value == _selA.value) {
                    _selB.value = tracts.first { it.id != _selA.value }.id
                }
                compare(token)
            } catch (e: Exception) {
                _state.value = CompareState.Error(e.message ?: "No se pudieron cargar las zonas")
            }
        }
    }

    fun pick(slotA: Boolean, id: Int, token: String) {
        val a = _selA.value
        val b = _selB.value
        if (slotA) {
            if (id == b) _selB.value = a   // intercambia para no comparar una zona consigo misma
            _selA.value = id
        } else {
            if (id == a) _selA.value = b
            _selB.value = id
        }
        viewModelScope.launch { compare(token) }
    }

    private suspend fun compare(token: String) {
        val idA = _selA.value ?: return
        val idB = _selB.value ?: return
        _state.value = CompareState.Loading
        _state.value = try {
            val profile = InvestSelection.profile.value
            CompareState.Ready(
                a = CompareSide(
                    TerraScopeApi.fetchZoneDetail(idA, token),
                    try { TerraScopeApi.fetchZoneScore(idA, token, profile) } catch (e: Exception) { null }
                ),
                b = CompareSide(
                    TerraScopeApi.fetchZoneDetail(idB, token),
                    try { TerraScopeApi.fetchZoneScore(idB, token, profile) } catch (e: Exception) { null }
                )
            )
        } catch (e: Exception) {
            CompareState.Error(e.message ?: "No se pudo comparar")
        }
    }
}

// ── Pantalla ──────────────────────────────────────────────────────────────

private val CompareRows = listOf(
    "population" to "Población",
    "households" to "Hogares",
    "median_income" to "Ingreso mediano",
    "median_age" to "Edad mediana",
    "population_density" to "Densidad (hab/km²)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(viewModel: CompareViewModel, token: String, onBack: () -> Unit) {
    val zones by viewModel.zones.collectAsState()
    val selA by viewModel.selA.collectAsState()
    val selB by viewModel.selB.collectAsState()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(token) { viewModel.load(token) }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Comparar zonas", fontWeight = FontWeight.Bold, color = BrandText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = BrandPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ZonePicker("Zona A", zones, selA) { viewModel.pick(true, it, token) } }
            item { ZonePicker("Zona B", zones, selB) { viewModel.pick(false, it, token) } }

            when (val s = state) {
                is CompareState.Loading -> item { CenteredLoader() }
                is CompareState.Error -> item { ErrorChip(s.message) }
                is CompareState.Ready -> {
                    item { VerdictCard(s.a, s.b) }
                    item { ComparisonTable(s.a, s.b) }
                    item {
                        Text(
                            "Fuente: U.S. Census Bureau, ACS 5-Year. Los valores son estimaciones con margen de error; consulta la ficha de cada zona para verlo.",
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZonePicker(label: String, zones: List<ZoneItem>, selected: Int?, onPick: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            zones.forEach { z ->
                val isSel = z.id == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSel) BrandPrimary else BrandSurface)
                        .clickable { onPick(z.id) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (z.level == "place") z.name else zoneTitle(z),
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSel) Color.White else BrandTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun VerdictCard(a: CompareSide, b: CompareSide) {
    val sa = a.score?.score
    val sb = b.score?.score
    val profile = a.score?.profileLabel ?: b.score?.profileLabel ?: ""
    Surface(shape = RoundedCornerShape(28.dp), color = BrandPrimary, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(
                if (profile.isNotEmpty()) "Perfil: $profile" else "Perfil de inversión",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            if (sa != null && sb != null) {
                val winner = when {
                    sa > sb -> zoneTitle(a.zone)
                    sb > sa -> zoneTitle(b.zone)
                    else -> null
                }
                Text(
                    winner?.let { "Conviene más: $it" } ?: "Empate técnico",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandAccent,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "$sa vs $sb puntos de 100",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    "Datos insuficientes para puntuar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandAccent,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                "Cambia el perfil en la pestaña Invertir. El puntaje es relativo entre zonas y usa solo datos demográficos.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ComparisonTable(a: CompareSide, b: CompareSide) {
    Surface(shape = RoundedCornerShape(22.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1.1f))
                Text(zoneTitle(a.zone), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 13.sp)
                Text(zoneTitle(b.zone), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 13.sp)
            }
            HorizontalDivider(color = BrandPrimarySoft, modifier = Modifier.padding(vertical = 8.dp))

            val sa = a.score?.score
            val sb = b.score?.score
            CompareRow(
                label = "Puntaje",
                left = sa?.toString() ?: "–",
                right = sb?.toString() ?: "–",
                leftBetter = sa != null && sb != null && sa > sb,
                rightBetter = sa != null && sb != null && sb > sa
            )
            CompareRows.forEach { (code, label) ->
                val ia = a.zone.indicators.firstOrNull { it.code == code }
                val ib = b.zone.indicators.firstOrNull { it.code == code }
                // La edad mediana no tiene un "mejor": no se resalta.
                val comparable = code != "median_age" && ia != null && ib != null
                CompareRow(
                    label = label,
                    left = ia?.let { formatIndicatorValue(code, it.value) } ?: "–",
                    right = ib?.let { formatIndicatorValue(code, it.value) } ?: "–",
                    leftBetter = comparable && ia!!.value > ib!!.value,
                    rightBetter = comparable && ib!!.value > ia!!.value
                )
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, left: String, right: String, leftBetter: Boolean, rightBetter: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1.1f), fontSize = 12.sp, color = BrandTextMuted)
        Text(
            left,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = if (leftBetter) FontWeight.Bold else FontWeight.Normal,
            color = if (leftBetter) BrandPrimary else BrandText
        )
        Text(
            right,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = if (rightBetter) FontWeight.Bold else FontWeight.Normal,
            color = if (rightBetter) BrandPrimary else BrandText
        )
    }
}
