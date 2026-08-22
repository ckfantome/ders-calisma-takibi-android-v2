package com.derscalismatakibi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// study_tracker2.py -> DARK/LIGHT Palette (bkz. build_stylesheet) renklerinden
// esinlenen sabit bir Material3 temasi. Bu turda tasarim/tema ozellestirme
// editoru (masaustundeki DesignPageDialog) TASINMADI - tek, sabit bir tema var.

val AccentBlue = Color(0xFF5B8CFF)
val SuccessGreen = Color(0xFF3DDC84)
val WarningOrange = Color(0xFFF5A623)
val DangerRed = Color(0xFFFF5D6C)

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    secondary = Color(0xFFA879FF),
    background = Color(0xFF0E0F12),
    surface = Color(0xFF16181D),
    surfaceVariant = Color(0xFF1E2128),
    onBackground = Color(0xFFF2F3F5),
    onSurface = Color(0xFFF2F3F5),
    error = DangerRed,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3763E0),
    secondary = Color(0xFF7C3FD6),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F2F5),
    onBackground = Color(0xFF1A1C22),
    onSurface = Color(0xFF1A1C22),
    error = Color(0xFFD8394A),
)

@Composable
fun DersCalismaTakibiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
