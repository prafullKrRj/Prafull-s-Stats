package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.clickable
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
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.GoalStep
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.dueLabel
import app.prafullkumar.stats.data.formatNumber
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
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.WhyLine
import app.prafullkumar.stats.ui.theme.StreakColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.GoalColor
import kotlinx.datetime.LocalDate

@Composable
fun GoalsScreen(today: LocalDate, contentPadding: PaddingValues) {
    var picking by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(GoalStatus.ACTIVE) }

    val shown = StatsRepo.goals.filter { it.status == filter }
        .sortedWith(compareByDescending<Goal> { it.pinned }.thenBy { it.targetDate ?: "9999" })
    val active = StatsRepo.goals.filter { it.status == GoalStatus.ACTIVE }
    val avg = if (active.isEmpty()) 0f else active.map { it.progress }.average().toFloat()

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
                    Hint("${plural(stepsLeft, "step")} left across your active goals. Pick one and put it on today.")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GoalStatus.entries.forEach { s ->
                    ChoiceChip("${s.title} ${StatsRepo.goals.count { it.status == s }}", filter == s) { filter = s }
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                SectionCard {
                    Hint(
                        if (filter == GoalStatus.ACTIVE)
                            "No active goals. Start with the one that would change the most if it were done — e.g. shipping your next app on Play Store."
                        else "Nothing here."
                    )
                    if (filter == GoalStatus.ACTIVE) {
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
    SectionCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag("${goal.area.emoji} ${goal.area.title}", GoalColor)
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
        val next = goal.nextStep()
        val detail = buildList {
            if (goal.hasMetric) add("${formatNumber(goal.metricCurrent)} / ${formatNumber(goal.metricTarget)} ${goal.metricName}")
            if (goal.steps.isNotEmpty()) add("${goal.steps.count { it.done }}/${goal.steps.size} steps")
        }.joinToString(" · ")
        if (detail.isNotBlank()) Hint(detail)
        if (next != null && goal.status == GoalStatus.ACTIVE) {
            Text("Next → ${next.title}", style = MaterialTheme.typography.labelLarge, color = scheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun GoalDetailScreen(goalId: String, today: LocalDate, contentPadding: PaddingValues) {
    val goal = StatsRepo.goal(goalId) ?: return
    var editing by remember { mutableStateOf(false) }
    var addingStep by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<GoalStep?>(null) }
    var addingTask by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val linked = StatsRepo.allTasks().filter { it.second.goalId == goalId }
    val focus = StatsRepo.focusOnGoal(goalId)

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
                        parseDate(goal.targetDate)?.let { Hint("Deadline ${it} · ${dueLabel(it, today)}") }
                        Hint("${minutesLabel(focus)} focused · ${linked.count { it.second.done }} tasks done")
                        Hint("Status: ${goal.status.title}")
                    }
                }
            }
        }

        if (goal.hasMetric) {
            item {
                SectionCard {
                    SectionHeader(goal.metricName, "${formatNumber(goal.metricStart)} → ${formatNumber(goal.metricTarget)}")
                    Spacer(Modifier.height(10.dp))
                    var value by remember(goal.metricCurrent) { mutableStateOf(formatNumber(goal.metricCurrent)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberInput(value, { value = it }, "Current", Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        PrimaryButton("Update", enabled = value.toDoubleOrNull() != null) {
                            value.toDoubleOrNull()?.let { StatsRepo.setMetric(goal.id, it) }
                        }
                    }
                }
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
                Row {
                    TextButton(onClick = { StatsRepo.upsertGoal(goal.copy(pinned = !goal.pinned)) }) {
                        Text(if (goal.pinned) "Unpin from dashboard" else "Pin to dashboard")
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
