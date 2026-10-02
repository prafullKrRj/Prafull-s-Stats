package app.prafullkumar.stats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Weight over time. One point per day that has a weight logged. */
@Composable
fun WeightChart(
    points: List<Double>,
    goal: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.secondary
    if (points.size < 2) {
        Box(
            modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Log weight on two days to see the graph.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Scale to the logged weights only — pulling the goal into the range
    // would squash a month of real change into a flat line.
    val rawMin = points.min()
    val rawMax = points.max()
    val pad = ((rawMax - rawMin) * 0.15).coerceAtLeast(0.5)
    val min = rawMin - pad
    val max = rawMax + pad

    Canvas(
        modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        fun y(v: Double): Float =
            (size.height * (1.0 - (v - min) / (max - min))).toFloat()

        val stepX = size.width / (points.size - 1).toFloat()

        repeat(4) { i ->
            val gy = size.height * i / 3f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }

        goal?.takeIf { it in min..max }?.let { g ->
            drawLine(
                color = goalColor,
                start = Offset(0f, y(g)),
                end = Offset(size.width, y(g)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            )
        }

        val path = Path()
        val fill = Path()
        points.forEachIndexed { i, v ->
            val px = stepX * i
            val py = y(v)
            if (i == 0) {
                path.moveTo(px, py)
                fill.moveTo(px, size.height)
                fill.lineTo(px, py)
            } else {
                path.lineTo(px, py)
                fill.lineTo(px, py)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, color.copy(alpha = 0.12f))
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))

        points.forEachIndexed { i, v ->
            drawCircle(color, radius = 5f, center = Offset(stepX * i, y(v)))
        }
    }
}

/** Simple vertical bars — one per day — with a dashed target line. */
@Composable
fun BarChart(
    bars: List<Pair<String, Float>>,
    color: Color,
    targetFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            targetFraction?.let { t ->
                val line = MaterialTheme.colorScheme.outline
                Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                    val y = size.height * (1f - t.coerceIn(0f, 1f))
                    drawLine(
                        line,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { (_, fraction) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = if (fraction > 0.02f) 0.85f else 0.25f))
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bars.forEach { (label, _) ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Green / grey squares, one per day, for at-a-glance streaks. */
@Composable
fun DayDots(done: List<Boolean>, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val empty = MaterialTheme.colorScheme.outlineVariant
        done.forEach { isDone ->
            Box(
                Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDone) color else empty)
            )
        }
    }
}

/**
 * GitHub-style grid: one column per week, Monday on top. [cells] runs
 * oldest to newest and must start on a Monday; null cells are days outside
 * tracking (before start or in the future) and stay blank.
 */
@Composable
fun HeatGrid(cells: List<Float?>, color: Color, modifier: Modifier = Modifier) {
    val empty = MaterialTheme.colorScheme.outlineVariant
    val weeks = cells.chunked(7)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (0 until 7).forEach { i ->
                    val v = week.getOrNull(i)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    v == null -> Color.Transparent
                                    v <= 0f -> empty
                                    else -> color.copy(alpha = 0.2f + 0.8f * v.coerceIn(0f, 1f))
                                }
                            )
                    )
                }
            }
        }
    }
}
