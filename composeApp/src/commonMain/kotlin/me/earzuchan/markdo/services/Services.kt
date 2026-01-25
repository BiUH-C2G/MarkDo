package me.earzuchan.markdo.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleResult
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository

sealed class AuthState {
    data object Initial : AuthState()
    data object Busy : AuthState()
    data object Authed : AuthState()
    data class Unauthed(val reason: String) : AuthState()
}

class AuthService(val moodleFetcher: MoodleFetcher, val prefRepo: AppPreferenceRepository) {
    val state = MutableStateFlow<AuthState>(AuthState.Initial)

    private companion object {
        val String.https get() = "https://$this"
    }

    suspend fun autoLogin() {
        val s = state.value
        if (s !is AuthState.Initial && s !is AuthState.Unauthed) throw IllegalStateException("此时不能自动登录")

        state.value = AuthState.Busy

        val (site, user, pwd) = combine(prefRepo.baseSite, prefRepo.username, prefRepo.password) { a, b, c -> Triple(a, b, c) }.first()

        if (site.isBlank() || user.isBlank() || pwd.isBlank()) {
            state.value = AuthState.Unauthed("缺少登录信息")
            return
        }

        when (val result = moodleFetcher.login(site.https, user, pwd)) {
            is MoodleResult.Success -> state.value = AuthState.Authed

            is MoodleResult.Failure -> state.value = AuthState.Unauthed(result.exception.message ?: "登录失败：未知原因")
        }
    }

    suspend fun manualLogin(site: String, user: String, pwd: String) {
        val s = state.value
        if (s !is AuthState.Initial && s !is AuthState.Unauthed) throw IllegalStateException("此时不能手动登录")

        state.value = AuthState.Busy

        if (site.isBlank() || user.isBlank() || pwd.isBlank()) {
            state.value = AuthState.Unauthed("缺少登录信息")
            return
        }

        when (val result = moodleFetcher.login(site.https, user, pwd)) {
            is MoodleResult.Success -> {
                prefRepo.setBaseSite(site)
                prefRepo.setUsername(user)
                prefRepo.setPassword(pwd)

                state.value = AuthState.Authed
            }

            is MoodleResult.Failure -> state.value = AuthState.Unauthed("登录失败：${result.exception.message}")
        }
    }

    suspend fun logout() {
        if (state.value !is AuthState.Authed) throw IllegalStateException("此时不能退出登录")

        state.value = AuthState.Busy

        moodleFetcher.clearSessionData()
        prefRepo.resetLoginData()

        state.value = AuthState.Unauthed("用户主动退出登录")
    }
}