package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.LocalPlatform
import app.prafullkumar.stats.data.Phase
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.clock
import app.prafullkumar.stats.data.daysBetween
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.pad2
import app.prafullkumar.stats.data.weekStart
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.NeonButton
import app.prafullkumar.stats.ui.components.ScreenTitle
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.StatTile
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.NeonBrush
import app.prafullkumar.stats.ui.theme.StreakColor
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate

@Composable
fun FocusScreen(today: LocalDate, contentPadding: PaddingValues) {
    val timer = StatsRepo.timer
    val log = StatsRepo.logFor(today)
    val targets = StatsRepo.targets
    val platform = LocalPlatform.current
    val weekFocus = daysBetween(maxOf(weekStart(today), StatsRepo.startDate), today).sumOf { StatsRepo.logFor(it).totalFocus }

    ScreenColumn(contentPadding) {
        item {
            ScreenTitle("Focus", "Pomodoro · ${log.completedPomodoros} done today") {
                if (platform.isDesktop) TextButton(onClick = { platform.openMiniTimer() }) { Text("Mini timer ↗") }
            }
        }

        item { TimerCard() }

        item {
            SectionCard {
                SectionHeader("Working on", if (timer.taskId == null) "nothing linked" else null)
                Spacer(Modifier.height(8.dp))
                val open = sortTasks(StatsRepo.tasksFor(today)).filter { !it.done }
                if (open.isEmpty()) Hint("No open tasks today — add some on the Tasks screen, or just focus.")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (open.isNotEmpty()) ChoiceChip("Nothing", timer.taskId == null) { StatsRepo.linkTimerTask(null) }
                    open.forEach { t ->
                        ChoiceChip("${if (t.top) "★ " else ""}${t.title.take(28)}", timer.taskId == t.id) { StatsRepo.linkTimerTask(t.id) }
                    }
                }
                timer.taskId?.let { StatsRepo.findTask(it) }?.let { t ->
                    if (t.why.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("why: ${t.why}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Hint("🍅 ${t.pomodoros} of ${t.estimate} estimated")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Today", "${minutesLabel(log.totalFocus)} / ${minutesLabel(targets.focusMinutes)}")
                Spacer(Modifier.height(8.dp))
                ThinBar(
                    if (targets.focusMinutes <= 0) 0f else log.totalFocus.toFloat() / targets.focusMinutes,
                    FocusColor,
                    brush = NeonBrush
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${log.completedPomodoros}", "pomodoros", StreakColor, Modifier.weight(1f))
                    StatTile(minutesLabel(weekFocus), "this week", FocusColor, Modifier.weight(1f))
                    StatTile("${StatsRepo.focusStreak(today).current}", "day focus streak", CalorieColor, Modifier.weight(1f))
                }
                if (log.sessions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    log.sessions.sortedByDescending { it.start }.forEach { s ->
                        val t = Instant.fromEpochMilliseconds(s.start).toLocalDateTime(TimeZone.currentSystemDefault())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${pad2(t.hour)}:${pad2(t.minute)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
                            Text(
                                (if (s.complete) "🍅 " else "◐ ") + s.label.ifBlank { "Focus" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(minutesLabel(s.minutes), style = MaterialTheme.typography.labelLarge, color = FocusColor)
                        }
                    }
                }
            }
        }

        item {
            Hint(
                "The timer keeps running when you leave this screen, close the app or switch devices — " +
                    "it's synced. Stopping early still logs the minutes you put in.",
                Modifier
            )
        }
    }
}

/** The big ring with the countdown and controls. Also used on the dashboard. */
@Composable
fun TimerCard(compact: Boolean = false) {
    val timer = StatsRepo.timer
    val now = StatsRepo.now
    val left = timer.secondsLeft(now)
    val total = (timer.length * 60L).coerceAtLeast(1)
    val fraction = if (timer.idle) 0f else 1f - left.toFloat() / total
    val scheme = MaterialTheme.colorScheme
    val color = if (timer.phase == Phase.WORK) FocusColor else CalorieColor
    val ringSize = if (compact) 120.dp else 220.dp

    SectionCard(glow = !timer.idle) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Phase.entries.forEach { p ->
                    ChoiceChip(p.title, timer.phase == p) {
                        if (timer.idle) StatsRepo.setPhase(p)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                val track = scheme.outlineVariant
                Canvas(Modifier.size(ringSize)) {
                    val stroke = size.minDimension * 0.06f
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (fraction > 0f) drawArc(NeonBrush, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        clock(left),
                        style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        when {
                            timer.paused -> "paused"
                            timer.running -> timer.phase.title.lowercase()
                            else -> "ready"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = color
                    )
                }
            }
            if (timer.label.isNotBlank() && !compact) {
                Spacer(Modifier.height(10.dp))
                Text(timer.label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            CycleDots(timer.cycle, StatsRepo.pomodoro.longEvery)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    timer.running -> {
                        AddButton("Pause", icon = null) { StatsRepo.pauseTimer() }
                        AddButton("Stop", icon = null) { StatsRepo.stopTimer() }
                        AddButton("Skip", icon = null) { StatsRepo.skipPhase() }
                    }
                    timer.paused -> {
                        NeonButton("Resume") { StatsRepo.resumeTimer() }
                        AddButton("Stop", icon = null) { StatsRepo.stopTimer() }
                    }
                    else -> {
                        NeonButton("▶  Start ${timer.length} min") { StatsRepo.startTimer() }
                        if (timer.phase != Phase.WORK) AddButton("Skip break", icon = null) { StatsRepo.skipPhase() }
                    }
                }
            }
            if (!compact && timer.running) {
                Spacer(Modifier.height(8.dp))
                val end = timer.endsAt
                val endsAt = Instant.fromEpochMilliseconds(end).toLocalDateTime(TimeZone.currentSystemDefault())
                Hint("Ends at ${pad2(endsAt.hour)}:${pad2(endsAt.minute)} · phone down, one thing only.")
            }
        }
    }
}

@Composable
private fun CycleDots(cycle: Int, every: Int) {
    val n = every.coerceIn(1, 8)
    val done = cycle % n
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(n) { i ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i < done) StreakColor else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
