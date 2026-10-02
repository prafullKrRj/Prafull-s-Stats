package app.prafullkumar.stats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.prafullkumar.stats.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF0B0820),
    primaryContainer = Color(0xFF2A2260),
    onPrimaryContainer = Color(0xFFE4DEFF),
    secondary = Ember,
    onSecondary = Color(0xFF2A0E00),
    secondaryContainer = Color(0xFF4A2310),
    onSecondaryContainer = Color(0xFFFFDCCB),
    tertiary = Cyan,
    onTertiary = Color(0xFF00222A),
    tertiaryContainer = Color(0xFF0C3A44),
    onTertiaryContainer = Color(0xFFC9F6FF),
    error = Rose,
    background = Night,
    onBackground = InkDark,
    surface = NightCard,
    onSurface = InkDark,
    surfaceVariant = NightRaised,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = Night,
    surfaceContainerLow = Color(0xFF0B101C),
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightRaised,
    surfaceContainerHighest = Color(0xFF1C2439),
    outline = Color(0xFF2E3854),
    outlineVariant = NightLine
)

private val LightColors = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1E1466),
    secondary = Color(0xFFE2572B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3D6),
    onSecondaryContainer = Color(0xFF4A1A05),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3F4FB),
    onTertiaryContainer = Color(0xFF053340),
    error = Color(0xFFE11D48),
    background = Paper,
    onBackground = InkLight,
    surface = PaperCard,
    onSurface = InkLight,
    surfaceVariant = Color(0xFFECEEF6),
    onSurfaceVariant = MutedLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = PaperCard,
    surfaceContainerHigh = Color(0xFFF3F4FA),
    surfaceContainerHighest = Color(0xFFEBEDF5),
    outline = Color(0xFFD5D9E8),
    outlineVariant = Color(0xFFE6E9F2)
)

@Composable
fun PrafullStatsTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
