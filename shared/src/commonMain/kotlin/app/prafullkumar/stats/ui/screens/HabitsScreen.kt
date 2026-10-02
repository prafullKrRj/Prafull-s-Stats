package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.DateStrip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.ProgressRing
import app.prafullkumar.stats.ui.components.RoundStep
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.theme.StreakColor
import kotlinx.datetime.LocalDate

/** Ideas shown when there are no habits yet — tap to add, nothing is pre-seeded. */
private val suggestions = listOf(
    Triple("Workout", 1, ""),
    Triple("Steps", 10000, "steps"),
    Triple("Read", 20, "pages"),
    Triple("Meditate", 10, "min"),
    Triple("No sugar", 1, ""),
    Triple("No phone first hour", 1, ""),
    Triple("Push-ups", 100, "reps"),
    Triple("In bed by 11", 1, "")
)

@Composable
fun HabitsScreen(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    val log = StatsRepo.logFor(selectedDate)
    val score = StatsRepo.dayScore(selectedDate)
    val beast = StatsRepo.beastStreak(today)
    val due = StatsRepo.scheduledHabits(selectedDate)
    val notDue = StatsRepo.activeHabits().filter { it !in due }
    val archived = StatsRepo.habits.filter { it.archived }

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
                    if (it <= today && it >= StatsRepo.startDate) onSelectDate(it)
                },
                dayState = { day -> dayMark(day) }
            )
        }

        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        value = score.score.toDouble(),
                        target = 100.0,
                        color = if (score.score >= StatsRepo.targets.beastScore) StreakColor
                        else MaterialTheme.colorScheme.primary,
                        label = "Day score",
                        unit = "",
                        size = 96.dp
                    )
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "🔥 ${beast.current} day${if (beast.current == 1) "" else "s"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = StreakColor
                        )
                        Hint("Beast streak · best ${beast.best}")
                        Spacer(Modifier.height(8.dp))
                        Hint(
                            if (score.score >= StatsRepo.targets.beastScore) "Beast day locked in."
                            else "${StatsRepo.targets.beastScore - score.score} points to a beast day."
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                score.parts.forEach { part ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            part.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(84.dp)
                        )
                        ThinBar(
                            fraction = part.value,
                            color = if (part.value >= 1f) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            height = 8.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            SectionCard {
                val doneCount = due.count { StatsRepo.habitDone(selectedDate, it) }
                SectionHeader("Habits", if (due.isEmpty()) null else "$doneCount / ${due.size} done")
                Spacer(Modifier.height(6.dp))
                val ideas = suggestions.filter { (name, _, _) ->
                    StatsRepo.habits.none { it.name.equals(name, ignoreCase = true) }
                }
                if (StatsRepo.activeHabits().isEmpty()) {
                    Hint("No habits yet. Start with one or two — tap an idea or make your own.")
                }
                if (StatsRepo.activeHabits().size < 5 && ideas.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ideas.forEach { (name, target, unit) ->
                            ChoiceChip("+ $name", false) {
                                StatsRepo.upsertHabit(
                                    Habit(
                                        id = StatsRepo.newId("habit"),
                                        name = name,
                                        target = target,
                                        unit = unit,
                                        createdAt = today.toString()
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (StatsRepo.activeHabits().isNotEmpty() && due.isEmpty()) {
                    Hint("Nothing due on this day.")
                }
                due.forEach { habit ->
                    HabitRow(
                        habit = habit,
                        count = log.habitCount(habit.id),
                        streak = StatsRepo.habitStreak(habit, today).current,
                        onBump = { step -> StatsRepo.bumpHabit(selectedDate, habit, step) },
                        onLongPress = { editing = habit }
                    )
                }
            }
        }

        if (notDue.isNotEmpty()) {
            item {
                SectionCard {
                    SectionHeader("Off today")
                    Spacer(Modifier.height(6.dp))
                    notDue.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            count = log.habitCount(habit.id),
                            streak = StatsRepo.habitStreak(habit, today).current,
                            onBump = { step -> StatsRepo.bumpHabit(selectedDate, habit, step) },
                            onLongPress = { editing = habit }
                        )
                    }
                }
            }
        }

        item {
            AddButton("New habit", Modifier.fillMaxWidth()) { adding = true }
        }

        if (archived.isNotEmpty()) {
            item {
                SectionCard {
                    SectionHeader("Archived", "${archived.size}")
                    archived.forEach { habit ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                StatsRepo.upsertHabit(habit.copy(archived = false))
                            }) { Text("Restore") }
                        }
                    }
                }
            }
        }

        item {
            Hint(
                "Tip: long-press a habit to edit, archive or delete it.",
                Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    if (adding) {
        HabitDialog(
            initial = null,
            today = today,
            onDismiss = { adding = false },
            onSave = {
                StatsRepo.upsertHabit(it)
                adding = false
            }
        )
    }

    editing?.let { habit ->
        HabitDialog(
            initial = habit,
            today = today,
            onDismiss = { editing = null },
            onSave = {
                StatsRepo.upsertHabit(it)
                editing = null
            },
            onArchive = {
                StatsRepo.archiveHabit(habit.id)
                editing = null
            },
            onDelete = {
                deleting = habit
                editing = null
            }
        )
    }

    deleting?.let { habit ->
        ConfirmDialog(
            title = "Delete ${habit.name}?",
            body = "Its whole history and streak go too. Archive instead to keep the history.",
            confirm = "Delete",
            onDismiss = { deleting = null },
            onConfirm = {
                StatsRepo.deleteHabit(habit.id)
                deleting = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habit: Habit,
    count: Int,
    streak: Int,
    onBump: (Int) -> Unit,
    onLongPress: () -> Unit
) {
    val done = count >= habit.target
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (!habit.isCounter) onBump(1) },
                onLongClick = onLongPress
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(done)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                habit.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done && !habit.isCounter) TextDecoration.LineThrough
                else TextDecoration.None
            )
            val sub = buildList {
                if (habit.isCounter) add("$count / ${habit.target} ${habit.unit}".trim())
            }.joinToString(" · ")
            if (sub.isNotBlank()) Hint(sub)
            if (habit.why.isNotBlank()) {
                Text(
                    "why: ${habit.why}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
            }
            if (habit.isCounter) {
                Spacer(Modifier.height(6.dp))
                ThinBar(
                    fraction = count.toFloat() / habit.target,
                    color = MaterialTheme.colorScheme.primary,
                    height = 6.dp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (habit.isCounter) {
            val step = habitStep(habit.target)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundStep("–", size = 34.dp) { onBump(-step) }
                Spacer(Modifier.width(6.dp))
                RoundStep("+", size = 34.dp) { onBump(step) }
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            if (streak > 0) "🔥$streak" else "",
            style = MaterialTheme.typography.labelLarge,
            color = StreakColor
        )
    }
}
