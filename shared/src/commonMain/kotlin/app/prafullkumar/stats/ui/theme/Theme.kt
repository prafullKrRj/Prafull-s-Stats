package app.prafullkumar.stats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.prafullkumar.stats.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageLight,
    onPrimaryContainer = Color(0xFF11351F),
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = ClayLight,
    onSecondaryContainer = Color(0xFF4A2409),
    tertiary = Plum,
    onTertiary = Color.White,
    tertiaryContainer = PlumLight,
    onTertiaryContainer = Color(0xFF241C63),
    error = Color(0xFFB3261E),
    background = Cream,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = Color(0xFFEFECE5),
    onSurfaceVariant = MutedLight,
    // Dialogs and sheets use the same warm surface, not Material's purple tint.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F1EB),
    surfaceContainer = CardLight,
    surfaceContainerHigh = Color(0xFFFAF8F3),
    surfaceContainerHighest = Color(0xFFF1EEE7),
    outline = Color(0xFFD6D1C6),
    outlineVariant = Color(0xFFE8E4DB)
)

private val DarkColors = darkColorScheme(
    primary = SageDark,
    onPrimary = Color(0xFF0C2B1A),
    primaryContainer = Color(0xFF26473A),
    onPrimaryContainer = Color(0xFFCDEEDB),
    secondary = ClayDark,
    onSecondary = Color(0xFF3A1B06),
    secondaryContainer = Color(0xFF4F2E14),
    onSecondaryContainer = Color(0xFFFBE7D6),
    tertiary = PlumDark,
    onTertiary = Color(0xFF1D1754),
    tertiaryContainer = Color(0xFF353076),
    onTertiaryContainer = Color(0xFFE6E4F8),
    error = Color(0xFFF2B8B5),
    background = Charcoal,
    onBackground = InkDark,
    surface = CharcoalCard,
    onSurface = InkDark,
    surfaceVariant = CharcoalRaised,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = Color(0xFF0F110F),
    surfaceContainerLow = Color(0xFF181A18),
    surfaceContainer = CharcoalCard,
    surfaceContainerHigh = Color(0xFF232723),
    surfaceContainerHighest = Color(0xFF2B302B),
    outline = Color(0xFF3B413B),
    outlineVariant = Color(0xFF2C312C)
)

/** True while the dark scheme is active — cards switch from shadow to hairline border. */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun PrafullStatsTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
