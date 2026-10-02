package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.InboxItem
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.dayMonth
import app.prafullkumar.stats.data.daysBetween
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.minutesLabel
import app.prafullkumar.stats.data.plusDays
import app.prafullkumar.stats.data.weekStart
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.DateStrip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.StatTile
import app.prafullkumar.stats.ui.components.cleanDecimal
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.StreakColor
import kotlinx.datetime.LocalDate

private val moods = listOf("😫", "😕", "😐", "🙂", "🤩")

@Composable
fun MindScreen(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    contentPadding: PaddingValues
) {
    val log = StatsRepo.logFor(selectedDate)
    var pendingDelete by remember { mutableStateOf<InboxItem?>(null) }

    ScreenColumn(contentPadding) {
        item {
            DateStrip(
                selected = selectedDate,
                today = today,
                earliest = StatsRepo.startDate,
                onSelect = { if (it <= today && it >= StatsRepo.startDate) onSelectDate(it) },
                dayState = { day -> dayMark(day) }
            )
        }

        item {
            SectionCard(glow = true) {
                SectionHeader("Brain dump", "${StatsRepo.inbox.count { !it.done }} open")
                Spacer(Modifier.height(6.dp))
                Hint("Get it out of your head. One thought per line — sort it later into tasks.")
                Spacer(Modifier.height(8.dp))
                var text by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("call bank, app idea, buy oats…") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AddButton("Dump it", Modifier.fillMaxWidth()) {
                    StatsRepo.dump(text, today)
                    text = ""
                }
                Spacer(Modifier.height(6.dp))
                StatsRepo.inbox.sortedBy { it.done }.forEach { item ->
                    InboxRow(
                        item = item,
                        onToggle = { StatsRepo.toggleInbox(item.id) },
                        onToday = { StatsRepo.promoteInbox(item.id, today) },
                        onLater = { StatsRepo.promoteInbox(item.id, null) },
                        onLongPress = { pendingDelete = item }
                    )
                }
                if (StatsRepo.inbox.any { it.done }) {
                    TextButton(onClick = { StatsRepo.clearDoneInbox() }) { Text("Clear done") }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Check-in")
                Spacer(Modifier.height(10.dp))
                RatingRow("Mood", log.mood, moods) { StatsRepo.setMood(selectedDate, it) }
                Spacer(Modifier.height(10.dp))
                RatingRow("Energy", log.energy, (1..5).map { "$it" }) { StatsRepo.setEnergy(selectedDate, it) }
                Spacer(Modifier.height(12.dp))
                var sleep by remember(selectedDate) { mutableStateOf(log.sleep?.let { formatNumber(it) } ?: "") }
                OutlinedTextField(
                    value = sleep,
                    onValueChange = { raw ->
                        sleep = cleanDecimal(raw)
                        val v = sleep.toDoubleOrNull()
                        if (sleep.isBlank()) StatsRepo.setSleep(selectedDate, null)
                        else if (v != null && v <= 16) StatsRepo.setSleep(selectedDate, v)
                    },
                    label = { Text("Sleep last night") },
                    suffix = { Text("h") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(200.dp)
                )
            }
        }

        item {
            SectionCard {
                SectionHeader("Journal", selectedDate.dayMonth())
                Spacer(Modifier.height(8.dp))
                JournalField(selectedDate, log.win, "Win of the day") { StatsRepo.setWin(selectedDate, it) }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.gratitude, "Grateful for") { StatsRepo.setGratitude(selectedDate, it) }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.lesson, "Lesson / what to fix") { StatsRepo.setLesson(selectedDate, it) }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.tomorrow, "Tomorrow's first move") { StatsRepo.setTomorrow(selectedDate, it) }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.note, "Free notes", minLines = 3) { StatsRepo.setNote(selectedDate, it) }
            }
        }

        item { WeeklyReview(today) }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "Delete this?",
            body = item.text,
            confirm = "Delete",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                StatsRepo.removeInbox(item.id)
                pendingDelete = null
            }
        )
    }
}

/** Monday-to-today summary: what the week actually looked like. */
@Composable
private fun WeeklyReview(today: LocalDate) {
    val start = maxOf(weekStart(today), StatsRepo.startDate)
    val days = daysBetween(start, today)
    val logs = days.map { StatsRepo.logFor(it) }
    val beast = days.count { StatsRepo.isBeastDay(it) }
    val tasksDone = logs.sumOf { l -> l.tasks.count { it.done } }
    val tasksAll = logs.sumOf { it.tasks.size }
    val focus = logs.sumOf { it.totalFocus }
    val wins = days.zip(logs).filter { it.second.win.isNotBlank() }

    SectionCard {
        SectionHeader("Weekly review", "${weekStart(today).dayMonth()} – ${weekStart(today).plusDays(6).dayMonth()}")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("$beast / ${days.size}", "beast days", StreakColor, Modifier.weight(1f))
            StatTile("$tasksDone / $tasksAll", "tasks done", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatTile(minutesLabel(focus), "deep focus", FocusColor, Modifier.weight(1f))
        }
        if (wins.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Wins", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            wins.forEach { (d, l) -> Hint("• ${d.dayMonth()}: ${l.win}") }
        } else {
            Spacer(Modifier.height(8.dp))
            Hint("Write a win each day — on Sunday you'll see the whole week of them here.")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxRow(
    item: InboxItem,
    onToggle: () -> Unit,
    onToday: () -> Unit,
    onLater: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(item.done, size = 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        if (!item.done) {
            SmallAction("Today", onToday)
            SmallAction("Later", onLater)
        }
    }
}

@Composable
fun SmallAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun RatingRow(label: String, value: Int, labels: List<String>, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(64.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, l ->
                val v = i + 1
                ChoiceChip(l, value == v) { onChange(if (value == v) 0 else v) }
            }
        }
    }
}

@Composable
private fun JournalField(
    date: LocalDate,
    value: String,
    label: String,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    var text by remember(date) { mutableStateOf(value) }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChange(it)
            },
            label = { Text(label) },
            shape = RoundedCornerShape(16.dp),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
