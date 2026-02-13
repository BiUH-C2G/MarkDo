package me.earzuchan.markdo.utils

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.earzuchan.markdo.data.APP_DATABASE_NAME
import me.earzuchan.markdo.data.databases.AppDatabase
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual object MarkDoLog {
    actual fun d(tag: String, vararg messages: Any?) = printLog("D", tag, messages)

    actual fun i(tag: String, vararg messages: Any?) = printLog("I", tag, messages)

    actual fun w(tag: String, vararg messages: Any?) = printLog("W", tag, messages)

    actual fun e(tag: String, vararg messages: Any?) = printLog("E", tag, messages)

    private fun printLog(level: String, tag: String, messages: Array<out Any?>) {
        println("[$level] $tag > ${messages.joinToString(" ") { it?.toString() ?: "null" }}")
    }
}

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFunctions {
    private const val PICKER_NOT_READY = "picker_not_ready"
    private const val APP_CONTAINER_DIR = "me.earzuchan.markdo"
    private const val REFERENCE_DATE_UNIX_OFFSET_SECONDS = 978307200.0

    actual fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbDir = ensureDirectory("${getAppDataPath()}/databases")
        return Room.databaseBuilder<AppDatabase>("$dbDir/$APP_DATABASE_NAME")
    }

    actual fun getAppFilesPath(): String = ensureDirectory("${getAppDataPath()}/files")

    actual fun importTextFromFile(onResult: (content: String?, error: String?) -> Unit) {
        onResult(null, PICKER_NOT_READY)
    }

    actual fun exportTextToFile(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit) {
        onResult(false, PICKER_NOT_READY)
    }

    actual fun formatEpochSecond(epochSecond: Long): String {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterMediumStyle
        }
        val referenceSeconds = epochSecond.toDouble() - REFERENCE_DATE_UNIX_OFFSET_SECONDS
        return formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = referenceSeconds))
    }

    actual fun currentTimeMillis(): Long =
        ((NSDate().timeIntervalSinceReferenceDate + REFERENCE_DATE_UNIX_OFFSET_SECONDS) * 1000.0).toLong()

    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    actual fun setupApp() = Unit

    actual fun stopApp() = Unit

    private fun getAppDataPath(): String {
        val root = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).firstOrNull() as? String) ?: "."

        return ensureDirectory("$root/$APP_CONTAINER_DIR")
    }

    private fun ensureDirectory(path: String): String {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        return path
    }
}
