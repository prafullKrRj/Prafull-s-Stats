package app.prafullkumar.stats.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.prafullkumar.stats.data.AppJson
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.data.Storage
import app.prafullkumar.stats.data.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Firebase project + signed-in account, kept in the app's private folder. */
@Serializable
data class CloudConfig(
    val apiKey: String = "",
    val projectId: String = "",
    val email: String = "",
    val uid: String = "",
    val refreshToken: String = ""
) {
    val configured: Boolean get() = apiKey.isNotBlank() && projectId.isNotBlank()
    val signedIn: Boolean get() = configured && uid.isNotBlank() && refreshToken.isNotBlank()
}

/**
 * Two-way sync with Firebase over its REST APIs, so the very same code runs on
 * Android and on the Mac.
 *
 * Layout in Firestore:
 *   users/{uid}/days/{yyyy-mm-dd}     one document per day
 *   users/{uid}/sections/{name}       plan, habits, goals, inbox, backlog, settings, timer
 * Each holds `data` (the JSON) and `updatedAt` (epoch millis). Conflicts are
 * settled per document: the newer `updatedAt` wins.
 */
object CloudSync {

    private const val FILE = "prafull_cloud.json"
    private const val INTERVAL_MS = 20_000L

    private var storage: Storage? = null
    private val client by lazy { HttpClient() }
    private val mutex = Mutex()
    private var loop: Job? = null

    private var idToken: String? = null
    private var idTokenExpires = 0L

    var config by mutableStateOf(CloudConfig())
        private set
    var status by mutableStateOf("Not connected")
        private set
    var lastSync by mutableStateOf(0L)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var authBase = "https://identitytoolkit.googleapis.com/v1"
    private var tokenBase = "https://securetoken.googleapis.com/v1"
    private var api = "https://firestore.googleapis.com/v1"

    /**
     * Points everything at the Firebase Local Emulator Suite (for tests and
     * offline development), e.g. useEmulator("127.0.0.1", 9099, 8080).
     */
    fun useEmulator(host: String, authPort: Int, firestorePort: Int) {
        authBase = "http://$host:$authPort/identitytoolkit.googleapis.com/v1"
        tokenBase = "http://$host:$authPort/securetoken.googleapis.com/v1"
        api = "http://$host:$firestorePort/v1"
    }

    fun init(storage: Storage, defaults: CloudConfig = FirebaseProject.defaults) {
        if (this.storage != null) return
        this.storage = storage
        val saved = storage.read(FILE)?.let {
            runCatching { AppJson.decodeFromString<CloudConfig>(it) }.getOrNull()
        }
        // A saved account keeps its own project; otherwise use the built-in one.
        config = when {
            saved == null -> defaults
            saved.configured -> saved
            else -> saved.copy(apiKey = defaults.apiKey, projectId = defaults.projectId)
        }
        if (config.signedIn) {
            status = "Signed in as ${config.email}"
            start()
        }
    }

    private fun persist() {
        storage?.write(FILE, AppJson.encodeToString(CloudConfig.serializer(), config))
    }

    fun setProject(apiKey: String, projectId: String) {
        config = config.copy(apiKey = apiKey.trim(), projectId = projectId.trim())
        persist()
    }

    // ---- auth ------------------------------------------------------------

    /** Signs in, or creates the account first when [create] is true. */
    suspend fun signIn(email: String, password: String, create: Boolean): Boolean {
        if (!config.configured) {
            error = "Enter the Firebase API key and project ID first."
            return false
        }
        busy = true
        error = null
        return try {
            val endpoint = if (create) "accounts:signUp" else "accounts:signInWithPassword"
            val body = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                put("returnSecureToken", true)
            }
            val resp = io {
                client.post("$authBase/$endpoint?key=${config.apiKey}") {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            val json = parse(resp)
            idToken = json.str("idToken")
            idTokenExpires = nowMillis() + (json.str("expiresIn")?.toLongOrNull() ?: 3600) * 1000 - 60_000
            config = config.copy(
                email = json.str("email") ?: email,
                uid = json.str("localId").orEmpty(),
                refreshToken = json.str("refreshToken").orEmpty()
            )
            persist()
            // Local data never stamped yet loses to whatever is already in the cloud.
            StatsRepo.markAllDirty(force = false)
            StatsRepo.setLastPull(0)
            status = "Signed in as ${config.email}"
            start()
            true
        } catch (e: Throwable) {
            error = friendly(e)
            false
        } finally {
            busy = false
        }
    }

    fun signOut() {
        loop?.cancel()
        loop = null
        idToken = null
        config = config.copy(email = "", uid = "", refreshToken = "")
        persist()
        StatsRepo.resetSyncMeta()
        status = "Signed out"
    }

    private suspend fun token(): String {
        idToken?.let { if (nowMillis() < idTokenExpires) return it }
        val resp = io {
            client.submitForm(
                url = "$tokenBase/token?key=${config.apiKey}",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", config.refreshToken)
                }
            )
        }
        val json = parse(resp)
        val t = json.str("id_token") ?: error("No token in refresh response")
        idToken = t
        idTokenExpires = nowMillis() + (json.str("expires_in")?.toLongOrNull() ?: 3600) * 1000 - 60_000
        json.str("refresh_token")?.let {
            if (it != config.refreshToken) {
                config = config.copy(refreshToken = it)
                persist()
            }
        }
        return t
    }

    // ---- loop ------------------------------------------------------------

