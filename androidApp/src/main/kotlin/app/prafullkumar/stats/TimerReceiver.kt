package app.prafullkumar.stats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.prafullkumar.stats.data.StatsRepo

/** Fires when a pomodoro phase should end; closes it even if the app was not open. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StatsApplication.ensureStarted(context)
        StatsRepo.tick()
        StatsRepo.flush()
    }
}
