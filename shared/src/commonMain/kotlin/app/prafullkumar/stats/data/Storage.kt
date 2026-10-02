package app.prafullkumar.stats.data

/** Plain text files in the app's private folder. Each platform supplies its own. */
interface Storage {
    fun read(name: String): String?
    fun write(name: String, text: String)
}

/** Platform hooks the store calls out to — notifications, timer alarms. */
interface PlatformHooks {
    /** A pomodoro phase ended; [next] is the phase that is now set up. */
    fun phaseFinished(finished: Phase, next: Phase, autoStarted: Boolean) {}

    /** Called whenever the timer starts, pauses or stops so alarms can be rescheduled. */
    fun timerChanged(timer: TimerState) {}
}
