package com.terrascope.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.model.ZoneDetail
import com.terrascope.app.data.network.ApiException
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.screen.MapIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MapUiState {
    object Loading : MapUiState()
    data class Error(val message: String) : MapUiState()
    /** [skipped] = zonas cuyo detalle o límite no se pudo cargar. */
    data class Ready(val zones: List<ZoneDetail>, val skipped: Int) : MapUiState()
}

class MapViewModel : ViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val state: StateFlow<MapUiState> = _state

    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId

    private val _indicator = MutableStateFlow(MapIndicator.POPULATION)
    val indicator: StateFlow<MapIndicator> = _indicator

    private var loadJob: Job? = null

    fun load(token: String, force: Boolean = false) {
        if (!force && _state.value is MapUiState.Ready) return
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = MapUiState.Loading
            try {
                val zones = TerraScopeApi.fetchZones(token)
                // Detalle de todas las zonas en paralelo; una falla individual no tumba el mapa.
                val details = coroutineScope {
                    zones.map { z ->
                        async { runCatching { TerraScopeApi.fetchZoneDetail(z.id, token) }.getOrNull() }
                    }.awaitAll()
                }
                val usable = details.filterNotNull().filter { !it.boundaryGeoJson.isNullOrBlank() }
                _state.value = when {
                    zones.isEmpty() -> MapUiState.Error("Todavía no hay zonas cargadas en el servidor.")
                    usable.isEmpty() -> MapUiState.Error("No se pudieron cargar los límites de las zonas.")
                    else -> MapUiState.Ready(usable, zones.size - usable.size)
                }
            } catch (e: Exception) {
                _state.value = MapUiState.Error(
                    if (e is ApiException && e.statusCode == 401) "Tu sesión expiró. Cierra sesión y vuelve a entrar."
                    else "No se pudo conectar con el servidor. Revisa que el backend esté corriendo."
                )
            }
        }
    }

    fun select(id: Int?) { _selectedId.value = id }

    fun setIndicator(indicator: MapIndicator) { _indicator.value = indicator }
}
