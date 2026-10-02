package app.prafullkumar.stats

import app.prafullkumar.stats.data.AppJson
import app.prafullkumar.stats.data.AppState
import app.prafullkumar.stats.data.Goal
import app.prafullkumar.stats.data.Habit
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Storage
import app.prafullkumar.stats.data.Task
import app.prafullkumar.stats.data.today
import app.prafullkumar.stats.sync.CloudSync
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trips data through Firebase. Skipped unless asked for — against the
 * local emulators (PRAFULL_EMULATOR=1) or the real project
 * (PRAFULL_SYNC_TARGET=live, creates a throwaway account to delete after):
 *
 *   cd firebase && firebase emulators:start --only auth,firestore --project demo-prafull
 *   PRAFULL_EMULATOR=1 ./gradlew :shared:desktopTest --tests '*SyncEmulatorTest*'
 */
class SyncEmulatorTest {

    @Test
    fun pushThenFreshDevicePull_restoresEverything() = runBlocking {
        val target = System.getenv("PRAFULL_SYNC_TARGET") ?: if (System.getenv("PRAFULL_EMULATOR") == "1") "emulator" else return@runBlocking
        val memory = object : Storage {
            val files = mutableMapOf<String, String>()
            override fun read(name: String): String? = files[name]
            override fun write(name: String, text: String) { files[name] = text }
        }
        StatsRepo.init(memory)
        CloudSync.init(memory)
        if (target == "emulator") {
            CloudSync.useEmulator("127.0.0.1", 9099, 8080)
            CloudSync.setProject("fake-api-key", "demo-prafull")
        }

        // Device A: make some data, sign up, sync.
        val day = today()
        StatsRepo.upsertGoal(Goal("g1", "Ship app", why = "Freedom"))
        StatsRepo.addTask(day, Task("t1", "Write listing", why = "The listing is the ad", goalId = "g1"))
        StatsRepo.upsertHabit(Habit("h1", "Read", target = 20, unit = "pages"))
        StatsRepo.bumpHabit(day, StatsRepo.habits.first(), 20)
        val email = "sync${System.nanoTime()}@example.com"
        assertTrue(CloudSync.signIn(email, "secret123", create = true), "sign up: ${CloudSync.error}")
        CloudSync.syncNow()
        assertNull(CloudSync.error, "push failed")
        assertTrue(StatsRepo.meta.dirty.isEmpty(), "everything uploaded")

        // Device B: same account, empty local store.
        StatsRepo.importJson(AppJson.encodeToString(AppState.serializer(), AppState()))
        StatsRepo.resetSyncMeta()
        assertTrue(StatsRepo.goals.isEmpty())
        CloudSync.syncNow()
        assertNull(CloudSync.error, "pull failed")

        assertEquals("Ship app", StatsRepo.goal("g1")?.title)
        assertEquals("Freedom", StatsRepo.goal("g1")?.why)
        assertEquals("Write listing", StatsRepo.tasksFor(day).single().title)
        assertEquals(20, StatsRepo.logFor(day).habitCount("h1"))

        // An edit on B goes up and is not overwritten by the next pull.
        StatsRepo.toggleTask("t1")
        CloudSync.syncNow()
        CloudSync.syncNow()
        assertTrue(StatsRepo.tasksFor(day).single().done)
        assertTrue(StatsRepo.meta.dirty.isEmpty())

        println("SYNC_TEST_UID=${CloudSync.config.uid}")
        CloudSync.signOut()
    }
}
