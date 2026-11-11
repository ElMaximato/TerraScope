package com.terrascope.app.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.terrascope.app.data.model.ZoneDetail
import com.terrascope.app.data.model.ZoneItem
import com.terrascope.app.ui.theme.BrandBackground
import com.terrascope.app.ui.theme.BrandLine
import com.terrascope.app.ui.theme.BrandPrimary
import com.terrascope.app.ui.theme.BrandSurface
import com.terrascope.app.ui.theme.BrandText
import com.terrascope.app.ui.theme.BrandTextMuted
import com.terrascope.app.ui.viewmodel.MapUiState
import com.terrascope.app.ui.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener

private val DangerColor = Color(0xFFC4473A)
private val SuccessColor = Color(0xFF2E8B57)
private val WarningColor = Color(0xFFC98A1B)

/** Espacio inferior (dp) que ocupa la ficha: el mapa lo respeta al encuadrar y al mover la cámara. */
private const val BOTTOM_INSET_DP = 230

private const val TAG = "TerraMap"

/**
 * Puente JavaScript -> Kotlin. DEBE ser una clase pública: WebView invoca sus métodos por reflexión y
 * una clase privada (paquete) puede hacer que las llamadas fallen en silencio. Los métodos llegan en un
 * hilo del WebView: se pasan al hilo principal.
 */
