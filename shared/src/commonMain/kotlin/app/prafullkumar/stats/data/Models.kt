package app.prafullkumar.stats.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Meal slots of the day, in the order they happen. */
@Serializable
enum class Slot(val title: String) {
    WAKE("Wake up"),
    BREAKFAST("Breakfast"),
    MID_MORNING("Mid-morning"),
    LUNCH("Lunch"),
    PRE_WORKOUT("Pre-workout"),
    POST_WORKOUT("Post-workout"),
    DINNER("Dinner"),
    BEDTIME("Before bed")
}

/**
 * One item of the daily plan — the food eaten almost every day. Ticked off on
 * the checklist instead of being re-entered. [core] items are the must-haves
 * (usually the protein base) and count toward the day score on their own.
 */
@Serializable
data class FoodItem(
    val id: String,
    val name: String,
    val detail: String = "",
    val slot: Slot = Slot.LUNCH,
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val core: Boolean = false
)

/** Off-plan food. Nutrition is stored per 100 g / 100 ml so logging only asks for the quantity. */
@Serializable
data class LibraryFood(
    val id: String,
    val name: String,
    /** "g" or "ml" */
    val unit: String = "g",
    @SerialName("kcal100") val kcalPer100: Double = 0.0,
    @SerialName("protein100") val proteinPer100: Double = 0.0,
    @SerialName("carbs100") val carbsPer100: Double = 0.0,
    @SerialName("fat100") val fatPer100: Double = 0.0,
    @SerialName("fiber100") val fiberPer100: Double = 0.0
)

/** How much of a library food was eaten on one day. */
@Serializable
data class Portion(val foodId: String, val qty: Double)

/**
 * A habit to keep a streak on. [target] 1 means a plain checkbox; anything
 * higher is a counter (e.g. 20 pages). [days] holds ISO days of week
 * (1 = Monday) it is due on — other days never break the streak.
 */
@Serializable
data class Habit(
    val id: String,
    val name: String,
    /** Why this habit matters — shown on the habit so the reason is never forgotten. */
    @SerialName("detail") val why: String = "",
    val target: Int = 1,
    val unit: String = "",
    val days: Set<Int> = ALL_DAYS,
    val goalId: String? = null,
    val createdAt: String = "",
    val archived: Boolean = false
) {
    val isCounter: Boolean get() = target > 1

    companion object {
        val ALL_DAYS = (1..7).toSet()
    }
}

/** A thought dumped out of the head, triaged later. */
@Serializable
data class InboxItem(
    val id: String,
    val text: String,
    val createdAt: String = "",
    val done: Boolean = false
)

@Serializable
enum class GoalArea(val title: String, val emoji: String) {
    APPS("App launch", "🚀"),
    CAREER("Career", "💼"),
    HEALTH("Health", "💪"),
    LEARNING("Learning", "📚"),
    MONEY("Money", "💰"),
    MIND("Mind", "🧘"),
    PERSONAL("Personal", "✨")
}

@Serializable
enum class GoalStatus(val title: String) { ACTIVE("Active"), PAUSED("Paused"), DONE("Done"), DROPPED("Dropped") }

/** One concrete step toward a goal. */
@Serializable
data class GoalStep(
    val id: String,
    val title: String,
    val why: String = "",
    val done: Boolean = false,
    val doneAt: String? = null,
    val due: String? = null
)

/**
 * A big checkpoint on the way to a goal — "MVP built", "First 100 installs".
 * Smaller than the goal, bigger than a step. When [metricValue] is set and the
 * goal tracks a number, the milestone ticks itself once the number passes it.
 */
@Serializable
data class GoalMilestone(
    val id: String,
    val title: String,
    val why: String = "",
    val due: String? = null,
    /** Metric value that auto-completes this milestone. Null = tick by hand. */
    val metricValue: Double? = null,
    val done: Boolean = false,
    val doneAt: String? = null
)

