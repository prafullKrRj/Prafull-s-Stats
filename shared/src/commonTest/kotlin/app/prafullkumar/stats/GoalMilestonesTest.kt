package app.prafullkumar.stats

import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.GoalHealth
import app.prafullkumar.stats.data.GoalMilestone
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.GoalStep
import app.prafullkumar.stats.data.GoalTemplates
import app.prafullkumar.stats.data.goalPriorityName
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalMilestonesTest {

    private var ids = 0
    private val newId: (String) -> String = { p -> "${p}_${ids++}" }

    private fun steps(done: Int, total: Int) =
        List(total) { GoalStep("s$it", "Step $it", done = it < done, doneAt = if (it < done) "2026-10-05" else null) }

    private fun milestone(id: String, threshold: Double? = null, done: Boolean = false) =
        GoalMilestone(id, "M $id", metricValue = threshold, done = done, doneAt = if (done) "2026-10-01" else null)

    @Test
    fun milestoneProgress_isShareOfMilestonesDone() {
        val g = Goal("g", "G", milestones = listOf(milestone("a", done = true), milestone("b")))
        assertEquals(0.5f, g.milestoneProgress)
    }

    @Test
    fun milestoneProgress_isZeroWithoutMilestones() {
        assertEquals(0f, Goal("g", "G").milestoneProgress)
    }

    @Test
    fun milestones_doNotChangeOverallProgress() {
        val plain = Goal("g", "G", steps = steps(1, 4))
        val withMilestones = plain.copy(milestones = listOf(milestone("a", done = true), milestone("b")))
        assertEquals(plain.progress, withMilestones.progress)
    }

    @Test
    fun nextMilestone_isFirstOpenOne() {
        val g = Goal("g", "G", milestones = listOf(milestone("a", done = true), milestone("b"), milestone("c")))
        assertEquals("b", g.nextMilestone()?.id)
        assertNull(g.copy(milestones = emptyList()).nextMilestone())
    }

    @Test
    fun autoMilestones_tickWhenCountingUp() {
        val g = Goal(
            "g", "Launch", metricName = "Installs", metricTarget = 1000.0, metricCurrent = 250.0,
            milestones = listOf(milestone("a", 100.0), milestone("b", 500.0))
        ).withAutoMilestones("2026-10-14")
        assertTrue(g.milestones[0].done)
        assertEquals("2026-10-14", g.milestones[0].doneAt)
        assertFalse(g.milestones[1].done)
    }

    @Test
    fun autoMilestones_tickWhenCountingDown() {
        val g = Goal(
            "g", "Cut", metricName = "kg", metricStart = 80.0, metricTarget = 70.0, metricCurrent = 74.0,
            milestones = listOf(milestone("a", 75.0), milestone("b", 70.0))
        ).withAutoMilestones("2026-10-14")
        assertTrue(g.milestones[0].done)
        assertFalse(g.milestones[1].done)
    }

    @Test
    fun autoMilestones_neverUntick_andNeedAMetric() {
        val done = milestone("a", 100.0, done = true)
        val g = Goal("g", "G", milestones = listOf(done)).withAutoMilestones("2026-10-14")
        assertTrue(g.milestones[0].done)
        assertEquals("2026-10-01", g.milestones[0].doneAt)
        val noMetric = Goal("g", "G", milestones = listOf(milestone("b", 100.0)))
        assertFalse(noMetric.withAutoMilestones("2026-10-14").milestones[0].done)
    }

    @Test
    fun withMetric_movesNumber_recordsCheckin_andTicksMilestones() {
        val g = Goal(
            "g", "Launch", metricName = "Installs", metricTarget = 1000.0,
            milestones = listOf(milestone("a", 100.0))
        ).withMetric(150.0, "2026-10-14", newId)
        assertEquals(150.0, g.metricCurrent)
        assertEquals(1, g.checkins.size)
        assertEquals(150.0, g.checkins[0].value)
        assertEquals("2026-10-14", g.checkins[0].date)
        assertTrue(g.milestones[0].done)
    }

    @Test
    fun withMetric_sameDayBareReentry_overwritesInsteadOfSpamming() {
        var g = Goal("g", "G", metricName = "x", metricTarget = 10.0).withMetric(3.0, "2026-10-14", newId)
        g = g.withMetric(4.0, "2026-10-14", newId)
        assertEquals(1, g.checkins.size)
        assertEquals(4.0, g.checkins[0].value)
        g = g.withMetric(5.0, "2026-10-14", newId, note = "big push")
        assertEquals(2, g.checkins.size)
        g = g.withMetric(6.0, "2026-10-15", newId)
        assertEquals(3, g.checkins.size)
    }

    @Test
    fun health_doneGoal_isAchieved() {
        assertEquals(GoalHealth.DONE, Goal("g", "G", status = GoalStatus.DONE).health(LocalDate(2026, 10, 14)))
    }

    @Test
    fun health_overdueDeadline_isOverdue() {
        val g = Goal("g", "G", steps = steps(2, 4), createdAt = "2026-10-01", targetDate = "2026-10-10")
        assertEquals(GoalHealth.OVERDUE, g.health(LocalDate(2026, 10, 14)))
    }

    @Test
    fun health_withoutDates_isOpen() {
        assertEquals(GoalHealth.OPEN, Goal("g", "G", steps = steps(1, 4)).health(LocalDate(2026, 10, 14)))
    }

    @Test
    fun health_comparesProgressAgainstTimeUsed() {
        // 10-day window, 8 days used → 80% of time gone.
        fun goal(done: Int) = Goal(
            "g", "G", steps = steps(done, 10),
            createdAt = "2026-10-01", targetDate = "2026-10-11"
        )
        val today = LocalDate(2026, 10, 9)
        assertEquals(GoalHealth.ON_TRACK, goal(8).health(today))
        assertEquals(GoalHealth.NEEDS_PUSH, goal(7).health(today))
        assertEquals(GoalHealth.BEHIND, goal(2).health(today))
    }

    @Test
    fun health_finishedWork_isOnTrackEvenPastDeadline() {
        val g = Goal("g", "G", steps = steps(4, 4), createdAt = "2026-10-01", targetDate = "2026-10-10")
        assertEquals(GoalHealth.ON_TRACK, g.health(LocalDate(2026, 10, 14)))
    }

    @Test
    fun expectedProgress_isNullWithoutWindow_andClampedInsideIt() {
        assertNull(Goal("g", "G").expectedProgress(LocalDate(2026, 10, 14)))
        val g = Goal("g", "G", startDate = "2026-10-01", targetDate = "2026-10-11")
        assertEquals(0.5f, g.expectedProgress(LocalDate(2026, 10, 6)))
        assertEquals(0f, g.expectedProgress(LocalDate(2026, 9, 20)))
        assertEquals(1f, g.expectedProgress(LocalDate(2026, 11, 20)))
    }

    @Test
    fun daysLeft_countsDown_andGoesNegativeWhenOverdue() {
        val g = Goal("g", "G", targetDate = "2026-10-20")
        assertEquals(6, g.daysLeft(LocalDate(2026, 10, 14)))
        assertEquals(-4, g.daysLeft(LocalDate(2026, 10, 24)))
        assertNull(Goal("g", "G").daysLeft(LocalDate(2026, 10, 14)))
    }

    @Test
    fun duplicate_keepsPlan_resetsScoreboard() {
        val g = Goal(
            "g", "Launch", status = GoalStatus.DONE, pinned = true, priority = 3,
            steps = steps(2, 3), milestones = listOf(milestone("a", done = true)),
            metricName = "Installs", metricStart = 0.0, metricTarget = 1000.0, metricCurrent = 800.0,
            createdAt = "2026-09-01", doneAt = "2026-10-01"
        ).withMetric(800.0, "2026-10-01", newId)
        val copy = g.duplicate(newId, "2026-10-14")
        assertEquals("Launch (copy)", copy.title)
        assertEquals(GoalStatus.ACTIVE, copy.status)
        assertEquals(3, copy.steps.size)
        assertTrue(copy.steps.none { it.done })
        assertEquals(1, copy.milestones.size)
        assertTrue(copy.milestones.none { it.done })
        assertEquals(0.0, copy.metricCurrent)
        assertTrue(copy.checkins.isEmpty())
        assertEquals("2026-10-14", copy.createdAt)
        assertNull(copy.doneAt)
        assertFalse(copy.pinned)
        assertTrue(copy.id != g.id && copy.steps.all { s -> g.steps.none { it.id == s.id } })
    }

    @Test
    fun timeline_isNewestFirst_andStartsAtCreation() {
        val g = Goal(
            "g", "G", createdAt = "2026-10-01", steps = steps(1, 2),
            milestones = listOf(milestone("a", done = true))
        ).withMetric(5.0, "2026-10-14", newId, note = "moving")
        val timeline = g.timeline()
        assertEquals(listOf("2026-10-14", "2026-10-05", "2026-10-01", "2026-10-01"), timeline.map { it.date })
        assertTrue(timeline.any { it.text.contains("created") })
        assertTrue(timeline.first().text.contains("moving"))
    }

    @Test
    fun priorityNames_coverWholeScale() {
        assertEquals("None", goalPriorityName(0))
        assertEquals("Low", goalPriorityName(1))
        assertEquals("Medium", goalPriorityName(2))
        assertEquals("High", goalPriorityName(3))
    }

    @Test
    fun templates_shipWithMilestones() {
        val g = GoalTemplates.publishAndroidApp.build("2026-10-14", newId)
        assertEquals(5, g.milestones.size)
        assertTrue(g.milestones.any { it.metricValue == 1000.0 })
        assertTrue(GoalTemplates.all.all { it.build("2026-10-14", newId).milestones.isNotEmpty() })
    }
}
