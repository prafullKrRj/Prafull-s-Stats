package app.prafullkumar.stats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.screens.FuelScreen
import app.prafullkumar.stats.ui.screens.HabitsScreen
import app.prafullkumar.stats.ui.screens.MindScreen
import app.prafullkumar.stats.ui.screens.StatsScreen
import app.prafullkumar.stats.ui.screens.TargetsDialog
import app.prafullkumar.stats.ui.theme.PrafullStatsTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatsRepo.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            PrafullStatsTheme {
                StatsApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    FUEL("Fuel", Icons.Filled.Favorite),
    HABITS("Habits", Icons.Filled.CheckCircle),
    MIND("Mind", Icons.Filled.Create),
    STATS("Stats", Icons.Filled.DateRange)
}

@Composable
private fun StatsApp() {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var tab by rememberSaveable { mutableStateOf(Tab.FUEL) }
    var showTargets by remember { mutableStateOf(false) }

    // Closes a focus session that ran out while the app was closed or on
    // another tab. The Mind tab's own ticker does the same; whichever is
    // first wins and the other is a no-op.
    val focusEndsAt = StatsRepo.focusEndsAt
    LaunchedEffect(focusEndsAt) {
        if (focusEndsAt <= 0) return@LaunchedEffect
        delay((focusEndsAt - System.currentTimeMillis()).coerceAtLeast(0))
        StatsRepo.finishFocus(today)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.FUEL -> FuelScreen(
                selectedDate = selectedDate,
                today = today,
                onSelectDate = { selectedDate = it },
                onOpenTargets = { showTargets = true },
                contentPadding = padding
            )

            Tab.HABITS -> HabitsScreen(
                selectedDate = selectedDate,
                today = today,
                onSelectDate = { selectedDate = it },
                contentPadding = padding
            )

            Tab.MIND -> MindScreen(
                selectedDate = selectedDate,
                today = today,
                onSelectDate = { selectedDate = it },
                contentPadding = padding
            )

            Tab.STATS -> StatsScreen(
                today = today,
                onOpenTargets = { showTargets = true },
                contentPadding = padding
            )
        }
    }

    if (showTargets) {
        TargetsDialog(
            initial = StatsRepo.targets,
            onDismiss = { showTargets = false },
            onSave = {
                StatsRepo.updateTargets(it)
                showTargets = false
            }
        )
    }
}
