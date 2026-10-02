package app.prafullkumar.stats

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.prafullkumar.stats.data.Phase
import app.prafullkumar.stats.data.PlatformHooks
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Storage
import app.prafullkumar.stats.data.TimerState
import app.prafullkumar.stats.data.clock
import app.prafullkumar.stats.data.nowMillis
import app.prafullkumar.stats.sync.CloudSync
import java.io.File

/** Loads the store once per process so the activity and the timer alarm share it. */
class StatsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureStarted(this)
    }

    companion object {
        fun ensureStarted(context: Context) {
            val app = context.applicationContext
            val storage = FileStorage(app.filesDir)
            StatsRepo.init(storage)
            StatsRepo.hooks = AndroidHooks(app)
            CloudSync.init(storage)
            createChannels(app)
        }

        private fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TIMER, "Focus timer", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "The running pomodoro countdown" }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "Timer finished", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "When a focus session or break ends" }
            )
        }
    }
}

const val CHANNEL_TIMER = "timer"
const val CHANNEL_ALERTS = "alerts"
private const val NOTIF_TIMER = 1
private const val NOTIF_DONE = 2

class FileStorage(private val dir: File) : Storage {
    override fun read(name: String): String? =
        File(dir, name).takeIf { it.exists() }?.readText()

    /** Write to a temp file then rename, so a crash mid-write never leaves half a file. */
    override fun write(name: String, text: String) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(dir, name))
    }
}

/**
 * Pomodoro on Android: an ongoing notification counts down by itself, and an
 * alarm wakes the app at the end so the phase closes even if it was killed.
 */
class AndroidHooks(private val context: Context) : PlatformHooks {

    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, TimerReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun canNotify() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun timerChanged(timer: TimerState) {
        val nm = NotificationManagerCompat.from(context)
        alarms.cancel(alarmIntent())
        if (timer.idle) {
            nm.cancel(NOTIF_TIMER)
            return
        }
        if (timer.running) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endsAt, alarmIntent())
        }
        if (!canNotify()) return
        val title = timer.label.ifBlank { timer.phase.title }
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (timer.running) "${timer.phase.title} · $title" else "Paused · $title")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        if (timer.running) {
            builder.setUsesChronometer(true).setChronometerCountDown(true).setWhen(timer.endsAt).setShowWhen(true)
        } else {
            builder.setContentText("${clock(timer.secondsLeft(nowMillis()))} left")
        }
        runCatching { nm.notify(NOTIF_TIMER, builder.build()) }
    }

    override fun phaseFinished(finished: Phase, next: Phase, autoStarted: Boolean) {
        if (!canNotify()) return
        val text = when (finished) {
            Phase.WORK -> if (autoStarted) "Nice. ${next.title} started — step away." else "Nice. Take a ${next.title.lowercase()}."
            else -> if (autoStarted) "Break over — next focus started." else "Break over. Ready for the next one?"
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (finished == Phase.WORK) "🍅 Focus done" else "Break over")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_DONE, n) }
    }
}
