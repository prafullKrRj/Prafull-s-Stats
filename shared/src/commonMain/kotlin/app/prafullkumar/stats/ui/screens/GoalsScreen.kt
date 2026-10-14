package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.GoalArea
import app.prafullkumar.stats.data.GoalCheckin
import app.prafullkumar.stats.data.GoalHealth
import app.prafullkumar.stats.data.GoalMilestone
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.GoalStep
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.dayMonth
import app.prafullkumar.stats.data.daysBetween
import app.prafullkumar.stats.data.dueLabel
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.goalPriorityName
import app.prafullkumar.stats.data.minusDays
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.parseDate
import app.prafullkumar.stats.data.plural
import kotlin.math.abs
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.PrimaryButton
import app.prafullkumar.stats.ui.components.NumberInput
import app.prafullkumar.stats.ui.components.ProgressRing
import app.prafullkumar.stats.ui.components.ScreenTitle
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.StatTile
import app.prafullkumar.stats.ui.components.Tag
import app.prafullkumar.stats.ui.components.TextInput
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.WeightChart
import app.prafullkumar.stats.ui.components.WhyLine
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FatColor
import app.prafullkumar.stats.ui.theme.StreakColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.GoalColor
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate

private enum class GoalSort(val title: String) {
    ATTENTION("Needs attention"),
    DEADLINE("Deadline"),
    PRIORITY("Priority"),
    PROGRESS("Least progress")
}

