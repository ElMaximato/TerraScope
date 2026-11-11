package com.terrascope.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terrascope.app.data.local.TokenStore
import com.terrascope.app.data.model.AuthUser
import com.terrascope.app.data.network.ApiException
import com.terrascope.app.data.network.TerraScopeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    object Checking : AuthState()          // Verificando token guardado
    object Unauthenticated : AuthState()   // Sin sesión
    data class Authenticated(val user: AuthUser, val token: String) : AuthState()
    data class Error(val message: String, val code: String = "unknown") : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        checkStoredToken()
    }

    private fun checkStoredToken() {
        viewModelScope.launch {
            val token = TokenStore.tokenFlow(getApplication()).first()
            if (token.isNullOrEmpty()) {
                _state.value = AuthState.Unauthenticated
                return@launch
            }
            try {
                val user = TerraScopeApi.fetchMe(token)
                _state.value = AuthState.Authenticated(user, token)
            } catch (e: Exception) {
                // Token expirado o inválido
                TokenStore.clear(getApplication())
                _state.value = AuthState.Unauthenticated
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val (token, user) = TerraScopeApi.login(email, password)
                TokenStore.save(getApplication(), token, user.email, user.displayName)
                _state.value = AuthState.Authenticated(user, token)
            } catch (e: ApiException) {
                val msg = when (e.errorCode) {
                    "invalid_credentials" -> "Correo o contraseña incorrectos"
                    "rate_limit" -> "Demasiados intentos. Espera un momento."
                    else -> "Error de servidor (${e.statusCode})"
                }
                _state.value = AuthState.Error(msg, e.errorCode)
            } catch (e: Exception) {
                _state.value = AuthState.Error("No se pudo conectar al servidor. ¿Está corriendo el backend?")
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val (token, user) = TerraScopeApi.register(email, password, displayName)
                TokenStore.save(getApplication(), token, user.email, user.displayName)
                _state.value = AuthState.Authenticated(user, token)
            } catch (e: ApiException) {
                val msg = when (e.errorCode) {
                    "email_taken" -> "Ese correo ya está registrado"
                    "validation_error" -> "Revisa los datos ingresados"
                    else -> "Error de servidor (${e.statusCode})"
                }
                _state.value = AuthState.Error(msg, e.errorCode)
            } catch (e: Exception) {
                _state.value = AuthState.Error("Fallo: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            TokenStore.clear(getApplication())
            _state.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}

