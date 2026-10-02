package app.prafullkumar.stats.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.prafullkumar.stats.App
import app.prafullkumar.stats.PlatformActions
import app.prafullkumar.stats.data.AppJson
import app.prafullkumar.stats.data.AppState
import app.prafullkumar.stats.data.DayLog
import app.prafullkumar.stats.data.FocusSession
import app.prafullkumar.stats.data.FoodItem
import app.prafullkumar.stats.data.GoalTemplates
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.InboxItem
import app.prafullkumar.stats.data.Slot
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Storage
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.ThemeMode
import app.prafullkumar.stats.data.minusDays
import app.prafullkumar.stats.data.plusDays
import app.prafullkumar.stats.data.nowMillis
import app.prafullkumar.stats.data.today
import app.prafullkumar.stats.ui.Dest
import app.prafullkumar.stats.ui.Nav
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.random.Random

/**
 * Renders every screen offscreen with demo data and writes PNGs — a quick way
 * to eyeball the UI (and keep README screenshots fresh) without a display.
 *
 *   ./gradlew :desktopApp:snapshots
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "build/snapshots").apply { mkdirs() }
    StatsRepo.init(object : Storage {
        override fun read(name: String): String? = null
        override fun write(name: String, text: String) {}
    })
    StatsRepo.importJson(AppJson.encodeToString(AppState.serializer(), demoState()))
    StatsRepo.updateSettings(StatsRepo.settings.copy(theme = ThemeMode.LIGHT))
    val platform = object : PlatformActions {
        override val isDesktop = true
        override fun exportBackup(json: String) {}
        override fun importBackup(onResult: (String?) -> Unit) {}
    }

    val screens = Dest.entries.filter { it != Dest.MORE }
    val sizes = listOf(Triple("mac", 1240 to 860, 1f), Triple("phone", 412 to 915, 2f))
    for ((tag, wh, density) in sizes) {
        for (d in screens + listOfNotNull(Dest.MORE.takeIf { tag == "phone" })) {
            Nav.open(d)
            if (d == Dest.GOALS && tag == "mac") Nav.openGoal(StatsRepo.goals.first().id)
            val (w, h) = wh
            val scene = ImageComposeScene((w * density).toInt(), (h * density).toInt(), Density(density)) { App(platform) }
            var t = 0L
            repeat(6) { scene.render(t); t += 16_000_000 }
            val img = scene.render(t)
            File(out, "${tag}_${d.name.lowercase()}.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            scene.close()
        }
    }
    // Dark theme sample (the rest render in light).
    StatsRepo.updateSettings(StatsRepo.settings.copy(theme = ThemeMode.DARK))
    Nav.open(Dest.HOME)
    val dark = ImageComposeScene(1240, 860, Density(1f)) { App(platform) }
    repeat(6) { dark.render(it * 16_000_000L) }
    File(out, "mac_home_dark.png").writeBytes(dark.render(100_000_000).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    dark.close()
    println("Wrote ${out.listFiles()?.size} snapshots to ${out.absolutePath}")
    kotlin.system.exitProcess(0)
}

private fun demoState(): AppState {
    val today = today()
    val rnd = Random(7)
    val foods = listOf(
        FoodItem("milk", "Milk", "300 ml", Slot.BREAKFAST, 190, 10.0, 15.0, 10.0, 0.0, core = true),
        FoodItem("oats", "Oats", "60 g", Slot.BREAKFAST, 230, 8.0, 40.0, 4.0, 6.0),
        FoodItem("whey", "Whey scoop", "1 scoop", Slot.POST_WORKOUT, 120, 24.0, 3.0, 2.0, 0.0, core = true),
        FoodItem("soya", "Soya chunks", "50 g dry", Slot.LUNCH, 170, 26.0, 16.0, 0.5, 6.5, core = true),
        FoodItem("roti", "2 roti", "70 g atta", Slot.LUNCH, 220, 7.0, 44.0, 1.5, 7.0),
        FoodItem("dal", "Dal + sabzi", "1 bowl each", Slot.DINNER, 280, 12.0, 35.0, 9.0, 9.0)
    )
    val habits = listOf(
        Habit("gym", "Workout", why = "Strong body, clear head.", createdAt = today.minusDays(40).toString()),
        Habit("read", "Read", why = "Learn from people smarter than me.", target = 20, unit = "pages", createdAt = today.minusDays(40).toString()),
        Habit("nophone", "No phone first hour", why = "Own the morning.", createdAt = today.minusDays(40).toString()),
        Habit("steps", "Steps", target = 10000, unit = "steps", createdAt = today.minusDays(40).toString())
    )
    var ids = 0
    val newId: (String) -> String = { "${it}_${ids++}" }
    val launch = GoalTemplates.publishAndroidApp.build(today.minusDays(30).toString(), newId).let { g ->
        g.copy(
            title = "Ship CoinLens on Play Store",
            steps = g.steps.mapIndexed { i, s -> if (i < 7) s.copy(done = true) else s },
            targetDate = today.plusDays(45).toString(),
            metricCurrent = 0.0,
            pinned = true
        )
    }
    val body = GoalTemplates.bodyRecomp.build(today.minusDays(30).toString(), newId).let { g ->
        g.copy(steps = g.steps.mapIndexed { i, s -> if (i < 2) s.copy(done = true) else s }, metricName = "kg", metricStart = 78.0, metricTarget = 72.0, metricCurrent = 75.6)
    }
    val learn = GoalTemplates.learnSkill.build(today.minusDays(20).toString(), newId).copy(title = "Master Compose Multiplatform", metricCurrent = 38.0)
    val step = launch.steps[7]

    val logs = (0..34).associate { back ->
        val d = today.minusDays(back)
        val isToday = back == 0
        val ate = foods.filter { isToday && it.id != "dal" || !isToday && rnd.nextFloat() < 0.85f }.map { it.id }.toSet()
        val tasks = if (isToday) listOf(
            Task("t1", step.title, why = step.why, top = true, priority = 3, goalId = launch.id, stepId = step.id, estimate = 3, pomodoros = 2, time = "09:30"),
            Task("t2", "Write store listing copy", why = "The listing is the ad.", top = true, priority = 2, goalId = launch.id, estimate = 2),
            Task("t3", "Gym — push day", top = true, done = true, time = "07:00"),
            Task("t4", "Reply to Rahul about the API", priority = 1),
            Task("t5", "Pay electricity bill", done = true),
            Task("t6", "Plan next week's meals")
        ) else List(rnd.nextInt(3, 7)) { i -> Task("p${back}_$i", "Task $i", done = rnd.nextFloat() < 0.7f, top = i < 3) }
        val sessions = List(if (isToday) 4 else rnd.nextInt(0, 7)) { i ->
            FocusSession(nowMillis() - (back * 86_400_000L) - (i + 1) * 3_600_000L, 25, if (isToday) launch.steps[7].title else "Deep work", if (isToday) "t1" else null, if (isToday) launch.id else null)
        }
        d.toString() to DayLog(
            date = d.toString(),
            foods = ate,
            habits = habits.filter { rnd.nextFloat() < if (isToday) 0.5f else 0.8f }.associate { it.id to it.target },
            tasks = tasks,
            sessions = sessions,
            weight = if (back % 3 == 0) 78.0 - (34 - back) * 0.07 + rnd.nextDouble(-0.3, 0.3) else null,
            water = if (isToday) 7 else rnd.nextInt(6, 13),
            sleep = rnd.nextDouble(6.0, 8.5),
            mood = rnd.nextInt(3, 6),
            energy = rnd.nextInt(2, 6),
            win = if (back in 1..5) listOf("Shipped onboarding", "PR merged", "Hit protein 4 days straight", "Ran 5k", "Cleared inbox")[back - 1] else ""
        )
    }
    return AppState(
        foods = foods,
        habits = habits,
        goals = listOf(launch, body, learn),
        inbox = listOf(InboxItem("i1", "Idea: widget for streaks", today.toString()), InboxItem("i2", "Book dentist", today.toString())),
        backlog = listOf(Task("b1", "Try RevenueCat paywall", goalId = launch.id), Task("b2", "Read Atomic Habits again")),
        logs = logs,
        startDate = today.minusDays(34).toString()
    )
}


