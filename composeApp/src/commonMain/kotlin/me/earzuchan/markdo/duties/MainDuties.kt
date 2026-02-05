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
import lib.fetchmoodle.MoodleTimelineEvent
import me.earzuchan.markdo.services.MoodleService
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
    private val moodleService: MoodleService by inject()

    val recentItemsState = moodleService.recentItemsState

    val timelineState = moodleService.timelineState

    fun refetchRecent() = ioDispatcherLaunch{ moodleService.refreshRecentItems() }

    fun refetchTimeline()= ioDispatcherLaunch{ moodleService.refreshTimeline() }
}