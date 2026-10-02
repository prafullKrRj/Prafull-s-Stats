package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.fullLabel
import app.prafullkumar.stats.data.letter
import app.prafullkumar.stats.data.minusDays
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.nowLocal
import app.prafullkumar.stats.sync.CloudSync
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.components.BarChart
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.ProgressRing
import app.prafullkumar.stats.ui.components.RoundStep
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.Tag
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FiberColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.GoalColor
import app.prafullkumar.stats.ui.theme.ProteinColor
import app.prafullkumar.stats.ui.theme.StreakColor
import kotlinx.datetime.LocalDate

@Composable
fun DashboardScreen(today: LocalDate, contentPadding: PaddingValues, wide: Boolean) {
    ScreenColumn(contentPadding) {
        item { Greeting(today) }
        item { ScoreHero(today) }
        if (wide) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TodayTasks(today)
                        HabitsToday(today)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimerCard(compact = true)
                        GoalsGlance(today)
                        DietGlance(today)
                    }
                }
            }
        } else {
            item { TodayTasks(today) }
            item { TimerCard(compact = true) }
            item { HabitsToday(today) }
            item { GoalsGlance(today) }
            item { DietGlance(today) }
        }
        item { Capture(today) }
        item { WeekScores(today) }
    }
}

@Composable
private fun Greeting(today: LocalDate) {
    val hour = nowLocal().hour
    val hello = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Still up"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "$hello, ${StatsRepo.settings.name.ifBlank { "you" }}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Hint("${today.fullLabel()} · day ${StatsRepo.dayNumber(today)}")
        }
        Tag(
            if (CloudSync.config.signedIn) (if (CloudSync.error == null) "☁ synced" else "☁ error") else "☁ local",
            if (CloudSync.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.clickable { Nav.open(Dest.SETTINGS) }
        )
    }
}

