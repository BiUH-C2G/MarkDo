package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import lib.fetchmoodle.MoodleCourse
import lib.fetchmoodle.MoodleCourseGrade
import lib.fetchmoodle.MoodleCourseInfo
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleResult
import me.earzuchan.markdo.data.preferences.AppPreferences
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import me.earzuchan.markdo.utils.MiscUtils.mainDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    val moodleFetcher: MoodleFetcher by inject()
    val appPrefRepo: AppPreferenceRepository by inject()

    init {
        ioDispatcherLaunch {
            combine(appPrefRepo.baseSite, appPrefRepo.username, appPrefRepo.password) { site, user, pwd -> Triple(site, user, pwd) }.collectLatest { (site, user, pwd) ->
                val isValid = site.isNotEmpty() && user.isNotEmpty() && pwd.isNotEmpty() && moodleFetcher.login("https://$site", user, pwd) is MoodleResult.Success

                mainDispatcherLaunch {
                    if (isValid) navigation.replaceAll(AppNavis.Main)
                    else navigation.replaceAll(AppNavis.Login)
                }
            }
        }
    }

    private val navigation = StackNavigation<AppNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, AppNavis.serializer(), AppNavis.Splash, "AppStack", false, ::mapDuties)

    private fun mapDuties(navi: AppNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is AppNavis.Splash -> SplashDuty(subCtx)

        is AppNavis.Main -> MainDuty(subCtx) { logout() }

        is AppNavis.Login -> LoginDuty(subCtx) { navigation.replaceAll(AppNavis.Main) }
    }

    fun logout() {
        moodleFetcher.clearSessionData()

        ioDispatcherLaunch {
            appPrefRepo.setBaseSite(AppPreferences.DEFAULT_BASE_SITE)
            appPrefRepo.setUsername("")
            appPrefRepo.setPassword("")

            mainDispatcherLaunch { navigation.replaceAll(AppNavis.Login) }
        }
    }
}

class SplashDuty(ctx: ComponentContext) : ComponentContext by ctx

class MainDuty(ctx: ComponentContext, val logout: () -> Unit) : ComponentContext by ctx {
    private val navigation = StackNavigation<MainNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, MainNavis.serializer(), MainNavis.Grades, "MainStack", false, ::mapDuties)

    private fun mapDuties(navi: MainNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is MainNavis.Grades -> GradesDuty(subCtx)

        is MainNavis.Course -> CourseDuty(subCtx)

        is MainNavis.User -> UserDuty(subCtx, logout)
    }

    fun naviGrades() = navigation.bringToFront(MainNavis.Grades)

    fun naviCourse() = navigation.bringToFront(MainNavis.Course)

    fun naviUser() = navigation.bringToFront(MainNavis.User)
}

class LoginDuty(ctx: ComponentContext, private val onLoginSuccess: () -> Unit) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()
    private val appPrefRepo: AppPreferenceRepository by inject()

    // UI 状态
    val baseSite = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val isLoading = MutableStateFlow(false)

    init {
        ioDispatcherLaunch {
            baseSite.value = appPrefRepo.baseSite.first()
            username.value = appPrefRepo.username.first()
            password.value = appPrefRepo.password.first()
        }
    }

    fun onLoginClick() {
        val baseSite = baseSite.value
        val user = username.value
        val pwd = password.value

        if (baseSite.isBlank() || user.isBlank() || pwd.isBlank()) {
            errorMessage.value = "站点、用户名、密码不能为空"
            return
        }

        ioDispatcherLaunch {
            isLoading.value = true
            errorMessage.value = null

            when (val result = moodleFetcher.login("https://$baseSite", user, pwd)) {
                is MoodleResult.Success -> {
                    appPrefRepo.setBaseSite(baseSite)
                    appPrefRepo.setUsername(user)
                    appPrefRepo.setPassword(pwd)

                    mainDispatcherLaunch { onLoginSuccess() }
                }

                is MoodleResult.Failure -> errorMessage.value = "登录失败：" + (result.exception.message ?: "未知错误")
            }

            isLoading.value = false
        }
    }
}

class GradesDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()

    sealed interface UIState {
        object Loading : UIState
        data class Success(val data: List<MoodleCourseGrade>) : UIState
        data class Error(val msg: String) : UIState
    }

    private val _state = MutableStateFlow<UIState>(UIState.Loading)
    val state: StateFlow<UIState> = _state

    init {
        loadGrades()
    }

    fun loadGrades() {
        ioDispatcherLaunch {
            _state.value = UIState.Loading

            when (val result = moodleFetcher.getGrades()) {
                is MoodleResult.Success -> _state.value = UIState.Success(result.data)
                is MoodleResult.Failure -> _state.value = UIState.Error(result.exception.message ?: "成绩列表加载失败")
            }
        }
    }
}

class CourseDuty(ctx: ComponentContext) : ComponentContext by ctx {
    private val navigation = StackNavigation<CourseNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, CourseNavis.serializer(), CourseNavis.AllCourses, "CourseStack", false, ::mapDuties)

    private fun mapDuties(navi: CourseNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is CourseNavis.AllCourses -> AllCoursesDuty(subCtx) { naviCourseDetail(it) }

        is CourseNavis.CourseDetail -> CourseDetailDuty(subCtx, navi.courseId) { naviBack() }
    }

    fun naviBack() = navigation.replaceAll(CourseNavis.AllCourses)

    fun naviCourseDetail(courseId: Int) = navigation.bringToFront(CourseNavis.CourseDetail(courseId))
}

class AllCoursesDuty(ctx: ComponentContext, val onClickCourse: (Int) -> Unit) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()

    sealed interface UIState {
        object Loading : UIState
        data class Success(val data: List<MoodleCourseInfo>) : UIState
        data class Error(val msg: String) : UIState
    }

    private val _state = MutableStateFlow<UIState>(UIState.Loading)
    val state: StateFlow<UIState> = _state

    init {
        loadCourses()
    }

    fun loadCourses() {
        ioDispatcherLaunch {
            _state.value = UIState.Loading

            when (val result = moodleFetcher.getCourses()) {
                is MoodleResult.Success -> _state.value = UIState.Success(result.data)
                is MoodleResult.Failure -> _state.value = UIState.Error(result.exception.message ?: "课程列表加载失败")
            }
        }
    }
}

class CourseDetailDuty(ctx: ComponentContext, val courseId: Int, val naviBack: () -> Unit) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()

    sealed interface UIState {
        object Loading : UIState
        data class Success(val data: MoodleCourse) : UIState
        data class Error(val msg: String) : UIState
    }

    private val _state = MutableStateFlow<UIState>(UIState.Loading)
    val state: StateFlow<UIState> = _state

    init {
        loadCourse()
    }

    fun loadCourse() {
        ioDispatcherLaunch {
            _state.value = UIState.Loading

            when (val result = moodleFetcher.getCourseById(courseId)) {
                is MoodleResult.Success -> _state.value = UIState.Success(result.data)
                is MoodleResult.Failure -> _state.value = UIState.Error(result.exception.message ?: "课程加载失败")
            }
        }
    }
}

class UserDuty(ctx: ComponentContext, val logout: () -> Unit) : ComponentContext by ctx, KoinComponent {
    val appPrefRepo: AppPreferenceRepository by inject()

    lateinit var userName: String

    init {
        ioDispatcherLaunch { userName = appPrefRepo.username.first() }
    }
}