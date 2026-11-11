package com.terrascope.app.ui.screen

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.model.PropertyDraft
import com.terrascope.app.data.model.PropertyItem
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────

class PropertyFormViewModel : ViewModel() {
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun save(draft: PropertyDraft, token: String, editingId: Int?, onSaved: (PropertyItem) -> Unit) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                onSaved(
                    if (editingId != null) TerraScopeApi.updateProperty(editingId, draft, token)
                    else TerraScopeApi.createProperty(draft, token)
                )
            } catch (e: Exception) {
                _error.value = e.message?.takeIf { it.isNotBlank() } ?: "No se pudo guardar la propiedad"
            } finally {
                _saving.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}

// ── Formulario ────────────────────────────────────────────────────────────

private val TypeOptions = listOf("land" to "Terreno", "commercial" to "Comercial", "residential" to "Residencial", "other" to "Otro")
private val UnitOptions = listOf("m2" to "m²", "sqft" to "ft²", "acre" to "acres")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFormScreen(
    viewModel: PropertyFormViewModel,
    token: String,
    onBack: () -> Unit,
    onSaved: (PropertyItem) -> Unit,
    initialLocation: Pair<Double, Double>? = null,
    initialName: String = "",
    editing: PropertyItem? = null
) {
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf((editing?.name ?: initialName).take(80)) }
    var type by remember { mutableStateOf(editing?.propertyType ?: "land") }
    var price by remember { mutableStateOf((editing?.askingPrice?.let { "%.0f".format(it) } ?: "").take(12)) }
    var area by remember { mutableStateOf((editing?.areaValue?.let { if (it % 1.0 == 0.0) "%.0f".format(it) else it.toString() } ?: "").take(12)) }
    var unit by remember { mutableStateOf(editing?.areaUnit ?: "m2") }
    var notes by remember { mutableStateOf((editing?.notes ?: "").take(500)) }
    var location by remember { mutableStateOf(if (editing != null) editing.lat to editing.lon else initialLocation) }
    var showPicker by remember { mutableStateOf(false) }

    val priceValue = price.replace(",", "").toDoubleOrNull()
    val areaValue = area.replace(",", "").toDoubleOrNull()
    val priceInvalid = price.isNotBlank() && (priceValue == null || priceValue < 0)
    val areaInvalid = area.isNotBlank() && (areaValue == null || areaValue <= 0)
    val canSave = name.isNotBlank() && location != null && !priceInvalid && !areaInvalid && !saving

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (editing != null) "Editar propiedad" else "Nueva propiedad", fontWeight = FontWeight.Bold, color = BrandText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = BrandPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Lote esquina Main St") },
                supportingText = { CharCounter(name.length, 80) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            FormLabel("Tipo")
            ChipRow(TypeOptions, type) { type = it }

            FormLabel("Ubicación")
            Surface(shape = RoundedCornerShape(18.dp), color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        location?.let { "%.5f, %.5f".format(it.first, it.second) } ?: "Aún no has elegido un punto en el mapa",
                        fontSize = 13.sp,
                        color = if (location != null) BrandText else BrandTextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showPicker = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
                    ) { Text(if (location == null) "Elegir en el mapa" else "Cambiar punto", fontWeight = FontWeight.SemiBold) }
                }
            }

            OutlinedTextField(
                value = price,
                onValueChange = { price = it.take(12) },
                label = { Text("Precio de venta (USD, opcional)") },
                isError = priceInvalid,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = area,
                onValueChange = { area = it.take(12) },
                label = { Text("Superficie (opcional)") },
                isError = areaInvalid,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            ChipRow(UnitOptions, unit) { unit = it }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(500) },
                label = { Text("Notas (opcional)") },
                supportingText = { CharCounter(notes.length, 500) },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { ErrorChip(it) }

            Button(
                onClick = {
                    val loc = location ?: return@Button
                    viewModel.save(
                        PropertyDraft(
                            name = name.trim(),
                            propertyType = type,
                            lat = loc.first,
                            lon = loc.second,
                            askingPrice = priceValue,
                            areaValue = areaValue,
                            areaUnit = if (areaValue != null) unit else null,
                            notes = notes.trim().ifEmpty { null }
                        ),
                        token,
                        editing?.id,
                        onSaved
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Guardar propiedad", fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Al guardar, TerraScope ubica la propiedad en su zona censal y te muestra los datos de esa zona.",
                fontSize = 11.sp,
                color = BrandTextMuted
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        LocationPickerDialog(
            initial = location,
            onPicked = { location = it; viewModel.clearError() },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun CharCounter(count: Int, max: Int) {
    Text(
        "$count/$max",
        fontSize = 11.sp,
        color = BrandTextMuted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandText)
}

@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val isSel = id == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSel) BrandPrimary else BrandSurface)
                    .clickable { onSelect(id) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSel) Color.White else BrandTextMuted
                )
            }
        }
    }
}

// ── Selector de ubicación en el mapa ─────────────────────────────────────

@androidx.annotation.Keep
class PickerBridge(private val callback: (Double, Double) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPick(lat: Double, lon: Double) { main.post { callback(lat, lon) } }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LocationPickerDialog(
    initial: Pair<Double, Double>?,
    onPicked: (Pair<Double, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var picked by remember { mutableStateOf(initial) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var widthDp by remember { mutableStateOf(0) }
    var heightDp by remember { mutableStateOf(0) }

    LaunchedEffect(pageReady, widthDp, heightDp) {
        val wv = webView
        if (pageReady && wv != null && heightDp > 0) {
            wv.evaluateJavascript("Picker.setSize($widthDp,$heightDp);", null)
            initial?.let { wv.evaluateJavascript("Picker.setMarker(${it.first},${it.second});", null) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = BrandBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            widthDp = with(density) { it.width.toDp().value.toInt() }
                            heightDp = with(density) { it.height.toDp().value.toInt() }
                        },
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true
                            settings.setSupportZoom(false)
                            setBackgroundColor(0xFFEEF2F0.toInt())
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) { pageReady = true }
                            }
                            addJavascriptInterface(PickerBridge { lat, lon -> picked = lat to lon }, "Android")
                            loadUrl("file:///android_asset/map/picker.html")
                            webView = this
                        }
                    }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = BrandSurface.copy(alpha = 0.96f),
                    shadowElevation = 3.dp
                ) {
                    Text(
                        "Toca el mapa para marcar la propiedad",
                        fontSize = 13.sp,
                        color = BrandText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { picked?.let { onPicked(it) }; onDismiss() },
                        enabled = picked != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text(
                            picked?.let { "Usar este punto (%.5f, %.5f)".format(it.first, it.second) } ?: "Usar este punto",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = BrandSurface, contentColor = BrandPrimary)
                    ) { Text("Cancelar") }
                }
            }
        }
    }
}
