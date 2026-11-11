package com.terrascope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.terrascope.app.ui.navigation.MainNavigation
import com.terrascope.app.ui.screen.AuthScreen
import com.terrascope.app.ui.theme.BrandBackground
import com.terrascope.app.ui.theme.BrandPrimary
import com.terrascope.app.ui.theme.TerraScopeTheme
import com.terrascope.app.ui.viewmodel.AuthState
import com.terrascope.app.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TerraScopeTheme {
                val authState by authViewModel.state.collectAsState()

                when (val state = authState) {
                    // Verificando token guardado en DataStore
                    AuthState.Checking -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandPrimary)
                        }
                    }

                    // Sin sesión o error → pantalla de login/registro
                    AuthState.Unauthenticated,
                    is AuthState.Error -> {
                        AuthScreen(viewModel = authViewModel)
                    }

                    // Con sesión → navegación principal
                    is AuthState.Authenticated -> {
                        MainNavigation(
                            user = state.user,
                            token = state.token,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}
