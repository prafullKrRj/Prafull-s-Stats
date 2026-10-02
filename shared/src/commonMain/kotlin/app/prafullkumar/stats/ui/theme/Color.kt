package app.prafullkumar.stats.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Midnight neon: near-black blue base, electric violet + cyan accents.
val Violet = Color(0xFF8B7BFF)
val VioletDeep = Color(0xFF6C5CE7)
val Cyan = Color(0xFF22D3EE)
val Lime = Color(0xFFA3E635)
val Ember = Color(0xFFFF7A45)
val Rose = Color(0xFFFB7185)

val Night = Color(0xFF070A12)
val NightCard = Color(0xFF0F1422)
val NightRaised = Color(0xFF161D30)
val NightLine = Color(0xFF232B42)
val InkDark = Color(0xFFE8ECF8)
val MutedDark = Color(0xFF8D97B5)

val Paper = Color(0xFFF5F6FB)
val PaperCard = Color(0xFFFFFFFF)
val InkLight = Color(0xFF12152A)
val MutedLight = Color(0xFF5E6684)

// Accents reused by chips and charts.
val ProteinColor = Color(0xFF8B7BFF)
val CarbColor = Color(0xFF22D3EE)
val FatColor = Color(0xFFFBBF24)
val FiberColor = Color(0xFFA3E635)
val CalorieColor = Color(0xFF34D399)
val StreakColor = Ember
val FocusColor = Cyan
val GoalColor = Color(0xFFC084FC)

/** Signature gradient for hero numbers, progress and primary buttons. */
val NeonBrush = Brush.linearGradient(listOf(VioletDeep, Violet, Cyan))
