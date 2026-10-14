package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.dueLabel
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.goalPriorityName
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.parseDate
import app.prafullkumar.stats.data.plural
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
    var sort by remember { mutableStateOf(GoalSort.DEADLINE) }
    var query by remember { mutableStateOf("") }

    val active = StatsRepo.goals.filter { it.status == GoalStatus.ACTIVE }
    val avg = if (active.isEmpty()) 0f else active.map { it.progress }.average().toFloat()
    val shown = StatsRepo.goals.filter { it.status == filter }
        .filter { area == null || it.area == area }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.why.contains(query, ignoreCase = true) }
        .sortedWith(
            compareByDescending<Goal> { it.pinned }.then(
                when (sort) {
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
                SectionCard(highlight = true) {
                    SectionHeader("Overall progress", "${(avg * 100).toInt()}%")
                    Spacer(Modifier.height(10.dp))
                    ThinBar(avg, GoalColor, height = 12.dp)
                    Spacer(Modifier.height(8.dp))
                    val stepsLeft = active.sumOf { g -> g.steps.count { !it.done } }
                    val milestonesLeft = active.sumOf { g -> g.milestones.count { !it.done } }
                    val struggling = active.count {
                        val h = it.health(today)
                        h == GoalHealth.BEHIND || h == GoalHealth.OVERDUE || h == GoalHealth.NEEDS_PUSH
                    }
                    Hint(
                        "${plural(stepsLeft, "step")} and ${plural(milestonesLeft, "milestone")} left across your active goals." +
                            if (struggling > 0) " ${plural(struggling, "goal")} need${if (struggling == 1) "s" else ""} a push." else " Pick one and put it on today."
                    )
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
            item(key = goal.id) { GoalCard(goal, today) { Nav.openGoal(goal.id) } }
        }
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
fun GoalCard(goal: Goal, today: LocalDate, compact: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val health = goal.health(today)
    SectionCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag("${goal.area.emoji} ${goal.area.title}", GoalColor)
                    if (goal.status == GoalStatus.ACTIVE) Tag("${health.emoji} ${health.title}", healthColor(health))
                    if (goal.priority > 0) Tag(
                        goalPriorityName(goal.priority),
                        if (goal.priority == 3) StreakColor else scheme.secondary
                    )
                    parseDate(goal.targetDate)?.let { d ->
                        val late = d < today && goal.status == GoalStatus.ACTIVE
                        Tag(dueLabel(d, today), if (late) StreakColor else scheme.onSurfaceVariant)
                    }
                    if (goal.pinned) Tag("📌", scheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(goal.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!compact && goal.why.isNotBlank()) {
                    Text(goal.why, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        Spacer(Modifier.height(8.dp))
        val detail = buildList {
            if (goal.hasMetric) add("${formatNumber(goal.metricCurrent)} / ${formatNumber(goal.metricTarget)} ${goal.metricName}")
            if (goal.steps.isNotEmpty()) add("${goal.steps.count { it.done }}/${goal.steps.size} steps")
            if (goal.milestones.isNotEmpty()) add("🏁 ${goal.milestones.count { it.done }}/${goal.milestones.size}")
        }.joinToString(" · ")
        if (detail.isNotBlank()) Hint(detail)
        if (goal.status == GoalStatus.ACTIVE) {
            val nextMilestone = goal.nextMilestone()
            val nextStep = goal.nextStep()
            val next = when {
                nextMilestone != null -> "🏁 ${nextMilestone.title}"
                nextStep != null -> "Next → ${nextStep.title}"
                else -> null
            }
            next?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = scheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                Text(goal.title, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
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
                        if (goal.hasMetric && goal.steps.isNotEmpty()) {
                            Hint("Steps ${(goal.stepProgress * 100).toInt()}% · ${goal.metricName} ${(goal.metricProgress * 100).toInt()}%")
                        }
                        if (goal.milestones.isNotEmpty()) {
                            Hint("🏁 ${goal.milestones.count { it.done }}/${goal.milestones.size} milestones")
                        }
                        val expected = goal.expectedProgress(today)
                        if (goal.status == GoalStatus.ACTIVE && expected != null) {
                            Hint("${health.emoji} ${health.title} — should be ~${(expected * 100).toInt()}% by now")
                        } else if (goal.status == GoalStatus.ACTIVE) {
                            Hint("${health.emoji} ${health.title}")
                        }
                        parseDate(goal.targetDate)?.let { Hint("Deadline ${it} · ${dueLabel(it, today)}") }
                        Hint("${minutesLabel(focus)} focused · ${linked.count { it.second.done }} tasks done")
                        Hint("Status: ${goal.status.title}" + if (goal.priority > 0) " · ${goalPriorityName(goal.priority)} priority" else "")
                    }
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
                if (linked.isEmpty()) Hint("Tasks you link to this goal (or plan from a step) show up here.")
                sortTasks(linked.map { it.second }).take(12).forEach { t ->
                    TaskRow(t, onEdit = {}, showWhy = false)
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Task for today", Modifier.fillMaxWidth()) { addingTask = true }
            }
        }

        val journey = goal.timeline()
        if (journey.size > 1) {
            item {
                SectionCard {
                    SectionHeader("Journey", plural(journey.size, "moment"))
                    Spacer(Modifier.height(8.dp))
                    journey.take(15).forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(e.emoji, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
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

@Composable
private fun MilestoneRow(goal: Goal, milestone: GoalMilestone, first: Boolean, last: Boolean, today: LocalDate, onEdit: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(CircleShape).clickable { StatsRepo.toggleMilestone(goal.id, milestone.id) }) {
            CheckDot(milestone.done, size = 26.dp, color = GoalColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
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
