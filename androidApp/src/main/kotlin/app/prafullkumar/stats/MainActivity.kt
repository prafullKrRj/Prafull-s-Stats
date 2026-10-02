package app.prafullkumar.stats

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.Nav

class MainActivity : ComponentActivity(), PlatformActions {

    private var pendingImport: ((String?) -> Unit)? = null

    private val importer = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val text = uri?.let {
            runCatching { contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }.getOrNull()
        }
        pendingImport?.invoke(text)
        pendingImport = null
    }

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatsApplication.ensureStarted(this)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!Nav.back()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { App(this) }
    }

    override fun onStop() {
        super.onStop()
        StatsRepo.flush()
    }

    // ---- PlatformActions ---------------------------------------------------

    override val isDesktop: Boolean = false

    override fun exportBackup(json: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Prafull Stats backup")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        startActivity(Intent.createChooser(send, "Export backup"))
    }

    override fun importBackup(onResult: (String?) -> Unit) {
        pendingImport = onResult
        importer.launch(arrayOf("application/json", "text/plain", "*/*"))
    }
}
