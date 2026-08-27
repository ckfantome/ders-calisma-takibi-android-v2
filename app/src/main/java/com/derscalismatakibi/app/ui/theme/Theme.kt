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

// Onceden sadece birkac rol (primary/secondary/background/surface/error) set
// edilmisti - geri kalan M3 rolleri (container'lar, tertiary, outline, inverse*)
// varsayilan Material mor paletinde kaliyordu, bu yuzden bazi bilesenler
// (orn. secondary/tertiary container kullanan kartlar) istemeden marka
// paletiyle uyusmayan mor tonlarda gorunuyordu. Tum rolleri tamamlandi -
// tertiary rolu icin var olan SuccessGreen marka rengi kullanildi.
private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF06255C),
    primaryContainer = Color(0xFF2B4B99),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFA879FF),
    onSecondary = Color(0xFF2B0069),
    secondaryContainer = Color(0xFF4A2E8F),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = SuccessGreen,
    onTertiary = Color(0xFF00391B),
    tertiaryContainer = Color(0xFF00522A),
    onTertiaryContainer = Color(0xFFB6F2CB),
    error = DangerRed,
    onError = Color(0xFF680014),
    errorContainer = Color(0xFF93000F),
    onErrorContainer = Color(0xFFFFDAD8),
    background = Color(0xFF0E0F12),
    onBackground = Color(0xFFF2F3F5),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF1E2128),
    onSurfaceVariant = Color(0xFFC5C6CF),
    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF43454E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF2F3F5),
    inverseOnSurface = Color(0xFF1A1C22),
    inversePrimary = Color(0xFF3763E0),
    surfaceTint = AccentBlue,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3763E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF7C3FD6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF280059),
    tertiary = Color(0xFF1C7D45),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB6F2CB),
    onTertiaryContainer = Color(0xFF002110),
    error = Color(0xFFD8394A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD8),
    onErrorContainer = Color(0xFF410008),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFF1F2F5),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF75767F),
    outlineVariant = Color(0xFFC5C6D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF16181D),
    inverseOnSurface = Color(0xFFF2F3F5),
    inversePrimary = AccentBlue,
    surfaceTint = Color(0xFF3763E0),
)

@Composable
fun DersCalismaTakibiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
