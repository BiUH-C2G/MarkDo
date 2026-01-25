package me.earzuchan.markdo.duties

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import lib.fetchmoodle.MoodleFetcher
import lib.fetchmoodle.MoodleRecentItem
import lib.fetchmoodle.MoodleResult
import lib.fetchmoodle.MoodleTimelineEvent
import me.earzuchan.markdo.utils.MarkDoLog
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue


class MainDuty(ctx: ComponentContext) : ComponentContext by ctx, ICanHandleBack {
    companion object {
        private const val TAG = "MainDuty"
    }

    override fun back(): Boolean {
        val ins = navStack.active.instance
        return if (ins is ICanHandleBack) {
            val backed = ins.back()
            MarkDoLog.d(TAG, "$ins，情况：${backed}")
            backed
        } else false
    }

    private val navigation = StackNavigation<MainNavis>()

    val navStack: Value<ChildStack<*, ComponentContext>> = childStack(navigation, MainNavis.serializer(), MainNavis.Dashboard, "MainStack", false, ::mapDuties)

    private fun mapDuties(navi: MainNavis, subCtx: ComponentContext): ComponentContext = when (navi) {
        is MainNavis.Dashboard -> DashboardDuty(subCtx)

        is MainNavis.Course -> CourseDuty(subCtx)

        is MainNavis.My -> MyDuty(subCtx)
    }

    fun navDashboard() = navigation.bringToFront(MainNavis.Dashboard)

    fun navCourse() = navigation.bringToFront(MainNavis.Course)

    fun navMy() = navigation.bringToFront(MainNavis.My)
}

class DashboardDuty(ctx: ComponentContext) : ComponentContext by ctx, KoinComponent {
    private val moodleFetcher: MoodleFetcher by inject()

    sealed interface RecentItemsState {
        object Loading : RecentItemsState
        data class Success(val data: List<MoodleRecentItem>) : RecentItemsState
        data class Error(val msg: String) : RecentItemsState
    }

    sealed interface TimelineState {
        object Loading : TimelineState
        data class Success(val data: List<MoodleTimelineEvent>) : TimelineState
        data class Error(val msg: String) : TimelineState
    }

    val recentItemsState = MutableStateFlow<RecentItemsState>(RecentItemsState.Loading)
    val timelineState = MutableStateFlow<TimelineState>(TimelineState.Loading)

    init {
        fetchData()
    }

    fun fetchData() {
        // 两线程拉取

        ioDispatcherLaunch {
            recentItemsState.value = RecentItemsState.Loading
            when (val result = moodleFetcher.getRecentItems()) {
                is MoodleResult.Success -> recentItemsState.value = RecentItemsState.Success(result.data)
                is MoodleResult.Failure -> recentItemsState.value = RecentItemsState.Error(result.exception.message ?: "最近的项目加载失败")
            }
        }

        ioDispatcherLaunch {
            timelineState.value = TimelineState.Loading
            when (val result = moodleFetcher.getTimeline()) {
                is MoodleResult.Success -> timelineState.value = TimelineState.Success(result.data)
                is MoodleResult.Failure -> timelineState.value = TimelineState.Error(result.exception.message ?: "时间线加载失败")
            }
        }
    }
}