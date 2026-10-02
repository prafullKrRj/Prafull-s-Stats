package app.prafullkumar.stats.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Everything the app knows, as written to disk and exported. */
@Serializable
data class AppState(
    val version: Int = 2,
    val foods: List<FoodItem> = emptyList(),
    val library: List<LibraryFood> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val inbox: List<InboxItem> = emptyList(),
    val backlog: List<Task> = emptyList(),
    val logs: Map<String, DayLog> = emptyMap(),
    val targets: Targets = Targets(),
    val pomodoro: PomodoroSettings = PomodoroSettings(),
    val timer: TimerState = TimerState(),
    val settings: Settings = Settings(),
    val startDate: String = ""
)

/**
 * Sync bookkeeping. Every synced document has a key — `d:<date>` for a day,
 * `s:<section>` for a section — and the time it last changed.
 */
@Serializable
data class SyncMeta(
    val stamps: Map<String, Long> = emptyMap(),
    val dirty: Set<String> = emptySet(),
    val lastPull: Long = 0
)

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Single in-memory store, observable by Compose. Loaded at startup, written
 * back shortly after every change, and synced document by document.
 */
object StatsRepo {

    private const val FILE_NAME = "prafull_stats.json"
    private const val META_FILE = "prafull_sync.json"

    /** Section names; each is one synced document. */
    object Section {
        const val PLAN = "plan"
        const val HABITS = "habits"
        const val GOALS = "goals"
        const val INBOX = "inbox"
        const val BACKLOG = "backlog"
        const val SETTINGS = "settings"
        const val TIMER = "timer"
        val all = listOf(PLAN, HABITS, GOALS, INBOX, BACKLOG, SETTINGS, TIMER)
    }

    private var storage: Storage? = null
    var hooks: PlatformHooks = object : PlatformHooks {}
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val foods = mutableStateListOf<FoodItem>()
    val library = mutableStateListOf<LibraryFood>()
    val habits = mutableStateListOf<Habit>()
    val goals = mutableStateListOf<Goal>()
    val inbox = mutableStateListOf<InboxItem>()
    val backlog = mutableStateListOf<Task>()
    private val logs = mutableStateMapOf<String, DayLog>()

    var targets by mutableStateOf(Targets())
        private set
    var pomodoro by mutableStateOf(PomodoroSettings())
        private set
    var timer by mutableStateOf(TimerState())
        private set
    var settings by mutableStateOf(Settings())
        private set

    /** First day the app was used — nothing before it is counted. */
    var startDate by mutableStateOf(today())
        private set

    /** Live clock for countdowns; ticked once a second by the app. */
    var now by mutableStateOf(nowMillis())
        private set

    var meta = SyncMeta()
        private set

    /** Bumped on every local change; the sync engine listens to it. */
    val changes = MutableStateFlow(0L)
    private val saveRequests = MutableStateFlow(0L)

    fun init(storage: Storage) {
        if (this.storage != null) return
        this.storage = storage
        storage.read(META_FILE)?.let { text ->
            meta = runCatching { AppJson.decodeFromString<SyncMeta>(text) }.getOrDefault(SyncMeta())
        }
        val text = storage.read(FILE_NAME)
        if (text != null) restore(text) else startDate = today()
        scope.launch {
            saveRequests.collectLatest {
                if (it == 0L) return@collectLatest
                delay(250)
                writeNow()
            }
        }
    }

    // ---- persistence -----------------------------------------------------

    fun snapshot(): AppState = AppState(
        foods = foods.toList(),
        library = library.toList(),
        habits = habits.toList(),
        goals = goals.toList(),
        inbox = inbox.toList(),
        backlog = backlog.toList(),
        logs = logs.toMap(),
        targets = targets,
        pomodoro = pomodoro,
        timer = timer,
        settings = settings,
        startDate = startDate.toString()
    )

    private fun apply(state: AppState) {
        foods.replaceAll(state.foods)
        library.replaceAll(state.library)
        habits.replaceAll(state.habits)
        goals.replaceAll(state.goals)
        inbox.replaceAll(state.inbox)
        backlog.replaceAll(state.backlog)
        logs.clear()
        logs.putAll(state.logs)
        targets = state.targets
        pomodoro = state.pomodoro
        timer = state.timer
        settings = state.settings
        startDate = parseDate(state.startDate) ?: loggedDates().minOrNull() ?: today()
    }

