package app.prafullkumar.stats.data

import java.time.LocalDate
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
     * Parts with nothing to measure (no core foods, no habits scheduled, no
     * priorities set) are left out instead of counting as zero.
     */
    fun dayScore(
        log: DayLog,
        totals: DayTotals,
        targets: Targets,
        plan: List<FoodItem>,
        scheduledHabits: List<Habit>
    ): DayScore {
        val parts = mutableListOf<ScorePart>()
        parts += ScorePart("Protein", ratio(totals.protein, targets.protein.toDouble()))
        parts += ScorePart("Calories", calorieFit(totals.kcal, targets.kcal))
        parts += ScorePart("Fiber", ratio(totals.fiber, targets.fiber.toDouble()))

        val core = plan.filter { it.core }
        if (core.isNotEmpty()) {
            parts += ScorePart(
                "Core foods",
                core.count { it.id in log.foods }.toFloat() / core.size
            )
        }
        if (scheduledHabits.isNotEmpty()) {
            val sum = scheduledHabits.sumOf {
                ratio(log.habitCount(it.id).toDouble(), it.target.toDouble()).toDouble()
            }
            parts += ScorePart("Habits", (sum / scheduledHabits.size).toFloat())
        }
        parts += ScorePart("Water", ratio(log.water.toDouble(), targets.water.toDouble()))
        if (log.priorities.isNotEmpty()) {
            parts += ScorePart(
                "Top 3",
                log.priorities.count { it.done }.toFloat() / log.priorities.size
            )
        }
        if (targets.focusMinutes > 0) {
            parts += ScorePart(
                "Focus",
                ratio(log.focusMinutes.toDouble(), targets.focusMinutes.toDouble())
            )
        }
        if (targets.sleep > 0) {
            parts += ScorePart("Sleep", ratio(log.sleep ?: 0.0, targets.sleep))
        }

        val score = (parts.map { it.value }.average() * 100).roundToInt()
        return DayScore(score, parts)
    }

    /**
     * Current and best run over [days] (ascending, ending today).
     * [isDone] returns null for days that do not count either way (habit not
     * scheduled). Today not done yet keeps the current streak alive — the day
     * is not over.
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
