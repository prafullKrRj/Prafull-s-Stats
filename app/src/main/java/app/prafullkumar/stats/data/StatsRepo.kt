package app.prafullkumar.stats.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

/**
 * Single in-memory store backed by one JSON file. Small enough that the
 * whole thing is loaded at startup and written back on every change.
 * Nothing is seeded — the plan, habits and targets are all the user's own.
 */
object StatsRepo {

    private const val FILE_NAME = "prafull_stats.json"
    private val io = Executors.newSingleThreadExecutor()
    private var file: File? = null

    /** Daily plan — the food eaten almost every day, ticked off on the checklist. */
    val foods = mutableStateListOf<FoodItem>()

    /** Off-plan foods, logged by quantity. */
    val library = mutableStateListOf<LibraryFood>()
    val habits = mutableStateListOf<Habit>()
    val inbox = mutableStateListOf<InboxItem>()
    private val logs = mutableStateMapOf<String, DayLog>()

    var targets by mutableStateOf(Targets())
        private set

    /** First day the app was used — nothing before it is counted. */
    var startDate by mutableStateOf(LocalDate.now())
        private set

    /** Running focus session: wall-clock end in millis, 0 when idle. */
    var focusEndsAt by mutableStateOf(0L)
        private set
    var focusLength by mutableStateOf(25)
        private set

    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, FILE_NAME)
        file = f
        if (f.exists()) load(f.readText()) else save()
    }

    private fun load(text: String) {
        runCatching { restore(JSONObject(text)) }
        save()
    }

    private fun restore(root: JSONObject) {
        foods.clear(); library.clear(); habits.clear(); inbox.clear(); logs.clear()

        root.optJSONArray("foods")?.forEachObject { foods.add(FoodItem.fromJson(it)) }
        root.optJSONArray("library")?.forEachObject { library.add(LibraryFood.fromJson(it)) }
        root.optJSONArray("habits")?.forEachObject { habits.add(Habit.fromJson(it)) }
        root.optJSONArray("inbox")?.forEachObject { inbox.add(InboxItem.fromJson(it)) }
        root.optJSONArray("logs")?.forEachObject {
            val log = DayLog.fromJson(it)
            logs[log.date] = log
        }
        targets = root.optJSONObject("targets")?.let { Targets.fromJson(it) } ?: Targets()
        startDate = root.optString("startDate")
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: loggedDates().minOrNull()
                    ?: LocalDate.now()
        focusEndsAt = root.optLong("focusEndsAt")
        focusLength = root.optInt("focusLength", 25)
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) runCatching { block(getJSONObject(i)) }
    }

    private fun snapshot(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("foods", JSONArray(foods.map { it.toJson() }))
        put("library", JSONArray(library.map { it.toJson() }))
        put("habits", JSONArray(habits.map { it.toJson() }))
        put("inbox", JSONArray(inbox.map { it.toJson() }))
        put("logs", JSONArray(logs.values.map { it.toJson() }))
        put("targets", targets.toJson())
        put("startDate", startDate.toString())
        put("focusEndsAt", focusEndsAt)
        put("focusLength", focusLength)
    }

    private fun save() {
        val f = file ?: return
        val text = snapshot().toString()
        io.execute { runCatching { f.writeText(text) } }
    }

    /** Whole store as pretty JSON, for backup through the share sheet. */
    fun exportJson(): String = snapshot().toString(2)

    /** Replaces everything with a previous export. Returns false if it does not parse. */
    fun importJson(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (!root.has("logs") && !root.has("foods")) return false
        restore(root)
        save()
        return true
    }

    // ---- reads: days ------------------------------------------------------

    /** Tracked days: from start to today, at most [maxDays]. */
    fun trackedDays(today: LocalDate, maxDays: Int = 30): List<LocalDate> {
        val from = maxOf(startDate, today.minusDays((maxDays - 1).toLong()))
        val count = ChronoUnit.DAYS.between(from, today).toInt() + 1
        if (count <= 0) return listOf(today)
        return (0 until count).map { from.plusDays(it.toLong()) }
    }

    /** Which day of tracking today is (first day = 1). */
    fun dayNumber(today: LocalDate): Int =
        (ChronoUnit.DAYS.between(startDate, today).toInt() + 1).coerceAtLeast(1)

    fun logFor(date: LocalDate): DayLog = logs[date.toString()] ?: DayLog(date.toString())

    fun loggedDates(): List<LocalDate> =
        logs.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()

    fun weightHistory(): List<Pair<LocalDate, Double>> =
        logs.values.mapNotNull { log ->
            val w = log.weight ?: return@mapNotNull null
            runCatching { LocalDate.parse(log.date) to w }.getOrNull()
        }.sortedBy { it.first }

    // ---- reads: food ------------------------------------------------------

    fun totalsFor(date: LocalDate): DayTotals {
        val log = logFor(date)
        var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0; var fi = 0.0
        foods.filter { it.id in log.foods }.forEach {
            kcal += it.kcal; p += it.protein; c += it.carbs; f += it.fat; fi += it.fiber
        }
        log.portions.forEach { portion ->
            val t = portionTotals(portion)
            kcal += t.kcal; p += t.protein; c += t.carbs; f += t.fat; fi += t.fiber
        }
        return DayTotals(kcal.toInt(), p, c, f, fi)
    }

    /** What the full plan adds up to if every item is ticked. */
    fun planTotals(): DayTotals = DayTotals(
        kcal = foods.sumOf { it.kcal },
        protein = foods.sumOf { it.protein },
        carbs = foods.sumOf { it.carbs },
        fat = foods.sumOf { it.fat },
        fiber = foods.sumOf { it.fiber }
    )

    fun libraryFood(id: String): LibraryFood? = library.firstOrNull { it.id == id }

    /** Macros of one logged portion, scaled by its quantity. */
    fun portionTotals(portion: Portion): DayTotals {
        val item = libraryFood(portion.foodId) ?: return DayTotals()
        val factor = portion.qty / 100.0
        return DayTotals(
            kcal = (item.kcalPer100 * factor).toInt(),
            protein = item.proteinPer100 * factor,
            carbs = item.carbsPer100 * factor,
            fat = item.fatPer100 * factor,
            fiber = item.fiberPer100 * factor
        )
    }

    fun coreFoods(): List<FoodItem> = foods.filter { it.core }

    fun coreDoneCount(date: LocalDate): Int {
        val log = logFor(date)
        return coreFoods().count { it.id in log.foods }
    }

    // ---- reads: habits, score, streaks -----------------------------------

    fun activeHabits(): List<Habit> = habits.filter { !it.archived }

    /** Habits that are due on [date] and already existed then. */
    fun scheduledHabits(date: LocalDate): List<Habit> = activeHabits().filter { h ->
        date.dayOfWeek.value in h.days && !date.isBefore(habitStart(h))
    }

    private fun habitStart(h: Habit): LocalDate =
        runCatching { LocalDate.parse(h.createdAt) }.getOrNull() ?: startDate

    fun habitDone(date: LocalDate, habit: Habit): Boolean =
        logFor(date).habitCount(habit.id) >= habit.target

    fun habitStreak(habit: Habit, today: LocalDate): Streak {
        val from = maxOf(startDate, habitStart(habit)).let { minOf(it, today) }
        val days = daysBetween(from, today)
        return Scoring.streak(days, today) { day ->
            if (day.dayOfWeek.value !in habit.days) null else habitDone(day, habit)
        }
    }

    fun dayScore(date: LocalDate): DayScore = Scoring.dayScore(
        log = logFor(date),
        totals = totalsFor(date),
        targets = targets,
        plan = foods,
        scheduledHabits = scheduledHabits(date)
    )

    fun isBeastDay(date: LocalDate): Boolean = dayScore(date).score >= targets.beastScore

    /** Consecutive days at or above the beast score. */
    fun beastStreak(today: LocalDate): Streak =
        Scoring.streak(daysBetween(minOf(startDate, today), today), today) { isBeastDay(it) }

    fun proteinStreak(today: LocalDate): Streak =
        Scoring.streak(daysBetween(minOf(startDate, today), today), today) {
            totalsFor(it).protein >= targets.protein
        }

    /** Days with anything logged at all — the "showed up" streak. */
    fun loggingStreak(today: LocalDate): Streak =
        Scoring.streak(daysBetween(minOf(startDate, today), today), today) {
            logs[it.toString()]?.let { log -> log != DayLog(log.date) } ?: false
        }

    private fun daysBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        val count = ChronoUnit.DAYS.between(from, to).toInt()
        if (count < 0) return emptyList()
        return (0..count).map { from.plusDays(it.toLong()) }
    }

    // ---- writes: day log --------------------------------------------------

    private fun edit(date: LocalDate, block: (DayLog) -> DayLog) {
        val key = date.toString()
        logs[key] = block(logs[key] ?: DayLog(key))
        if (date.isBefore(startDate)) startDate = date
        save()
    }

    fun toggleFood(date: LocalDate, id: String) = edit(date) { log ->
        log.copy(foods = if (id in log.foods) log.foods - id else log.foods + id)
    }

    /** Tick every plan item at once — the "ate the usual" button. */
    fun tickWholePlan(date: LocalDate) = edit(date) { log ->
        log.copy(foods = log.foods + foods.map { it.id })
    }

    fun setWeight(date: LocalDate, weight: Double?) = edit(date) { it.copy(weight = weight) }

    fun setWater(date: LocalDate, glasses: Int) = edit(date) {
        it.copy(water = glasses.coerceIn(0, 30))
    }

    fun setSleep(date: LocalDate, hours: Double?) = edit(date) {
        it.copy(sleep = hours?.coerceIn(0.0, 16.0))
    }

    fun setMood(date: LocalDate, mood: Int) = edit(date) { it.copy(mood = mood.coerceIn(0, 5)) }

    fun setEnergy(date: LocalDate, energy: Int) = edit(date) {
        it.copy(energy = energy.coerceIn(0, 5))
    }

    fun setNote(date: LocalDate, note: String) = edit(date) { it.copy(note = note) }
    fun setGratitude(date: LocalDate, text: String) = edit(date) { it.copy(gratitude = text) }
    fun setWin(date: LocalDate, text: String) = edit(date) { it.copy(win = text) }

    fun addPortion(date: LocalDate, foodId: String, qty: Double) = edit(date) { log ->
        log.copy(portions = log.portions + Portion(foodId, qty))
    }

    fun removePortion(date: LocalDate, index: Int) = edit(date) { log ->
        if (index !in log.portions.indices) log
        else log.copy(portions = log.portions.filterIndexed { i, _ -> i != index })
    }

    /** Copies the previous day's extra portions — same food, same amounts. */
    fun repeatPortionsFrom(from: LocalDate, to: LocalDate) {
        val source = logFor(from).portions.filter { libraryFood(it.foodId) != null }
        if (source.isEmpty()) return
        edit(to) { it.copy(portions = it.portions + source) }
    }

    /** Checkbox habits toggle; counter habits go up by [step]. */
    fun bumpHabit(date: LocalDate, habit: Habit, step: Int = 1) = edit(date) { log ->
        val now = log.habitCount(habit.id)
        val next = if (!habit.isCounter) (if (now >= 1) 0 else 1)
        else (now + step).coerceIn(0, 100_000)
        val map = if (next == 0) log.habits - habit.id else log.habits + (habit.id to next)
        log.copy(habits = map)
    }

    fun setHabitCount(date: LocalDate, habit: Habit, count: Int) = edit(date) { log ->
        val next = count.coerceIn(0, 100_000)
        log.copy(habits = if (next == 0) log.habits - habit.id else log.habits + (habit.id to next))
    }

    fun addPriority(date: LocalDate, text: String) = edit(date) { log ->
        if (log.priorities.size >= 3 || text.isBlank()) log
        else log.copy(priorities = log.priorities + Priority(text.trim()))
    }

    fun togglePriority(date: LocalDate, index: Int) = edit(date) { log ->
        log.copy(priorities = log.priorities.mapIndexed { i, p ->
            if (i == index) p.copy(done = !p.done) else p
        })
    }

    fun removePriority(date: LocalDate, index: Int) = edit(date) { log ->
        log.copy(priorities = log.priorities.filterIndexed { i, _ -> i != index })
    }

    /** Unfinished priorities from [from] carried into [to], up to three. */
    fun carryPriorities(from: LocalDate, to: LocalDate) = edit(to) { log ->
        val open = logFor(from).priorities.filter { !it.done }
            .filter { p -> log.priorities.none { it.text == p.text } }
        log.copy(priorities = (log.priorities + open.map { it.copy(done = false) }).take(3))
    }

    fun addFocusMinutes(date: LocalDate, minutes: Int, session: Boolean) = edit(date) { log ->
        log.copy(
            focusMinutes = (log.focusMinutes + minutes).coerceIn(0, 24 * 60),
            focusSessions = log.focusSessions + if (session) 1 else 0
        )
    }

    // ---- writes: focus timer ---------------------------------------------

    fun startFocus(minutes: Int, now: Long = System.currentTimeMillis()) {
        focusLength = minutes
        focusEndsAt = now + minutes * 60_000L
        save()
    }

    /**
     * Stops the running session and logs the minutes actually focused.
     * Called both when the timer runs out and when it is stopped early.
     */
    fun finishFocus(today: LocalDate, now: Long = System.currentTimeMillis()) {
        if (focusEndsAt == 0L) return
        val startedAt = focusEndsAt - focusLength * 60_000L
        val elapsed = ((minOf(now, focusEndsAt) - startedAt) / 60_000L).toInt()
        val complete = now >= focusEndsAt
        focusEndsAt = 0L
        if (elapsed > 0) addFocusMinutes(today, elapsed, complete) else save()
    }

    // ---- writes: plan, library, habits, inbox, targets --------------------

    fun upsertFood(item: FoodItem) {
        val i = foods.indexOfFirst { it.id == item.id }
        if (i >= 0) foods[i] = item else foods.add(item)
        save()
    }

    fun removeFood(id: String) {
        foods.removeAll { it.id == id }
        save()
    }

    fun addLibraryFood(item: LibraryFood) {
        library.add(item)
        save()
    }

    /** Removing a library food also removes its logged portions. */
    fun removeLibraryFood(id: String) {
        library.removeAll { it.id == id }
        logs.keys.toList().forEach { key ->
            val log = logs[key] ?: return@forEach
            if (log.portions.any { it.foodId == id }) {
                logs[key] = log.copy(portions = log.portions.filterNot { it.foodId == id })
            }
        }
        save()
    }

    fun upsertHabit(habit: Habit) {
        val i = habits.indexOfFirst { it.id == habit.id }
        if (i >= 0) habits[i] = habit else habits.add(habit)
        save()
    }

    fun archiveHabit(id: String) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        habits[i] = habits[i].copy(archived = true)
        save()
    }

    fun deleteHabit(id: String) {
        habits.removeAll { it.id == id }
        logs.keys.toList().forEach { key ->
            val log = logs[key] ?: return@forEach
            if (id in log.habits) logs[key] = log.copy(habits = log.habits - id)
        }
        save()
    }

    fun moveHabit(id: String, up: Boolean) {
        val i = habits.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in habits.indices) return
        val tmp = habits[i]
        habits[i] = habits[j]
        habits[j] = tmp
        save()
    }

    fun dump(text: String, today: LocalDate) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        lines.forEach { inbox.add(InboxItem(newId("in"), it, today.toString())) }
        save()
    }

    fun toggleInbox(id: String) {
        val i = inbox.indexOfFirst { it.id == id }
        if (i < 0) return
        inbox[i] = inbox[i].copy(done = !inbox[i].done)
        save()
    }

    fun removeInbox(id: String) {
        inbox.removeAll { it.id == id }
        save()
    }

    fun clearDoneInbox() {
        inbox.removeAll { it.done }
        save()
    }

    /** Moves a dumped thought into the day's top 3, if there is room. */
    fun promoteInbox(id: String, date: LocalDate): Boolean {
        val item = inbox.firstOrNull { it.id == id } ?: return false
        if (logFor(date).priorities.size >= 3) return false
        inbox.remove(item)
        addPriority(date, item.text)
        return true
    }

    fun updateTargets(t: Targets) {
        targets = t
        save()
    }

    fun newId(prefix: String): String = "${prefix}_${System.nanoTime()}"
}
