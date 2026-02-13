package me.earzuchan.markdo.utils

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import androidx.room.Room
import androidx.room.RoomDatabase
import me.earzuchan.markdo.data.APP_DATABASE_NAME
import me.earzuchan.markdo.data.databases.AppDatabase
import me.earzuchan.markdo.misc.AndroidApp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.system.exitProcess

actual object MarkDoLog {
    actual fun d(tag: String, vararg messages: Any?) {
        Log.d(tag, messages.toStr())
    }

    actual fun i(tag: String, vararg messages: Any?) {
        Log.i(tag, messages.toStr())
    }

    actual fun w(tag: String, vararg messages: Any?) {
        Log.w(tag, messages.toStr())
    }

    actual fun e(tag: String, vararg messages: Any?) {
        if (messages.isNotEmpty() && messages.last() is Throwable) {
            val throwable = messages.last() as Throwable

            // 拼接除了最后一个 Throwable 之外的所有信息
            val msg = messages.dropLast(1).joinToString(" ") { it?.toString() ?: "null" }

            Log.e(tag, msg, throwable)
        } else Log.e(tag, messages.toStr())
    }

    private fun Array<out Any?>.toStr() = joinToString(" ") { it?.toString() ?: "null" }
}

actual object PlatformFunctions {
    private const val TAG = "AndroidPlatformFunctions"

    // Data

    actual fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbPath = AndroidApp.appContext.getDatabasePath(APP_DATABASE_NAME).absolutePath
        return Room.databaseBuilder<AppDatabase>(context = AndroidApp.appContext, name = dbPath)
    }

    actual fun getAppFilesPath(): String = AndroidApp.appContext.filesDir.absolutePath // 在 /data/data/包名/files/ 下

    actual fun importTextFromFile(onResult: (content: String?, error: String?) -> Unit) {
        AndroidFilePickerBridge.requestImport(onResult)
    }

    actual fun exportTextToFile(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit) {
        AndroidFilePickerBridge.requestExport(defaultName, content, onResult)
    }

    actual fun formatEpochSecond(epochSecond: Long): String = Instant.ofEpochSecond(epochSecond)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // App Helper

    actual fun setupApp() {}

    actual fun stopApp(): Unit = exitProcess(0)
}