@androidx.annotation.Keep
class MapBridge(
    private val onReady: () -> Unit,
    private val onSelected: (Int) -> Unit,
    private val onProperty: (Int) -> Unit = {}
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMapReady() { main.post { onReady() } }

    @JavascriptInterface
    fun onZoneSelected(id: Int) { main.post { onSelected(id) } }

    @JavascriptInterface
    fun onPropertySelected(id: Int) { main.post { onProperty(id) } }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createMapWebView(
    context: Context,
    bridge: MapBridge,
    onPageLoaded: () -> Unit,
    onProblem: (String) -> Unit
): WebView =
    WebView(context).apply {
        // Imprescindible en Compose: sin esto el WebView puede medirse con WRAP_CONTENT y el viewport queda con alto 0.
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        WebView.setWebContentsDebuggingEnabled(true)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(0xFFEEF2F0.toInt())
        webViewClient = object : WebViewClient() {
            // La página ya ejecutó sus scripts: no depende del puente JavaScript para saber que puede recibir datos.
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "Página cargada: $url")
                onPageLoaded()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    onProblem("La página del mapa no cargó: ${error?.description}")
                } else {
                    Log.w(TAG, "Recurso con error: ${request?.url} -> ${error?.description}")
                }
            }
        }
        // Todo lo que escribe el JavaScript del mapa (console.log / errores) queda en Logcat con la etiqueta TerraMap.
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                Log.d(TAG, "JS ${message?.messageLevel()}: ${message?.message()} (${message?.sourceId()}:${message?.lineNumber()})")
                return true
            }
        }
        addJavascriptInterface(bridge, "Android")
        loadUrl("file:///android_asset/map/map.html")
    }

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    token: String,
    onZoneClick: (ZoneItem) -> Unit,
    onPropertyClick: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val indicator by viewModel.indicator.collectAsState()
    LaunchedEffect(token) { viewModel.load(token) }

    val ready = state as? MapUiState.Ready
    val zones = ready?.zones ?: emptyList()
    val geoJson = remember(zones) { if (zones.isEmpty()) null else buildMapGeoJson(zones) }
    val ranges = remember(zones) { computeRanges(zones) }
    val selected = zones.firstOrNull { it.id == selectedId }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var mapProblem by remember { mutableStateOf<String?>(null) }
    var topInsetDp by remember { mutableStateOf(190) }
    var mapWidthDp by remember { mutableStateOf(0) }
    var mapHeightDp by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val mapReady = pageReady && webView != null

    // Envío de datos al mapa (y estado inicial de indicador y selección).
    LaunchedEffect(mapReady, geoJson, mapWidthDp, mapHeightDp) {
        val wv = webView
        if (mapReady && wv != null && geoJson != null && mapHeightDp > 0) {
            wv.evaluateJavascript(
                "TerraMap.setSize($mapWidthDp,$mapHeightDp);" +
                    "TerraMap.setInsets($topInsetDp,$BOTTOM_INSET_DP);" +
                    "TerraMap.setData(JSON.parse(${JSONObject.quote(geoJson)}));" +
                    "TerraMap.setIndicator('${indicator.code}');" +
                    "TerraMap.select(${selectedId ?: -1});",
                null
            )
        }
    }
    LaunchedEffect(mapReady, indicator) {
        if (mapReady) webView?.evaluateJavascript("TerraMap.setIndicator('${indicator.code}');", null)
    }
    LaunchedEffect(mapReady, selectedId) {
        if (mapReady) webView?.evaluateJavascript("TerraMap.select(${selectedId ?: -1});", null)
    }
    LaunchedEffect(mapReady, topInsetDp) {
        if (mapReady) webView?.evaluateJavascript("TerraMap.setInsets($topInsetDp,$BOTTOM_INSET_DP);", null)
    }
    // Propiedades del usuario como pines (se recargan cada vez que se vuelve a la pestaña).
    var properties by remember { mutableStateOf<List<com.terrascope.app.data.model.PropertyItem>>(emptyList()) }
    LaunchedEffect(token) {
        properties = try { com.terrascope.app.data.network.TerraScopeApi.fetchProperties(token) } catch (e: Exception) { emptyList() }
    }
    LaunchedEffect(mapReady, properties) {
        if (mapReady) {
            val arr = org.json.JSONArray()
            properties.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("lat", it.lat).put("lon", it.lon))
            }
            webView?.evaluateJavascript("TerraMap.setProperties(${arr});", null)
        }
    }
    // Diagnóstico: unos segundos después de enviar los datos, pregunta al mapa si dibujó zonas y mosaicos.
    LaunchedEffect(mapReady, geoJson) {
        if (mapReady && geoJson != null) {
            delay(6000)
            webView?.evaluateJavascript("TerraMap.problem()") { raw ->
                mapProblem = if (raw == null || raw == "null") {
                    "El script del mapa no responde (TerraMap no existe). Mira Logcat con la etiqueta TerraMap."
                } else {
                    runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()?.takeIf { it.isNotBlank() }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    mapWidthDp = with(density) { it.width.toDp().value.toInt() }
                    mapHeightDp = with(density) { it.height.toDp().value.toInt() }
                },
            factory = { ctx ->
                createMapWebView(
                    ctx,
                    MapBridge(
                        onReady = { pageReady = true },
                        onSelected = { id -> viewModel.select(if (id < 0) null else id) },
                        onProperty = { id -> onPropertyClick(id) }
                    ),
                    onPageLoaded = { pageReady = true },
                    onProblem = { mapProblem = it }
                )
            },
            update = { wv -> if (webView !== wv) webView = wv },
            onRelease = { wv ->
                wv.removeJavascriptInterface("Android")
                wv.destroy()
            }
        )

        // ── Encabezado: título, selector de indicador y leyenda (compacto) ──
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { topInsetDp = with(density) { it.height.toDp().value.toInt() } }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BrandSurface.copy(alpha = 0.97f),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Somerton", fontWeight = FontWeight.Bold, color = BrandText, fontSize = 20.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${zones.count { it.level == "tract" }} zonas · ACS 2020–2024",
                            fontSize = 11.sp, color = BrandTextMuted
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapIndicator.values().forEach { item ->
                            IndicatorChip(
                                label = item.chipLabel,
                                selected = item == indicator,
                                onClick = { viewModel.setIndicator(item) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Legend(indicator = indicator, range = ranges[indicator.code])
                }
            }
        }

        // ── Controles de zoom y encuadre ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapControlButton(Icons.Filled.Add, "Acercar") { webView?.evaluateJavascript("TerraMap.zoomIn();", null) }
            MapControlButton(Icons.Filled.Remove, "Alejar") { webView?.evaluateJavascript("TerraMap.zoomOut();", null) }
            MapControlButton(Icons.Filled.MyLocation, "Centrar en Somerton") {
                webView?.evaluateJavascript("TerraMap.reset();", null)
            }
        }

        // ── Estados de carga y error ─────────────────────────────────────
        when (val s = state) {
            is MapUiState.Loading -> CenterCard {
                CircularProgressIndicator(color = BrandPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text("Cargando zonas…", color = BrandText, fontSize = 14.sp)
            }
            is MapUiState.Error -> CenterCard {
                Text("No se pudo cargar el mapa", color = DangerColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(s.message, color = BrandTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.load(token, force = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) { Text("Reintentar") }
            }
            is MapUiState.Ready -> Unit
        }

        // ── Ficha inferior y atribución ──────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mapProblem?.let { problem ->
                Surface(shape = RoundedCornerShape(10.dp), color = BrandSurface.copy(alpha = 0.96f), shadowElevation = 4.dp) {
                    Text(problem, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = DangerColor)
                }
            }
            if (ready != null && ready.skipped > 0) {
                Text(
                    "${ready.skipped} zona(s) no se pudieron cargar.",
                    fontSize = 11.sp, color = DangerColor
                )
            }
            if (selected != null) {
                ZoneCard(
                    zone = selected,
                    indicator = indicator,
                    onClose = { viewModel.select(null) },
                    onOpen = { onZoneClick(ZoneItem(selected.id, selected.code, selected.name, selected.level)) }
                )
            } else if (ready != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandSurface.copy(alpha = 0.96f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Toca una zona para ver sus indicadores",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        fontSize = 12.sp, color = BrandTextMuted
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandSurface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "© OpenStreetMap contributors © CARTO · Límites: U.S. Census Bureau",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp, color = BrandTextMuted, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Componentes ──────────────────────────────────────────────────────────

@Composable
private fun IndicatorChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) BrandPrimary else BrandBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else BrandTextMuted
        )
    }
}

@Composable
private fun Legend(indicator: MapIndicator, range: IndicatorRange?) {
    Column {
        Text(indicator.legendTitle, fontSize = 11.sp, color = BrandTextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(MapRamp))
        )
        Spacer(Modifier.height(4.dp))
        if (range != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatIndicatorValue(indicator.code, range.min), fontSize = 10.sp, color = BrandTextMuted)
                Text(formatIndicatorValue(indicator.code, range.max), fontSize = 10.sp, color = BrandTextMuted)
            }
        } else {
            Text("Sin datos para este indicador", fontSize = 10.sp, color = BrandTextMuted)
        }
        if (indicator == MapIndicator.DENSITY) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Población ÷ área total del tract; en tracts extensos resulta baja.",
                fontSize = 9.sp, color = BrandTextMuted
            )
        }
    }
}

