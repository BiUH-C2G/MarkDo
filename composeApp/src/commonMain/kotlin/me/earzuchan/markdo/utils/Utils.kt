package me.earzuchan.markdo.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.earzuchan.markdo.data.databases.AppDatabase
import me.earzuchan.markdo.data.APP_PREFERENCES_NAME
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

expect object MarkDoLog {
    fun d(tag: String, vararg messages: Any?)
    fun i(tag: String, vararg messages: Any?)
    fun w(tag: String, vararg messages: Any?)
    fun e(tag: String, vararg messages: Any?)
}

expect object PlatformFunctions {
    fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun getAppFilesPath(): File

    fun importTextFromFile(onResult: (content: String?, error: String?) -> Unit)

    fun exportTextToFile(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit)

    // App Helper
    fun setupApp()

    fun stopApp()
}

object MiscUtils {
    private const val TAG = "MiscUtils"

    // 每次新建
    fun buildAppDatabase(): AppDatabase = PlatformFunctions.getAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()

    fun buildAppPreferences(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(PlatformFunctions.getAppFilesPath(), APP_PREFERENCES_NAME).toOkioPath() }
    )

    // 预制菜
    val defaultDispatcherScope = CoroutineScope(Dispatchers.Default)
    val ioDispatcherScope = CoroutineScope(Dispatchers.IO)
    val mainDispatcherScope = CoroutineScope(Dispatchers.Main)

    // 使用默认的协程上下文来启动任务
    fun defaultDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = defaultDispatcherScope.launch(block = task)

    // 使用IO协程上下文来启动任务
    fun ioDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = ioDispatcherScope.launch(block = task)

    fun mainDispatcherLaunch(task: suspend CoroutineScope.() -> Unit) = mainDispatcherScope.launch(block = task)
}

object ComposeUtils {
    @Composable
    inline fun Modifier.only(
        condition: Boolean,
        elseBlock: @Composable Modifier.() -> Modifier = { this },
        ifBlock: @Composable Modifier.() -> Modifier
    ): Modifier = if (condition) ifBlock() else elseBlock()

    inline fun Color.opacity(opacity: Float): Color {
        val newAlpha = alpha * opacity
        return this.copy(newAlpha)
    }
}

object ResUtils {
    val DrawableResource.v
        @Composable
        get() = vectorResource(this)

    val DrawableResource.i
        @Composable
        get() = imageResource(this)

    val DrawableResource.p
        @Composable
        get() = painterResource(this)


    @Composable
    fun StringResource.t(vararg format: String): String = stringResource(this, *format)

    val StringResource.t
        @Composable
        get() = this.t()
}

object DataUtils {
    @Suppress("NewApi")
    val Long.timeStr: String
        get() {
            val instant = Instant.ofEpochSecond(this)

            val zoneId = ZoneId.systemDefault()

            val formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())

            // 4. 结合时区进行格式化
            return instant.atZone(zoneId).format(formatter)
        }
}
