package com.floatingscreen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ===== Color Palette =====
val PrimaryBlue = Color(0xFF2196F3)
val PrimaryDark = Color(0xFF1565C0)
val AccentRed = Color(0xFFE53935)
val AccentGreen = Color(0xFF43A047)
val AccentOrange = Color(0xFFFF6D00)
val SurfaceDark = Color(0xFF121212)
val SurfaceDark2 = Color(0xFF1E1E1E)
val SurfaceDark3 = Color(0xFF2C2C2C)
val SurfaceLight = Color(0xFFFAFAFA)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0BEC5)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    error = AccentRed,
    background = SurfaceDark,
    surface = SurfaceDark2,
    surfaceVariant = SurfaceDark3,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = AccentOrange,
    tertiary = AccentGreen,
    error = AccentRed,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF616161)
)

@Composable
fun FloatingScreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
