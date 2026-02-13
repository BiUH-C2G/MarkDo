package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.click_again_to_exit
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.ui.models.AppToastModel
import me.earzuchan.markdo.utils.MarkDoLog
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import me.earzuchan.markdo.utils.MiscUtils.mainDispatcherLaunch
import me.earzuchan.markdo.utils.PlatformFunctions
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class AppDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    companion object {
        private const val TAG = "AppDuty"
    }

    val moodleService: MoodleService by inject()

    var lastBackTime: Long = 0L

    lateinit var clickAgainStr: String

    init {
        // Init Data
        ioDispatcherLaunch {
            clickAgainStr = getString(Res.string.click_again_to_exit)
            when (moodleService.bootstrap()) {
                MoodleService.BootstrapRoute.Main -> {
                    mainDispatcherLaunch { navigation.replaceAll(AppNavis.Main) }
                    moodleService.autoLogin(allowOfflineFallback = true)
                }

                MoodleService.BootstrapRoute.SplashAndLogin -> {
                    mainDispatcherLaunch { navigation.replaceAll(AppNavis.Splash) }
                    moodleService.autoLogin(allowOfflineFallback = false)
                }

                MoodleService.BootstrapRoute.Login -> {
                    mainDispatcherLaunch { navigation.replaceAll(AppNavis.Login) }
                }
            }
        }

        // TODO：实现子视图的返回
        backHandler.register(object : BackCallback() {
            override fun onBack() {
                // For sub ui
                val ins = navStack.active.instance
                if (ins is ICanHandleBack) {
                    val backed = ins.back()
                    MarkDoLog.d(TAG, "$ins，情况：$backed")
                    if (backed) return
                }

                val nowBackTime = PlatformFunctions.currentTimeMillis() // 或者 SystemClock.elapsedRealtime()

                if (nowBackTime - lastBackTime < 2000) PlatformFunctions.stopApp()
                else {
                    lastBackTime = nowBackTime

                    showToast(clickAgainStr)
                }
            }
        })

        // Toast Loop
        mainDispatcherLaunch {
            for (model in toastChannel) {
                toast.value = model

                // 等待显示时间
                delay(model.duration)

                // 结束后隐藏，并加一点淡出缓冲
                toast.value = null
                delay(300)
            }
        }

        // Screen Navigation
        mainDispatcherLaunch {
            moodleService.authState.collect { state ->
                if (state is MoodleService.AuthState.Unauthed) {
                    if (state.reason.isNotBlank() && navStack.active.instance !is LoginDuty) showToast(state.reason)

                    navigation.replaceAll(AppNavis.Login)
                }
                else if (state is MoodleService.AuthState.Authed) navigation.replaceAll(AppNavis.Main)
            }
        }
    }

    val toast = MutableStateFlow<AppToastModel?>(null)
    private val toastChannel = Channel<AppToastModel>(Channel.UNLIMITED)

    fun showToast(string: String, isLong: Boolean = false) {
        val duration = if (isLong) 3500L else 2000L

        // 将请求发送到管道中排队
        toastChannel.trySend(AppToastModel(string, duration))
    }

    private val navigation = StackNavigation<AppNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, AppNavis.serializer(), AppNavis.Splash, "AppStack", false, ::mapDuties)

    private fun mapDuties(navi: AppNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is AppNavis.Splash -> SplashDuty(subCtx)

        is AppNavis.Main -> MainDuty(subCtx)

        is AppNavis.Login -> LoginDuty(subCtx)
    }
}

interface ICanHandleBack {
    fun back(): Boolean
}
