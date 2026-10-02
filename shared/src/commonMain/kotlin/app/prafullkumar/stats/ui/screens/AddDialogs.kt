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
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.FoodItem
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.LibraryFood
import app.prafullkumar.stats.data.Slot
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Targets
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.NumberInput
import app.prafullkumar.stats.ui.components.TextInput
import app.prafullkumar.stats.data.formatNumber
import app.prafullkumar.stats.data.letter
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** Add or edit one item of the daily plan. */
@Composable
fun PlanFoodDialog(
    initial: FoodItem?,
    defaultSlot: Slot = Slot.BREAKFAST,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var detail by remember { mutableStateOf(initial?.detail.orEmpty()) }
    var slot by remember { mutableStateOf(initial?.slot ?: defaultSlot) }
    var kcal by remember { mutableStateOf(initial?.kcal?.toString().orEmpty()) }
    var protein by remember { mutableStateOf(initial?.protein?.let(::formatNumber).orEmpty()) }
    var carbs by remember { mutableStateOf(initial?.carbs?.let(::formatNumber).orEmpty()) }
    var fat by remember { mutableStateOf(initial?.fat?.let(::formatNumber).orEmpty()) }
    var fiber by remember { mutableStateOf(initial?.fiber?.let(::formatNumber).orEmpty()) }
    var core by remember { mutableStateOf(initial?.core ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(if (initial == null) "Add to daily plan" else "Edit plan item") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextInput(name, { name = it }, "Name")
                Spacer(Modifier.height(10.dp))
                TextInput(detail, { detail = it }, "Amount / note (e.g. 250 ml)")
                Spacer(Modifier.height(14.dp))
                SmallLabel("Meal")
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Slot.entries.forEach { s -> ChoiceChip(s.title, slot == s) { slot = s } }
                }
                Spacer(Modifier.height(14.dp))
                SmallLabel("Macros for this amount")
                Spacer(Modifier.height(6.dp))
                MacroInputs(kcal, { kcal = it }, protein, { protein = it }, carbs, { carbs = it },
                    fat, { fat = it }, fiber, { fiber = it })
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Core item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Hint("Non-negotiable — counted separately in the day score.")
                    }
                    Switch(checked = core, onCheckedChange = { core = it })
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text("Remove from plan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        FoodItem(
                            id = initial?.id ?: StatsRepo.newId("food"),
                            name = name.trim(),
                            detail = detail.trim(),
                            slot = slot,
                            kcal = kcal.toDoubleOrNull()?.toInt() ?: 0,
                            protein = protein.toDoubleOrNull() ?: 0.0,
                            carbs = carbs.toDoubleOrNull() ?: 0.0,
                            fat = fat.toDoubleOrNull() ?: 0.0,
                            fiber = fiber.toDoubleOrNull() ?: 0.0,
                            core = core
                        )
                    )
                },
                enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MacroInputs(
    kcal: String, onKcal: (String) -> Unit,
    protein: String, onProtein: (String) -> Unit,
    carbs: String, onCarbs: (String) -> Unit,
    fat: String, onFat: (String) -> Unit,
    fiber: String, onFiber: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberInput(kcal, onKcal, "kcal", Modifier.weight(1f))
        NumberInput(protein, onProtein, "Protein g", Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberInput(carbs, onCarbs, "Carbs g", Modifier.weight(1f))
        NumberInput(fat, onFat, "Fat g", Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    NumberInput(fiber, onFiber, "Fiber g", Modifier.fillMaxWidth())
}

@Composable
fun AddLibraryFoodDialog(onDismiss: () -> Unit, onAdd: (LibraryFood) -> Unit) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("New library food") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextInput(name, { name = it }, "Name")
                Spacer(Modifier.height(14.dp))
                SmallLabel("Unit")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("gram (g)", unit == "g") { unit = "g" }
                    ChoiceChip("millilitre (ml)", unit == "ml") { unit = "ml" }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Per 100 $unit:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                MacroInputs(kcal, { kcal = it }, protein, { protein = it }, carbs, { carbs = it },
                    fat, { fat = it }, fiber, { fiber = it })
                Spacer(Modifier.height(10.dp))
                Hint("Copy the per-100 $unit column from the label. You only enter the quantity when logging.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        LibraryFood(
                            id = StatsRepo.newId("lib"),
                            name = name.trim(),
                            unit = unit,
                            kcalPer100 = kcal.toDoubleOrNull() ?: 0.0,
                            proteinPer100 = protein.toDoubleOrNull() ?: 0.0,
                            carbsPer100 = carbs.toDoubleOrNull() ?: 0.0,
                            fatPer100 = fat.toDoubleOrNull() ?: 0.0,
                            fiberPer100 = fiber.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Pick a library food and enter how much was eaten. */
@Composable
fun LogPortionDialog(
    onDismiss: () -> Unit,
    onLog: (String, Double) -> Unit,
    onCreateNew: () -> Unit
) {
    var selected by remember { mutableStateOf(StatsRepo.library.firstOrNull()?.id) }
    var qty by remember { mutableStateOf("") }
    val item = StatsRepo.library.firstOrNull { it.id == selected }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Log extra food") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (StatsRepo.library.isEmpty()) {
                    Hint(
                        "Library is empty. Add a food once with its per-100 g / 100 ml " +
                            "numbers, then logging only needs the quantity."
                    )
                } else {
                    StatsRepo.library.forEach { food ->
                        LibraryPickRow(
                            food = food,
                            selected = food.id == selected,
                            onClick = { selected = food.id }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    NumberInput(
                        qty,
                        { qty = it },
                        "How much (${item?.unit ?: "g"})",
                        Modifier.fillMaxWidth()
                    )
                    val amount = qty.toDoubleOrNull()
                    if (item != null && amount != null && amount > 0) {
                        val factor = amount / 100.0
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${(item.kcalPer100 * factor).toInt()} kcal · " +
                                "P ${formatNumber(item.proteinPer100 * factor)}g · " +
                                "C ${formatNumber(item.carbsPer100 * factor)}g · " +
                                "F ${formatNumber(item.fatPer100 * factor)}g · " +
                                "Fi ${formatNumber(item.fiberPer100 * factor)}g",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCreateNew) { Text("+ New library food") }
                }
            }
        },
        confirmButton = {
            if (StatsRepo.library.isEmpty()) {
                TextButton(onClick = onCreateNew) { Text("Create food") }
            } else {
                TextButton(
                    onClick = {
                        val amount = qty.toDoubleOrNull() ?: return@TextButton
                        val id = selected ?: return@TextButton
                        onLog(id, amount)
                    },
                    enabled = selected != null && (qty.toDoubleOrNull() ?: 0.0) > 0
                ) { Text("Log") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The whole library — create and remove foods here. */
@Composable
fun LibraryDialog(onDismiss: () -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Food library") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (StatsRepo.library.isEmpty()) {
                    Hint("Nothing yet. Add foods that are not in your daily plan — per 100 g / 100 ml.")
                }
                StatsRepo.library.forEach { food ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                food.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Hint(
                                "100 ${food.unit}: ${food.kcalPer100.toInt()} kcal · " +
                                    "P ${formatNumber(food.proteinPer100)}g · " +
                                    "C ${formatNumber(food.carbsPer100)}g · " +
                                    "F ${formatNumber(food.fatPer100)}g · " +
                                    "Fi ${formatNumber(food.fiberPer100)}g"
                            )
                        }
                        TextButton(onClick = { StatsRepo.removeLibraryFood(food.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCreateNew) { Text("Add food") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun LibraryPickRow(food: LibraryFood, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            food.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface
        )
        Text(
            "100 ${food.unit} = ${food.kcalPer100.toInt()} kcal, " +
                "P ${formatNumber(food.proteinPer100)}g",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
        )
    }
}

/** Add or edit a habit: checkbox or counter, and which weekdays it is due. */
@Composable
fun HabitDialog(
    initial: Habit?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var why by remember { mutableStateOf(initial?.why.orEmpty()) }
    var counter by remember { mutableStateOf((initial?.target ?: 1) > 1) }
    var target by remember { mutableStateOf(initial?.target?.takeIf { it > 1 }?.toString().orEmpty()) }
    var unit by remember { mutableStateOf(initial?.unit.orEmpty()) }
    var days by remember { mutableStateOf(initial?.days ?: Habit.ALL_DAYS) }

    val targetValue = if (counter) target.toDoubleOrNull()?.toInt() ?: 0 else 1

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(if (initial == null) "New habit" else "Edit habit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextInput(name, { name = it }, "Habit")
                Spacer(Modifier.height(10.dp))
                TextInput(why, { why = it }, "Why does this matter to you?")
                Spacer(Modifier.height(14.dp))
                SmallLabel("Type")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Yes / no", !counter) { counter = false }
                    ChoiceChip("Count", counter) { counter = true }
                }
                if (counter) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberInput(target, { target = it }, "Daily target", Modifier.weight(1f))
                        TextInput(unit, { unit = it }, "Unit", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(14.dp))
                SmallLabel("Days")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        ChoiceChip(
                            d.letter(),
                            d.isoDayNumber in days
                        ) {
                            days = if (d.isoDayNumber in days) days - d.isoDayNumber else days + d.isoDayNumber
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Hint("Days off never break the streak.")
                if (onArchive != null || onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        onArchive?.let { TextButton(onClick = it) { Text("Archive") } }
                        onDelete?.let {
                            TextButton(onClick = it) {
                                Text("Delete + history", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Habit(
                            id = initial?.id ?: StatsRepo.newId("habit"),
                            name = name.trim(),
                            why = why.trim(),
                            target = targetValue,
                            unit = if (counter) unit.trim() else "",
                            days = days,
                            createdAt = initial?.createdAt ?: today.toString(),
                            archived = initial?.archived ?: false
                        )
                    )
                },
                enabled = name.isNotBlank() && days.isNotEmpty() && targetValue >= 1
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** All daily goals in one place. */
@Composable
fun TargetsDialog(initial: Targets, onDismiss: () -> Unit, onSave: (Targets) -> Unit) {
    var kcal by remember { mutableStateOf(initial.kcal.toString()) }
    var protein by remember { mutableStateOf(initial.protein.toString()) }
    var carbs by remember { mutableStateOf(initial.carbs.toString()) }
    var fat by remember { mutableStateOf(initial.fat.toString()) }
    var fiber by remember { mutableStateOf(initial.fiber.toString()) }
    var water by remember { mutableStateOf(initial.water.toString()) }
    var weight by remember { mutableStateOf(formatNumber(initial.goalWeight)) }
    var sleep by remember { mutableStateOf(formatNumber(initial.sleep)) }
    var focus by remember { mutableStateOf(initial.focusMinutes.toString()) }
    var beast by remember { mutableStateOf(initial.beastScore.toString()) }

    fun int(s: String, fallback: Int) = s.toDoubleOrNull()?.toInt() ?: fallback

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Daily targets") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SmallLabel("Nutrition")
                Spacer(Modifier.height(6.dp))
                MacroInputs(kcal, { kcal = it }, protein, { protein = it }, carbs, { carbs = it },
                    fat, { fat = it }, fiber, { fiber = it })
                Spacer(Modifier.height(14.dp))
                SmallLabel("Body")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberInput(water, { water = it }, "Water glasses", Modifier.weight(1f))
                    NumberInput(weight, { weight = it }, "Goal kg", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                SmallLabel("Mind")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberInput(sleep, { sleep = it }, "Sleep h", Modifier.weight(1f))
                    NumberInput(focus, { focus = it }, "Focus min / day", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                NumberInput(beast, { beast = it }, "Beast day score (0–100)", Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Hint("A day at or above this score extends the beast streak. Set 0 to skip focus or sleep in the score.")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Targets(
                        kcal = int(kcal, initial.kcal),
                        protein = int(protein, initial.protein),
                        carbs = int(carbs, initial.carbs),
                        fat = int(fat, initial.fat),
                        fiber = int(fiber, initial.fiber),
                        water = int(water, initial.water),
                        goalWeight = weight.toDoubleOrNull() ?: initial.goalWeight,
                        sleep = sleep.toDoubleOrNull() ?: initial.sleep,
                        focusMinutes = int(focus, initial.focusMinutes),
                        beastScore = int(beast, initial.beastScore).coerceIn(0, 100)
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SmallLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
