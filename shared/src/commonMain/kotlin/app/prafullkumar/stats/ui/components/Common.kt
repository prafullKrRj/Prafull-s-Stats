package app.prafullkumar.stats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.ui.theme.NeonBrush
import kotlin.math.min

/** Rounded card used for every block on every screen. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = if (glow) NeonBrush else Brush.linearGradient(listOf(scheme.outlineVariant, scheme.outlineVariant)),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Big screen title with an optional subtitle and trailing slot. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Hint(subtitle)
        }
        trailing()
    }
}

@Composable
fun MacroChip(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** Circular progress ring with the value stacked in the middle. */
@Composable
fun ProgressRing(
    value: Double,
    target: Double,
    color: Color,
    label: String,
    unit: String,
    size: Dp = 92.dp,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    /** False shows just "value+unit" in the middle, e.g. "45%". */
    showTarget: Boolean = true
) {
    val fraction = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.outlineVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = this.size.minDimension * 0.11f
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (fraction > 0f) {
                    if (brush != null) {
                        drawArc(brush, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    } else {
                        drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatNumber(value) + if (showTarget) "" else unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (showTarget) {
                    Text(
                        "/ ${formatNumber(target)}$unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Flat progress bar. Pass [brush] for the neon gradient. */
@Composable
fun ThinBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    brush: Brush? = null
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2f
        drawRoundRect(color = track, size = size, cornerRadius = CornerRadius(r, r))
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) {
            val s = Size(min(w, size.width).coerceAtLeast(size.height), size.height)
            if (brush != null) drawRoundRect(brush = brush, size = s, cornerRadius = CornerRadius(r, r))
            else drawRoundRect(color = color, size = s, cornerRadius = CornerRadius(r, r))
        }
    }
}

@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

/** Digits with at most one decimal point. */
fun cleanDecimal(raw: String): String {
    val sb = StringBuilder()
    var dotUsed = false
    raw.forEach { ch ->
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !dotUsed -> {
                dotUsed = true
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}

@Composable
fun CheckDot(checked: Boolean, size: Dp = 30.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) color else scheme.surfaceVariant)
            .border(1.dp, if (checked) color else scheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = "Done", tint = scheme.onPrimary, modifier = Modifier.size(size * 0.63f))
        }
    }
}

@Composable
fun RoundStep(label: String, size: Dp = 40.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Soft tonal button with a plus icon. */
@Composable
fun AddButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = Icons.Filled.Add, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
    }
}

/** The primary action on a screen — neon gradient fill. */
@Composable
fun NeonButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) NeonBrush else Brush.linearGradient(listOf(Color.Gray, Color.Gray)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** Pill that is either selected or not — slots, units, weekdays, ratings. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) scheme.primary else scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

/** Small coloured tag, e.g. a goal's area or a task's priority. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/** The reason behind something, set apart so it is read every time. */
@Composable
fun WhyLine(why: String, modifier: Modifier = Modifier) {
    if (why.isBlank()) return
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text("WHY ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

@Composable
fun NumberInput(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onChange(cleanDecimal(raw)) },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/** Hint line — muted, body size. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}
