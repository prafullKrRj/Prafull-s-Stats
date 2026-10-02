package app.prafullkumar.stats.data

import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * Pure scoring rules, kept free of storage so they can be unit tested.
 */
object Scoring {

    /**
     * 1.0 when calories land between 90 % and 105 % of target. Under-eating
     * ramps up linearly; overshooting drops to zero by 130 %.
     */
    fun calorieFit(kcal: Int, target: Int): Float {
        if (kcal <= 0 || target <= 0) return 0f
        val r = kcal.toFloat() / target
        return when {
            r < 0.9f -> r / 0.9f
            r <= 1.05f -> 1f
            else -> (1f - (r - 1.05f) * 4f).coerceAtLeast(0f)
        }
    }

    private fun ratio(value: Double, target: Double): Float =
        if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

    /**
     * Day score 0–100: the average of every part that applies that day.
     * Parts with nothing to measure (no plan, no habits due, no tasks) are
     * left out instead of counting as zero. Nutrition only counts once a
     * plan or a calorie target exists.
     */
    fun dayScore(
        log: DayLog,
        totals: DayTotals,
        targets: Targets,
        plan: List<FoodItem>,
        scheduledHabits: List<Habit>
    ): DayScore {
        val parts = mutableListOf<ScorePart>()
        if (targets.protein > 0) parts += ScorePart("Protein", ratio(totals.protein, targets.protein.toDouble()))
        if (targets.kcal > 0) parts += ScorePart("Calories", calorieFit(totals.kcal, targets.kcal))
        if (targets.fiber > 0) parts += ScorePart("Fiber", ratio(totals.fiber, targets.fiber.toDouble()))

        val core = plan.filter { it.core }
        if (core.isNotEmpty()) {
            parts += ScorePart("Core foods", core.count { it.id in log.foods }.toFloat() / core.size)
        }
        if (scheduledHabits.isNotEmpty()) {
            val sum = scheduledHabits.sumOf {
                ratio(log.habitCount(it.id).toDouble(), it.target.toDouble()).toDouble()
            }
            parts += ScorePart("Habits", (sum / scheduledHabits.size).toFloat())
        }
        if (log.tasks.isNotEmpty()) {
            parts += ScorePart("Tasks", log.tasks.count { it.done }.toFloat() / log.tasks.size)
        }
        val top = log.topTasks
        if (top.isNotEmpty()) {
            parts += ScorePart("Top 3", top.count { it.done }.toFloat() / top.size)
        }
        if (targets.water > 0) parts += ScorePart("Water", ratio(log.water.toDouble(), targets.water.toDouble()))
        if (targets.focusMinutes > 0) {
            parts += ScorePart("Focus", ratio(log.totalFocus.toDouble(), targets.focusMinutes.toDouble()))
        }
        if (targets.sleep > 0) parts += ScorePart("Sleep", ratio(log.sleep ?: 0.0, targets.sleep))

        if (parts.isEmpty()) return DayScore(0, parts)
        val score = (parts.map { it.value }.average() * 100).roundToInt()
        return DayScore(score, parts)
    }

    /**
     * Current and best run over [days] (ascending, ending today).
     * [isDone] returns null for days that do not count either way (habit not
     * due). Today not done yet keeps the current streak alive — the day is
     * not over.
     */
    fun streak(days: List<LocalDate>, today: LocalDate, isDone: (LocalDate) -> Boolean?): Streak {
        var best = 0
        var run = 0
        days.forEach { day ->
            when (isDone(day)) {
                true -> {
                    run++
                    best = maxOf(best, run)
                }
                false -> if (day != today) run = 0
                null -> Unit
            }
        }

        var current = 0
        for (day in days.asReversed()) {
            when (isDone(day)) {
                true -> current++
                false -> if (day != today) break
                null -> Unit
            }
        }
        return Streak(current, best)
    }
}
