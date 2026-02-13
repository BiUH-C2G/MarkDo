package me.earzuchan.markdo.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.defaultComponentContext
import me.earzuchan.markdo.MarkDoApp
import me.earzuchan.markdo.di.appModule
import me.earzuchan.markdo.duties.AppDuty
import me.earzuchan.markdo.utils.AndroidFilePickerBridge
import org.koin.compose.KoinApplication

class InitAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var importCallback: ((String?, String?) -> Unit)? = null
        var exportRequest: AndroidFilePickerBridge.ExportRequest? = null

        val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val callback = importCallback.also { importCallback = null } ?: return@registerForActivityResult

            if (uri == null) {
                callback(null, null)
                return@registerForActivityResult
            }

            runCatching {
                callback(AndroidFilePickerBridge.readTextFromUri(uri), null)
            }.onFailure { callback(null, it.message) }
        }

        val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val req = exportRequest.also { exportRequest = null } ?: return@registerForActivityResult

            if (uri == null) {
                req.onResult(false, null)
                return@registerForActivityResult
            }

            runCatching {
                AndroidFilePickerBridge.writeTextToUri(uri, req.content)
                req.onResult(true, null)
            }.onFailure { req.onResult(false, it.message) }
        }

        AndroidFilePickerBridge.importInvoker = { callback ->
            importCallback = callback
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        AndroidFilePickerBridge.exportInvoker = { req ->
            exportRequest = req
            exportLauncher.launch(req.defaultName)
        }

        enableEdgeToEdge()

        setContent {
            KoinApplication({ modules(appModule) }) {
                val appDuty = AppDuty(defaultComponentContext())
                MarkDoApp(appDuty)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        AndroidFilePickerBridge.importInvoker = null
        AndroidFilePickerBridge.exportInvoker = null
    }
}
