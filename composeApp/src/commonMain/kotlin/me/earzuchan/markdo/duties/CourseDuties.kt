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
import lib.fetchmoodle.MoodleCourse
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleResult
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class CourseDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent, ICanHandleBack {
    companion object {
        private const val TAG = "CourseDuty"
    }

    private val moodleService by inject<MoodleService>()

    override fun back() = if (navStack.backStack.isNotEmpty()) {
        navBack()
        true
    } else false

    private val navigation = StackNavigation<CourseNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, CourseNavis.serializer(), CourseNavis.AllCourses, "CourseStack", false, ::mapDuties)

    private fun mapDuties(navi: CourseNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is CourseNavis.AllCourses -> this

        is CourseNavis.CourseDetail -> CourseDetailDuty(subCtx, navi.courseId) { navBack() }
    }

    fun navBack() = navigation.replaceAll(CourseNavis.AllCourses)

    fun navCourseDetail(courseId: Int) = navigation.bringToFront(CourseNavis.CourseDetail(courseId))

    val state = moodleService.coursesState

    fun refetchCourses() = ioDispatcherLaunch { moodleService.refreshCourses() }
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