package com.pairplay.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF6366F1)
private val PrimaryContainer = Color(0xFF818CF8)
private val Secondary = Color(0xFF22D3EE)
private val SecondaryContainer = Color(0xFF06B6D4)
private val Tertiary = Color(0xFFF59E0B)
private val ErrorColor = Color(0xFFEF4444)
private val BgLight = Color(0xFFF8FAFC)
private val SurfaceLight = Color(0xFFFFFFFF)
private val BgDark = Color(0xFF0F172A)
private val SurfaceDark = Color(0xFF1E293B)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    tertiary = Tertiary,
    background = BgLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    error = ErrorColor
)

private val DarkColors = darkColorScheme(
    primary = PrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = Primary,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    tertiary = Tertiary,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF334155),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ErrorColor
)

@Composable
fun PairPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
