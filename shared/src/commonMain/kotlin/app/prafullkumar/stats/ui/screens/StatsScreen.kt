package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.dayMonth
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.letter
import app.prafullkumar.stats.data.minusDays
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.plusDays
import app.prafullkumar.stats.data.weekStart
import app.prafullkumar.stats.ui.components.BarChart
import app.prafullkumar.stats.ui.components.HeatGrid
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.ScreenTitle
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.StatTile
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.WeightChart
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FiberColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.GoalColor
import app.prafullkumar.stats.ui.theme.NeonBrush
import app.prafullkumar.stats.ui.theme.ProteinColor
import app.prafullkumar.stats.ui.theme.StreakColor
import kotlinx.datetime.LocalDate
import kotlin.math.abs

private const val WINDOW = 30
private const val HEAT_WEEKS = 16

@Composable
fun StatsScreen(today: LocalDate, contentPadding: PaddingValues) {
    val targets = StatsRepo.targets
    // Only days since tracking started — empty days before it never drag averages down.
    val window = StatsRepo.trackedDays(today, WINDOW)
    val lastWeek = (6 downTo 0).map { today.minusDays(it) }

    val beast = StatsRepo.beastStreak(today)
    val protein = StatsRepo.proteinStreak(today)
    val focusStreak = StatsRepo.focusStreak(today)
    val logging = StatsRepo.loggingStreak(today)

    val weights = StatsRepo.weightHistory()
    val first = weights.firstOrNull()
    val latest = weights.lastOrNull()
    val change = if (first != null && latest != null) latest.second - first.second else 0.0

    val totals = window.map { StatsRepo.totalsFor(it) }
    val fedDays = totals.count { it.kcal > 0 }
    val avgKcal = if (fedDays == 0) 0.0 else totals.sumOf { it.kcal }.toDouble() / fedDays
    val avgProtein = if (fedDays == 0) 0.0 else totals.sumOf { it.protein } / fedDays
    val avgFiber = if (fedDays == 0) 0.0 else totals.sumOf { it.fiber } / fedDays
    val logs = window.map { StatsRepo.logFor(it) }
    val sleeps = logs.mapNotNull { it.sleep }
    val moods = logs.map { it.mood }.filter { it > 0 }
    val focusTotal = logs.sumOf { it.totalFocus }
    val pomos = logs.sumOf { it.completedPomodoros }
    val tasksDone = logs.sumOf { l -> l.tasks.count { it.done } }
    val avgScore = window.map { StatsRepo.dayScore(it).score }.average()

    ScreenColumn(contentPadding) {
        item {
            ScreenTitle("Stats", "Day ${StatsRepo.dayNumber(today)} · last ${window.size} days")
        }

        item {
            SectionCard(glow = true) {
                SectionHeader("Streaks", "today counts once it's done")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("🔥${beast.current}", "beast days · best ${beast.best}", StreakColor, Modifier.weight(1f))
                    StatTile("${focusStreak.current}", "focus goal · best ${focusStreak.best}", FocusColor, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${protein.current}", "protein goal · best ${protein.best}", ProteinColor, Modifier.weight(1f))
                    StatTile("${logging.current}", "days showed up · best ${logging.best}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Day score", "last $HEAT_WEEKS weeks · avg ${avgScore.toInt()}")
                Spacer(Modifier.height(12.dp))
                HeatGrid(cells = heatCells(today), color = StreakColor)
                Spacer(Modifier.height(8.dp))
                Hint("Brighter = higher score. Each column is a week, Monday on top.")
            }
        }

        item {
            SectionCard {
                SectionHeader("Work", "last ${window.size} days")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(minutesLabel(focusTotal), "deep focus", FocusColor, Modifier.weight(1f))
                    StatTile("$pomos", "pomodoros", StreakColor, Modifier.weight(1f))
                    StatTile("$tasksDone", "tasks done", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
                val byGoal = StatsRepo.goals.map { it to StatsRepo.focusOnGoal(it.id, window) }
                    .filter { it.second > 0 }.sortedByDescending { it.second }
                if (byGoal.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("Focus by goal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    val max = byGoal.first().second.toFloat()
                    byGoal.forEach { (g, m) ->
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${g.area.emoji} ${g.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(minutesLabel(m), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        ThinBar(m / max, GoalColor, height = 8.dp, brush = NeonBrush)
                    }
                }
            }
        }

        item {
            WeekBars("Focus this week", targets.focusMinutes.toDouble(), " min", FocusColor, lastWeek) {
                StatsRepo.logFor(it).totalFocus.toDouble()
            }
        }
        item {
            WeekBars("Tasks done this week", 0.0, "", MaterialTheme.colorScheme.primary, lastWeek) { d ->
                StatsRepo.logFor(d).tasks.count { it.done }.toDouble()
            }
        }

        item {
            SectionCard {
                SectionHeader("Weight", "goal ${formatNumber(targets.goalWeight)} kg")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        latest?.let { "${formatNumber(it.second)} kg" } ?: "— kg",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (first != null && latest != null && first != latest) {
                        Text(
                            "  ${formatNumber(abs(change))} kg ${if (change <= 0) "down" else "up"} since ${first.first.dayMonth()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                WeightChart(points = weights.takeLast(60).map { it.second }, goal = targets.goalWeight, color = CalorieColor)
                if (latest != null) {
                    Spacer(Modifier.height(8.dp))
                    Hint("${formatNumber(abs(latest.second - targets.goalWeight))} kg to goal.")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Averages", "$fedDays day${if (fedDays == 1) "" else "s"} with food logged")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(if (fedDays == 0) "—" else "${avgKcal.toInt()}", "kcal / day", CalorieColor, Modifier.weight(1f))
                    StatTile(if (fedDays == 0) "—" else "${formatNumber(avgProtein)}g", "protein / day", ProteinColor, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(if (fedDays == 0) "—" else "${formatNumber(avgFiber)}g", "fiber / day", FiberColor, Modifier.weight(1f))
                    StatTile(if (sleeps.isEmpty()) "—" else "${formatNumber(sleeps.average())}h", "sleep / night", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    StatTile(if (moods.isEmpty()) "—" else formatNumber(moods.average()), "mood (1–5)", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }

        item {
            WeekBars("Protein this week", targets.protein.toDouble(), "g", ProteinColor, lastWeek) {
                StatsRepo.totalsFor(it).protein
            }
        }
        item {
            WeekBars("Calories this week", targets.kcal.toDouble(), " kcal", CalorieColor, lastWeek) {
                StatsRepo.totalsFor(it).kcal.toDouble()
            }
        }

        val habits = StatsRepo.activeHabits()
        if (habits.isNotEmpty()) {
            item {
                SectionCard {
                    SectionHeader("Habits", "last ${window.size} days")
                    Spacer(Modifier.height(12.dp))
                    habits.forEach { habit ->
                        val due = window.filter { habit in StatsRepo.scheduledHabits(it) }
                        val done = due.count { StatsRepo.habitDone(it, habit) }
                        val streak = StatsRepo.habitStreak(habit, today)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("🔥${streak.current} · best ${streak.best} · $done/${due.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        ThinBar(fraction = if (due.isEmpty()) 0f else done.toFloat() / due.size, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

/** 16 whole weeks ending this Sunday; null outside tracking so it is drawn faint. */
private fun heatCells(today: LocalDate): List<Float?> {
    val end = weekStart(today).plusDays(6)
    val start = end.minusDays(HEAT_WEEKS * 7 - 1)
    return (0 until HEAT_WEEKS * 7).map { i ->
        val day = start.plusDays(i)
        if (day > today || day < StatsRepo.startDate) null
        else StatsRepo.dayScore(day).score / 100f
    }
}

@Composable
fun WeekBars(
    title: String,
    target: Double,
    unit: String,
    color: Color,
    days: List<LocalDate>,
    value: (LocalDate) -> Double
) {
    val values = days.map { if (it < StatsRepo.startDate) 0.0 else value(it) }
    SectionCard {
        SectionHeader(title, if (target > 0) "target ${formatNumber(target)}$unit" else "total ${formatNumber(values.sum())}")
        Spacer(Modifier.height(12.dp))
        val scale = if (target > 0) target * 1.3 else (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        BarChart(
            bars = days.zip(values).map { (d, v) -> d.dayOfWeek.letter() to (v / scale).toFloat() },
            color = color,
            targetFraction = if (target > 0) (1f / 1.3f) else null
        )
    }
}
