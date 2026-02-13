package me.earzuchan.markdo.utils

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.earzuchan.markdo.data.APP_DATABASE_NAME
import me.earzuchan.markdo.data.databases.AppDatabase
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.swing.SwingUtilities

actual object MarkDoLog {
    actual fun d(tag: String, vararg messages: Any?) = printLog("D", tag, messages)

    actual fun i(tag: String, vararg messages: Any?) = printLog("I", tag, messages)

    actual fun w(tag: String, vararg messages: Any?) = printLog("W", tag, messages)

    actual fun e(tag: String, vararg messages: Any?) = if (messages.isNotEmpty() && messages.last() is Throwable) {
        val throwable = messages.last() as Throwable

        // 拼接除了最后一个 Throwable 之外的所有信息
        println("[E] $tag > ${messages.dropLast(1).joinToString(" ") { it?.toString() ?: "null" }}")

        throwable.printStackTrace()  // 输出详细堆栈信息
    } else printLog("E", tag, messages)

    private fun printLog(level: String, tag: String, messages: Array<out Any?>) {
        println("[$level] $tag > ${messages.joinToString(" ") { it?.toString() ?: "null" }}")
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PlatformFunctions {
    private const val TAG = "DesktopPlatformFunctions"
    private const val PICKER_NOT_READY = "picker_not_ready"

    // Data

    actual fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(getAppDataPath(), "databases/$APP_DATABASE_NAME")
        dbFile.parentFile?.mkdirs()
        return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
    }

    actual fun getAppFilesPath(): String = File(getAppDataPath(), "files").also { it.mkdirs() }.absolutePath

    private fun getAppDataPath(): File = File(System.getProperty("user.home"), "me.earzuchan.markdo")

    actual fun importTextFromFile(onResult: (content: String?, error: String?) -> Unit) {
        runCatching {
            val picker = FileDialog(null as Frame?, "Import Rules File", FileDialog.LOAD).apply {
                file = "*.json"
                isVisible = true
            }

            val selected = picker.file ?: return onResult(null, null)
            val chosen = File(picker.directory, selected)
            onResult(chosen.readText(), null)
        }.onFailure { onResult(null, it.message ?: PICKER_NOT_READY) }
    }

    actual fun exportTextToFile(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit) {
        runCatching {
            val picker = FileDialog(null as Frame?, "Export Rules File", FileDialog.SAVE).apply {
                file = defaultName
                isVisible = true
            }

            val selected = picker.file ?: return onResult(false, null)
            val chosen = File(picker.directory, selected)
            chosen.writeText(content)
            onResult(true, null)
        }.onFailure { onResult(false, it.message ?: PICKER_NOT_READY) }
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

    actual fun stopApp(): Unit = exitAppMethod()

    lateinit var exitAppMethod: () -> Unit
}

object DesktopUtils {
    fun <T> runOnUiThread(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()

        var error: Throwable? = null
        var result: T? = null

        SwingUtilities.invokeAndWait {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
            }
        }

        error?.also { throw it }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
