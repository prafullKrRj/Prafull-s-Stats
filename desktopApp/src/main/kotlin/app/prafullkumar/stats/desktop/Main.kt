package app.prafullkumar.stats.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import app.prafullkumar.stats.App
import app.prafullkumar.stats.PlatformActions
import app.prafullkumar.stats.data.Phase
import app.prafullkumar.stats.data.PlatformHooks
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Storage
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.clock
import app.prafullkumar.stats.data.today
import app.prafullkumar.stats.sync.CloudSync
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import app.prafullkumar.stats.ui.theme.CalorieColor
import app.prafullkumar.stats.ui.theme.FocusColor
import app.prafullkumar.stats.ui.theme.StreakColor
import app.prafullkumar.stats.ui.theme.PrafullStatsTheme
import app.prafullkumar.stats.ui.theme.Sage
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private val dataDir = File(System.getProperty("user.home"), "Library/Application Support/PrafullStats").apply { mkdirs() }

private class FileStorage(private val dir: File) : Storage {
    override fun read(name: String): String? = File(dir, name).takeIf { it.exists() }?.readText()
    override fun write(name: String, text: String) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(dir, name))
    }
}

fun main() {
    System.setProperty("apple.awt.application.name", "Prafull Stats")
    System.setProperty("apple.awt.application.appearance", "system")
    val storage = FileStorage(dataDir)
    StatsRepo.init(storage)
    CloudSync.init(storage)

    application {
        var windowVisible by remember { mutableStateOf(true) }
        var miniVisible by remember { mutableStateOf(false) }
        val trayState = rememberTrayState()

        DisposableEffect(Unit) {
            StatsRepo.hooks = object : PlatformHooks {
                override fun phaseFinished(finished: Phase, next: Phase, autoStarted: Boolean) {
                    val (title, text) = if (finished == Phase.WORK) "🍅 Focus done" to "Take a ${next.title.lowercase()}." +
                        if (autoStarted) " It's already running." else ""
                    else "Break over" to if (autoStarted) "Next focus started." else "Ready for the next one?"
                    trayState.sendNotification(Notification(title, text, Notification.Type.Info))
                    java.awt.Toolkit.getDefaultToolkit().beep()
                }
            }
            // Clicking the Dock icon brings the window back after it was closed.
            runCatching {
                Desktop.getDesktop().addAppEventListener(java.awt.desktop.AppReopenedListener { windowVisible = true })
            }
            onDispose { }
        }

        fun quit() {
            StatsRepo.flush()
            exitApplication()
        }

        val timer = StatsRepo.timer
        val today = today()
        val todays = StatsRepo.tasksFor(today)
        val open = todays.filter { !it.done }.sortedWith(compareByDescending<Task> { it.top }.thenByDescending { it.priority })
        val score = StatsRepo.dayScore(today).score

        Tray(
            icon = TrayIcon(
                fraction = if (timer.idle) score / 100f else 1f - timer.secondsLeft(StatsRepo.now).toFloat() / (timer.length * 60f).coerceAtLeast(1f),
                active = !timer.idle,
                work = timer.phase == Phase.WORK
            ),
            state = trayState,
            tooltip = if (timer.idle) "Prafull Stats · score $score" else "${timer.phase.title} ${clock(timer.secondsLeft(StatsRepo.now))}",
            onAction = { windowVisible = true },
            menu = {
                Item(
                    if (timer.idle) "Score $score · 🔥${StatsRepo.beastStreak(today).current} · ${todays.count { it.done }}/${todays.size} tasks"
                    else "${if (timer.paused) "⏸" else "⏱"} ${timer.phase.title} ${clock(timer.secondsLeft(StatsRepo.now))}" +
                        if (timer.label.isNotBlank()) " · ${timer.label.take(30)}" else "",
                    enabled = false,
                    onClick = {}
                )
                Separator()
                when {
                    timer.running -> {
                        Item("Pause", onClick = { StatsRepo.pauseTimer() })
                        Item("Stop (log minutes)", onClick = { StatsRepo.stopTimer() })
                        Item("Skip phase", onClick = { StatsRepo.skipPhase() })
                    }
                    timer.paused -> {
                        Item("Resume", onClick = { StatsRepo.resumeTimer() })
                        Item("Stop (log minutes)", onClick = { StatsRepo.stopTimer() })
                    }
                    else -> Item("▶ Start ${timer.phase.title.lowercase()} (${timer.length} min)", onClick = { StatsRepo.startTimer() })
                }
                if (open.isNotEmpty()) {
                    Menu("Focus on task…") {
                        open.take(10).forEach { t ->
                            Item((if (t.top) "★ " else "") + t.title.take(40), onClick = {
                                StatsRepo.startTimer(taskId = t.id, phase = Phase.WORK)
                            })
                        }
                    }
                }
                Item(if (miniVisible) "Hide mini timer" else "Show mini timer", onClick = { miniVisible = !miniVisible })
                Separator()
                Menu("Today's tasks (${todays.count { it.done }}/${todays.size})") {
                    if (todays.isEmpty()) Item("Nothing planned", enabled = false, onClick = {})
                    todays.take(15).forEach { t ->
                        CheckboxItem(t.title.take(45), checked = t.done, onCheckedChange = { StatsRepo.toggleTask(t.id) })
                    }
                }
                Separator()
                listOf(Dest.HOME, Dest.TASKS, Dest.GOALS, Dest.FOCUS, Dest.HABITS, Dest.DIET).forEach { d ->
                    Item("Open ${d.label}", onClick = { Nav.open(d); windowVisible = true })
                }
                Separator()
                Item(if (CloudSync.config.signedIn) "☁ ${CloudSync.status}" else "☁ Sync off", enabled = false, onClick = {})
                Item("Quit Prafull Stats", onClick = { quit() })
            }
        )

        val windowState = rememberWindowState(size = DpSize(1240.dp, 840.dp), position = WindowPosition(Alignment.Center))
        Window(
            onCloseRequest = { windowVisible = false },
            visible = windowVisible,
            state = windowState,
            title = "Prafull Stats",
            icon = TrayIcon(0.75f, active = false, work = true),
            onPreviewKeyEvent = { e ->
                if (e.isMetaPressed && e.key == Key.Q) { quit(); true }
                else if (e.isMetaPressed && e.key == Key.W) { windowVisible = false; true }
                else false
            }
        ) {
            val platform = remember {
                DesktopPlatform(
                    frame = { window },
                    openMini = { miniVisible = true }
                )
            }
            App(platform)
        }

        if (miniVisible) {
            val miniState = rememberWindowState(
                size = DpSize(250.dp, 96.dp),
                position = WindowPosition(Alignment.TopEnd),
                placement = WindowPlacement.Floating
            )
            Window(
                onCloseRequest = { miniVisible = false },
                state = miniState,
                title = "Focus",
                undecorated = true,
                transparent = true,
                resizable = false,
                alwaysOnTop = true,
                focusable = false
            ) {
                PrafullStatsTheme(StatsRepo.settings.theme) {
                    MiniTimer(onOpen = { Nav.open(Dest.FOCUS); windowVisible = true }, onClose = { miniVisible = false })
                }
            }
        }
    }
}

