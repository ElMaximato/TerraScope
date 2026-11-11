package com.terrascope.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.model.FieldSession
import com.terrascope.app.data.model.PropertyItem
import com.terrascope.app.data.model.ZoneItem
import com.terrascope.app.data.network.TerraScopeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado genérico para listas remotas
sealed class ListState<out T> {
    object Idle : ListState<Nothing>()
    object Loading : ListState<Nothing>()
    data class Success<T>(val data: List<T>) : ListState<T>()
    data class Error(val message: String) : ListState<Nothing>()
}

// ── HomeViewModel ─────────────────────────────────────────────────────────

class HomeViewModel : ViewModel() {
    private val _zones = MutableStateFlow<ListState<ZoneItem>>(ListState.Idle)
    val zones: StateFlow<ListState<ZoneItem>> = _zones

    private val _watchlist = MutableStateFlow<ListState<ZoneItem>>(ListState.Idle)
    val watchlist: StateFlow<ListState<ZoneItem>> = _watchlist

    fun load(token: String) {
        viewModelScope.launch {
            _zones.value = ListState.Loading
            _watchlist.value = ListState.Loading
            try {
                val z = TerraScopeApi.fetchZones(token)
                _zones.value = ListState.Success(z)
            } catch (e: Exception) {
                _zones.value = ListState.Error(e.message ?: "Error al cargar zonas")
            }
            try {
                val w = TerraScopeApi.fetchWatchlist(token)
                _watchlist.value = ListState.Success(w)
            } catch (e: Exception) {
                _watchlist.value = ListState.Error(e.message ?: "Error al cargar watchlist")
            }
        }
    }
}

// ── ZoneDetailViewModel ───────────────────────────────────────────────────

sealed class DetailState {
    object Idle : DetailState()
    object Loading : DetailState()
    data class Success(
        val zone: com.terrascope.app.data.model.ZoneDetail,
        val inWatchlist: Boolean,
        val score: com.terrascope.app.data.model.ZoneScore? = null
    ) : DetailState()
    data class Error(val message: String) : DetailState()
}

class ZoneDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow<DetailState>(DetailState.Idle)
    val state: StateFlow<DetailState> = _state

    private val _actionMsg = MutableStateFlow<String?>(null)
    val actionMsg: StateFlow<String?> = _actionMsg

    fun load(zoneId: Int, token: String) {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            try {
                val detail = TerraScopeApi.fetchZoneDetail(zoneId, token)
                val watchlist = TerraScopeApi.fetchWatchlist(token)
                val inWl = watchlist.any { it.id == zoneId }
                // El puntaje es opcional: si falla, la ficha se muestra igual sin él.
                val score = try { TerraScopeApi.fetchZoneScore(zoneId, token, InvestSelection.profile.value) } catch (e: Exception) { null }
                _state.value = DetailState.Success(detail, inWl, score)
            } catch (e: Exception) {
                _state.value = DetailState.Error(e.message ?: "Error al cargar la zona")
            }
        }
    }

    fun toggleWatchlist(zoneId: Int, token: String) {
        val current = _state.value as? DetailState.Success ?: return
        viewModelScope.launch {
            try {
                if (current.inWatchlist) {
                    TerraScopeApi.removeFromWatchlist(zoneId, token)
                    _state.value = current.copy(inWatchlist = false)
                    com.terrascope.app.data.wear.ZoneSyncTrigger.bump()
                    _actionMsg.value = "Zona eliminada de guardados"
                } else {
                    TerraScopeApi.addToWatchlist(zoneId, token)
                    _state.value = current.copy(inWatchlist = true)
                    com.terrascope.app.data.wear.ZoneSyncTrigger.bump()
                    _actionMsg.value = "Zona guardada"
                }
            } catch (e: Exception) {
                _actionMsg.value = "Error al actualizar la lista"
            }
        }
    }

    fun clearActionMsg() { _actionMsg.value = null }
}

// ── SavedViewModel ────────────────────────────────────────────────────────

class SavedViewModel : ViewModel() {
    private val _watchlist = MutableStateFlow<ListState<ZoneItem>>(ListState.Idle)
    val watchlist: StateFlow<ListState<ZoneItem>> = _watchlist

    private val _properties = MutableStateFlow<ListState<PropertyItem>>(ListState.Idle)
    val properties: StateFlow<ListState<PropertyItem>> = _properties

