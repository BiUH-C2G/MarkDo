package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
import me.earzuchan.markdo.data.models.SavedLoginAccount
import me.earzuchan.markdo.data.preferences.AppPreferences
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SplashDuty(ctx: ComponentContext) : ComponentContext by ctx

class LoginDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    private val moodleService: MoodleService by inject()

    // UI 状态
    val baseSite = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val rememberedAccounts = MutableStateFlow<List<SavedLoginAccount>>(emptyList())

    val errorMessage = MutableStateFlow<String?>(null)
    val disableButton = MutableStateFlow(false)
    val inHere = MutableStateFlow(false) // 只显示在当前界面带来的Unauthed

    init {
        ioDispatcherLaunch {
            val draft = moodleService.getPreferredLoginDraft()
            baseSite.value = draft.baseSite
            username.value = draft.username
            password.value = draft.password

            rememberedAccounts.value = moodleService.getRememberedAccounts()

            moodleService.authState.collect {
                if (it is MoodleService.AuthState.Unauthed) {
                    if (it.reason == MoodleService.AUTH_MSG_USER_LOGOUT) {
                        baseSite.value = AppPreferences.DEFAULT_BASE_SITE
                        username.value = ""
                        password.value = ""
                        errorMessage.value = null
                        disableButton.value = false
                        inHere.value = false
                    } else if (inHere.value) {
                        errorMessage.value = it.reason
                        disableButton.value = false
                    }
                }
            }
        }

        ioDispatcherLaunch {
            moodleService.rememberedAccounts.collect { accounts ->
                rememberedAccounts.value = accounts
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

    fun onRememberedAccountClick(accountKey: String) {
        ioDispatcherLaunch {
            val draft = moodleService.getLoginDraftByAccountKey(accountKey) ?: return@ioDispatcherLaunch
            baseSite.value = draft.baseSite
            username.value = draft.username
            password.value = draft.password
            errorMessage.value = null
        }
    }

    fun wannaDelete(accountKey: String) {
        ioDispatcherLaunch {
            val removed = moodleService.removeRememberedAccount(accountKey)

            if (!removed) errorMessage.value = "当前账号不支持删除"
        }
    }
}