@Composable
private fun ScoreHero(today: LocalDate) {
    val score = StatsRepo.dayScore(today)
    val beast = StatsRepo.beastStreak(today)
    val log = StatsRepo.logFor(today)
    val totals = StatsRepo.totalsFor(today)
    val due = StatsRepo.scheduledHabits(today)
    val target = StatsRepo.targets.beastScore
    SectionCard(highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(score.score.toDouble(), 100.0, StreakColor, "", "", size = 108.dp, showTarget = false)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("🔥 ${beast.current}", style = MaterialTheme.typography.displaySmall, color = StreakColor)
                Hint("beast streak · best ${beast.best}")
                Spacer(Modifier.height(4.dp))
                Text(
                    if (score.score >= target) "Beast day locked in. Keep going." else "${target - score.score} points to a beast day",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("${log.tasks.count { it.done }}/${log.tasks.size}", "tasks", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { Nav.open(Dest.TASKS) }
            Pill(minutesLabel(log.totalFocus), "focus", FocusColor, Modifier.weight(1f)) { Nav.open(Dest.FOCUS) }
            Pill("${due.count { StatsRepo.habitDone(today, it) }}/${due.size}", "habits", CalorieColor, Modifier.weight(1f)) { Nav.open(Dest.HABITS) }
            Pill("${formatNumber(totals.protein)}g", "protein", ProteinColor, Modifier.weight(1f)) { Nav.open(Dest.DIET) }
        }
    }
}

@Composable
private fun Pill(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayTasks(today: LocalDate) {
    val tasks = sortTasks(StatsRepo.tasksFor(today))
    val shown: List<Task> = (tasks.filter { it.top } + tasks.filter { !it.top && !it.done }).distinct().take(7)
    SectionCard {
        SectionHeader("Today's plan", "${tasks.count { it.done }} / ${tasks.size}")
        Spacer(Modifier.height(4.dp))
        if (tasks.isEmpty()) Hint("Nothing planned. What are the 3 things that would make today a win?")
        shown.forEach { TaskRow(it, onEdit = { Nav.open(Dest.TASKS) }) }
        Spacer(Modifier.height(6.dp))
        QuickAdd("Quick add for today") { StatsRepo.addTask(today, Task(id = StatsRepo.newId("task"), title = it)) }
        val open = StatsRepo.openPastTasks(today)
        Row {
            if (open > 0) TextButton(onClick = { StatsRepo.rollOver(today) }) { Text("Roll over $open") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { Nav.open(Dest.TASKS) }) { Text("All tasks →") }
        }
    }
}

@Composable
private fun HabitsToday(today: LocalDate) {
    val due = StatsRepo.scheduledHabits(today)
    SectionCard {
        SectionHeader("Habits", "${due.count { StatsRepo.habitDone(today, it) }} / ${due.size}")
        Spacer(Modifier.height(8.dp))
        if (due.isEmpty()) {
            Hint("No habits due. Build one on the Habits screen.")
        }
        due.forEach { h ->
            val count = StatsRepo.logFor(today).habitCount(h.id)
            val done = count >= h.target
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { StatsRepo.bumpHabit(today, h, if (h.isCounter) habitStep(h.target) else 1) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                app.prafullkumar.stats.ui.components.CheckDot(done, size = 22.dp, color = CalorieColor)
                Spacer(Modifier.width(10.dp))
                Text(h.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (h.isCounter) Hint("$count/${h.target}")
                val streak = StatsRepo.habitStreak(h, today).current
                if (streak > 0) Text("  🔥$streak", style = MaterialTheme.typography.labelLarge, color = StreakColor)
            }
        }
    }
}

/** Same stepping as the Habits screen, so a tap is meaningful on big counters. */
fun habitStep(target: Int): Int = when {
    target >= 5000 -> 1000
    target >= 500 -> 100
    target >= 50 -> 10
    target >= 20 -> 5
    else -> 1
}

@Composable
private fun GoalsGlance(today: LocalDate) {
    val active = StatsRepo.goals.filter { it.status == GoalStatus.ACTIVE }
        .sortedWith(compareByDescending<app.prafullkumar.stats.data.Goal> { it.pinned }.thenBy { it.targetDate ?: "9999" })
        .take(3)
    SectionCard {
        SectionHeader("Goals", "${StatsRepo.goals.count { it.status == GoalStatus.ACTIVE }} active")
        Spacer(Modifier.height(6.dp))
        if (active.isEmpty()) {
            Hint("No goals yet. Your next Play Store launch is a good first one.")
            TextButton(onClick = { Nav.open(Dest.GOALS) }) { Text("Set a goal →") }
        }
        active.forEach { g ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { Nav.openGoal(g.id) }
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${g.area.emoji} ${g.title}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${(g.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = GoalColor)
                }
                Spacer(Modifier.height(5.dp))
                ThinBar(g.progress, GoalColor, height = 7.dp)
                g.nextStep()?.let { step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next: ${step.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (!StatsRepo.isStepPlanned(step.id)) SmallAction("→ Today") { StatsRepo.planStep(g.id, step.id, today) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DietGlance(today: LocalDate) {
    val totals = StatsRepo.totalsFor(today)
    val t = StatsRepo.targets
    val log = StatsRepo.logFor(today)
    SectionCard(Modifier.clickable { Nav.open(Dest.DIET) }) {
        SectionHeader("Diet", "${totals.kcal} / ${t.kcal} kcal")
        Spacer(Modifier.height(10.dp))
        MacroLine("Protein", totals.protein, t.protein.toDouble(), ProteinColor)
        MacroLine("Fiber", totals.fiber, t.fiber.toDouble(), FiberColor)
        MacroLine("Calories", totals.kcal.toDouble(), t.kcal.toDouble(), CalorieColor)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💧 ${log.water} / ${t.water} glasses", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            RoundStep("–", 32.dp) { StatsRepo.setWater(today, log.water - 1) }
            Spacer(Modifier.width(8.dp))
            RoundStep("+", 32.dp) { StatsRepo.setWater(today, log.water + 1) }
        }
    }
}

@Composable
private fun MacroLine(label: String, value: Double, target: Double, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        ThinBar(if (target <= 0) 0f else (value / target).toFloat(), color, height = 8.dp, modifier = Modifier.weight(1f))
        Text(" ${formatNumber(value)}", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun Capture(today: LocalDate) {
    SectionCard {
        SectionHeader("Brain dump", "${StatsRepo.inbox.count { !it.done }} in inbox")
        Spacer(Modifier.height(6.dp))
        QuickAdd("Anything on your mind? Dump it.") { StatsRepo.dump(it, today) }
        if (StatsRepo.inbox.any { !it.done }) {
            TextButton(onClick = { Nav.open(Dest.MIND) }) { Text("Sort the inbox →") }
        }
    }
}

@Composable
private fun WeekScores(today: LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it) }
    SectionCard {
        SectionHeader("Last 7 days", "day score")
        Spacer(Modifier.height(10.dp))
        BarChart(
            bars = days.map { d ->
                d.dayOfWeek.letter() to if (d < StatsRepo.startDate) 0f else StatsRepo.dayScore(d).score / 100f
            },
            color = MaterialTheme.colorScheme.primary,
            targetFraction = StatsRepo.targets.beastScore / 100f
        )
    }
}