@Composable
private fun healthColor(health: GoalHealth): Color = when (health) {
    GoalHealth.DONE -> GoalColor
    GoalHealth.ON_TRACK -> CalorieColor
    GoalHealth.NEEDS_PUSH -> FatColor
    GoalHealth.BEHIND -> StreakColor
    GoalHealth.OVERDUE -> MaterialTheme.colorScheme.error
    GoalHealth.OPEN -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun GoalsScreen(today: LocalDate, contentPadding: PaddingValues) {
    var picking by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(GoalStatus.ACTIVE) }
    var area by remember { mutableStateOf<GoalArea?>(null) }
    var sort by remember { mutableStateOf(GoalSort.ATTENTION) }
    var query by remember { mutableStateOf("") }
    var loggingGoal by remember { mutableStateOf<Goal?>(null) }

    val active = StatsRepo.goals.filter { it.status == GoalStatus.ACTIVE }
    val avg = if (active.isEmpty()) 0f else active.map { it.progress }.average().toFloat()
    val shown = StatsRepo.goals.filter { it.status == filter }
        .filter { area == null || it.area == area }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.why.contains(query, ignoreCase = true) }
        .sortedWith(
            compareByDescending<Goal> { it.pinned }.then(
                when (sort) {
                    GoalSort.ATTENTION -> compareBy<Goal> { it.health(today).attention }.thenBy { it.targetDate ?: "9999" }
                    GoalSort.DEADLINE -> compareBy { it.targetDate ?: "9999" }
                    GoalSort.PRIORITY -> compareByDescending { it.priority }
                    GoalSort.PROGRESS -> compareBy { it.progress }
                }
            )
        )

    ScreenColumn(contentPadding) {
        item {
            ScreenTitle("Goals", "${active.size} active · ${StatsRepo.goals.count { it.status == GoalStatus.DONE }} achieved") {
                PrimaryButton("+ Goal") { picking = true }
            }
        }

        if (active.isNotEmpty()) {
            item {
                val stepsLeft = active.sumOf { g -> g.steps.count { !it.done } }
                val milestonesLeft = active.sumOf { g -> g.milestones.count { !it.done } }
                val struggling = active.count {
                    val h = it.health(today)
                    h == GoalHealth.BEHIND || h == GoalHealth.OVERDUE || h == GoalHealth.NEEDS_PUSH
                }
                SectionCard(highlight = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing((avg * 100).toInt().toDouble(), 100.0, GoalColor, "", "%", size = 88.dp, showTarget = false)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "OVERALL PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(avg * 100).toInt()}% across ${plural(active.size, "goal")}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Hint(
                                if (struggling > 0) "${plural(struggling, "goal")} need${if (struggling == 1) "s" else ""} a push — sorted first below."
                                else "Everything is on pace. Pick one and put it on today."
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("$stepsLeft", "steps left", GoalColor, Modifier.weight(1f))
                        StatTile("$milestonesLeft", "milestones left", FocusColor, Modifier.weight(1f))
                        StatTile("$struggling", "need a push", StreakColor, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalStatus.entries.forEach { s ->
                        ChoiceChip("${s.title} ${StatsRepo.goals.count { it.status == s }}", filter == s) { filter = s }
                    }
                }
                if (StatsRepo.goals.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChoiceChip("All areas", area == null) { area = null }
                        GoalArea.entries.forEach { a ->
                            ChoiceChip("${a.emoji} ${a.title}", area == a) { area = if (area == a) null else a }
                        }
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sort", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoalSort.entries.forEach { s ->
                            ChoiceChip(s.title, sort == s) { sort = s }
                        }
                    }
                    if (StatsRepo.goals.size > 3) {
                        TextInput(query, { query = it }, "Search goals…")
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                SectionCard {
                    Hint(
                        when {
                            query.isNotBlank() -> "No goals match “$query”. Try another word, or start it as a fresh goal."
                            area != null -> "No ${filter.title.lowercase()} goals in ${area!!.title}. Loosen the filter, or start one."
                            filter == GoalStatus.ACTIVE ->
                                "No active goals. Start with the one that would change the most if it were done — e.g. shipping your next app on Play Store."
                            else -> "Nothing here."
                        }
                    )
                    if (filter == GoalStatus.ACTIVE && query.isBlank() && area == null) {
                        Spacer(Modifier.height(10.dp))
                        AddButton("Pick a goal template", Modifier.fillMaxWidth()) { picking = true }
                    }
                }
            }
        }

        shown.forEach { goal ->
            item(key = goal.id) {
                GoalCard(goal, today, onClick = { Nav.openGoal(goal.id) }, onLog = { loggingGoal = goal })
            }
        }
    }

    loggingGoal?.let { g ->
        CheckinDialog(
            metricName = g.metricName,
            current = g.metricCurrent,
            onDismiss = { loggingGoal = null },
            onSave = { value, note -> StatsRepo.logCheckin(g.id, value, note); loggingGoal = null }
        )
    }

    if (picking) {
        TemplatePicker(
            onDismiss = { picking = false },
            onBlank = { picking = false; creating = true },
            onPick = { t ->
                val g = t.build(today.toString(), StatsRepo::newId)
                StatsRepo.upsertGoal(g)
                picking = false
                Nav.openGoal(g.id)
            }
        )
    }
    if (creating) {
        GoalDialog(null, today, onDismiss = { creating = false }, onSave = {
            StatsRepo.upsertGoal(it); creating = false; Nav.openGoal(it.id)
        })
    }
}

