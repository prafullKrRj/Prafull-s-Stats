package app.prafullkumar.stats

import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.GoalStatus
import app.prafullkumar.stats.data.GoalStep
import kotlin.test.Test
import kotlin.test.assertEquals

class GoalProgressTest {

    private fun steps(done: Int, total: Int) = List(total) { GoalStep("s$it", "Step $it", done = it < done) }

    @Test
    fun stepsOnly_isShareOfStepsDone() {
        assertEquals(0.25f, Goal("g", "G", steps = steps(1, 4)).progress)
    }

    @Test
    fun metricOnly_isShareOfDistanceCovered_evenWhenTargetIsBelowStart() {
        val g = Goal("g", "Cut", metricName = "kg", metricStart = 80.0, metricTarget = 70.0, metricCurrent = 75.0)
        assertEquals(0.5f, g.progress)
    }

    @Test
    fun metricAndSteps_countHalfEach() {
        val g = Goal("g", "Launch", steps = steps(2, 4), metricName = "Installs", metricTarget = 1000.0, metricCurrent = 0.0)
        assertEquals(0.25f, g.progress)
    }

    @Test
    fun doneGoal_isFull_andMetricNeverOvershoots() {
        assertEquals(1f, Goal("g", "G", status = GoalStatus.DONE).progress)
        assertEquals(1f, Goal("g", "G", metricName = "x", metricTarget = 10.0, metricCurrent = 50.0).progress)
    }
}
