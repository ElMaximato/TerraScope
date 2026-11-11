package com.terrascope.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerraScopeColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimarySoft,
    onPrimaryContainer = BrandPrimary,
    secondary = BrandAccent,
    background = BrandBackground,
    onBackground = BrandText,
    surface = BrandSurface,
    onSurface = BrandText,
    outline = BrandLine
)

@Composable
fun TerraScopeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerraScopeColorScheme,
        typography = Typography,
        content = content
    )
}
