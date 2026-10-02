package app.prafullkumar.stats.data

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
    val metricName: String = "",
    val metricStart: Double = 0.0,
    val metricCurrent: Double = 0.0,
    val metricTarget: Double = 0.0,
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

    /**
     * 0..1 overall. With both a number and steps, each counts half — steps are
     * the plan, the number is the outcome, and neither alone tells the story.
     */
    val progress: Float
        get() = when {
            status == GoalStatus.DONE -> 1f
            hasMetric && steps.isNotEmpty() -> (metricProgress + stepProgress) / 2f
            hasMetric -> metricProgress
            else -> stepProgress
        }

    fun nextStep(): GoalStep? = steps.firstOrNull { !it.done }
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
