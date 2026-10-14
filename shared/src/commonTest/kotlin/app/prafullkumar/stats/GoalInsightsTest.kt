package app.prafullkumar.stats

import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.GoalCheckin
import app.prafullkumar.stats.data.GoalHealth
import app.prafullkumar.stats.data.GoalMilestone
import app.prafullkumar.stats.data.GoalStep
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalInsightsTest {

    private fun checkin(date: String, value: Double? = null, note: String = "") =
        GoalCheckin("c$date$value$note", date, value, note)

    @Test
    fun displayIcon_prefersCustomIcon_overAreaEmoji() {
        assertEquals("🔥", Goal("g", "G", icon = "🔥").displayIcon())
        assertEquals(Goal("g", "G").area.emoji, Goal("g", "G").displayIcon())
    }

    @Test
    fun requiredPace_spreadsRemainingWork_overDaysLeft() {
        val g = Goal(
            "g", "Launch", metricName = "Installs", metricTarget = 1000.0, metricCurrent = 400.0,
            targetDate = "2026-10-24"
        )
        assertEquals(60.0, g.requiredDailyPace(LocalDate(2026, 10, 14)))
    }

    @Test
    fun requiredPace_countsDownGoals_readNegative() {
        val g = Goal(
            "g", "Cut", metricName = "kg", metricStart = 80.0, metricTarget = 70.0, metricCurrent = 75.0,
            targetDate = "2026-10-24"
        )
        assertEquals(-0.5, g.requiredDailyPace(LocalDate(2026, 10, 14)))
    }

    @Test
    fun requiredPace_isZeroWhenThere_andNullWithoutRunway() {
        val done = Goal("g", "G", metricName = "x", metricTarget = 10.0, metricCurrent = 12.0, targetDate = "2026-10-24")
        assertEquals(0.0, done.requiredDailyPace(LocalDate(2026, 10, 14)))
        assertNull(Goal("g", "G").requiredDailyPace(LocalDate(2026, 10, 14)))
        val noDate = Goal("g", "G", metricName = "x", metricTarget = 10.0, metricCurrent = 1.0)
        assertNull(noDate.requiredDailyPace(LocalDate(2026, 10, 14)))
        val overdue = noDate.copy(targetDate = "2026-10-10")
        assertNull(overdue.requiredDailyPace(LocalDate(2026, 10, 14)))
    }

    @Test
    fun projectedFinish_extrapolatesRecentPace() {
        // +10/day for 10 days, 800 to go → 80 days from today.
        val g = Goal(
            "g", "Launch", metricName = "Installs", metricTarget = 1000.0, metricCurrent = 200.0,
            checkins = listOf(checkin("2026-10-04", 100.0), checkin("2026-10-14", 200.0))
        )
        assertEquals(LocalDate(2027, 1, 2), g.projectedFinish(LocalDate(2026, 10, 14)))
    }

    @Test
    fun projectedFinish_isNull_withoutReadableTrend() {
        val base = Goal("g", "G", metricName = "x", metricTarget = 100.0, metricCurrent = 50.0)
        assertNull(base.projectedFinish(LocalDate(2026, 10, 14)))
        assertNull(base.copy(checkins = listOf(checkin("2026-10-14", 50.0))).projectedFinish(LocalDate(2026, 10, 14)))
        // flat
        assertNull(
            base.copy(checkins = listOf(checkin("2026-10-10", 50.0), checkin("2026-10-14", 50.0)))
                .projectedFinish(LocalDate(2026, 10, 14))
        )
        // moving away
        assertNull(
            base.copy(checkins = listOf(checkin("2026-10-10", 60.0), checkin("2026-10-14", 50.0)))
                .projectedFinish(LocalDate(2026, 10, 14))
        )
        // stale points outside the 30-day window
        assertNull(
            base.copy(checkins = listOf(checkin("2026-08-01", 10.0), checkin("2026-09-01", 40.0)))
                .projectedFinish(LocalDate(2026, 10, 14))
        )
    }

    @Test
    fun activityStreak_countsConsecutiveActiveDays() {
        val g = Goal(
            "g", "G",
            checkins = listOf(checkin("2026-10-14"), checkin("2026-10-13"), checkin("2026-10-11")),
            steps = listOf(GoalStep("s", "S", done = true, doneAt = "2026-10-12")),
            milestones = listOf(GoalMilestone("m", "M", done = true, doneAt = "2026-10-10"))
        )
        // 14, 13, 12, then gap at… 12 present via step, 11 present, 10 via milestone → 5 straight.
        assertEquals(5, g.activityStreak(LocalDate(2026, 10, 14)))
    }

    @Test
    fun activityStreak_givesTodayGrace_andZeroWhenIdle() {
        val g = Goal("g", "G", checkins = listOf(checkin("2026-10-13"), checkin("2026-10-12")))
        assertEquals(2, g.activityStreak(LocalDate(2026, 10, 14)))
        assertEquals(0, Goal("g", "G").activityStreak(LocalDate(2026, 10, 14)))
        val stale = Goal("g", "G", checkins = listOf(checkin("2026-10-01")))
        assertEquals(0, stale.activityStreak(LocalDate(2026, 10, 14)))
    }

    @Test
    fun healthAttention_ordersOverdueFirst_andDoneLast() {
        val ordered = GoalHealth.entries.sortedBy { it.attention }
        assertEquals(GoalHealth.OVERDUE, ordered.first())
        assertEquals(GoalHealth.DONE, ordered.last())
        assertTrue(GoalHealth.BEHIND.attention < GoalHealth.ON_TRACK.attention)
    }
}