@Composable
private fun MapControlButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = BrandSurface,
        shadowElevation = 2.dp,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = description, tint = BrandPrimary)
        }
    }
}

@Composable
private fun CenterCard(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BrandSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { content() }
        }
    }
}

private fun shortLabel(code: String): String = when (code) {
    "population" -> "Población"
    "households" -> "Hogares"
    "median_income" -> "Ingreso"
    "median_age" -> "Edad"
    "population_density" -> "Densidad"
    else -> code
}

@Composable
private fun ZoneCard(zone: ZoneDetail, indicator: MapIndicator, onClose: () -> Unit, onOpen: () -> Unit) {
    val main = zone.indicators.firstOrNull { it.code == indicator.code }
    val others = listOf("population", "households", "median_income", "median_age")
        .filter { it != indicator.code }
        .mapNotNull { code -> zone.indicators.firstOrNull { it.code == code } }
        .take(3)
    val period = main?.referencePeriod ?: zone.indicators.firstOrNull()?.referencePeriod

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BrandSurface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(zone.name, fontWeight = FontWeight.Bold, color = BrandText, fontSize = 17.sp)
                    Text("GEOID ${zone.code}", fontSize = 10.sp, color = BrandTextMuted)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar ficha", tint = BrandTextMuted)
                }
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Spacer(Modifier.height(2.dp))
                if (main != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatIndicatorValue(main.code, main.value),
                            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = BrandPrimary
                        )
                        Text(
                            " ${main.name}",
                            fontSize = 12.sp, color = BrandTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        main.marginOfError?.let {
                            Text("Margen ${formatMargin(main.code, it)}", fontSize = 11.sp, color = BrandTextMuted)
                        }
                        val reliability = reliabilityOf(main)
                        if (reliability != null) {
                            val color = when (reliability) {
                                Reliability.HIGH -> SuccessColor
                                Reliability.MEDIUM -> WarningColor
                                Reliability.LOW -> DangerColor
                            }
                            StatusPill(reliability.label, color)
                        } else if (main.marginOfError == null) {
                            StatusPill("Indicador derivado", BrandTextMuted)
                        }
                    }
                } else {
                    Text("Sin datos para este indicador en esta zona.", fontSize = 13.sp, color = BrandTextMuted)
                }

                if (others.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        others.forEach { ind ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(shortLabel(ind.code), fontSize = 10.sp, color = BrandTextMuted)
                                Text(
                                    formatIndicatorValue(ind.code, ind.value),
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandText
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Fuente: U.S. Census Bureau${if (period != null) " · $period" else ""}",
                    fontSize = 10.sp, color = BrandTextMuted
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) { Text("Ver ficha completa", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
