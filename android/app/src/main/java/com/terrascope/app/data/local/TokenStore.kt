package com.terrascope.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión de Context para acceder al DataStore con un nombre fijo
val Context.tokenDataStore by preferencesDataStore(name = "terrascope_auth")

object TokenStore {
    private val KEY_TOKEN = stringPreferencesKey("jwt_token")
    private val KEY_EMAIL = stringPreferencesKey("user_email")
    private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")

    fun tokenFlow(context: Context): Flow<String?> =
        context.tokenDataStore.data.map { it[KEY_TOKEN] }

    suspend fun save(context: Context, token: String, email: String, displayName: String?) {
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_EMAIL] = email
            if (displayName != null) prefs[KEY_DISPLAY_NAME] = displayName
        }
    }

    suspend fun clear(context: Context) {
        context.tokenDataStore.edit { it.clear() }
    }

    fun emailFlow(context: Context): Flow<String?> =
        context.tokenDataStore.data.map { it[KEY_EMAIL] }

    fun displayNameFlow(context: Context): Flow<String?> =
        context.tokenDataStore.data.map { it[KEY_DISPLAY_NAME] }
}