    private fun <T> MutableList<T>.replaceAll(items: List<T>) {
        clear()
        addAll(items)
    }

    private fun restore(text: String): Boolean {
        val root = runCatching { AppJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return false
        val state = if (root["logs"] is JsonArray) migrateV1(root)
        else runCatching { AppJson.decodeFromJsonElement<AppState>(root) }.getOrNull()
        state ?: return false
        apply(state)
        return true
    }

    /**
     * Version 1 kept logs as an array and Top 3 as plain priorities. Priorities
     * become starred tasks so nothing typed in is lost.
     */
    private fun migrateV1(root: JsonObject): AppState {
        fun <T> list(key: String, decode: (JsonElement) -> T): List<T> =
            (root[key] as? JsonArray)?.mapNotNull { runCatching { decode(it) }.getOrNull() }.orEmpty()

        val logs = list("logs") { el ->
            val o = el.jsonObject
            val log = AppJson.decodeFromJsonElement<DayLog>(o)
            val tasks = (o["priorities"] as? JsonArray)?.mapIndexedNotNull { i, p ->
                val po = p.jsonObject
                val title = po["text"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                Task(
                    id = "t_v1_${log.date}_$i",
                    title = title,
                    top = true,
                    done = po["done"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }.orEmpty()
            log.copy(tasks = log.tasks + tasks)
        }
        return AppState(
            foods = list("foods") { AppJson.decodeFromJsonElement<FoodItem>(it) },
            library = list("library") { AppJson.decodeFromJsonElement<LibraryFood>(it) },
            habits = list("habits") { AppJson.decodeFromJsonElement<Habit>(it) },
            inbox = list("inbox") { AppJson.decodeFromJsonElement<InboxItem>(it) },
            logs = logs.associateBy { it.date },
            targets = root["targets"]?.let { runCatching { AppJson.decodeFromJsonElement<Targets>(it) }.getOrNull() }
                ?: Targets(),
            startDate = root["startDate"]?.jsonPrimitive?.contentOrNull.orEmpty()
        )
    }

    private fun requestSave() {
        saveRequests.value = saveRequests.value + 1
    }

    /** Writes immediately — call when the app goes to the background or quits. */
    fun flush() = writeNow()

    private fun writeNow() {
        val s = storage ?: return
        val text = AppJson.encodeToString(AppState.serializer(), snapshot())
        val metaText = AppJson.encodeToString(SyncMeta.serializer(), meta)
        runCatching {
            s.write(FILE_NAME, text)
            s.write(META_FILE, metaText)
        }
    }

    /** Whole store as pretty JSON, for backup. */
    fun exportJson(): String =
        Json(AppJson) { prettyPrint = true }.encodeToString(AppState.serializer(), snapshot())

    /** Replaces everything with a previous export. Returns false if it does not parse. */
    fun importJson(text: String): Boolean {
        if (!restore(text)) return false
        markAllDirty(force = true)
        requestSave()
        return true
    }

    // ---- change tracking (for sync) --------------------------------------

    private fun touch(key: String) {
        val t = maxOf(nowMillis(), (meta.stamps[key] ?: 0) + 1)
        meta = meta.copy(stamps = meta.stamps + (key to t), dirty = meta.dirty + key)
        changes.value = changes.value + 1
        requestSave()
    }

    private fun touchSection(name: String) = touch("s:$name")
    private fun touchDay(date: String) = touch("d:$date")

    /**
     * Marks every local document for upload. With [force] false, documents
     * never stamped get stamp 1 so anything already in the cloud wins.
     */
    fun markAllDirty(force: Boolean) {
        val keys = Section.all.map { "s:$it" } + logs.keys.map { "d:$it" }
        val now = nowMillis()
        val stamps = meta.stamps.toMutableMap()
        keys.forEach { k -> if (force) stamps[k] = now else if (k !in stamps) stamps[k] = 1L }
        meta = meta.copy(stamps = stamps, dirty = meta.dirty + keys)
        changes.value = changes.value + 1
        requestSave()
    }

    fun sectionJson(name: String): String = when (name) {
        Section.PLAN -> AppJson.encodeToString(PlanDoc.serializer(), PlanDoc(foods.toList(), library.toList(), targets, startDate.toString()))
        Section.HABITS -> AppJson.encodeToString(HabitsDoc.serializer(), HabitsDoc(habits.toList()))
        Section.GOALS -> AppJson.encodeToString(GoalsDoc.serializer(), GoalsDoc(goals.toList()))
        Section.INBOX -> AppJson.encodeToString(InboxDoc.serializer(), InboxDoc(inbox.toList()))
        Section.BACKLOG -> AppJson.encodeToString(BacklogDoc.serializer(), BacklogDoc(backlog.toList()))
        Section.SETTINGS -> AppJson.encodeToString(SettingsDoc.serializer(), SettingsDoc(pomodoro, settings))
        Section.TIMER -> AppJson.encodeToString(TimerState.serializer(), timer)
        else -> "{}"
    }

    fun dayJson(date: String): String =
        AppJson.encodeToString(DayLog.serializer(), logs[date] ?: DayLog(date))

    /** Applies a document pulled from the cloud. Does not mark anything dirty. */
    fun applyRemote(key: String, json: String, stamp: Long) {
        runCatching {
            if (key.startsWith("d:")) {
                val log = AppJson.decodeFromString<DayLog>(json)
                logs[key.removePrefix("d:")] = log
                parseDate(log.date)?.let { if (it < startDate) startDate = it }
            } else when (key.removePrefix("s:")) {
                Section.PLAN -> AppJson.decodeFromString<PlanDoc>(json).let {
                    foods.replaceAll(it.foods); library.replaceAll(it.library); targets = it.targets
                    parseDate(it.startDate)?.let { d -> if (d < startDate) startDate = d }
                }
                Section.HABITS -> habits.replaceAll(AppJson.decodeFromString<HabitsDoc>(json).habits)
                Section.GOALS -> goals.replaceAll(AppJson.decodeFromString<GoalsDoc>(json).goals)
                Section.INBOX -> inbox.replaceAll(AppJson.decodeFromString<InboxDoc>(json).items)
                Section.BACKLOG -> backlog.replaceAll(AppJson.decodeFromString<BacklogDoc>(json).tasks)
                Section.SETTINGS -> AppJson.decodeFromString<SettingsDoc>(json).let {
                    pomodoro = it.pomodoro; settings = it.settings
                }
                Section.TIMER -> {
                    timer = AppJson.decodeFromString<TimerState>(json)
                    hooks.timerChanged(timer)
                }
            }
            meta = meta.copy(stamps = meta.stamps + (key to stamp), dirty = meta.dirty - key)
            requestSave()
        }
    }

    fun markPushed(keys: Map<String, Long>) {
        // Only clear keys that were not changed again while the upload ran.
        val cleared = keys.filter { (k, t) -> meta.stamps[k] == t }.keys
        meta = meta.copy(dirty = meta.dirty - cleared)
        requestSave()
    }

    fun setLastPull(t: Long) {
        meta = meta.copy(lastPull = t)
        requestSave()
    }

    fun resetSyncMeta() {
        meta = SyncMeta()
        requestSave()
    }

    // ---- reads: days ------------------------------------------------------

    /** Tracked days: from start to today, at most [maxDays]. */
    fun trackedDays(today: LocalDate, maxDays: Int = 30): List<LocalDate> {
        val from = maxOf(startDate, today.minusDays(maxDays - 1))
        return daysBetween(from, today).ifEmpty { listOf(today) }
    }

    fun dayNumber(today: LocalDate): Int = (startDate.daysTo(today) + 1).coerceAtLeast(1)

    fun logFor(date: LocalDate): DayLog = logs[date.toString()] ?: DayLog(date.toString())

    fun loggedDates(): List<LocalDate> = logs.keys.mapNotNull { parseDate(it) }.sorted()

    fun weightHistory(): List<Pair<LocalDate, Double>> =
        logs.values.mapNotNull { log ->
            val w = log.weight ?: return@mapNotNull null
            parseDate(log.date)?.let { it to w }
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

    fun planTotals(): DayTotals = DayTotals(
        kcal = foods.sumOf { it.kcal },
        protein = foods.sumOf { it.protein },
        carbs = foods.sumOf { it.carbs },
        fat = foods.sumOf { it.fat },
        fiber = foods.sumOf { it.fiber }
    )

    fun libraryFood(id: String): LibraryFood? = library.firstOrNull { it.id == id }

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

    /** Habits due on [date] that already existed then. */
    fun scheduledHabits(date: LocalDate): List<Habit> = activeHabits().filter { h ->
        date.dayOfWeek.isoDayNumber in h.days && date >= habitStart(h)
    }

    private fun habitStart(h: Habit): LocalDate = parseDate(h.createdAt) ?: startDate

    fun habitDone(date: LocalDate, habit: Habit): Boolean =
        logFor(date).habitCount(habit.id) >= habit.target

    fun habitStreak(habit: Habit, today: LocalDate): Streak {
        val from = minOf(maxOf(startDate, habitStart(habit)), today)
        return Scoring.streak(daysBetween(from, today), today) { day ->
            if (day.dayOfWeek.isoDayNumber !in habit.days) null else habitDone(day, habit)
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

    private fun allDays(today: LocalDate) = daysBetween(minOf(startDate, today), today)

    fun beastStreak(today: LocalDate): Streak =
        Scoring.streak(allDays(today), today) { isBeastDay(it) }

    fun proteinStreak(today: LocalDate): Streak =
        Scoring.streak(allDays(today), today) { totalsFor(it).protein >= targets.protein }

    fun focusStreak(today: LocalDate): Streak =
        Scoring.streak(allDays(today), today) { logFor(it).totalFocus >= targets.focusMinutes }

    /** Days with anything logged at all — the "showed up" streak. */
    fun loggingStreak(today: LocalDate): Streak =
        Scoring.streak(allDays(today), today) { logs[it.toString()]?.isEmpty == false }

    // ---- writes: day log --------------------------------------------------

    private fun edit(date: LocalDate, block: (DayLog) -> DayLog) {
        val key = date.toString()
        val before = logs[key] ?: DayLog(key)
        val after = block(before)
        if (after == before) return
        logs[key] = after
        if (date < startDate) {
            startDate = date
            touchSection(Section.PLAN)
        }
        touchDay(key)
    }

    fun toggleFood(date: LocalDate, id: String) = edit(date) { log ->
        log.copy(foods = if (id in log.foods) log.foods - id else log.foods + id)
    }

    /** Tick every plan item at once — the "ate the usual" button. */
    fun tickWholePlan(date: LocalDate) = edit(date) { log ->
        log.copy(foods = log.foods + foods.map { it.id })
    }

    fun setWeight(date: LocalDate, weight: Double?) = edit(date) { it.copy(weight = weight) }
    fun setWater(date: LocalDate, glasses: Int) = edit(date) { it.copy(water = glasses.coerceIn(0, 30)) }
    fun setSleep(date: LocalDate, hours: Double?) = edit(date) { it.copy(sleep = hours?.coerceIn(0.0, 16.0)) }
    fun setMood(date: LocalDate, mood: Int) = edit(date) { it.copy(mood = mood.coerceIn(0, 5)) }
    fun setEnergy(date: LocalDate, energy: Int) = edit(date) { it.copy(energy = energy.coerceIn(0, 5)) }
    fun setNote(date: LocalDate, note: String) = edit(date) { it.copy(note = note) }
    fun setGratitude(date: LocalDate, text: String) = edit(date) { it.copy(gratitude = text) }
    fun setWin(date: LocalDate, text: String) = edit(date) { it.copy(win = text) }
    fun setLesson(date: LocalDate, text: String) = edit(date) { it.copy(lesson = text) }
    fun setTomorrow(date: LocalDate, text: String) = edit(date) { it.copy(tomorrow = text) }

    fun addPortion(date: LocalDate, foodId: String, qty: Double) = edit(date) { log ->
        log.copy(portions = log.portions + Portion(foodId, qty))
    }

    fun removePortion(date: LocalDate, index: Int) = edit(date) { log ->
        if (index !in log.portions.indices) log
        else log.copy(portions = log.portions.filterIndexed { i, _ -> i != index })
    }

    fun repeatPortionsFrom(from: LocalDate, to: LocalDate) {
        val source = logFor(from).portions.filter { libraryFood(it.foodId) != null }
        if (source.isEmpty()) return
        edit(to) { it.copy(portions = it.portions + source) }
    }

    /** Checkbox habits toggle; counter habits go up (or down) by [step]. */
    fun bumpHabit(date: LocalDate, habit: Habit, step: Int = 1) = edit(date) { log ->
        val now = log.habitCount(habit.id)
        val next = if (!habit.isCounter) (if (now >= 1) 0 else 1)
        else (now + step).coerceIn(0, 1_000_000)
        log.copy(habits = if (next == 0) log.habits - habit.id else log.habits + (habit.id to next))
    }

    // ---- writes: tasks ---------------------------------------------------

    fun tasksFor(date: LocalDate): List<Task> = logFor(date).tasks

    /** Where a task lives: a day, or null for the backlog. */
    private fun locate(id: String): Pair<LocalDate?, Task>? {
        backlog.firstOrNull { it.id == id }?.let { return null to it }
        logs.values.forEach { log ->
            log.tasks.firstOrNull { it.id == id }?.let { return parseDate(log.date) to it }
        }
        return null
    }

    fun findTask(id: String): Task? = locate(id)?.second

    fun addTask(date: LocalDate?, task: Task) {
        val t = task.copy(createdAt = if (task.createdAt == 0L) nowMillis() else task.createdAt)
        if (date == null) {
            backlog.add(t)
            touchSection(Section.BACKLOG)
        } else edit(date) { it.copy(tasks = it.tasks + t) }
    }

    fun updateTask(task: Task) {
        val (date, _) = locate(task.id) ?: return
        if (date == null) {
            val i = backlog.indexOfFirst { it.id == task.id }
            backlog[i] = task
            touchSection(Section.BACKLOG)
        } else edit(date) { log -> log.copy(tasks = log.tasks.map { if (it.id == task.id) task else it }) }
    }

    fun deleteTask(id: String) {
        val (date, _) = locate(id) ?: return
        if (date == null) {
            backlog.removeAll { it.id == id }
            touchSection(Section.BACKLOG)
        } else edit(date) { log -> log.copy(tasks = log.tasks.filterNot { it.id == id }) }
    }

    /** Ticks a task; a task made from a goal step ticks the step too. */
    fun toggleTask(id: String) {
        val task = findTask(id) ?: return
        val done = !task.done
        updateTask(task.copy(done = done, doneAt = if (done) nowMillis() else null))
        if (task.goalId != null && task.stepId != null) setStepDone(task.goalId, task.stepId, done)
    }

    fun toggleTop(id: String) {
        val task = findTask(id) ?: return
        updateTask(task.copy(top = !task.top))
    }

    /** Moves a task to another day, or to the backlog with null. */
    fun moveTask(id: String, to: LocalDate?) {
        val (from, task) = locate(id) ?: return
        if (from == to) return
        deleteTask(id)
        addTask(to, task.copy(time = if (to == null) null else task.time))
    }

    /** Unfinished tasks from the last week land on [today]. Returns how many moved. */
    fun rollOver(today: LocalDate): Int {
        var moved = 0
        (1..7).forEach { back ->
            val day = today.minusDays(back)
            val open = logFor(day).tasks.filter { !it.done }
            open.forEach { moveTask(it.id, today); moved++ }
        }
        return moved
    }

    fun openPastTasks(today: LocalDate): Int =
        (1..7).sumOf { back -> logFor(today.minusDays(back)).tasks.count { !it.done } }

    fun allTasks(): List<Pair<LocalDate?, Task>> =
        backlog.map { null to it } + logs.values.flatMap { log -> log.tasks.map { parseDate(log.date) to it } }

    // ---- writes: goals ---------------------------------------------------

    fun goal(id: String?): Goal? = goals.firstOrNull { it.id == id }

    fun upsertGoal(goal: Goal) {
        val i = goals.indexOfFirst { it.id == goal.id }
        if (i >= 0) goals[i] = goal else goals.add(goal)
        touchSection(Section.GOALS)
    }

    private fun editGoal(id: String, block: (Goal) -> Goal) {
        val i = goals.indexOfFirst { it.id == id }
        if (i < 0) return
        val updated = block(goals[i])
        // Hitting the last step or the metric target does not close the goal on
        // its own — finishing is a decision, offered in the UI.
        goals[i] = updated
        touchSection(Section.GOALS)
    }

    fun deleteGoal(id: String) {
        goals.removeAll { it.id == id }
        touchSection(Section.GOALS)
    }

    fun setGoalStatus(id: String, status: GoalStatus) = editGoal(id) {
        it.copy(status = status, doneAt = if (status == GoalStatus.DONE) today().toString() else null)
    }

    fun setStepDone(goalId: String, stepId: String, done: Boolean) = editGoal(goalId) { g ->
        g.copy(steps = g.steps.map {
            if (it.id == stepId) it.copy(done = done, doneAt = if (done) today().toString() else null) else it
        })
    }

    fun toggleStep(goalId: String, stepId: String) {
        val step = goal(goalId)?.steps?.firstOrNull { it.id == stepId } ?: return
        setStepDone(goalId, stepId, !step.done)
        // Keep any task made from this step in line with it.
        allTasks().filter { it.second.stepId == stepId && it.second.done == step.done }.forEach { (_, t) ->
            updateTask(t.copy(done = !step.done, doneAt = if (!step.done) nowMillis() else null))
        }
    }

    fun addStep(goalId: String, title: String, why: String = "") {
        if (title.isBlank()) return
        editGoal(goalId) { it.copy(steps = it.steps + GoalStep(newId("step"), title.trim(), why.trim())) }
    }

    fun updateStep(goalId: String, step: GoalStep) = editGoal(goalId) { g ->
        g.copy(steps = g.steps.map { if (it.id == step.id) step else it })
    }

    fun removeStep(goalId: String, stepId: String) = editGoal(goalId) { g ->
        g.copy(steps = g.steps.filterNot { it.id == stepId })
    }

    fun moveStep(goalId: String, stepId: String, up: Boolean) = editGoal(goalId) { g ->
        val list = g.steps.toMutableList()
        val i = list.indexOfFirst { it.id == stepId }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) g
        else {
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
            g.copy(steps = list)
        }
    }

    fun setMetric(goalId: String, value: Double) = editGoal(goalId) { it.copy(metricCurrent = value) }

    /** Turns a goal step into a task on [date] (or the backlog), carrying the goal's why. */
    fun planStep(goalId: String, stepId: String, date: LocalDate?) {
        val g = goal(goalId) ?: return
        val step = g.steps.firstOrNull { it.id == stepId } ?: return
        if (allTasks().any { it.second.stepId == stepId && !it.second.done }) return
        addTask(
            date,
            Task(
                id = newId("task"),
                title = step.title,
                why = step.why.ifBlank { g.why },
                goalId = goalId,
                stepId = stepId,
                priority = 2
            )
        )
    }

    fun isStepPlanned(stepId: String): Boolean = allTasks().any { it.second.stepId == stepId && !it.second.done }

    /** Focus minutes spent on a goal, through tasks linked to it. */
    fun focusOnGoal(goalId: String, days: List<LocalDate>? = null): Int {
        val source = days?.map { logFor(it) } ?: logs.values
        return source.sumOf { log -> log.sessions.filter { it.goalId == goalId }.sumOf { it.minutes } }
    }

    // ---- writes: pomodoro ------------------------------------------------

    fun tick(t: Long = nowMillis()) {
        now = t
        val tm = timer
        if (tm.running && t >= tm.endsAt) completePhase(t)
    }

    private fun updateTimer(t: TimerState) {
        timer = t
        touchSection(Section.TIMER)
        hooks.timerChanged(t)
    }

    fun phaseLength(phase: Phase): Int = when (phase) {
        Phase.WORK -> pomodoro.work
        Phase.SHORT_BREAK -> pomodoro.shortBreak
        Phase.LONG_BREAK -> pomodoro.longBreak
    }

    /** Starts (or restarts) the current phase, optionally linked to a task. */
    fun startTimer(taskId: String? = timer.taskId, phase: Phase = timer.phase, minutes: Int = phaseLength(phase)) {
        val label = taskId?.let { findTask(it)?.title }.orEmpty()
        val t = nowMillis()
        updateTimer(timer.copy(phase = phase, length = minutes, endsAt = t + minutes * 60_000L, pausedLeft = 0, taskId = taskId, label = label))
        now = t
    }

    fun pauseTimer() {
        val tm = timer
        if (!tm.running) return
        updateTimer(tm.copy(pausedLeft = tm.secondsLeft(nowMillis()).coerceAtLeast(1), endsAt = 0))
    }

    fun resumeTimer() {
        val tm = timer
        if (!tm.paused) return
        updateTimer(tm.copy(endsAt = nowMillis() + tm.pausedLeft * 1000, pausedLeft = 0))
    }

    /** Stops the current phase. Work stopped early still logs the minutes done. */
    fun stopTimer() {
        val tm = timer
        if (tm.idle) return
        if (tm.phase == Phase.WORK) {
            val leftSec = tm.secondsLeft(nowMillis())
            val minutes = ((tm.length * 60L - leftSec) / 60).toInt()
            if (minutes >= 1) logSession(tm, minutes, complete = false, end = nowMillis())
        }
        updateTimer(tm.copy(endsAt = 0, pausedLeft = 0, length = phaseLength(tm.phase)))
    }

    /** Ends the current phase now and moves to the next one. */
    fun skipPhase() {
        val tm = timer
        if (tm.phase == Phase.WORK) {
            stopTimer()
            updateTimer(timer.copy(phase = Phase.SHORT_BREAK, length = phaseLength(Phase.SHORT_BREAK)))
        } else {
            updateTimer(tm.copy(phase = Phase.WORK, endsAt = 0, pausedLeft = 0, length = phaseLength(Phase.WORK)))
        }
    }

    /** Switches phase while idle, e.g. to take a long break now. */
    fun setPhase(phase: Phase) {
        if (!timer.idle) return
        updateTimer(timer.copy(phase = phase, length = phaseLength(phase)))
    }

    fun linkTimerTask(taskId: String?) {
        updateTimer(timer.copy(taskId = taskId, label = taskId?.let { findTask(it)?.title }.orEmpty()))
    }

    private fun logSession(tm: TimerState, minutes: Int, complete: Boolean, end: Long) {
        val start = end - minutes * 60_000L
        val task = tm.taskId?.let { findTask(it) }
        val session = FocusSession(
            start = start,
            minutes = minutes,
            label = task?.title ?: tm.label,
            taskId = tm.taskId,
            goalId = task?.goalId,
            complete = complete
        )
        edit(today()) { log ->
            // The same pomodoro may finish on two synced devices; keep one.
            if (log.sessions.any { kotlin.math.abs(it.start - start) < 60_000 }) log
            else log.copy(sessions = log.sessions + session)
        }
        if (complete && task != null) updateTask(task.copy(pomodoros = task.pomodoros + 1))
    }

    private fun completePhase(t: Long) {
        val tm = timer
        val finished = tm.phase
        var cycle = tm.cycle
        val next = if (finished == Phase.WORK) {
            logSession(tm, tm.length, complete = true, end = tm.endsAt)
            cycle += 1
            if (cycle % pomodoro.longEvery.coerceAtLeast(1) == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
        } else {
            if (finished == Phase.LONG_BREAK) cycle = 0
            Phase.WORK
        }
        val auto = if (next == Phase.WORK) pomodoro.autoStartWork else pomodoro.autoStartBreaks
        val len = phaseLength(next)
        updateTimer(
            tm.copy(
                phase = next,
                cycle = cycle,
                length = len,
                endsAt = if (auto) t + len * 60_000L else 0,
                pausedLeft = 0
            )
        )
        hooks.phaseFinished(finished, next, auto)
    }

    fun updatePomodoro(p: PomodoroSettings) {
        pomodoro = p
        touchSection(Section.SETTINGS)
        if (timer.idle) timer = timer.copy(length = phaseLength(timer.phase))
    }

    // ---- writes: plan, library, habits, inbox, targets, settings ----------

    fun upsertFood(item: FoodItem) {
        val i = foods.indexOfFirst { it.id == item.id }
        if (i >= 0) foods[i] = item else foods.add(item)
        touchSection(Section.PLAN)
    }

    fun removeFood(id: String) {
        foods.removeAll { it.id == id }
        touchSection(Section.PLAN)
    }

    fun addLibraryFood(item: LibraryFood) {
        library.add(item)
        touchSection(Section.PLAN)
    }

    /** Removing a library food also removes its logged portions. */
    fun removeLibraryFood(id: String) {
        library.removeAll { it.id == id }
        logs.keys.toList().forEach { key ->
            val log = logs[key] ?: return@forEach
            if (log.portions.any { it.foodId == id }) {
                logs[key] = log.copy(portions = log.portions.filterNot { it.foodId == id })
                touchDay(key)
            }
        }
        touchSection(Section.PLAN)
    }

    fun upsertHabit(habit: Habit) {
        val i = habits.indexOfFirst { it.id == habit.id }
        if (i >= 0) habits[i] = habit else habits.add(habit)
        touchSection(Section.HABITS)
    }

    fun archiveHabit(id: String) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        habits[i] = habits[i].copy(archived = true)
        touchSection(Section.HABITS)
    }

    fun deleteHabit(id: String) {
        habits.removeAll { it.id == id }
        logs.keys.toList().forEach { key ->
            val log = logs[key] ?: return@forEach
            if (id in log.habits) {
                logs[key] = log.copy(habits = log.habits - id)
                touchDay(key)
            }
        }
        touchSection(Section.HABITS)
    }

    fun dump(text: String, today: LocalDate) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        lines.forEach { inbox.add(InboxItem(newId("in"), it, today.toString())) }
        touchSection(Section.INBOX)
    }

    fun toggleInbox(id: String) {
        val i = inbox.indexOfFirst { it.id == id }
        if (i < 0) return
        inbox[i] = inbox[i].copy(done = !inbox[i].done)
        touchSection(Section.INBOX)
    }

    fun removeInbox(id: String) {
        inbox.removeAll { it.id == id }
        touchSection(Section.INBOX)
    }

    fun clearDoneInbox() {
        inbox.removeAll { it.done }
        touchSection(Section.INBOX)
    }

    /** Turns a dumped thought into a task on [date] (or the backlog). */
    fun promoteInbox(id: String, date: LocalDate?) {
        val item = inbox.firstOrNull { it.id == id } ?: return
        inbox.remove(item)
        touchSection(Section.INBOX)
        addTask(date, Task(id = newId("task"), title = item.text))
    }

    fun updateTargets(t: Targets) {
        targets = t
        touchSection(Section.PLAN)
    }

    fun updateSettings(s: Settings) {
        settings = s
        touchSection(Section.SETTINGS)
    }

    private var idCounter = 0
    fun newId(prefix: String): String = "${prefix}_${nowMillis()}_${idCounter++}"

    /** Runs blocking file work off the main thread. */
    suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}

@Serializable
private data class PlanDoc(
    val foods: List<FoodItem> = emptyList(),
    val library: List<LibraryFood> = emptyList(),
    val targets: Targets = Targets(),
    val startDate: String = ""
)

@Serializable
private data class HabitsDoc(val habits: List<Habit> = emptyList())

@Serializable
private data class GoalsDoc(val goals: List<Goal> = emptyList())

@Serializable
private data class InboxDoc(val items: List<InboxItem> = emptyList())

@Serializable
private data class BacklogDoc(val tasks: List<Task> = emptyList())

@Serializable
private data class SettingsDoc(
    val pomodoro: PomodoroSettings = PomodoroSettings(),
    val settings: Settings = Settings()
)