/** One logged progress update: a metric snapshot, a note, or both. */
@Serializable
data class GoalCheckin(
    val id: String,
    /** "YYYY-MM-DD". */
    val date: String,
    /** Metric snapshot at check-in time. Null when the goal has no number. */
    val value: Double? = null,
    val note: String = ""
)

/** Pace of a goal against its own deadline. */
enum class GoalHealth(val title: String, val emoji: String) {
    DONE("Achieved", "🏆"),
    ON_TRACK("On track", "🟢"),
    NEEDS_PUSH("Needs push", "🟡"),
    BEHIND("Behind", "🔴"),
    OVERDUE("Overdue", "⏰"),
    OPEN("Open", "⚪")
}

/** One row of a goal's story: created, steps, milestones, check-ins. */
data class GoalEvent(val date: String, val text: String, val emoji: String)

/** 0 none, 1 low, 2 medium, 3 high — same scale as [Task.priority]. */
fun goalPriorityName(priority: Int): String = when (priority) {
    3 -> "High"
    2 -> "Medium"
    1 -> "Low"
    else -> "None"
}

/**
 * Something worth chasing for weeks or months. Progress comes from a number
 * ([metricTarget], e.g. 1 000 installs) when one is set, else from steps.
 */
@Serializable
data class Goal(
    val id: String,
    val title: String,
    /** The reason. Shown above everything else on the goal. */
    val why: String = "",
    val area: GoalArea = GoalArea.PERSONAL,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val steps: List<GoalStep> = emptyList(),
    val milestones: List<GoalMilestone> = emptyList(),
    val checkins: List<GoalCheckin> = emptyList(),
    /** 0 none, 1 low, 2 medium, 3 high. */
    val priority: Int = 0,
    val metricName: String = "",
    val metricStart: Double = 0.0,
    val metricCurrent: Double = 0.0,
    val metricTarget: Double = 0.0,
    /** "YYYY-MM-DD". Falls back to [createdAt] for pacing when unset. */
    val startDate: String? = null,
    val targetDate: String? = null,
    val createdAt: String = "",
    val doneAt: String? = null,
    val notes: String = "",
    val pinned: Boolean = false
) {
    val hasMetric: Boolean get() = metricTarget != metricStart && metricName.isNotBlank()

    /** 0..1 share of the number moved from start to target. */
    val metricProgress: Float
        get() = if (!hasMetric) 0f
        else ((metricCurrent - metricStart) / (metricTarget - metricStart)).toFloat().coerceIn(0f, 1f)

    /** 0..1 share of steps ticked. */
    val stepProgress: Float
        get() = if (steps.isEmpty()) 0f else steps.count { it.done }.toFloat() / steps.size

    /** 0..1 share of milestones reached. */
    val milestoneProgress: Float
        get() = if (milestones.isEmpty()) 0f else milestones.count { it.done }.toFloat() / milestones.size

    /**
     * 0..1 overall. With both a number and steps, each counts half — steps are
     * the plan, the number is the outcome, and neither alone tells the story.
     * Milestones stay out of the number: they celebrate the journey, they don't
     * grade it.
     */
    val progress: Float
        get() = when {
            status == GoalStatus.DONE -> 1f
            hasMetric && steps.isNotEmpty() -> (metricProgress + stepProgress) / 2f
            hasMetric -> metricProgress
            else -> stepProgress
        }

    fun nextStep(): GoalStep? = steps.firstOrNull { !it.done }

    fun nextMilestone(): GoalMilestone? = milestones.firstOrNull { !it.done }

    fun startOrCreated(): LocalDate? = parseDate(startDate) ?: parseDate(createdAt)

    /** Days from [today] to the deadline; negative when overdue, null without one. */
    fun daysLeft(today: LocalDate): Int? = parseDate(targetDate)?.let { today.daysTo(it) }

    /**
     * Share of allotted time used up, 0..1. Null without a start and deadline
     * to pace against.
     */
    fun expectedProgress(today: LocalDate): Float? {
        val start = startOrCreated() ?: return null
        val end = parseDate(targetDate) ?: return null
        val total = start.daysTo(end)
        if (total <= 0) return 1f
        return start.daysTo(today).coerceIn(0, total).toFloat() / total
    }

    /** Pace against the deadline, from the gap between progress and time used. */
    fun health(today: LocalDate): GoalHealth {
        if (status == GoalStatus.DONE) return GoalHealth.DONE
        if (progress >= 1f) return GoalHealth.ON_TRACK
        val deadline = parseDate(targetDate)
        if (deadline != null && deadline < today) return GoalHealth.OVERDUE
        val expected = expectedProgress(today) ?: return GoalHealth.OPEN
        val gap = progress - expected
        return when {
            gap >= -0.05f -> GoalHealth.ON_TRACK
            gap >= -0.20f -> GoalHealth.NEEDS_PUSH
            else -> GoalHealth.BEHIND
        }
    }

    /**
     * Copy with the metric moved to [value]: today's check-in is recorded (a
     * bare re-entry on the same day overwrites instead of spamming history)
     * and metric-linked milestones tick themselves.
     */
    fun withMetric(value: Double, date: String, newId: (String) -> String, note: String = ""): Goal {
        val moved = copy(metricCurrent = value)
        val last = checkins.lastOrNull()
        val withCheckin = if (last != null && last.date == date && last.note.isBlank() && note.isBlank()) {
            moved.copy(checkins = checkins.dropLast(1) + last.copy(value = value))
        } else {
            moved.copy(checkins = checkins + GoalCheckin(newId("checkin"), date, value, note.trim()))
        }
        return withCheckin.withAutoMilestones(date)
    }

    /**
     * Copy with milestones whose metric threshold [metricCurrent] has reached
     * marked done. Direction-aware (losing kg counts down); never unticks.
     */
    fun withAutoMilestones(date: String): Goal {
        if (!hasMetric) return this
        val down = metricTarget < metricStart
        var changed = false
        val updated = milestones.map { m ->
            val threshold = m.metricValue
            if (!m.done && threshold != null && (if (down) metricCurrent <= threshold else metricCurrent >= threshold)) {
                changed = true
                m.copy(done = true, doneAt = date)
            } else m
        }
        return if (changed) copy(milestones = updated) else this
    }

    /** Fresh copy to chase again: same plan, blank scoreboard. */
    fun duplicate(newId: (String) -> String, today: String): Goal = copy(
        id = newId("goal"),
        title = "$title (copy)",
        status = GoalStatus.ACTIVE,
        steps = steps.map { it.copy(id = newId("step"), done = false, doneAt = null) },
        milestones = milestones.map { it.copy(id = newId("milestone"), done = false, doneAt = null) },
        checkins = emptyList(),
        metricCurrent = metricStart,
        createdAt = today,
        doneAt = null,
        pinned = false
    )

    /** Newest-first story of the goal: creation, steps, milestones, check-ins. */
    fun timeline(): List<GoalEvent> = buildList {
        if (createdAt.isNotBlank()) add(GoalEvent(createdAt, "Goal created", "🌱"))
        steps.filter { it.done && it.doneAt != null }.forEach {
            add(GoalEvent(it.doneAt!!, "Step done: ${it.title}", "✅"))
        }
        milestones.filter { it.done }.forEach {
            add(GoalEvent(it.doneAt ?: "", "Milestone reached: ${it.title}", "🏁"))
        }
        checkins.forEach { c ->
            val snapshot = c.value?.let {
                "${formatNumber(it)}${if (metricName.isNotBlank()) " $metricName" else ""}"
            } ?: "Check-in"
            add(GoalEvent(c.date, if (c.note.isBlank()) snapshot else "$snapshot — ${c.note}", "📝"))
        }
        if (status == GoalStatus.DONE && doneAt != null) add(GoalEvent(doneAt, "Goal achieved", "🏆"))
    }.sortedWith(compareByDescending<GoalEvent> { it.date }.thenBy { it.text })
}

