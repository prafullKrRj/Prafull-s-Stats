package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.GoalArea
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.GoalStep
import app.prafullkumar.stats.data.GoalTemplate
import app.prafullkumar.stats.data.GoalTemplates
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.parseDate
import app.prafullkumar.stats.data.plusDays
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.NumberInput
import app.prafullkumar.stats.ui.components.RoundStep
import app.prafullkumar.stats.ui.components.TextInput
import kotlinx.datetime.LocalDate

val priorityNames = listOf("None", "Low", "Medium", "High")

/** Valid "HH:mm" or null. Accepts "9", "930", "9:30". */
fun parseTime(raw: String): String? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    val (h, m) = when (digits.length) {
        1, 2 -> digits.toInt() to 0
        3 -> digits.take(1).toInt() to digits.drop(1).toInt()
        else -> digits.take(2).toInt() to digits.drop(2).take(2).toInt()
    }
    if (h !in 0..23 || m !in 0..59) return null
    return "${if (h < 10) "0$h" else "$h"}:${if (m < 10) "0$m" else "$m"}"
}

/**
 * Add or edit a task. For a new task [date] picks where it goes; null means
 * the backlog.
 */
@Composable
fun TaskDialog(
    initial: Task?,
    date: LocalDate?,
    today: LocalDate,
    presetGoalId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Task, LocalDate?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var why by remember { mutableStateOf(initial?.why.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var priority by remember { mutableStateOf(initial?.priority ?: 0) }
    var top by remember { mutableStateOf(initial?.top ?: false) }
    var time by remember { mutableStateOf(initial?.time.orEmpty()) }
    var estimate by remember { mutableStateOf(initial?.estimate ?: 1) }
    var goalId by remember { mutableStateOf(initial?.goalId ?: presetGoalId) }
    var target by remember { mutableStateOf(date) }
    val activeGoals = StatsRepo.goals.filter { it.status == GoalStatus.ACTIVE }
    val timeOk = time.isBlank() || parseTime(time) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextInput(title, { title = it }, "What needs doing?")
                Spacer(Modifier.height(10.dp))
                TextInput(why, { why = it }, "Why it matters")
                Spacer(Modifier.height(14.dp))
                if (initial == null) {
                    Label("When")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("Today", target == today) { target = today }
                        ChoiceChip("Tomorrow", target == today.plusDays(1)) { target = today.plusDays(1) }
                        if (date != null && date != today && date != today.plusDays(1)) {
                            ChoiceChip("Selected day", target == date) { target = date }
                        }
                        ChoiceChip("Someday", target == null) { target = null }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Label("Priority")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorityNames.forEachIndexed { i, n -> ChoiceChip(n, priority == i) { priority = i } }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextInput(time, { time = it.take(5) }, "Time (e.g. 9:30)", Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍅 $estimate", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Row {
                            RoundStep("–", 30.dp) { estimate = (estimate - 1).coerceAtLeast(1) }
                            Spacer(Modifier.width(6.dp))
                            RoundStep("+", 30.dp) { estimate = (estimate + 1).coerceAtMost(16) }
                        }
                    }
                }
                if (!timeOk) Text("Use a time like 9:30 or 14:00", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Top 3 for the day", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Hint("If only three things get done, this is one.")
                    }
                    Switch(checked = top, onCheckedChange = { top = it })
                }
                if (activeGoals.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Label("Moves which goal?")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("None", goalId == null) { goalId = null }
                        activeGoals.forEach { g -> ChoiceChip("${g.area.emoji} ${g.title}", goalId == g.id) { goalId = g.id } }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextInput(notes, { notes = it }, "Notes", singleLine = false, minLines = 2)
                if (onDelete != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onDelete) { Text("Delete task", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (initial ?: Task(id = StatsRepo.newId("task"), title = "")).copy(
                            title = title.trim(),
                            why = why.trim(),
                            notes = notes.trim(),
                            priority = priority,
                            top = top,
                            time = parseTime(time),
                            estimate = estimate,
                            goalId = goalId,
                            stepId = if (goalId == initial?.goalId) initial?.stepId else null
                        ),
                        target
                    )
                },
                enabled = title.isNotBlank() && timeOk
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Create or edit a goal's headline: title, why, area, number and deadline. */
@Composable
fun GoalDialog(
    initial: Goal?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var why by remember { mutableStateOf(initial?.why.orEmpty()) }
    var area by remember { mutableStateOf(initial?.area ?: GoalArea.APPS) }
    var metricName by remember { mutableStateOf(initial?.metricName.orEmpty()) }
    var metricStart by remember { mutableStateOf(initial?.metricStart?.takeIf { it != 0.0 }?.let(::formatNumber).orEmpty()) }
    var metricTarget by remember { mutableStateOf(initial?.metricTarget?.takeIf { it != 0.0 }?.let(::formatNumber).orEmpty()) }
    var deadline by remember { mutableStateOf(initial?.targetDate.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    val deadlineOk = deadline.isBlank() || parseDate(deadline) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(if (initial == null) "New goal" else "Edit goal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextInput(title, { title = it }, "Goal")
                Spacer(Modifier.height(10.dp))
                TextInput(why, { why = it }, "Why do you want this? Be honest.", singleLine = false, minLines = 2)
                Spacer(Modifier.height(14.dp))
                Label("Area")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalArea.entries.forEach { a -> ChoiceChip("${a.emoji} ${a.title}", area == a) { area = a } }
                }
                Spacer(Modifier.height(14.dp))
                Label("Measure it (optional)")
                TextInput(metricName, { metricName = it }, "What number? e.g. Installs, kg, ₹/month")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberInput(metricStart, { metricStart = it }, "From", Modifier.weight(1f))
                    NumberInput(metricTarget, { metricTarget = it }, "To", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                TextInput(deadline, { deadline = it.take(10) }, "Deadline (YYYY-MM-DD)")
                if (!deadlineOk) Text("Use YYYY-MM-DD, e.g. ${today.plusDays(90)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))
                TextInput(notes, { notes = it }, "Notes", singleLine = false, minLines = 2)
                Spacer(Modifier.height(6.dp))
                Hint("Without a number, progress comes from the steps you tick off.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = metricStart.toDoubleOrNull() ?: 0.0
                    val base = initial ?: Goal(id = StatsRepo.newId("goal"), title = "", createdAt = today.toString())
                    onSave(
                        base.copy(
                            title = title.trim(),
                            why = why.trim(),
                            area = area,
                            metricName = metricName.trim(),
                            metricStart = start,
                            metricTarget = metricTarget.toDoubleOrNull() ?: 0.0,
                            metricCurrent = if (initial == null) start else base.metricCurrent,
                            targetDate = parseDate(deadline)?.toString(),
                            notes = notes.trim()
                        )
                    )
                },
                enabled = title.isNotBlank() && deadlineOk
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StepDialog(initial: GoalStep?, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit, onDelete: (() -> Unit)? = null) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var why by remember { mutableStateOf(initial?.why.orEmpty()) }
    var due by remember { mutableStateOf(initial?.due.orEmpty()) }
    val dueOk = due.isBlank() || parseDate(due) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(if (initial == null) "New step" else "Edit step") },
        text = {
            Column {
                TextInput(title, { title = it }, "Step")
                Spacer(Modifier.height(10.dp))
                TextInput(why, { why = it }, "Why this step")
                Spacer(Modifier.height(10.dp))
                TextInput(due, { due = it.take(10) }, "Due (YYYY-MM-DD, optional)")
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete step", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim(), why.trim(), parseDate(due)?.toString()) }, enabled = title.isNotBlank() && dueOk) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Start from a blank goal or a ready-made plan. */
@Composable
fun TemplatePicker(onDismiss: () -> Unit, onBlank: () -> Unit, onPick: (GoalTemplate) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("New goal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TemplateRow("✏️  Blank goal", "Your own goal, your own steps.", onBlank)
                GoalTemplates.all.forEach { t ->
                    TemplateRow("${t.area.emoji}  ${t.name}", "${t.steps.size} steps · ${t.why}") { onPick(t) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TemplateRow(title: String, sub: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
}
