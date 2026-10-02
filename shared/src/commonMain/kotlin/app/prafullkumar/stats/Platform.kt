package app.prafullkumar.stats

import androidx.compose.runtime.staticCompositionLocalOf

/** What only the host platform can do: files, login items, permissions. */
interface PlatformActions {
    val isDesktop: Boolean

    /** Hands the backup JSON to the user (share sheet on Android, save dialog on Mac). */
    fun exportBackup(json: String)

    /** Lets the user pick a backup file; [onResult] gets its text or null. */
    fun importBackup(onResult: (String?) -> Unit)

    val supportsLaunchAtLogin: Boolean get() = false
    fun isLaunchAtLogin(): Boolean = false
    fun setLaunchAtLogin(on: Boolean) {}

    /** Opens the always-on-top mini timer (desktop only). */
    fun openMiniTimer() {}
}

val LocalPlatform = staticCompositionLocalOf<PlatformActions> {
    error("PlatformActions not provided")
}
