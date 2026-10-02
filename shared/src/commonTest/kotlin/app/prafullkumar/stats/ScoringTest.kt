package app.prafullkumar.stats

import app.prafullkumar.stats.data.DayLog
import app.prafullkumar.stats.data.DayTotals
import app.prafullkumar.stats.data.FocusSession
import app.prafullkumar.stats.data.FoodItem
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.Scoring
import app.prafullkumar.stats.data.Slot
import app.prafullkumar.stats.data.Streak
import app.prafullkumar.stats.data.Targets
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.minusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringTest {

    private val today = LocalDate(2026, 10, 2)
    private fun days(n: Int) = (n - 1 downTo 0).map { today.minusDays(it) }

    @Test
    fun calorieFit_isFullInsideBand_andZeroWhenWayOver() {
        assertEquals(1f, Scoring.calorieFit(2000, 2000))
        assertEquals(1f, Scoring.calorieFit(1800, 2000))
        assertEquals(0.5f, Scoring.calorieFit(900, 2000), 0.001f)
        assertEquals(0f, Scoring.calorieFit(2600, 2000))
        assertEquals(0f, Scoring.calorieFit(0, 2000))
    }

    @Test
    fun streak_todayNotDoneYet_keepsCurrentAlive() {
        val done = days(5).dropLast(1).toSet()
        assertEquals(Streak(4, 4), Scoring.streak(days(5), today) { it in done })
    }

    @Test
    fun streak_missedYesterday_resetsCurrent_butKeepsBest() {
        val d = days(6)
        val done = setOf(d[0], d[1], d[2], d[5])
        assertEquals(Streak(1, 3), Scoring.streak(d, today) { it in done })
    }

    @Test
    fun streak_unscheduledDaysAreSkipped() {
        val d = days(4)
        assertEquals(Streak(3, 3), Scoring.streak(d, today) { if (it == d[1]) null else true })
    }

    @Test
    fun dayScore_perfectDay_is100_withEveryPart() {
        val targets = Targets(kcal = 2000, protein = 100, fiber = 30, water = 10, sleep = 8.0, focusMinutes = 60)
        val core = FoodItem("a", "Milk", "", Slot.BREAKFAST, 150, 8.0, 12.0, 8.0, core = true)
        val habit = Habit("h", "Read", target = 20, createdAt = today.toString())
        val log = DayLog(
            date = today.toString(),
            foods = setOf("a"),
            habits = mapOf("h" to 25),
            tasks = listOf(Task("t", "Ship", top = true, done = true)),
            sessions = listOf(FocusSession(0, 90)),
            water = 10,
            sleep = 8.0
        )
        val score = Scoring.dayScore(log, DayTotals(2000, 120.0, 0.0, 0.0, 35.0), targets, listOf(core), listOf(habit))
        assertEquals(100, score.score)
        assertEquals(
            listOf("Protein", "Calories", "Fiber", "Core foods", "Habits", "Tasks", "Top 3", "Water", "Focus", "Sleep"),
            score.parts.map { it.label }
        )
    }

    @Test
    fun dayScore_leavesOutPartsWithNothingToMeasure() {
        val targets = Targets(kcal = 2000, protein = 100, fiber = 30, water = 10, sleep = 0.0, focusMinutes = 0)
        val score = Scoring.dayScore(DayLog(today.toString()), DayTotals(), targets, emptyList(), emptyList())
        assertEquals(listOf("Protein", "Calories", "Fiber", "Water"), score.parts.map { it.label })
        assertEquals(0, score.score)
    }

    @Test
    fun dayScore_halfTheTasksDone_countsHalf() {
        val targets = Targets(kcal = 0, protein = 0, fiber = 0, water = 0, sleep = 0.0, focusMinutes = 0)
        val log = DayLog(today.toString(), tasks = listOf(Task("a", "A", done = true), Task("b", "B")))
        assertEquals(50, Scoring.dayScore(log, DayTotals(), targets, emptyList(), emptyList()).score)
    }
}