    fun load(token: String) {
        viewModelScope.launch {
            _watchlist.value = ListState.Loading
            _properties.value = ListState.Loading
            try {
                _watchlist.value = ListState.Success(TerraScopeApi.fetchWatchlist(token))
            } catch (e: Exception) {
                _watchlist.value = ListState.Error(e.message ?: "Error")
            }
            try {
                _properties.value = ListState.Success(TerraScopeApi.fetchProperties(token))
            } catch (e: Exception) {
                _properties.value = ListState.Error(e.message ?: "Error")
            }
        }
    }

    fun deleteProperty(id: Int, token: String) {
        viewModelScope.launch {
            try {
                TerraScopeApi.deleteProperty(id, token)
                val current = (_properties.value as? ListState.Success)?.data ?: return@launch
                _properties.value = ListState.Success(current.filter { it.id != id })
            } catch (e: Exception) { /* ignorar silenciosamente */ }
        }
    }

    fun removeFromWatchlist(zoneId: Int, token: String) {
        viewModelScope.launch {
            try {
                TerraScopeApi.removeFromWatchlist(zoneId, token)
                val current = (_watchlist.value as? ListState.Success)?.data ?: return@launch
                _watchlist.value = ListState.Success(current.filter { it.id != zoneId })
            } catch (e: Exception) { /* ignorar silenciosamente */ }
        }
    }
}

// ── FieldViewModel ────────────────────────────────────────────────────────

class FieldViewModel : ViewModel() {
    private val _points = MutableStateFlow<ListState<com.terrascope.app.data.model.FieldPoint>>(ListState.Idle)
    val points: StateFlow<ListState<com.terrascope.app.data.model.FieldPoint>> = _points

    fun load(token: String) {
        viewModelScope.launch {
            _points.value = ListState.Loading
            try {
                _points.value = ListState.Success(TerraScopeApi.fetchObservations(token))
            } catch (e: Exception) {
                _points.value = ListState.Error(e.message ?: "Error al cargar los puntos")
            }
        }
    }

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    fun clearActionError() { _actionError.value = null }

    fun edit(id: String, note: String, tag: String, token: String) {
        viewModelScope.launch {
            try {
                TerraScopeApi.updateObservation(id, note, tag, token)
                _points.value = ListState.Success(TerraScopeApi.fetchObservations(token))
            } catch (e: Exception) { _actionError.value = "No se pudo guardar el cambio" }
        }
    }

    fun delete(id: String, token: String) {
        viewModelScope.launch {
            try {
                TerraScopeApi.deleteObservation(id, token)
                _points.value = ListState.Success(TerraScopeApi.fetchObservations(token))
            } catch (e: Exception) { _actionError.value = "No se pudo borrar el punto" }
        }
    }
}

// ── Perfil de inversión elegido (compartido entre "Invertir" y la ficha de zona) ──

object InvestSelection {
    val profile = MutableStateFlow("retail")
}

sealed class RankingState {
    object Loading : RankingState()
    data class Success(val ranking: com.terrascope.app.data.model.Ranking) : RankingState()
    data class Error(val message: String) : RankingState()
}

class InvestViewModel : ViewModel() {
    private val _profiles = MutableStateFlow<List<com.terrascope.app.data.model.InvestProfile>>(emptyList())
    val profiles: StateFlow<List<com.terrascope.app.data.model.InvestProfile>> = _profiles

    val selected: StateFlow<String> = InvestSelection.profile

    private val _ranking = MutableStateFlow<RankingState>(RankingState.Loading)
    val ranking: StateFlow<RankingState> = _ranking

    private val _matchMsg = MutableStateFlow<String?>(null)
    val matchMsg: StateFlow<String?> = _matchMsg

    fun load(token: String) {
        viewModelScope.launch {
            try { _profiles.value = TerraScopeApi.fetchProfiles(token) } catch (e: Exception) { /* se reintenta al recargar */ }
        }
        loadRanking(token)
    }

    fun select(id: String, token: String) {
        InvestSelection.profile.value = id
        loadRanking(token)
    }

    private fun loadRanking(token: String) {
        viewModelScope.launch {
            _ranking.value = RankingState.Loading
            _ranking.value = try {
                RankingState.Success(TerraScopeApi.fetchRanking(InvestSelection.profile.value, token))
            } catch (e: Exception) {
                RankingState.Error(e.message ?: "No se pudo cargar el ranking")
            }
        }
    }

    fun describe(text: String, token: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val m = TerraScopeApi.matchProfile(text, token)
                if (m == null) {
                    _matchMsg.value = "No reconocí el tipo de inversión. Elige un perfil de la lista."
                } else {
                    _matchMsg.value = "Entendí: ${m.label} (por \"${m.matched.joinToString(", ")}\")"
                    select(m.profile, token)
                }
            } catch (e: Exception) {
                _matchMsg.value = "No se pudo interpretar la descripción."
            }
        }
    }
}
