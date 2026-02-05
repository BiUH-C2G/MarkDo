package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.utils.MarkDoLog
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SplashDuty(ctx: ComponentContext) : ComponentContext by ctx

class LoginDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    private companion object {
        const val TAG = "LoginDuty"
    }

    private val moodleService: MoodleService by inject()
    private val appPrefRepo: AppPreferenceRepository by inject()

    // UI 状态
    val baseSite = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")

    val errorMessage = MutableStateFlow<String?>(null)
    val disableButton = MutableStateFlow(false)
    val inHere = MutableStateFlow(false) // 只显示在当前界面带来的Unauthed

    init {
        ioDispatcherLaunch {
            baseSite.value = appPrefRepo.baseSite.first()
            username.value = appPrefRepo.username.first()
            password.value = appPrefRepo.password.first()

            MarkDoLog.i(TAG, "带派吗老弟：${username.value}，${password.value}")

            moodleService.authState.collect {
                if (it is MoodleService.AuthState.Unauthed && inHere.value) {
                    errorMessage.value = it.reason
                    disableButton.value = false
                }
            }
        }
    }

    fun onLoginClick() {
        inHere.value = true

        val site = baseSite.value
        val user = username.value
        val pwd = password.value

        if (site.isBlank() || user.isBlank() || pwd.isBlank()) {
            errorMessage.value = "站点、用户名、密码不能为空"
            return
        }

        ioDispatcherLaunch {
            disableButton.value = true
            errorMessage.value = null

            moodleService.manualLogin(site, user, pwd)
        }
    }
}