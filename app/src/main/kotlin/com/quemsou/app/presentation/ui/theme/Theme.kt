package com.quemsou.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val QuemSouDarkColorScheme = darkColorScheme(
    primary = QuemSouPrimary,
    onPrimary = QuemSouOnPrimary,
    secondary = QuemSouSecondary,
    onSecondary = FestaTinta,
    primaryContainer = Color(0xFF293B94),
    onPrimaryContainer = Color(0xFFE1E5FF),
    secondaryContainer = Color(0xFF34432A),
    onSecondaryContainer = FestaLima,
    tertiary = FestaCoral,
    tertiaryContainer = Color(0xFF613C38),
    onTertiaryContainer = Color(0xFFFFDAD2),
    surfaceVariant = Color(0xFF303544),
    onSurfaceVariant = Color(0xFFC2C6D6),
    outline = Color(0xFF8C92A5),
    outlineVariant = Color(0xFF454B60),
    surfaceContainer = Color(0xFF242838),
    background = QuemSouBackground,
    surface = QuemSouSurface,
    onBackground = QuemSouOnBackground,
    onSurface = QuemSouOnSurface,
)

private val QuemSouLightColorScheme = lightColorScheme(
    primary = QuemSouPrimaryLight,
    onPrimary = QuemSouOnPrimaryLight,
    secondary = Color(0xFF4C642D),
    onSecondary = Color.White,
    primaryContainer = Color(0xFFE0E5FF),
    onPrimaryContainer = Color(0xFF1D2D7C),
    secondaryContainer = FestaLima,
    onSecondaryContainer = FestaTinta,
    tertiary = Color(0xFF925048),
    tertiaryContainer = Color(0xFFFFDAD2),
    onTertiaryContainer = Color(0xFF45231F),
    surfaceVariant = Color(0xFFE9E8E0),
    onSurfaceVariant = Color(0xFF565B6B),
    outline = Color(0xFF767B8A),
    outlineVariant = Color(0xFFD0D3DC),
    surfaceContainer = Color(0xFFEEEDE6),
    background = QuemSouBackgroundLight,
    surface = QuemSouSurfaceLight,
    onBackground = QuemSouOnBackgroundLight,
    onSurface = QuemSouOnSurfaceLight,
)

/**
 * Tema compartilhado pela central e pelos jogos. Segue o tema do aparelho.
 */
@Composable
fun QuemSouTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) QuemSouDarkColorScheme else QuemSouLightColorScheme,
        typography = QuemSouTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
