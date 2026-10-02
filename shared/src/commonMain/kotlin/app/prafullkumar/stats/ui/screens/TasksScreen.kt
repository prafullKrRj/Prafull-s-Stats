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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.plusDays
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.DateStrip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.Tag
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.theme.Ember
import app.prafullkumar.stats.ui.theme.GoalColor
import app.prafullkumar.stats.ui.theme.NeonBrush
import kotlinx.datetime.LocalDate

/** Time-blocked first (by time), then Top 3, then by priority; done sink. */
fun sortTasks(tasks: List<Task>): List<Task> = tasks.sortedWith(
    compareBy<Task> { it.done }
        .thenBy { it.time == null }
        .thenBy { it.time ?: "" }
        .thenByDescending { it.top }
        .thenByDescending { it.priority }
        .thenBy { it.createdAt }
)

@Composable
fun TasksScreen(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    contentPadding: PaddingValues
) {
    var editing by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf<LocalDate?>(null) }
    var addingBacklog by remember { mutableStateOf(false) }

    val tasks = sortTasks(StatsRepo.tasksFor(selectedDate))
    val top = tasks.filter { it.top }
    val rest = tasks.filter { !it.top }
    val done = tasks.count { it.done }
    val openPast = if (selectedDate == today) StatsRepo.openPastTasks(today) else 0

    ScreenColumn(contentPadding) {
        item {
            DateStrip(
                selected = selectedDate,
                today = today,
                latest = today.plusDays(7),
                earliest = minOf(StatsRepo.startDate, today),
                onSelect = { if (it <= today.plusDays(7)) onSelectDate(it) },
                dayState = { day -> dayMark(day) }
            )
        }

        item {
            SectionCard(glow = true) {
                SectionHeader("Plan the day", if (tasks.isEmpty()) "empty" else "$done / ${tasks.size} done")
                Spacer(Modifier.height(10.dp))
                ThinBar(if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size, MaterialTheme.colorScheme.primary, brush = NeonBrush)
                Spacer(Modifier.height(12.dp))
                QuickAdd(placeholder = "Add a task and press enter") { title ->
                    StatsRepo.addTask(selectedDate, Task(id = StatsRepo.newId("task"), title = title))
                }
                if (openPast > 0) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { StatsRepo.rollOver(today) }) {
                        Text("Roll over $openPast unfinished from this past week →")
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Top 3", "${top.count { it.done }} / ${top.size}")
                Spacer(Modifier.height(4.dp))
                if (top.isEmpty()) Hint("Star up to three tasks that make today a win (tap ★ on a task).")
                top.forEach { TaskRow(it, onEdit = { editing = it }) }
            }
        }

        item {
            SectionCard {
                SectionHeader("Everything else", "${rest.size}")
                Spacer(Modifier.height(4.dp))
                if (rest.isEmpty()) Hint("Nothing else planned.")
                rest.forEach { TaskRow(it, onEdit = { editing = it }) }
                Spacer(Modifier.height(8.dp))
                AddButton("Detailed task", Modifier.fillMaxWidth()) { adding = selectedDate }
            }
        }

        item {
            SectionCard {
                SectionHeader("Someday / backlog", "${StatsRepo.backlog.count { !it.done }}")
                Spacer(Modifier.height(4.dp))
                if (StatsRepo.backlog.isEmpty()) Hint("Ideas and tasks without a day yet.")
                sortTasks(StatsRepo.backlog).forEach { t ->
                    TaskRow(t, onEdit = { editing = t }, trailing = {
                        SmallAction("→ ${if (selectedDate == today) "Today" else "This day"}") { StatsRepo.moveTask(t.id, selectedDate) }
                    })
                }
                Spacer(Modifier.height(8.dp))
                AddButton("Backlog item", Modifier.fillMaxWidth()) { addingBacklog = true }
            }
        }

        item { Hint("Tap a task to edit, the circle to tick, ▶ to focus on it.", Modifier.padding(horizontal = 8.dp)) }
    }

    adding?.let { d ->
        TaskDialog(null, d, today, onDismiss = { adding = null }, onSave = { t, where ->
            StatsRepo.addTask(where, t); adding = null
        })
    }
    if (addingBacklog) {
        TaskDialog(null, null, today, onDismiss = { addingBacklog = false }, onSave = { t, where ->
            StatsRepo.addTask(where, t); addingBacklog = false
        })
    }
    editing?.let { t ->
        TaskDialog(t, null, today, onDismiss = { editing = null }, onSave = { updated, _ ->
            StatsRepo.updateTask(updated); editing = null
        }, onDelete = {
            StatsRepo.deleteTask(t.id); editing = null
        })
    }
}

/** Single-line capture: type, hit enter, done. */
@Composable
fun QuickAdd(placeholder: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) onAdd(text.trim())
        text = ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.replace("\n", "") },
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { submit() }, enabled = text.isNotBlank()) { Text("Add") }
    }
}

@Composable
fun TaskRow(
    task: Task,
    onEdit: () -> Unit,
    showWhy: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val goal = StatsRepo.goal(task.goalId)
    val running = StatsRepo.timer.taskId == task.id && !StatsRepo.timer.idle
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(CircleShape).clickable { StatsRepo.toggleTask(task.id) }) {
            CheckDot(task.done, size = 26.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (task.top) FontWeight.SemiBold else FontWeight.Normal,
                color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val hasMeta = task.time != null || task.priority > 0 || goal != null || task.pomodoros > 0 || task.estimate > 1
            if (hasMeta) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    task.time?.let { Tag("⏰ $it", scheme.tertiary) }
                    if (task.priority > 0) Tag(priorityNames[task.priority], if (task.priority == 3) Ember else scheme.secondary)
                    goal?.let { Tag("${it.area.emoji} ${it.title}", GoalColor, Modifier.weight(1f, fill = false)) }
                    if (task.pomodoros > 0 || task.estimate > 1) Tag("🍅 ${task.pomodoros}/${task.estimate}", scheme.onSurfaceVariant)
                }
            }
            if (showWhy && task.why.isNotBlank() && !task.done) {
                Text("why: ${task.why}", style = MaterialTheme.typography.labelMedium, color = scheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing()
        if (!task.done) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Top 3",
                tint = if (task.top) Ember else scheme.outline,
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable { StatsRepo.toggleTop(task.id) }.padding(7.dp)
            )
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Focus on this",
                tint = if (running) scheme.tertiary else scheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable {
                    if (!running) StatsRepo.startTimer(taskId = task.id, phase = app.prafullkumar.stats.data.Phase.WORK)
                    Nav.open(Dest.FOCUS)
                }.padding(6.dp)
            )
        }
    }
}
