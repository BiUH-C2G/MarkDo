package me.earzuchan.markdo.utils

import android.net.Uri
import me.earzuchan.markdo.misc.AndroidApp

object AndroidFilePickerBridge {
    const val PICKER_NOT_READY = "picker_not_ready"

    data class ExportRequest(
        val defaultName: String,
        val content: String,
        val onResult: (success: Boolean, error: String?) -> Unit,
    )

    var importInvoker: (((content: String?, error: String?) -> Unit) -> Unit)? = null
    var exportInvoker: ((ExportRequest) -> Unit)? = null

    fun requestImport(onResult: (content: String?, error: String?) -> Unit) {
        val invoker = importInvoker

        if (invoker == null) {
            onResult(null, PICKER_NOT_READY)
            return
        }

        invoker(onResult)
    }

    fun requestExport(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit) {
        val invoker = exportInvoker

        if (invoker == null) {
            onResult(false, PICKER_NOT_READY)
            return
        }

        invoker(ExportRequest(defaultName, content, onResult))
    }

    fun readTextFromUri(uri: Uri): String {
        val resolver = AndroidApp.appContext.contentResolver

        resolver.openInputStream(uri)?.bufferedReader().use { return it?.readText() ?: "" }
    }

    fun writeTextToUri(uri: Uri, content: String) {
        val resolver = AndroidApp.appContext.contentResolver

        resolver.openOutputStream(uri, "wt")?.bufferedWriter().use { it?.write(content) }
    }
}