/** A floating pill that stays above every app while you work. */
@Composable
private fun MiniTimer(onOpen: () -> Unit, onClose: () -> Unit) {
    val timer = StatsRepo.timer
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f)) {
                Text(
                    clock(timer.secondsLeft(StatsRepo.now)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (timer.phase == Phase.WORK) FocusColor else CalorieColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    timer.label.ifBlank { if (timer.idle) "ready" else timer.phase.title },
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniButton(if (timer.running) "⏸" else "▶") {
                    when {
                        timer.running -> StatsRepo.pauseTimer()
                        timer.paused -> StatsRepo.resumeTimer()
                        else -> StatsRepo.startTimer()
                    }
                }
                MiniButton("■") { StatsRepo.stopTimer() }
                MiniButton("×") { onClose() }
            }
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

/** Menu bar icon: a ring that fills with the pomodoro (or the day score when idle). */
private class TrayIcon(val fraction: Float, val active: Boolean, val work: Boolean) : Painter() {
    override val intrinsicSize = Size(64f, 64f)
    override fun DrawScope.onDraw() {
        val stroke = size.minDimension * 0.16f
        val inset = stroke / 2 + size.minDimension * 0.04f
        val arc = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(Color(0x66888888), -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
        val color = if (!active) Sage else if (work) FocusColor else CalorieColor
        drawArc(color, -90f, 360f * fraction.coerceIn(0.02f, 1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        if (active) drawCircle(StreakColor, radius = size.minDimension * 0.12f)
    }

    override fun equals(other: Any?) = other is TrayIcon && other.fraction == fraction && other.active == active && other.work == work
    override fun hashCode() = fraction.hashCode() * 31 + active.hashCode() * 7 + work.hashCode()
}

private class DesktopPlatform(
    private val frame: () -> Frame,
    private val openMini: () -> Unit
) : PlatformActions {
    override val isDesktop = true

    override fun exportBackup(json: String) {
        val dialog = FileDialog(frame(), "Export backup", FileDialog.SAVE).apply {
            file = "prafull-stats-backup-${today()}.json"
            isVisible = true
        }
        val name = dialog.file ?: return
        File(dialog.directory, name).writeText(json)
    }

    override fun importBackup(onResult: (String?) -> Unit) {
        val dialog = FileDialog(frame(), "Import backup", FileDialog.LOAD).apply { isVisible = true }
        val name = dialog.file
        onResult(name?.let { runCatching { File(dialog.directory, it).readText() }.getOrNull() })
    }

    private val agent = File(System.getProperty("user.home"), "Library/LaunchAgents/app.prafullkumar.stats.plist")

    override val supportsLaunchAtLogin = true
    override fun isLaunchAtLogin() = agent.exists()

    /** A per-user LaunchAgent that opens the installed app at login. */
    override fun setLaunchAtLogin(on: Boolean) {
        if (!on) {
            agent.delete()
            return
        }
        agent.parentFile.mkdirs()
        agent.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key><string>app.prafullkumar.stats</string>
                <key>ProgramArguments</key>
                <array><string>/usr/bin/open</string><string>-a</string><string>Prafull Stats</string></array>
                <key>RunAtLoad</key><true/>
            </dict>
            </plist>
            """.trimIndent()
        )
    }

    override fun openMiniTimer() = openMini()
}
