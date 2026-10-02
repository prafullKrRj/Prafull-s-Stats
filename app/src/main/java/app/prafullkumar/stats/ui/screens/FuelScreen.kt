package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.FoodItem
import app.prafullkumar.stats.data.Portion
import app.prafullkumar.stats.data.Slot
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.CheckDot
import app.prafullkumar.stats.ui.components.DateStrip
import app.prafullkumar.stats.ui.components.DayMark
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.MacroChip
import app.prafullkumar.stats.ui.components.ProgressRing
import app.prafullkumar.stats.ui.components.RoundStep
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.ThinBar
import app.prafullkumar.stats.ui.components.cleanDecimal
import app.prafullkumar.stats.ui.components.formatNumber
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.CarbColor
import app.prafullkumar.stats.ui.theme.FatColor
import app.prafullkumar.stats.ui.theme.FiberColor
import app.prafullkumar.stats.ui.theme.ProteinColor
import java.time.LocalDate

@Composable
fun FuelScreen(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpenTargets: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var showLogPortion by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var showNewLibraryFood by remember { mutableStateOf(false) }
    var planEdit by remember { mutableStateOf<FoodItem?>(null) }
    var planAdd by remember { mutableStateOf<Slot?>(null) }

    val log = StatsRepo.logFor(selectedDate)
    val totals = StatsRepo.totalsFor(selectedDate)
    val targets = StatsRepo.targets
    val coreFoods = StatsRepo.coreFoods()
    val coreDone = StatsRepo.coreDoneCount(selectedDate)

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
                SectionHeader("Fuel", "${totals.kcal} / ${targets.kcal} kcal")
                Spacer(Modifier.height(8.dp))
                ThinBar(
                    fraction = if (targets.kcal <= 0) 0f else totals.kcal.toFloat() / targets.kcal,
                    color = if (totals.kcal > targets.kcal * 1.05) MaterialTheme.colorScheme.error
                    else CalorieColor
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProgressRing(totals.protein, targets.protein.toDouble(), ProteinColor, "Protein", "g", size = 72.dp)
                    ProgressRing(totals.carbs, targets.carbs.toDouble(), CarbColor, "Carbs", "g", size = 72.dp)
                    ProgressRing(totals.fat, targets.fat.toDouble(), FatColor, "Fat", "g", size = 72.dp)
                    ProgressRing(totals.fiber, targets.fiber.toDouble(), FiberColor, "Fiber", "g", size = 72.dp)
                }
                if (coreFoods.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Core foods",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "$coreDone / ${coreFoods.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (coreDone == coreFoods.size) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ThinBar(
                        fraction = coreDone.toFloat() / coreFoods.size,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (coreDone < coreFoods.size) {
                        Spacer(Modifier.height(8.dp))
                        Hint("Still to eat: " + coreFoods.filter { it.id !in log.foods }.joinToString { it.name })
                    }
                }
                val left = targets.protein - totals.protein
                if (left > 0) {
                    Spacer(Modifier.height(8.dp))
                    Hint("${formatNumber(left)} g protein to go.")
                }
            }
        }

        if (StatsRepo.foods.isEmpty()) {
            item {
                SectionCard {
                    SectionHeader("Build your plan")
                    Spacer(Modifier.height(8.dp))
                    Hint(
                        "Add the food you eat almost every day, once, with its macros. " +
                            "After that each day is just ticking boxes. Mark the protein base as core."
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AddButton("Plan item", Modifier.weight(1f)) { planAdd = Slot.BREAKFAST }
                        AddButton("Set targets", Modifier.weight(1f), onOpenTargets)
                    }
                }
            }
        } else {
            item {
                val plan = StatsRepo.planTotals()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Hint(
                        "Full plan: ${plan.kcal} kcal · P ${formatNumber(plan.protein)}g · " +
                            "Fi ${formatNumber(plan.fiber)}g",
                        Modifier.weight(1f)
                    )
                    TextButton(onClick = { StatsRepo.tickWholePlan(selectedDate) }) {
                        Text("Tick all")
                    }
                }
            }
        }

        val bySlot = StatsRepo.foods.groupBy { it.slot }
        Slot.entries.forEach { slot ->
            val items = bySlot[slot].orEmpty()
            if (items.isNotEmpty()) {
                item(key = "slot_${slot.name}") {
                    SectionCard {
                        SectionHeader(slot.title, "${items.filter { it.id in log.foods }.sumOf { it.kcal }} kcal")
                        Spacer(Modifier.height(6.dp))
                        items.forEach { food ->
                            FoodRow(
                                food = food,
                                checked = food.id in log.foods,
                                onToggle = { StatsRepo.toggleFood(selectedDate, food.id) },
                                onLongPress = { planEdit = food }
                            )
                        }
                    }
                }
            }
        }

        if (StatsRepo.foods.isNotEmpty()) {
            item {
                AddButton("Add plan item", Modifier.fillMaxWidth()) { planAdd = Slot.BREAKFAST }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    "Extra food",
                    if (log.portions.isEmpty()) "none" else "${log.portions.size} logged"
                )
                Spacer(Modifier.height(6.dp))
                if (log.portions.isEmpty()) {
                    Hint("Ate something off-plan? Log it with the quantity. Long-press to remove.")
                }
                log.portions.forEachIndexed { index, portion ->
                    PortionRow(
                        portion = portion,
                        onRemove = { StatsRepo.removePortion(selectedDate, index) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddButton("Log food", Modifier.weight(1f)) { showLogPortion = true }
                    AddButton("Library", Modifier.weight(1f)) { showLibrary = true }
                }
                val yesterday = selectedDate.minusDays(1)
                if (StatsRepo.logFor(yesterday).portions.isNotEmpty()) {
                    TextButton(onClick = { StatsRepo.repeatPortionsFrom(yesterday, selectedDate) }) {
                        Text("Repeat previous day's extras")
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Water", "target ${targets.water} glasses")
                Spacer(Modifier.height(12.dp))
                WaterRow(
                    glasses = log.water,
                    target = targets.water,
                    onChange = { StatsRepo.setWater(selectedDate, it) }
                )
            }
        }

        item {
            SectionCard {
                SectionHeader("Weight", log.weight?.let { "${formatNumber(it)} kg" } ?: "not logged")
                Spacer(Modifier.height(12.dp))
                WeightField(
                    weight = log.weight,
                    onChange = { StatsRepo.setWeight(selectedDate, it) }
                )
            }
        }

        item {
            Hint(
                "Tip: long-press a plan item to edit or remove it.",
                Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    if (showLogPortion) {
        LogPortionDialog(
            onDismiss = { showLogPortion = false },
            onLog = { foodId, qty ->
                StatsRepo.addPortion(selectedDate, foodId, qty)
                showLogPortion = false
            },
            onCreateNew = {
                showLogPortion = false
                showNewLibraryFood = true
            }
        )
    }

    if (showLibrary) {
        LibraryDialog(
            onDismiss = { showLibrary = false },
            onCreateNew = {
                showLibrary = false
                showNewLibraryFood = true
            }
        )
    }

    if (showNewLibraryFood) {
        AddLibraryFoodDialog(
            onDismiss = { showNewLibraryFood = false },
            onAdd = { item ->
                StatsRepo.addLibraryFood(item)
                showNewLibraryFood = false
            }
        )
    }

    planAdd?.let { slot ->
        PlanFoodDialog(
            initial = null,
            defaultSlot = slot,
            onDismiss = { planAdd = null },
            onSave = {
                StatsRepo.upsertFood(it)
                planAdd = null
            }
        )
    }

    planEdit?.let { food ->
        PlanFoodDialog(
            initial = food,
            onDismiss = { planEdit = null },
            onSave = {
                StatsRepo.upsertFood(it)
                planEdit = null
            },
            onDelete = {
                StatsRepo.removeFood(food.id)
                planEdit = null
            }
        )
    }
}

/** Date strip dot: full = beast day, half = something logged. */
fun dayMark(day: LocalDate): DayMark {
    val score = StatsRepo.dayScore(day).score
    return when {
        score >= StatsRepo.targets.beastScore -> DayMark.DONE
        score > 0 -> DayMark.PARTIAL
        else -> DayMark.EMPTY
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodRow(
    food: FoodItem,
    checked: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(checked)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (food.core) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Core",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (food.core) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            if (food.detail.isNotBlank()) Hint(food.detail)
            Spacer(Modifier.height(6.dp))
            MacroChips(food.protein, food.carbs, food.fat, food.fiber)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${food.kcal}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroChips(protein: Double, carbs: Double, fat: Double, fiber: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MacroChip("P ${formatNumber(protein)}", ProteinColor)
        MacroChip("C ${formatNumber(carbs)}", CarbColor)
        MacroChip("F ${formatNumber(fat)}", FatColor)
        if (fiber > 0) MacroChip("Fi ${formatNumber(fiber)}", FiberColor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortionRow(portion: Portion, onRemove: () -> Unit) {
    val item = StatsRepo.libraryFood(portion.foodId) ?: return
    val totals = StatsRepo.portionTotals(portion)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = {}, onLongClick = onRemove)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Hint("${formatNumber(portion.qty)} ${item.unit}")
            Spacer(Modifier.height(6.dp))
            MacroChips(totals.protein, totals.carbs, totals.fat, totals.fiber)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${totals.kcal}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightField(weight: Double?, onChange: (Double?) -> Unit) {
    var text by remember(weight) { mutableStateOf(weight?.let { formatNumber(it) } ?: "") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val cleaned = cleanDecimal(raw)
                text = cleaned
                // Ignore half-typed or impossible numbers so one stray tap
                // cannot wipe out a real weight.
                val value = cleaned.toDoubleOrNull()
                if (cleaned.isBlank()) onChange(null)
                else if (value != null && value in 20.0..250.0) onChange(value)
            },
            modifier = Modifier.width(150.dp),
            label = { Text("Weight") },
            suffix = { Text("kg") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.width(14.dp))
        Hint("Morning, before eating.")
    }
}

@Composable
private fun WaterRow(glasses: Int, target: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Glasses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Hint("$glasses / $target · ≈${formatNumber(glasses * 0.25)} L")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundStep("–") { onChange(glasses - 1) }
            Spacer(Modifier.width(14.dp))
            Text(
                glasses.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(14.dp))
            RoundStep("+") { onChange(glasses + 1) }
        }
    }
    Spacer(Modifier.height(10.dp))
    ThinBar(
        fraction = if (target <= 0) 0f else glasses.toFloat() / target,
        color = MaterialTheme.colorScheme.tertiary
    )
}
