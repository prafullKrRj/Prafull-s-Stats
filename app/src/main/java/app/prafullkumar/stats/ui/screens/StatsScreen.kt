package app.prafullkumar.stats.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.components.BarChart
import app.prafullkumar.stats.ui.components.HeatGrid
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.StatTile
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.WeightChart
import app.prafullkumar.stats.ui.components.formatNumber
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FiberColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.ProteinColor
import app.prafullkumar.stats.ui.theme.StreakColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

private const val WINDOW = 30
private const val HEAT_WEEKS = 16

@Composable
fun StatsScreen(
    today: LocalDate,
    onOpenTargets: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirmImport by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        val ok = text != null && StatsRepo.importJson(text)
        Toast.makeText(
            context,
            if (ok) "Backup restored" else "Not a Prafull Stats backup",
            Toast.LENGTH_SHORT
        ).show()
    }

    val targets = StatsRepo.targets
    // Only days since tracking started — empty days before it never drag averages down.
    val window = StatsRepo.trackedDays(today, WINDOW)
    val lastWeek = (0 until 7).map { today.minusDays(it.toLong()) }.reversed()

    val beast = StatsRepo.beastStreak(today)
    val protein = StatsRepo.proteinStreak(today)
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
    val focusTotal = logs.sumOf { it.focusMinutes }
    val avgScore = window.map { StatsRepo.dayScore(it).score }.average()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Prafull Stats",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Hint("Day ${StatsRepo.dayNumber(today)} · ${windowTitle(window.size)}")
                }
                IconButton(onClick = onOpenTargets) {
                    Icon(Icons.Filled.Settings, contentDescription = "Targets")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Streaks", "today counts once it's done")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("🔥${beast.current}", "beast days · best ${beast.best}", StreakColor, Modifier.weight(1f))
                    StatTile("${protein.current}", "protein goal · best ${protein.best}", ProteinColor, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${logging.current}", "days showed up · best ${logging.best}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile(
                        if (window.isEmpty()) "—" else "${avgScore.toInt()}",
                        "avg day score",
                        MaterialTheme.colorScheme.secondary,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Day score", "last $HEAT_WEEKS weeks")
                Spacer(Modifier.height(12.dp))
                HeatGrid(cells = heatCells(today), color = StreakColor)
                Spacer(Modifier.height(8.dp))
                Hint("Darker = higher score. Each column is a week, Monday on top.")
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
                            "  ${formatNumber(abs(change))} kg ${if (change <= 0) "down" else "up"} since " +
                                first.first.format(DateTimeFormatter.ofPattern("d MMM")),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                WeightChart(
                    points = weights.takeLast(60).map { it.second },
                    goal = targets.goalWeight,
                    color = CalorieColor
                )
                if (latest != null) {
                    Spacer(Modifier.height(8.dp))
                    Hint("${formatNumber(abs(latest.second - targets.goalWeight))} kg to goal.")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Averages", "${fedDays} days with food logged")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(if (fedDays == 0) "—" else "${avgKcal.toInt()}", "kcal / day", CalorieColor, Modifier.weight(1f))
                    StatTile(if (fedDays == 0) "—" else "${formatNumber(avgProtein)}g", "protein / day", ProteinColor, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(if (fedDays == 0) "—" else "${formatNumber(avgFiber)}g", "fiber / day", FiberColor, Modifier.weight(1f))
                    StatTile(
                        if (sleeps.isEmpty()) "—" else "${formatNumber(sleeps.average())}h",
                        "sleep / night",
                        MaterialTheme.colorScheme.tertiary,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${focusTotal / 60}h ${focusTotal % 60}m", "deep focus total", FocusColor, Modifier.weight(1f))
                    StatTile(
                        if (moods.isEmpty()) "—" else formatNumber(moods.average()),
                        "mood (1–5)",
                        MaterialTheme.colorScheme.secondary,
                        Modifier.weight(1f)
                    )
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
        item {
            WeekBars("Fiber this week", targets.fiber.toDouble(), "g", FiberColor, lastWeek) {
                StatsRepo.totalsFor(it).fiber
            }
        }
        item {
            WeekBars("Focus this week", targets.focusMinutes.toDouble(), " min", FocusColor, lastWeek) {
                StatsRepo.logFor(it).focusMinutes.toDouble()
            }
        }

        val habits = StatsRepo.activeHabits()
        if (habits.isNotEmpty()) {
            item {
                SectionCard {
                    SectionHeader("Habits", windowTitle(window.size))
                    Spacer(Modifier.height(12.dp))
                    habits.forEach { habit ->
                        val due = window.filter { habit in StatsRepo.scheduledHabits(it) }
                        val done = due.count { StatsRepo.habitDone(it, habit) }
                        val streak = StatsRepo.habitStreak(habit, today)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "🔥${streak.current} · best ${streak.best} · $done/${due.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        ThinBar(
                            fraction = if (due.isEmpty()) 0f else done.toFloat() / due.size,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Backup")
                Spacer(Modifier.height(6.dp))
                Hint("Everything lives on this phone only. Export now and then — save it to Drive or send it to yourself.")
                Row {
                    TextButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Prafull Stats backup ${today}")
                            putExtra(Intent.EXTRA_TEXT, StatsRepo.exportJson())
                        }
                        context.startActivity(Intent.createChooser(send, "Export backup"))
                    }) { Text("Export") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { confirmImport = true }) { Text("Import") }
                }
            }
        }
    }

    if (confirmImport) {
        ConfirmDialog(
            title = "Replace all data?",
            body = "Importing a backup overwrites everything in the app with the backup's contents. This cannot be undone.",
            confirm = "Choose file",
            onDismiss = { confirmImport = false },
            onConfirm = {
                confirmImport = false
                importer.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        )
    }
}

/** 16 whole weeks ending this Sunday; null outside tracking so it stays blank. */
private fun heatCells(today: LocalDate): List<Float?> {
    val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val start = end.minusWeeks(HEAT_WEEKS.toLong()).plusDays(1)
    return (0 until HEAT_WEEKS * 7).map { i ->
        val day = start.plusDays(i.toLong())
        if (day.isAfter(today) || day.isBefore(StatsRepo.startDate)) null
        else StatsRepo.dayScore(day).score / 100f
    }
}

@Composable
private fun WeekBars(
    title: String,
    target: Double,
    unit: String,
    color: Color,
    days: List<LocalDate>,
    value: (LocalDate) -> Double
) {
    SectionCard {
        SectionHeader(title, "target ${formatNumber(target)}$unit")
        Spacer(Modifier.height(12.dp))
        val scale = if (target <= 0) 1.0 else target * 1.3
        BarChart(
            bars = days.map { day ->
                val v = if (day < StatsRepo.startDate) 0.0 else value(day)
                day.format(DateTimeFormatter.ofPattern("EEE")).take(1) to (v / scale).toFloat()
            },
            color = color,
            targetFraction = if (target <= 0) null else (1f / 1.3f)
        )
    }
}

private fun windowTitle(days: Int): String =
    if (days <= 1) "first day" else "last $days days"
