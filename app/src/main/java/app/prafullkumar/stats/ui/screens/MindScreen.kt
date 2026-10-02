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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.DateStrip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.cleanDecimal
import app.prafullkumar.stats.ui.components.formatNumber
import app.prafullkumar.stats.ui.theme.FocusColor
import kotlinx.coroutines.delay
import java.time.LocalDate

private val moods = listOf("😫", "😕", "😐", "🙂", "🤩")
private val focusLengths = listOf(15, 25, 50, 90)

@Composable
fun MindScreen(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val log = StatsRepo.logFor(selectedDate)
    var pendingDelete by remember { mutableStateOf<InboxItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 6.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateStrip(
                selected = selectedDate,
                today = today,
                earliest = StatsRepo.startDate,
                onSelect = {
                    if (!it.isAfter(today) && !it.isBefore(StatsRepo.startDate)) onSelectDate(it)
                },
                dayState = { day -> dayMark(day) }
            )
        }

        item {
            SectionCard {
                SectionHeader("Top 3", "${log.priorities.count { it.done }} / ${log.priorities.size}")
                Spacer(Modifier.height(6.dp))
                if (log.priorities.isEmpty()) {
                    Hint("If only three things get done today, which ones? Long-press to remove.")
                }
                log.priorities.forEachIndexed { index, p ->
                    PriorityRow(
                        text = p.text,
                        done = p.done,
                        onToggle = { StatsRepo.togglePriority(selectedDate, index) },
                        onLongPress = { StatsRepo.removePriority(selectedDate, index) }
                    )
                }
                if (log.priorities.size < 3) {
                    Spacer(Modifier.height(8.dp))
                    var text by remember(selectedDate) { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Priority #${log.priorities.size + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                StatsRepo.addPriority(selectedDate, text)
                                text = ""
                            },
                            enabled = text.isNotBlank()
                        ) { Text("Add") }
                    }
                }
                val previous = selectedDate.minusDays(1)
                val open = StatsRepo.logFor(previous).priorities.count { !it.done }
                if (open > 0 && log.priorities.size < 3) {
                    TextButton(onClick = { StatsRepo.carryPriorities(previous, selectedDate) }) {
                        Text("Carry over $open unfinished from yesterday")
                    }
                }
            }
        }

        item { FocusCard(today = today, selectedDate = selectedDate) }

        item {
            SectionCard {
                SectionHeader("Brain dump", "${StatsRepo.inbox.count { !it.done }} open")
                Spacer(Modifier.height(6.dp))
                Hint("Empty your head. One thought per line — sort it out later.")
                Spacer(Modifier.height(8.dp))
                var text by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("call bank, idea for app, buy oats…") },
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
                        canPromote = !item.done && log.priorities.size < 3,
                        onToggle = { StatsRepo.toggleInbox(item.id) },
                        onPromote = { StatsRepo.promoteInbox(item.id, selectedDate) },
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
                RatingRow("Energy", log.energy, (1..5).map { "$it" }) {
                    StatsRepo.setEnergy(selectedDate, it)
                }
                Spacer(Modifier.height(12.dp))
                var sleep by remember(selectedDate) {
                    mutableStateOf(log.sleep?.let { formatNumber(it) } ?: "")
                }
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
                    modifier = Modifier.width(180.dp)
                )
            }
        }

        item {
            SectionCard {
                SectionHeader("Journal")
                Spacer(Modifier.height(8.dp))
                JournalField(selectedDate, log.win, "Win of the day") {
                    StatsRepo.setWin(selectedDate, it)
                }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.gratitude, "Grateful for") {
                    StatsRepo.setGratitude(selectedDate, it)
                }
                Spacer(Modifier.height(10.dp))
                JournalField(selectedDate, log.note, "Notes — what went well, what to fix", minLines = 3) {
                    StatsRepo.setNote(selectedDate, it)
                }
            }
        }
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

/**
 * Focus timer. The end time lives in the repo, so the session survives the
 * app being closed; minutes are logged to today when it ends or is stopped.
 */
@Composable
private fun FocusCard(today: LocalDate, selectedDate: LocalDate) {
    val targets = StatsRepo.targets
    val log = StatsRepo.logFor(selectedDate)
    val endsAt = StatsRepo.focusEndsAt
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var length by remember { mutableStateOf(StatsRepo.focusLength) }

    LaunchedEffect(endsAt) {
        while (endsAt > 0) {
            now = System.currentTimeMillis()
            if (now >= endsAt) {
                StatsRepo.finishFocus(today, now)
                break
            }
            delay(1000)
        }
    }

    SectionCard {
        SectionHeader(
            "Deep focus",
            "${log.focusMinutes} / ${targets.focusMinutes} min · ${log.focusSessions} sessions"
        )
        Spacer(Modifier.height(8.dp))
        ThinBar(
            fraction = if (targets.focusMinutes <= 0) 0f
            else log.focusMinutes.toFloat() / targets.focusMinutes,
            color = FocusColor
        )
        Spacer(Modifier.height(14.dp))
        if (endsAt > 0) {
            val left = ((endsAt - now) / 1000).coerceAtLeast(0)
            Text(
                "%02d:%02d".format(left / 60, left % 60),
                style = MaterialTheme.typography.displaySmall,
                color = FocusColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Hint(
                "Phone down. One thing only.",
                Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { StatsRepo.finishFocus(today) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Stop & log minutes so far") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                focusLengths.forEach { m ->
                    ChoiceChip("$m min", length == m) { length = m }
                }
            }
            Spacer(Modifier.height(10.dp))
            AddButton("Start $length min focus", Modifier.fillMaxWidth()) {
                StatsRepo.startFocus(length)
            }
            if (selectedDate != today) {
                Spacer(Modifier.height(6.dp))
                Hint("Sessions always log to today.")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PriorityRow(text: String, done: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(done)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxRow(
    item: InboxItem,
    canPromote: Boolean,
    onToggle: () -> Unit,
    onPromote: () -> Unit,
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
            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        if (canPromote) {
            Text(
                "→ Top 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPromote)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
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
