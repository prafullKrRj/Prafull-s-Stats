package app.prafullkumar.stats.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class Dest(val label: String, val icon: ImageVector) {
    HOME("Dashboard", Icons.Filled.Home),
    TASKS("Tasks", Icons.Filled.CheckCircle),
    GOALS("Goals", Icons.Filled.Star),
    FOCUS("Focus", Icons.Filled.PlayArrow),
    HABITS("Habits", Icons.Filled.Refresh),
    DIET("Diet", Icons.Filled.Favorite),
    MIND("Mind", Icons.Filled.Create),
    STATS("Stats", Icons.Filled.DateRange),
    SETTINGS("Settings", Icons.Filled.Settings),
    MORE("More", Icons.Filled.Menu)
}

/** App-wide navigation state; tiny enough not to need a library. */
object Nav {
    var dest by mutableStateOf(Dest.HOME)
        private set
    var goalId by mutableStateOf<String?>(null)
        private set

    fun open(d: Dest) {
        dest = d
        goalId = null
    }

    fun openGoal(id: String) {
        dest = Dest.GOALS
        goalId = id
    }

    /** Returns false when there is nowhere further back to go. */
    fun back(): Boolean = when {
        goalId != null -> { goalId = null; true }
        dest != Dest.HOME -> { dest = Dest.HOME; true }
        else -> false
    }
}