    @OptIn(FlowPreview::class)
    private fun start() {
        if (loop?.isActive == true) return
        loop = StatsRepo.scope.launch {
            launch {
                // Push soon after local edits instead of waiting for the next round.
                StatsRepo.changes.drop(1).debounce(1500).collect { syncNow() }
            }
            while (true) {
                syncNow()
                delay(INTERVAL_MS)
            }
        }
    }

    /** One round: upload local changes, then fetch anything newer from the cloud. */
    suspend fun syncNow() {
        if (!config.signedIn) return
        mutex.withLock {
            busy = true
            try {
                push()
                pull()
                lastSync = nowMillis()
                error = null
                status = "Synced"
            } catch (e: Throwable) {
                error = friendly(e)
                status = "Sync failed"
            } finally {
                busy = false
            }
        }
    }

    private val base get() = "projects/${config.projectId}/databases/(default)/documents"
    private val userPath get() = "$base/users/${config.uid}"

    private fun docPath(key: String): String =
        if (key.startsWith("d:")) "$userPath/days/${key.removePrefix("d:")}"
        else "$userPath/sections/${key.removePrefix("s:")}"

    private suspend fun push() {
        val meta = StatsRepo.meta
        val dirty = meta.dirty.toList()
        if (dirty.isEmpty()) return
        val sent = dirty.associateWith { meta.stamps[it] ?: nowMillis() }
        dirty.chunked(400).forEach { chunk ->
            val body = buildJsonObject {
                putJsonArray("writes") {
                    chunk.forEach { key ->
                        val json = if (key.startsWith("d:")) StatsRepo.dayJson(key.removePrefix("d:"))
                        else StatsRepo.sectionJson(key.removePrefix("s:"))
                        add(buildJsonObject {
                            putJsonObject("update") {
                                put("name", docPath(key))
                                putJsonObject("fields") {
                                    putJsonObject("data") { put("stringValue", json) }
                                    putJsonObject("updatedAt") { put("integerValue", sent.getValue(key).toString()) }
                                }
                            }
                        })
                    }
                }
            }
            val tk = token()
            val resp = io {
                client.post("$api/$base:commit") {
                    header("Authorization", "Bearer $tk")
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            parse(resp)
        }
        StatsRepo.markPushed(sent)
    }

    private suspend fun pull() {
        // Clocks on two devices are never perfectly equal; overlap a little and
        // let the per-document stamps sort out repeats.
        val since = (StatsRepo.meta.lastPull - 10 * 60_000).coerceAtLeast(0)
        val startedAt = nowMillis()
        listOf("sections", "days").forEach { collection ->
            val query = buildJsonObject {
                putJsonObject("structuredQuery") {
                    putJsonArray("from") { add(buildJsonObject { put("collectionId", collection) }) }
                    putJsonObject("where") {
                        putJsonObject("fieldFilter") {
                            putJsonObject("field") { put("fieldPath", "updatedAt") }
                            put("op", "GREATER_THAN")
                            putJsonObject("value") { put("integerValue", since.toString()) }
                        }
                    }
                }
            }
            val tk = token()
            val resp = io {
                client.post("$api/$userPath:runQuery") {
                    header("Authorization", "Bearer $tk")
                    contentType(ContentType.Application.Json)
                    setBody(query.toString())
                }
            }
            val text = resp.bodyAsText()
            if (!resp.status.isSuccess()) error(apiError(text, resp))
            val rows = AppJson.parseToJsonElement(text) as? JsonArray ?: return@forEach
            rows.forEach { row ->
                val doc = (row as? JsonObject)?.get("document")?.jsonObject ?: return@forEach
                val name = doc.str("name") ?: return@forEach
                val fields = doc["fields"]?.jsonObject ?: return@forEach
                val data = fields["data"]?.jsonObject?.str("stringValue") ?: return@forEach
                val stamp = fields["updatedAt"]?.jsonObject?.str("integerValue")?.toLongOrNull() ?: return@forEach
                val id = name.substringAfterLast('/')
                val key = if (collection == "days") "d:$id" else "s:$id"
                val local = StatsRepo.meta.stamps[key] ?: 0
                if (stamp > local) StatsRepo.applyRemote(key, data, stamp)
            }
        }
        StatsRepo.setLastPull(startedAt)
    }

    // ---- helpers ---------------------------------------------------------

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    private suspend fun parse(resp: HttpResponse): JsonObject {
        val text = resp.bodyAsText()
        if (!resp.status.isSuccess()) error(apiError(text, resp))
        return runCatching { AppJson.parseToJsonElement(text).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    private fun apiError(text: String, resp: HttpResponse): String {
        val msg = runCatching {
            val root = AppJson.parseToJsonElement(text)
            val err = (if (root is JsonArray) root.firstOrNull() else root)?.jsonObject?.get("error")
            when (err) {
                is JsonObject -> err.str("message")
                else -> err?.jsonPrimitive?.contentOrNull
            }
        }.getOrNull()
        return msg ?: "HTTP ${resp.status.value}"
    }

    private fun friendly(e: Throwable): String = when (val m = e.message.orEmpty()) {
        "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> "Wrong email or password."
        "EMAIL_EXISTS" -> "That email already has an account — sign in instead."
        "OPERATION_NOT_ALLOWED" -> "Enable Email/Password sign-in in Firebase Authentication."
        "PERMISSION_DENIED", "Missing or insufficient permissions." -> "Firestore rules block access — publish firestore.rules from the repo."
        else -> if (m.startsWith("WEAK_PASSWORD")) "Password must be at least 6 characters."
        else if (m.contains("API key not valid")) "API key not valid — copy it from Firebase project settings."
        else m.ifBlank { e::class.simpleName ?: "Unknown error" }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonElement)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
}
