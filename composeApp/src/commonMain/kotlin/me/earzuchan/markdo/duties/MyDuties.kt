package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import lib.fetchmoodle.MoodleCourseGrade
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleResult
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.services.AuthService
import me.earzuchan.markdo.utils.MarkDoLog
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import me.earzuchan.markdo.utils.MiscUtils.mainDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class MyDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent, ICanHandleBack {
    companion object {
        private const val TAG = "MyDuty"
    }

    override fun back() = if (navStack.backStack.isNotEmpty()) {
        navBack()
        true
    } else false

    val appPrefRepo: AppPreferenceRepository by inject()
    val authService: AuthService by inject()
    val moodleFetcher: MoodleFetcher by inject()

    fun logout() = ioDispatcherLaunch { authService.logout() }

    val userName = MutableStateFlow("加载用户姓名中")

    init {
        ioDispatcherLaunch {
            val result = moodleFetcher.getUserProfile()
            userName.value = if (result is MoodleResult.Success) result.data.name
            else "用户：${appPrefRepo.username.first()}"
        }
    }

    private val navigation = StackNavigation<MyNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, MyNavis.serializer(), MyNavis.Overview, "MyStack", false, ::mapDuties)

    private fun mapDuties(navi: MyNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is MyNavis.Overview -> this

        is MyNavis.Grades -> GradesDuty(subCtx) { navBack() }

        is MyNavis.Settings -> SettingsDuty(subCtx) { navBack() }
    }

    fun navBack() = navigation.replaceAll(MyNavis.Overview)

    fun navGrades() = navigation.bringToFront(MyNavis.Grades)

    fun navSettings() = navigation.bringToFront(MyNavis.Settings)
}

class GradesDuty(ctx: ComponentContext, val navBack: () -> Unit) : ComponentContext by ctx, KoinComponent {
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

class SettingsDuty(ctx: ComponentContext, val navBack: () -> Unit) : ComponentContext by ctx, KoinComponent {
    val appPrefRepo: AppPreferenceRepository by inject()
}