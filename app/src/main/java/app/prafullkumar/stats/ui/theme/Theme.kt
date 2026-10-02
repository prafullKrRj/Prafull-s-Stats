package app.prafullkumar.stats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    tertiaryContainer = PlumLight,
    onTertiaryContainer = Color(0xFF241C63),
    background = Cream,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = Color(0xFFEFEBE3),
    onSurfaceVariant = MutedLight,
    // Dialogs and sheets use the same warm surface, not Material's purple tint.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFBF7),
    surfaceContainer = CardLight,
    surfaceContainerHigh = Color(0xFFFAF7F1),
    surfaceContainerHighest = Color(0xFFF3EFE7),
    outline = Color(0xFFD8D3C9),
    outlineVariant = Color(0xFFE8E4DC)
)

private val DarkColors = darkColorScheme(
    primary = SageDark,
    onPrimary = Color(0xFF0C2B1A),
    primaryContainer = Color(0xFF27503A),
    onPrimaryContainer = Color(0xFFCDEEDB),
    secondary = Color(0xFFF0B183),
    onSecondary = Color(0xFF3A1B06),
    secondaryContainer = Color(0xFF57320F),
    onSecondaryContainer = Color(0xFFFBE7D6),
    tertiary = Color(0xFFB9AFF3),
    tertiaryContainer = Color(0xFF3A3184),
    onTertiaryContainer = Color(0xFFE9E6FB),
    background = NightBg,
    onBackground = InkDark,
    surface = NightCard,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF262B31),
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = Color(0xFF0F1216),
    surfaceContainerLow = Color(0xFF171B20),
    surfaceContainer = NightCard,
    surfaceContainerHigh = Color(0xFF222831),
    surfaceContainerHighest = Color(0xFF2A313A),
    outline = Color(0xFF3A4149),
    outlineVariant = Color(0xFF2C3239)
)

@Composable
fun PrafullStatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
