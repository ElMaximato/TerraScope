package com.terrascope.app.ui.navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SensorOccupied
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.terrascope.app.data.model.AuthUser
import com.terrascope.app.data.model.PropertyItem
import com.terrascope.app.data.network.TerraScopeApi
import com.terrascope.app.ui.screen.*
import com.terrascope.app.ui.theme.BrandBackground
import com.terrascope.app.ui.theme.BrandLine
import com.terrascope.app.ui.theme.BrandPrimary
import com.terrascope.app.ui.theme.BrandTextMuted
import com.terrascope.app.ui.viewmodel.*

sealed class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Tab("home", "Inicio", Icons.Default.Home)
    object Map : Tab("map", "Mapa", Icons.Default.LocationOn)
    object Invest : Tab("invest", "Invertir", Icons.Default.TrendingUp)
    object Saved : Tab("saved", "Guardados", Icons.Default.Bookmark)
    object Field : Tab("field", "Campo", Icons.Default.SensorOccupied)
}

val BOTTOM_TABS = listOf(Tab.Home, Tab.Map, Tab.Invest, Tab.Saved, Tab.Field)

@Composable
fun MainNavigation(
    user: AuthUser,
    token: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    // ViewModels compartidos entre pestañas
    val homeVm: HomeViewModel = viewModel()
    val mapVm: MapViewModel = viewModel()
    val investVm: InvestViewModel = viewModel()
    val compareVm: CompareViewModel = viewModel()
    val propertyFormVm: PropertyFormViewModel = viewModel()
    val verdictVm: PropertyVerdictViewModel = viewModel()
    val savedVm: SavedViewModel = viewModel()
    val fieldVm: FieldViewModel = viewModel()
    val zoneDetailVm: ZoneDetailViewModel = viewModel()
    val reportVm: ReportViewModel = viewModel()
    val analysisVm: AnalysisViewModel = viewModel()
    val propertyCompareVm: PropertyCompareViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Zonas guardadas -> reloj: al entrar, al guardar/quitar una zona y al cambiar el perfil de inversión.
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    // Red inestable: al abrir la app y cada minuto mientras está abierta se reintenta subir lo que el reloj dejó pendiente.
    LaunchedEffect(token) {
        while (true) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.terrascope.app.data.wear.PhoneFieldSync.processPending(appContext)
                }
            } catch (e: Exception) { /* sin reloj o sin red: se reintenta */ }
            kotlinx.coroutines.delay(60_000)
        }
    }

    val zoneTick by com.terrascope.app.data.wear.ZoneSyncTrigger.tick.collectAsState()
    val syncProfile by InvestSelection.profile.collectAsState()
    LaunchedEffect(token, zoneTick, syncProfile) {
        com.terrascope.app.data.wear.PhoneZoneSync.refresh(appContext, token)
    }

    // Rutas que muestran la barra inferior
    val showBottomBar = BOTTOM_TABS.any { currentRoute == it.route } ||
            currentRoute == "profile"

    Scaffold(
        containerColor = BrandBackground,
        bottomBar = {
            if (showBottomBar && currentRoute != "profile") {
                NavigationBar(
                    containerColor = BrandBackground,
                    tonalElevation = 0.dp
                ) {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Tab.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                unselectedIconColor = BrandTextMuted,
                                unselectedTextColor = BrandTextMuted,
                                indicatorColor = BrandLine
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    viewModel = homeVm,
                    token = token,
                    userEmail = user.email,
                    onZoneClick = { zone ->
                        navController.navigate("zone/${zone.id}")
                    }
                )
            }

            composable(Tab.Map.route) {
                MapScreen(
                    viewModel = mapVm,
                    token = token,
                    onZoneClick = { zone ->
                        navController.navigate("zone/${zone.id}")
                    },
                    onPropertyClick = { id -> navController.navigate("property/$id") }
                )
            }

            composable(Tab.Invest.route) {
                InvestScreen(
                    viewModel = investVm,
                    token = token,
                    onZoneClick = { zoneId -> navController.navigate("zone/$zoneId") },
                    onCompare = { navController.navigate("compare") }
                )
            }

            composable(Tab.Saved.route) {
                SavedScreen(
                    viewModel = savedVm,
                    token = token,
                    onZoneClick = { zone ->
                        navController.navigate("zone/${zone.id}")
                    },
                    onAddProperty = { navController.navigate("property/new") },
                    onPropertyZone = { id -> navController.navigate("property/$id") },
                    onCompareProperties = { navController.navigate("compare-properties") }
                )
            }

            composable(Tab.Field.route) {
                FieldScreen(
                    viewModel = fieldVm,
                    token = token,
                    onEvaluate = { lat, lon, name ->
                        navController.navigate("property/new?lat=$lat&lon=$lon&name=${android.net.Uri.encode(name)}")
                    },
                    onAnalyze = { navController.navigate("analysis") }
                )
            }

            composable("analysis") {
                AnalysisScreen(viewModel = analysisVm, token = token, onBack = { navController.popBackStack() })
            }

            composable(
                route = "zone/{zoneId}",
                arguments = listOf(navArgument("zoneId") { type = NavType.IntType })
            ) { backStackEntry ->
                val zoneId = backStackEntry.arguments?.getInt("zoneId") ?: return@composable
                ZoneDetailScreen(
                    viewModel = zoneDetailVm,
                    zoneId = zoneId,
                    token = token,
                    onBack = { navController.popBackStack() },
                    onReport = { id -> navController.navigate("report/$id") }
                )
            }

            composable(
                route = "report/{zoneId}",
                arguments = listOf(navArgument("zoneId") { type = NavType.IntType })
            ) { entry ->
                val zoneId = entry.arguments?.getInt("zoneId") ?: return@composable
                ReportScreen(viewModel = reportVm, zoneId = zoneId, token = token, onBack = { navController.popBackStack() })
            }

            composable(
                route = "property/new?lat={lat}&lon={lon}&name={name}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
                val lon = entry.arguments?.getString("lon")?.toDoubleOrNull()
                PropertyFormScreen(
                    viewModel = propertyFormVm,
                    token = token,
                    initialLocation = if (lat != null && lon != null) lat to lon else null,
                    initialName = entry.arguments?.getString("name") ?: "",
                    onBack = { navController.popBackStack() },
                    onSaved = { property ->
                        navController.popBackStack()
                        // Se abre el veredicto de la propiedad: en qué zona quedó y qué tal está.
                        navController.navigate("property/${property.id}")
                    }
                )
            }

            composable(
                route = "property/{propertyId}",
                arguments = listOf(navArgument("propertyId") { type = NavType.IntType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getInt("propertyId") ?: return@composable
                PropertyVerdictScreen(
                    viewModel = verdictVm,
                    propertyId = propertyId,
                    token = token,
                    onBack = { navController.popBackStack() },
                    onOpenZone = { zoneId -> navController.navigate("zone/$zoneId") },
                    onCompare = { navController.navigate("compare") },
                    onEdit = { id -> navController.navigate("property/edit/$id") }
                )
            }

            composable(
                route = "property/edit/{propertyId}",
                arguments = listOf(navArgument("propertyId") { type = NavType.IntType })
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getInt("propertyId") ?: return@composable
                val loaded by produceState<PropertyItem?>(initialValue = null, propertyId, token) {
                    value = try { TerraScopeApi.fetchProperties(token).firstOrNull { it.id == propertyId } } catch (e: Exception) { null }
                }
                val prop = loaded
                if (prop == null) CenteredLoader() else PropertyFormScreen(
                    viewModel = propertyFormVm,
                    token = token,
                    editing = prop,
                    onBack = { navController.popBackStack() },
                    onSaved = { saved ->
                        // Se cierra el formulario y el veredicto viejo, y se abre el veredicto ya actualizado.
                        navController.popBackStack()
                        navController.popBackStack()
                        navController.navigate("property/${saved.id}")
                    }
                )
            }

            composable("compare-properties") {
                PropertyCompareScreen(viewModel = propertyCompareVm, token = token, onBack = { navController.popBackStack() })
            }

            composable("compare") {
                CompareScreen(
                    viewModel = compareVm,
                    token = token,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                ProfileScreen(
                    user = user,
                    onLogout = onLogout
                )
            }
        }
    }
}

