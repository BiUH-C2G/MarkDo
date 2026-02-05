package me.earzuchan.markdo.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import lib.fetchmoodle.*
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch

class MoodleService(val moodleFetcher: MoodleFetcher, val prefRepo: AppPreferenceRepository) {
    sealed class AuthState {
        data object Initial : AuthState()
        data object Busy : AuthState()
        data object Authed : AuthState()
        data class Unauthed(val reason: String) : AuthState()
    }

    val authState = MutableStateFlow<AuthState>(AuthState.Initial)

    val userProfile = MutableStateFlow<MoodleUserProfile?>(null)

    sealed interface TimelineState {
        object Loading : TimelineState
        data class Success(val data: List<MoodleTimelineEvent>) : TimelineState
        data class Error(val msg: String) : TimelineState
    }

    val timelineState = MutableStateFlow<TimelineState>(TimelineState.Loading)

    sealed interface RecentItemsState {
        object Loading : RecentItemsState
        data class Success(val data: List<MoodleRecentItem>) : RecentItemsState
        data class Error(val msg: String) : RecentItemsState
    }

    val recentItemsState = MutableStateFlow<RecentItemsState>(RecentItemsState.Loading)

    sealed interface CoursesState {
        object Loading : CoursesState
        data class Success(val data: List<MoodleCourseInfo>) : CoursesState
        data class Error(val msg: String) : CoursesState
    }

    val coursesState = MutableStateFlow<CoursesState>(CoursesState.Loading)

    companion object Companion {
        private val String.https get() = "https://$this"

        const val AUTH_MSG_NO_LOGIN_INFO = "缺少登录信息"
    }

    suspend fun autoLogin() {
        val s = authState.value
        if (s !is AuthState.Initial && s !is AuthState.Unauthed) throw IllegalStateException("此时不能自动登录")

        authState.value = AuthState.Busy

        val (site, user, pwd) = combine(prefRepo.baseSite, prefRepo.username, prefRepo.password) { a, b, c -> Triple(a, b, c) }.first()

        if (site.isBlank() || user.isBlank() || pwd.isBlank()) {
            authState.value = AuthState.Unauthed(AUTH_MSG_NO_LOGIN_INFO)
            return
        }

        authState.value = when (val result = moodleFetcher.login(site.https, user, pwd)) {
            is MoodleResult.Success -> {
                onLoginned()

                AuthState.Authed
            }

            is MoodleResult.Failure -> AuthState.Unauthed(result.exception.message ?: "登录失败：未知原因")
        }
    }

    suspend fun manualLogin(site: String, user: String, pwd: String) {
        val s = authState.value
        if (s !is AuthState.Initial && s !is AuthState.Unauthed) throw IllegalStateException("此时不能手动登录")

        authState.value = AuthState.Busy

        if (site.isBlank() || user.isBlank() || pwd.isBlank()) {
            authState.value = AuthState.Unauthed(AUTH_MSG_NO_LOGIN_INFO)
            return
        }

        authState.value = when (val result = moodleFetcher.login(site.https, user, pwd)) {
            is MoodleResult.Success -> {
                prefRepo.setBaseSite(site)
                prefRepo.setUsername(user)
                prefRepo.setPassword(pwd)

                onLoginned()

                AuthState.Authed
            }

            is MoodleResult.Failure -> AuthState.Unauthed("登录失败：${result.exception.message}")
        }
    }

    suspend fun logout() {
        if (authState.value !is AuthState.Authed) throw IllegalStateException("此时不能退出登录")

        authState.value = AuthState.Busy

        moodleFetcher.clearSessionData()
        prefRepo.resetLoginData()

        onLogoutted()
        authState.value = AuthState.Unauthed("用户主动退出登录")
    }

    private fun onLoginned() {
        ioDispatcherLaunch { refreshUserProfile() }
        ioDispatcherLaunch { refreshTimeline() }
        ioDispatcherLaunch { refreshRecentItems() }
        ioDispatcherLaunch { refreshCourses() }
    }

    private fun onLogoutted() {
        clearUserProfile()
        clearTimeline()
        clearRecentItems()
        clearCourses()
    }

    suspend fun refreshUserProfile(): Boolean = when (val result = moodleFetcher.getUserProfile()) {
        is MoodleResult.Success -> {
            userProfile.value = result.data
            true
        }

        else -> false // NOP：刷新失败
    }

    suspend fun refreshTimeline() {
        timelineState.value = when (val result = moodleFetcher.getTimeline()) {
            is MoodleResult.Success -> TimelineState.Success(result.data)

            is MoodleResult.Failure -> TimelineState.Error(result.exception.message ?: "未知")
        }
    }

    suspend fun refreshRecentItems() {
        recentItemsState.value = when (val result = moodleFetcher.getRecentItems()) {
            is MoodleResult.Success -> RecentItemsState.Success(result.data)

            is MoodleResult.Failure -> RecentItemsState.Error(result.exception.message ?: "未知")
        }
    }

    suspend fun refreshCourses() {
        coursesState.value = when (val result = moodleFetcher.getCourses()) {
            is MoodleResult.Success -> CoursesState.Success(result.data)

            is MoodleResult.Failure -> CoursesState.Error(result.exception.message ?: "未知")
        }
    }


    fun clearUserProfile() {
        userProfile.value = null
    }

    fun clearTimeline() {
        timelineState.value = TimelineState.Loading
    }

    fun clearRecentItems() {
        recentItemsState.value = RecentItemsState.Loading
    }

    fun clearCourses() {
        coursesState.value = CoursesState.Loading
    }
}