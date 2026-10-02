package app.prafullkumar.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.clock
import app.prafullkumar.stats.data.today
import app.prafullkumar.stats.sync.CloudSync
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.screens.DashboardScreen
import app.prafullkumar.stats.ui.screens.DietScreen
import app.prafullkumar.stats.ui.screens.FocusScreen
import app.prafullkumar.stats.ui.screens.GoalDetailScreen
import app.prafullkumar.stats.ui.screens.GoalsScreen
import app.prafullkumar.stats.ui.screens.HabitsScreen
import app.prafullkumar.stats.ui.screens.MindScreen
import app.prafullkumar.stats.ui.screens.MoreScreen
import app.prafullkumar.stats.ui.screens.SettingsScreen
import app.prafullkumar.stats.ui.screens.StatsScreen
import app.prafullkumar.stats.ui.screens.TasksScreen
import app.prafullkumar.stats.ui.theme.PrafullStatsTheme
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

private val phoneTabs = listOf(Dest.HOME, Dest.TASKS, Dest.GOALS, Dest.FOCUS, Dest.MORE)
private val sideTabs = Dest.entries.filter { it != Dest.MORE }

/** Root of the whole app, shared by Android and the Mac. */
@Composable
fun App(platform: PlatformActions) {
    LaunchedEffect(Unit) {
        while (true) {
            StatsRepo.tick()
            delay(1000)
        }
    }
    PrafullStatsTheme(StatsRepo.settings.theme) {
        CompositionLocalProvider(LocalPlatform provides platform) {
            // Re-read each tick so the app rolls over at midnight without a restart.
            val now = StatsRepo.now
            val today = remember(now / 60_000) { today() }
            var selectedDate by remember { mutableStateOf(today) }
            LaunchedEffect(today) { if (selectedDate > today) selectedDate = today }

            BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val wide = maxWidth >= 760.dp
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        Sidebar()
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            Box(Modifier.widthIn(max = 980.dp).align(Alignment.TopCenter)) {
                                Content(today, selectedDate, { selectedDate = it }, PaddingValues(top = 12.dp), wide = true)
                            }
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                phoneTabs.forEach { d ->
                                    val selected = Nav.dest == d || (d == Dest.MORE && Nav.dest !in phoneTabs)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { Nav.open(d) },
                                        icon = { Icon(d.icon, contentDescription = null) },
                                        label = { Text(if (d == Dest.HOME) "Home" else d.label) }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box {
                            Content(today, selectedDate, { selectedDate = it }, padding, wide = false)
                            // Opaque strip so scrolled content never runs under the status bar icons.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    today: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    padding: PaddingValues,
    wide: Boolean
) {
    when (Nav.dest) {
        Dest.HOME -> DashboardScreen(today, padding, wide)
        Dest.TASKS -> TasksScreen(selectedDate, today, onSelectDate, padding)
        Dest.GOALS -> {
            val id = Nav.goalId
            if (id != null && StatsRepo.goal(id) != null) GoalDetailScreen(id, today, padding)
            else GoalsScreen(today, padding)
        }
        Dest.FOCUS -> FocusScreen(today, padding)
        Dest.HABITS -> HabitsScreen(selectedDate, today, onSelectDate, padding)
        Dest.DIET -> DietScreen(selectedDate, today, onSelectDate, padding)
        Dest.MIND -> MindScreen(selectedDate, today, onSelectDate, padding)
        Dest.STATS -> StatsScreen(today, padding)
        Dest.SETTINGS -> SettingsScreen(padding)
        Dest.MORE -> MoreScreen(padding)
    }
}

@Composable
private fun Sidebar() {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(212.dp)
            .fillMaxHeight()
            .background(scheme.surfaceContainerLow)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 14.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(scheme.primary))
            Spacer(Modifier.width(10.dp))
            Text("Prafull Stats", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, fontWeight = FontWeight.Bold)
        }
        sideTabs.forEach { d ->
            val selected = Nav.dest == d
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) scheme.primary.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { Nav.open(d) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(d.icon, contentDescription = null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(d.label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.onSurface else scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f))
        val timer = StatsRepo.timer
        if (!timer.idle) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.tertiary.copy(alpha = 0.12f))
                    .clickable { Nav.open(Dest.FOCUS) }
                    .padding(12.dp)
            ) {
                Text(timer.phase.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = scheme.tertiary)
                Text(clock(timer.secondsLeft(StatsRepo.now)), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
                if (timer.label.isNotBlank()) Text(timer.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            if (CloudSync.config.signedIn) "☁ ${CloudSync.status}" else "☁ Local only",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp).clickable { Nav.open(Dest.SETTINGS) }
        )
    }
}
