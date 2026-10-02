package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.LocalPlatform
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.ThemeMode
import app.prafullkumar.stats.data.pad2
import app.prafullkumar.stats.sync.CloudSync
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.components.AddButton
import app.prafullkumar.stats.ui.components.ChoiceChip
import app.prafullkumar.stats.ui.components.Hint
import app.prafullkumar.stats.ui.components.PrimaryButton
import app.prafullkumar.stats.ui.components.NumberInput
import app.prafullkumar.stats.ui.components.ScreenTitle
import app.prafullkumar.stats.ui.components.SectionCard
import app.prafullkumar.stats.ui.components.SectionHeader
import app.prafullkumar.stats.ui.components.TextInput
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val platform = LocalPlatform.current
    var showTargets by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    ScreenColumn(contentPadding) {
        item { ScreenTitle("Settings") }

        item {
            SectionCard {
                SectionHeader("You")
                Spacer(Modifier.height(8.dp))
                var name by remember { mutableStateOf(StatsRepo.settings.name) }
                TextInput(name, {
                    name = it
                    StatsRepo.updateSettings(StatsRepo.settings.copy(name = it))
                }, "Your name")
                Spacer(Modifier.height(12.dp))
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { m ->
                        ChoiceChip(m.name.lowercase().replaceFirstChar { it.uppercase() }, StatsRepo.settings.theme == m) {
                            StatsRepo.updateSettings(StatsRepo.settings.copy(theme = m))
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Daily targets")
                Spacer(Modifier.height(6.dp))
                val t = StatsRepo.targets
                Hint("${t.kcal} kcal · P ${t.protein}g · C ${t.carbs}g · F ${t.fat}g · Fi ${t.fiber}g · ${t.water} glasses · ${pad2(t.focusMinutes / 60)}h${pad2(t.focusMinutes % 60)} focus · beast at ${t.beastScore}")
                Spacer(Modifier.height(8.dp))
                AddButton("Edit targets", Modifier.fillMaxWidth(), icon = null) { showTargets = true }
            }
        }

        item { PomodoroSettingsCard() }

        item { CloudCard() }

        if (platform.supportsLaunchAtLogin) {
            item {
                SectionCard {
                    SectionHeader("Mac")
                    Spacer(Modifier.height(6.dp))
                    var on by remember { mutableStateOf(platform.isLaunchAtLogin()) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Open at login", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Hint("Lives in the menu bar; closing the window keeps it running.")
                        }
                        Switch(checked = on, onCheckedChange = {
                            platform.setLaunchAtLogin(it)
                            on = platform.isLaunchAtLogin()
                        })
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader("Backup")
                Spacer(Modifier.height(6.dp))
                Hint("A full JSON copy of everything. Useful even with cloud sync on.")
                Row {
                    TextButton(onClick = { platform.exportBackup(StatsRepo.exportJson()) }) { Text("Export") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { confirmImport = true }) { Text("Import") }
                }
                importResult?.let { Hint(it) }
            }
        }
    }

    if (showTargets) {
        TargetsDialog(StatsRepo.targets, onDismiss = { showTargets = false }, onSave = {
            StatsRepo.updateTargets(it); showTargets = false
        })
    }
    if (confirmImport) {
        ConfirmDialog(
            title = "Replace all data?",
            body = "Importing overwrites everything here with the backup. If sync is on, the backup is also uploaded and replaces the cloud copy.",
            confirm = "Choose file",
            onDismiss = { confirmImport = false },
            onConfirm = {
                confirmImport = false
                platform.importBackup { text ->
                    importResult = when {
                        text == null -> null
                        StatsRepo.importJson(text) -> "Backup restored."
                        else -> "That file is not a Prafull Stats backup."
                    }
                }
            }
        )
    }
}

@Composable
private fun PomodoroSettingsCard() {
    val p = StatsRepo.pomodoro
    var work by remember { mutableStateOf(p.work.toString()) }
    var short by remember { mutableStateOf(p.shortBreak.toString()) }
    var long by remember { mutableStateOf(p.longBreak.toString()) }
    var every by remember { mutableStateOf(p.longEvery.toString()) }
    fun save() {
        StatsRepo.updatePomodoro(
            p.copy(
                work = work.toDoubleOrNull()?.toInt()?.coerceIn(1, 180) ?: p.work,
                shortBreak = short.toDoubleOrNull()?.toInt()?.coerceIn(1, 60) ?: p.shortBreak,
                longBreak = long.toDoubleOrNull()?.toInt()?.coerceIn(1, 90) ?: p.longBreak,
                longEvery = every.toDoubleOrNull()?.toInt()?.coerceIn(1, 12) ?: p.longEvery
            )
        )
    }
    SectionCard {
        SectionHeader("Pomodoro")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberInput(work, { work = it; save() }, "Focus", Modifier.weight(1f))
            NumberInput(short, { short = it; save() }, "Short", Modifier.weight(1f))
            NumberInput(long, { long = it; save() }, "Long", Modifier.weight(1f))
            NumberInput(every, { every = it; save() }, "Long every", Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        ToggleRow("Start breaks automatically", StatsRepo.pomodoro.autoStartBreaks) {
            StatsRepo.updatePomodoro(StatsRepo.pomodoro.copy(autoStartBreaks = it))
        }
        ToggleRow("Start next focus automatically", StatsRepo.pomodoro.autoStartWork) {
            StatsRepo.updatePomodoro(StatsRepo.pomodoro.copy(autoStartWork = it))
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun CloudCard() {
    val scope = rememberCoroutineScope()
    val cfg = CloudSync.config
    SectionCard(highlight = cfg.signedIn) {
        SectionHeader("Cloud sync · Firebase", if (cfg.signedIn) "on" else "off")
        Spacer(Modifier.height(8.dp))
        if (cfg.signedIn) {
            Text("Signed in as ${cfg.email}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            val last = CloudSync.lastSync
            if (last > 0) {
                val t = Instant.fromEpochMilliseconds(last).toLocalDateTime(TimeZone.currentSystemDefault())
                Hint("${CloudSync.status} · last at ${pad2(t.hour)}:${pad2(t.minute)}:${pad2(t.second)}")
            } else Hint(CloudSync.status)
            CloudSync.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
            Row {
                PrimaryButton(if (CloudSync.busy) "Syncing…" else "Sync now", enabled = !CloudSync.busy) {
                    scope.launch { CloudSync.syncNow() }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { CloudSync.signOut() }) { Text("Sign out") }
            }
            Spacer(Modifier.height(6.dp))
            Hint("Sign in with the same account on your Mac and phone — changes show up on the other in seconds.")
            return@SectionCard
        }

        Hint("Sign in with the same account on your Mac and phone to keep them in sync. First time? Use Create account.")
        Spacer(Modifier.height(10.dp))
        var apiKey by remember { mutableStateOf(cfg.apiKey) }
        var projectId by remember { mutableStateOf(cfg.projectId) }
        var custom by remember { mutableStateOf(!cfg.configured) }
        var email by remember { mutableStateOf(cfg.email) }
        var password by remember { mutableStateOf("") }
        TextInput(email, { email = it.trim() }, "Email")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        CloudSync.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (custom) {
            Spacer(Modifier.height(10.dp))
            TextInput(apiKey, { apiKey = it.trim() }, "Web API key")
            Spacer(Modifier.height(8.dp))
            TextInput(projectId, { projectId = it.trim() }, "Project ID")
        } else {
            TextButton(onClick = { custom = true }) { Text("Use my own Firebase project") }
        }
        Spacer(Modifier.height(10.dp))
        val ready = apiKey.isNotBlank() && projectId.isNotBlank() && email.contains('@') && password.length >= 6 && !CloudSync.busy
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(if (CloudSync.busy) "Working…" else "Sign in", enabled = ready) {
                CloudSync.setProject(apiKey, projectId)
                scope.launch { CloudSync.signIn(email, password, create = false) }
            }
            AddButton("Create account", icon = null) {
                if (ready) {
                    CloudSync.setProject(apiKey, projectId)
                    scope.launch { CloudSync.signIn(email, password, create = true) }
                }
            }
        }
    }
}

/** Phone-only menu for the screens that don't fit in the bottom bar. */
@Composable
fun MoreScreen(contentPadding: PaddingValues) {
    val items = listOf(Dest.HABITS, Dest.DIET, Dest.MIND, Dest.STATS, Dest.SETTINGS)
    Column(Modifier.padding(top = contentPadding.calculateTopPadding() + 10.dp, start = 14.dp, end = 14.dp)) {
        ScreenTitle("More")
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 20.dp)
        ) {
            items(items) { d ->
                SectionCard(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { Nav.open(d) }
                ) {
                    Icon(d.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(18.dp))
                    Text(d.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Hint(
                        when (d) {
                            Dest.HABITS -> "Streaks & routines"
                            Dest.DIET -> "Macros, plan, water"
                            Dest.MIND -> "Dump, journal, review"
                            Dest.STATS -> "Trends & history"
                            else -> "Sync, targets, backup"
                        }
                    )
                }
            }
        }
    }
}
