package me.earzuchan.markdo.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.earzuchan.markdo.data.APP_PREFERENCES_NAME
import me.earzuchan.markdo.data.databases.AppDatabase
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.*

expect object MarkDoLog {
    fun d(tag: String, vararg messages: Any?)
    fun i(tag: String, vararg messages: Any?)
    fun w(tag: String, vararg messages: Any?)
    fun e(tag: String, vararg messages: Any?)
}

expect object PlatformFunctions {
    fun getAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun getAppFilesPath(): String

    fun importTextFromFile(onResult: (content: String?, error: String?) -> Unit)

    fun exportTextToFile(defaultName: String, content: String, onResult: (success: Boolean, error: String?) -> Unit)

    fun formatEpochSecond(epochSecond: Long): String

    fun currentTimeMillis(): Long

    val ioDispatcher: CoroutineDispatcher

    // App Helper
    fun setupApp()

    fun stopApp()
}

object MiscUtils {
    private const val TAG = "MiscUtils"

    // 每次新建
    fun buildAppDatabase(): AppDatabase = PlatformFunctions.getAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(PlatformFunctions.ioDispatcher)
        .fallbackToDestructiveMigration(true)
        .build()

    fun buildAppPreferences(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { "${PlatformFunctions.getAppFilesPath()}/$APP_PREFERENCES_NAME".toPath() }
    )

    // 预制菜
    val defaultDispatcherScope = CoroutineScope(Dispatchers.Default)
    val ioDispatcherScope = CoroutineScope(PlatformFunctions.ioDispatcher)
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
    val Long.timeStr: String
        get() = PlatformFunctions.formatEpochSecond(this)
}
