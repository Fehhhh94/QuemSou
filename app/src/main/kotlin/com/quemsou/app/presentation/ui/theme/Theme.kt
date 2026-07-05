package com.quemsou.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val QuemSouDarkColorScheme = darkColorScheme(
    primary = QuemSouPrimary,
    onPrimary = QuemSouOnPrimary,
    secondary = QuemSouSecondary,
    background = QuemSouBackground,
    surface = QuemSouSurface,
    onBackground = QuemSouOnBackground,
    onSurface = QuemSouOnSurface,
)

private val QuemSouLightColorScheme = lightColorScheme(
    primary = QuemSouPrimaryLight,
    onPrimary = QuemSouOnPrimaryLight,
    secondary = QuemSouSecondary,
    background = QuemSouBackgroundLight,
    surface = QuemSouSurfaceLight,
    onBackground = QuemSouOnBackgroundLight,
    onSurface = QuemSouOnSurfaceLight,
)

/**
 * Tema Material 3 do QuemSou. Segue o tema claro/escuro do aparelho — cores
 * definitivas da identidade visual virão em uma fase futura.
 */
@Composable
fun QuemSouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) QuemSouDarkColorScheme else QuemSouLightColorScheme,
        typography = QuemSouTypography,
        content = content,
    )
}