@Composable
fun GoalCard(goal: Goal, today: LocalDate, compact: Boolean = false, onClick: () -> Unit, onLog: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    val health = goal.health(today)
    SectionCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(if (compact) 40.dp else 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoalColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(goal.displayIcon(), style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!compact && goal.why.isNotBlank()) {
                    Text(goal.why, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            ProgressRing(
                value = (goal.progress * 100).toInt().toDouble(),
                target = 100.0,
                color = GoalColor,
                label = "",
                unit = "%",
                size = if (compact) 52.dp else 64.dp,
                showTarget = false
            )
        }
        Spacer(Modifier.height(10.dp))
        ThinBar(goal.progress, GoalColor, height = 8.dp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tag(goal.area.title, GoalColor)
            if (goal.status == GoalStatus.ACTIVE) Tag("${health.emoji} ${health.title}", healthColor(health))
            if (goal.priority > 0) Tag(
                goalPriorityName(goal.priority),
                if (goal.priority == 3) StreakColor else scheme.secondary
            )
            parseDate(goal.targetDate)?.let { d ->
                val late = d < today && goal.status == GoalStatus.ACTIVE
                Tag(dueLabel(d, today), if (late) StreakColor else scheme.onSurfaceVariant)
            }
            if (goal.pinned) Tag("📌 Pinned", scheme.onSurfaceVariant)
        }
        val detail = buildList {
            if (goal.hasMetric) add("${formatNumber(goal.metricCurrent)} / ${formatNumber(goal.metricTarget)} ${goal.metricName}")
            if (goal.steps.isNotEmpty()) add("${goal.steps.count { it.done }}/${goal.steps.size} steps")
        }.joinToString(" · ")
        if (detail.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Hint(detail)
        }
        if (goal.milestones.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MilestoneDots(goal.milestones.count { it.done }, goal.milestones.size)
                Spacer(Modifier.width(8.dp))
                Text(
                    "🏁 ${goal.milestones.count { it.done }}/${goal.milestones.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        if (goal.status == GoalStatus.ACTIVE) {
            val nextMilestone = goal.nextMilestone()
            val nextStep = goal.nextStep()
            val next = when {
                nextMilestone != null -> "🏁 ${nextMilestone.title}"
                nextStep != null -> "Next → ${nextStep.title}"
                else -> null
            }
            if (next != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        next,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SmallAction("＋ Log", onLog)
                }
            }
        }
    }
}

/** Labeled mini-bar splitting overall progress into its parts. */
@Composable
private fun ProgressSplit(label: String, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        ThinBar(fraction, GoalColor, Modifier.weight(1f), height = 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One dot per milestone, filled in proportion to how many are reached. */
@Composable
private fun MilestoneDots(done: Int, total: Int) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        val show = total.coerceAtMost(10)
        val filled = if (total == 0) 0 else (done.toFloat() / total * show).toInt().coerceIn(0, show)
        repeat(show) { i ->
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) GoalColor else track)
            )
        }
        if (total > show) {
            Text("+${total - show}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GoalDetailScreen(goalId: String, today: LocalDate, contentPadding: PaddingValues) {
    val goal = StatsRepo.goal(goalId) ?: return
    var editing by remember { mutableStateOf(false) }
    var addingStep by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<GoalStep?>(null) }
    var addingMilestone by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<GoalMilestone?>(null) }
    var logging by remember { mutableStateOf(false) }
    var deletingCheckin by remember { mutableStateOf<GoalCheckin?>(null) }
    var addingTask by remember { mutableStateOf(false) }
    var linkingHabits by remember { mutableStateOf(false) }
    var addingHabit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val linked = StatsRepo.allTasks().filter { it.second.goalId == goalId }
    val focus = StatsRepo.focusOnGoal(goalId)
    val health = goal.health(today)

    ScreenColumn(contentPadding) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Nav.back() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                Text("${goal.area.emoji} ${goal.area.title}", style = MaterialTheme.typography.labelLarge, color = GoalColor, modifier = Modifier.weight(1f))
                IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit goal") }
            }
        }

        item {
            SectionCard(highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GoalColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(goal.displayIcon(), style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tag(goal.area.title, GoalColor)
                    Tag("${health.emoji} ${health.title}", healthColor(health))
                    if (goal.priority > 0) Tag(goalPriorityName(goal.priority), if (goal.priority == 3) StreakColor else scheme.secondary)
                    parseDate(goal.targetDate)?.let { d ->
                        Tag(dueLabel(d, today), if (d < today && goal.status == GoalStatus.ACTIVE) StreakColor else scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (goal.why.isBlank()) {
                    Text(
                        "+ Add your why — the reason you'll come back to on hard days",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier.clickable { editing = true }
                    )
                } else WhyLine(goal.why)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing((goal.progress * 100).toInt().toDouble(), 100.0, GoalColor, "", "%", size = 88.dp, showTarget = false)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${(goal.progress * 100).toInt()}% there",
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        val expected = goal.expectedProgress(today)
                        if (goal.status == GoalStatus.ACTIVE && expected != null) {
                            Hint("Should be ~${(expected * 100).toInt()}% by now")
                        }
                        parseDate(goal.targetDate)?.let { Hint("Deadline ${it} · ${dueLabel(it, today)}") }
                        val streak = goal.activityStreak(today)
                        if (streak >= 2) Hint("🔥 $streak-day streak")
                        else if (goal.activeDates().isNotEmpty()) Hint("${plural(goal.activeDates().size, "active day")}")
                        goal.requiredDailyPace(today)?.let { pace ->
                            if (pace == 0.0) Hint("✅ Number already hit")
                            else if (pace < 0) Hint("Need ${formatNumber(abs(pace))} less ${goal.metricName}/day")
                            else Hint("Need ${formatNumber(pace)} ${goal.metricName}/day")
                        }
                        goal.projectedFinish(today)?.let { proj ->
                            val deadline = parseDate(goal.targetDate)
                            val mark = if (deadline != null && proj <= deadline) "✅" else "⚠️"
                            Hint("$mark At this pace → ~${proj.dayMonth()}")
                        }
                        Hint("${minutesLabel(focus)} focused · ${linked.count { it.second.done }} tasks done")
                    }
                }
                if (goal.hasMetric && goal.steps.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ProgressSplit("🗂 Steps", goal.stepProgress)
                    Spacer(Modifier.height(8.dp))
                    ProgressSplit("🔢 ${goal.metricName}", goal.metricProgress)
                } else if (goal.milestones.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ProgressSplit("🏁 Milestones", goal.milestoneProgress)
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    if (goal.hasMetric) goal.metricName else "Progress log",
                    if (goal.hasMetric) "${formatNumber(goal.metricStart)} → ${formatNumber(goal.metricTarget)}" else null
                )
                Spacer(Modifier.height(10.dp))
                if (goal.hasMetric) {
                    var value by remember(goal.metricCurrent) { mutableStateOf(formatNumber(goal.metricCurrent)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberInput(value, { value = it }, "Current", Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        PrimaryButton("Update", enabled = value.toDoubleOrNull() != null) {
                            value.toDoubleOrNull()?.let { StatsRepo.setMetric(goal.id, it) }
                        }
                    }
                    val history = goal.checkins.mapNotNull { it.value }
                    if (history.size >= 2) {
                        Spacer(Modifier.height(10.dp))
                        WeightChart(
                            points = history.takeLast(30),
                            goal = goal.metricTarget,
                            color = GoalColor,
                            emptyText = "Log two updates to see the graph."
                        )
                    }
                } else {
                    Hint("No number on this goal — log notes as you go, or add a number via Edit.")
                }
                val recent = goal.checkins.takeLast(3).reversed()
                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    recent.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "📝 ${c.date}",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                buildString {
                                    c.value?.let { append("${formatNumber(it)}${if (goal.metricName.isNotBlank()) " ${goal.metricName}" else ""}") }
                                    if (c.note.isNotBlank()) {
                                        if (isNotEmpty()) append(" — ")
                                        append(c.note)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { deletingCheckin = c }) {
                                Text("✕", color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Log update", Modifier.fillMaxWidth()) { logging = true }
            }
        }

        item {
            SectionCard {
                SectionHeader("Milestones", "${goal.milestones.count { it.done }} / ${goal.milestones.size}")
                Spacer(Modifier.height(4.dp))
                if (goal.milestones.isEmpty()) {
                    Hint("Big checkpoints worth celebrating — “First 100 installs”, “MVP built”. Some tick themselves when your number reaches them.")
                } else {
                    ThinBar(goal.milestoneProgress, GoalColor, height = 8.dp)
                    Spacer(Modifier.height(4.dp))
                }
                goal.milestones.forEachIndexed { i, milestone ->
                    MilestoneRow(
                        goal = goal,
                        milestone = milestone,
                        first = i == 0,
                        last = i == goal.milestones.lastIndex,
                        today = today,
                        onEdit = { editingMilestone = milestone }
                    )
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Add milestone", Modifier.fillMaxWidth()) { addingMilestone = true }
            }
        }

        item {
            SectionCard {
                SectionHeader("Steps", "${goal.steps.count { it.done }} / ${goal.steps.size}")
                Spacer(Modifier.height(4.dp))
                if (goal.steps.isEmpty()) Hint("Break it down: what are the concrete steps from here to done?")
                goal.steps.forEachIndexed { i, step ->
                    StepRow(
                        goal = goal,
                        step = step,
                        first = i == 0,
                        last = i == goal.steps.lastIndex,
                        today = today,
                        onEdit = { editingStep = step }
                    )
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Add step", Modifier.fillMaxWidth()) { addingStep = true }
            }
        }

        item {
            SectionCard {
                SectionHeader("Tasks for this goal", "${linked.count { !it.second.done }} open")
                Spacer(Modifier.height(4.dp))
                val week = daysBetween(today.minusDays(6), today)
                val focus7 = StatsRepo.focusOnGoal(goalId, week)
                val done7 = linked.count { (date, t) -> t.done && date != null && date >= week.first() }
                Hint("Last 7 days · ${minutesLabel(focus7)} focused · ${plural(done7, "task")} done")
                Spacer(Modifier.height(4.dp))
                if (linked.isEmpty()) Hint("Tasks you link to this goal (or plan from a step) show up here.")
                sortTasks(linked.map { it.second }).take(12).forEach { t ->
                    TaskRow(t, onEdit = {}, showWhy = false)
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Task for today", Modifier.fillMaxWidth()) { addingTask = true }
            }
        }

        val linkedHabits = StatsRepo.habits.filter { it.goalId == goalId && !it.archived }
        item {
            SectionCard {
                SectionHeader("Habits feeding this goal", "${linkedHabits.size}")
                Spacer(Modifier.height(4.dp))
                if (linkedHabits.isEmpty()) {
                    Hint("Link a habit and its daily reps push this goal forward.")
                }
                linkedHabits.forEach { h ->
                    val streak = StatsRepo.habitStreak(h, today)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { StatsRepo.bumpHabit(today, h) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckDot(StatsRepo.habitDone(today, h), size = 26.dp, color = GoalColor)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                            Hint(
                                buildString {
                                    if (h.isCounter) append("${h.target}${if (h.unit.isNotBlank()) " ${h.unit}" else ""}/day · ")
                                    append(if (streak.current > 0) "🔥 ${streak.current}-day streak" else "no streak yet")
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddButton("Link habit", Modifier.weight(1f)) { linkingHabits = true }
                    AddButton("New habit", Modifier.weight(1f)) { addingHabit = true }
                }
            }
        }

        val journey = goal.timeline()
        if (journey.size > 1) {
            item {
                SectionCard {
                    SectionHeader("Journey", plural(journey.size, "moment"))
                    Spacer(Modifier.height(8.dp))
                    val moments = journey.take(15)
                    moments.forEachIndexed { i, e ->
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            RailColumn(last = i == moments.lastIndex) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(scheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(e.emoji, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).padding(vertical = 3.dp)) {
                                Text(
                                    e.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(e.date, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (journey.size > 15) Hint("+ ${journey.size - 15} earlier moments")
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Status")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalStatus.entries.forEach { s ->
                        ChoiceChip(s.title, goal.status == s) { StatsRepo.setGoalStatus(goal.id, s) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionHeader("Priority")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { p ->
                        ChoiceChip(goalPriorityName(p), goal.priority == p) { StatsRepo.setGoalPriority(goal.id, p) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    TextButton(onClick = { StatsRepo.upsertGoal(goal.copy(pinned = !goal.pinned)) }) {
                        Text(if (goal.pinned) "Unpin" else "Pin")
                    }
                    TextButton(onClick = { StatsRepo.duplicateGoal(goal.id)?.let { Nav.openGoal(it) } }) {
                        Text("Duplicate")
                    }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = scheme.error) }
                }
                if (goal.progress >= 1f && goal.status == GoalStatus.ACTIVE) {
                    Spacer(Modifier.height(6.dp))
                    PrimaryButton("🏆 Mark goal achieved", Modifier.fillMaxWidth()) { StatsRepo.setGoalStatus(goal.id, GoalStatus.DONE) }
                }
                if (goal.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Hint(goal.notes)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(minutesLabel(focus), "focused on it", FocusColor, Modifier.weight(1f))
                StatTile("${goal.steps.count { it.done }}", "steps done", GoalColor, Modifier.weight(1f))
            }
        }
    }

    if (editing) {
        GoalDialog(goal, today, onDismiss = { editing = false }, onSave = { StatsRepo.upsertGoal(it); editing = false })
    }
    if (addingStep) {
        StepDialog(null, onDismiss = { addingStep = false }, onSave = { title, why, due ->
            StatsRepo.addStep(goal.id, title, why)
            if (due != null) StatsRepo.goal(goal.id)?.steps?.lastOrNull()?.let { StatsRepo.updateStep(goal.id, it.copy(due = due)) }
            addingStep = false
        })
    }
    editingStep?.let { step ->
        StepDialog(step, onDismiss = { editingStep = null }, onSave = { title, why, due ->
            StatsRepo.updateStep(goal.id, step.copy(title = title, why = why, due = due)); editingStep = null
        }, onDelete = { StatsRepo.removeStep(goal.id, step.id); editingStep = null })
    }
    if (addingMilestone) {
        MilestoneDialog(null, goal.metricName, onDismiss = { addingMilestone = false }, onSave = { title, why, due, threshold ->
            StatsRepo.addMilestone(goal.id, title, why, due, threshold); addingMilestone = false
        })
    }
    editingMilestone?.let { milestone ->
        MilestoneDialog(milestone, goal.metricName, onDismiss = { editingMilestone = null }, onSave = { title, why, due, threshold ->
            StatsRepo.updateMilestone(goal.id, milestone.copy(title = title, why = why, due = due, metricValue = threshold))
            editingMilestone = null
        }, onDelete = { StatsRepo.removeMilestone(goal.id, milestone.id); editingMilestone = null })
    }
    if (logging) {
        CheckinDialog(goal.metricName, goal.metricCurrent, onDismiss = { logging = false }, onSave = { value, note ->
            StatsRepo.logCheckin(goal.id, value, note); logging = false
        })
    }
    deletingCheckin?.let { c ->
        ConfirmDialog(
            title = "Remove this update?",
            body = "The logged value stays in the goal's number — only this history row goes.",
            confirm = "Remove",
            onDismiss = { deletingCheckin = null },
            onConfirm = { StatsRepo.removeCheckin(goal.id, c.id); deletingCheckin = null }
        )
    }
    if (addingTask) {
        TaskDialog(null, today, today, presetGoalId = goal.id, onDismiss = { addingTask = false }, onSave = { t, where ->
            StatsRepo.addTask(where, t.copy(why = t.why.ifBlank { goal.why })); addingTask = false
        })
    }
    if (linkingHabits) {
        LinkHabitDialog(goalId = goal.id, onDismiss = { linkingHabits = false })
    }
    if (addingHabit) {
        HabitDialog(null, today, onDismiss = { addingHabit = false }, onSave = {
            StatsRepo.upsertHabit(it); addingHabit = false
        }, presetGoalId = goal.id)
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${goal.title}?",
            body = "Steps and progress go with it. Linked tasks stay, unlinked. Mark it Dropped instead to keep the history.",
            confirm = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = { StatsRepo.deleteGoal(goal.id); confirmDelete = false; Nav.back() }
        )
    }
}

/** Left rail with a node on top and a connector below — the timeline look. */
@Composable
private fun RailColumn(last: Boolean, node: @Composable () -> Unit) {
    Column(
        Modifier
            .width(28.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        node()
        if (!last) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MilestoneRow(goal: Goal, milestone: GoalMilestone, first: Boolean, last: Boolean, today: LocalDate, onEdit: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 6.dp)
    ) {
        RailColumn(last = last) {
            Box(Modifier.clip(CircleShape).clickable { StatsRepo.toggleMilestone(goal.id, milestone.id) }) {
                CheckDot(milestone.done, size = 26.dp, color = GoalColor)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(vertical = 2.dp)) {
            Text(
                "🏁 ${milestone.title}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (milestone.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (milestone.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (milestone.why.isNotBlank() && !milestone.done) {
                Text("why: ${milestone.why}", style = MaterialTheme.typography.labelMedium, color = scheme.primary, maxLines = 2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                parseDate(milestone.due)?.let {
                    if (!milestone.done) Text(
                        dueLabel(it, today),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it < today) StreakColor else scheme.onSurfaceVariant
                    )
                }
                milestone.metricValue?.let { threshold ->
                    if (!milestone.done && goal.hasMetric) {
                        Tag("⚡ auto @ ${formatNumber(threshold)}", scheme.tertiary)
                    }
                }
            }
        }
        Column {
            Icon(
                Icons.Filled.KeyboardArrowUp, contentDescription = "Move up",
                tint = if (first) scheme.outlineVariant else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable(enabled = !first) { StatsRepo.moveMilestone(goal.id, milestone.id, true) }
            )
            Icon(
                Icons.Filled.KeyboardArrowDown, contentDescription = "Move down",
                tint = if (last) scheme.outlineVariant else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable(enabled = !last) { StatsRepo.moveMilestone(goal.id, milestone.id, false) }
            )
        }
    }
}

/** Picks which habits feed a goal. A habit feeds one goal — linking moves it. */
@Composable
private fun LinkHabitDialog(goalId: String, onDismiss: () -> Unit) {
    val candidates = StatsRepo.activeHabits()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Link habits") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (candidates.isEmpty()) {
                    Hint("No habits yet — create one from the button below, then link it here.")
                }
                candidates.forEach { h: Habit ->
                    val linked = h.goalId == goalId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { StatsRepo.upsertHabit(h.copy(goalId = if (linked) null else goalId)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckDot(linked, size = 26.dp, color = GoalColor)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            if (h.goalId != null && !linked) Hint("Linked elsewhere — tapping moves it here")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun StepRow(goal: Goal, step: GoalStep, first: Boolean, last: Boolean, today: LocalDate, onEdit: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val planned = StatsRepo.isStepPlanned(step.id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(CircleShape).clickable { StatsRepo.toggleStep(goal.id, step.id) }) {
            CheckDot(step.done, size = 26.dp, color = GoalColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                step.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (step.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (step.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (step.why.isNotBlank() && !step.done) {
                Text("why: ${step.why}", style = MaterialTheme.typography.labelMedium, color = scheme.primary, maxLines = 2)
            }
            parseDate(step.due)?.let { if (!step.done) Text(dueLabel(it, today), style = MaterialTheme.typography.labelMedium, color = if (it < today) StreakColor else scheme.onSurfaceVariant) }
        }
        if (!step.done) {
            if (planned) Tag("planned", scheme.tertiary)
            else SmallAction("→ Today") { StatsRepo.planStep(goal.id, step.id, today) }
        }
        Column {
            Icon(
                Icons.Filled.KeyboardArrowUp, contentDescription = "Move up",
                tint = if (first) scheme.outlineVariant else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable(enabled = !first) { StatsRepo.moveStep(goal.id, step.id, true) }
            )
            Icon(
                Icons.Filled.KeyboardArrowDown, contentDescription = "Move down",
                tint = if (last) scheme.outlineVariant else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable(enabled = !last) { StatsRepo.moveStep(goal.id, step.id, false) }
            )
        }
    }
}
