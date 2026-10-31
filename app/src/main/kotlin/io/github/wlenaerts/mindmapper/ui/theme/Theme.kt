package io.github.wlenaerts.mindmapper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed colours instead of dynamic (wallpaper) colour, so the app looks the same on every device.
val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF3D5A80),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF0E2A4A),
    secondary = Color(0xFFE4572E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF3A0B00),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E1D8),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF1ECE4),
    surfaceContainerHigh = Color(0xFFEBE6DE),
    outline = Color(0xFF7A757F),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0E2A4A),
    primaryContainer = Color(0xFF254166),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFFFB59D),
    onSecondary = Color(0xFF5E1700),
    secondaryContainer = Color(0xFF842300),
    onSecondaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFF17171B),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF17171B),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF3A3840),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF222127),
    surfaceContainerHigh = Color(0xFF2B2A30),
    outline = Color(0xFF938F99),
)

@Composable
fun MindmapperTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
