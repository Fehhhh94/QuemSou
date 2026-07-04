package com.quemsou.app.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

/**
 * Tema Material 3 do QuemSou. Na v1 o app é sempre escuro;
 * cores definitivas e suporte a tema claro virão em uma fase futura.
 */
@Composable
fun QuemSouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuemSouDarkColorScheme,
        typography = QuemSouTypography,
        content = content,
    )
}