/** 0 none, 1 low, 2 medium, 3 high. */
@Serializable
data class Task(
    val id: String,
    val title: String,
    val why: String = "",
    val notes: String = "",
    val priority: Int = 0,
    /** One of the day's Top 3 — the "if only this gets done" list. */
    val top: Boolean = false,
    /** "HH:mm" if the task is time-blocked. */
    val time: String? = null,
    val estimate: Int = 1,
    val pomodoros: Int = 0,
    val goalId: String? = null,
    val stepId: String? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long = 0
)

/** One finished stretch of focused work. */
@Serializable
data class FocusSession(
    val start: Long,
    val minutes: Int,
    val label: String = "",
    val taskId: String? = null,
    val goalId: String? = null,
    /** True when the full pomodoro ran out, false when stopped early. */
    val complete: Boolean = true
)

/** Everything logged for one calendar day. Also holds the tasks scheduled on it. */
@Serializable
data class DayLog(
    val date: String,
    val foods: Set<String> = emptySet(),
    val portions: List<Portion> = emptyList(),
    /** Habit id -> count done that day (1 for a checkbox habit). */
    val habits: Map<String, Int> = emptyMap(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<FocusSession> = emptyList(),
    val weight: Double? = null,
    val water: Int = 0,
    val sleep: Double? = null,
    /** 1..5, 0 = not set. */
    val mood: Int = 0,
    /** 1..5, 0 = not set. */
    val energy: Int = 0,
    /** Minutes logged before sessions were tracked one by one. */
    val focusMinutes: Int = 0,
    val gratitude: String = "",
    val win: String = "",
    val lesson: String = "",
    val tomorrow: String = "",
    val note: String = ""
) {
    fun habitCount(id: String): Int = habits[id] ?: 0
    val totalFocus: Int get() = focusMinutes + sessions.sumOf { it.minutes }
    val completedPomodoros: Int get() = sessions.count { it.complete }
    val topTasks: List<Task> get() = tasks.filter { it.top }
    val isEmpty: Boolean get() = this == DayLog(date)
}

@Serializable
data class Targets(
    val kcal: Int = 2200,
    val protein: Int = 140,
    val fat: Int = 65,
    val carbs: Int = 230,
    val fiber: Int = 30,
    /** Glasses of ~250 ml. */
    val water: Int = 12,
    val goalWeight: Double = 70.0,
    val sleep: Double = 7.5,
    val focusMinutes: Int = 180,
    /** Day score (0–100) needed for a day to count toward the beast streak. */
    val beastScore: Int = 80
)

@Serializable
data class PomodoroSettings(
    val work: Int = 25,
    val shortBreak: Int = 5,
    val longBreak: Int = 15,
    val longEvery: Int = 4,
    val autoStartBreaks: Boolean = true,
    val autoStartWork: Boolean = false
)

@Serializable
enum class Phase(val title: String) { WORK("Focus"), SHORT_BREAK("Short break"), LONG_BREAK("Long break") }

/**
 * The live pomodoro. Stored with wall-clock end time so it survives restarts
 * and shows the same countdown on every synced device.
 */
@Serializable
data class TimerState(
    val phase: Phase = Phase.WORK,
    /** Epoch millis the phase ends; 0 when not running. */
    val endsAt: Long = 0,
    /** Seconds left while paused; 0 when not paused. */
    val pausedLeft: Long = 0,
    /** Length of the current phase in minutes. */
    val length: Int = 25,
    /** Work sessions finished in the current cycle (resets after a long break). */
    val cycle: Int = 0,
    val taskId: String? = null,
    val label: String = ""
) {
    val running: Boolean get() = endsAt > 0
    val paused: Boolean get() = pausedLeft > 0
    val idle: Boolean get() = !running && !paused

    fun secondsLeft(now: Long): Long = when {
        running -> ((endsAt - now) / 1000).coerceAtLeast(0)
        paused -> pausedLeft
        else -> length * 60L
    }
}

@Serializable
enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val name: String = "Prafull"
)

data class DayTotals(
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
)

/** One scored part of the day, e.g. "Protein 0.8". */
data class ScorePart(val label: String, val value: Float)

data class DayScore(val score: Int, val parts: List<ScorePart>)

data class Streak(val current: Int, val best: Int)
