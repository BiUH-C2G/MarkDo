package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.earzuchan.markdo.duties.DashboardDuty
import me.earzuchan.markdo.data.models.MoodleTextContext
import me.earzuchan.markdo.data.models.MoodleTextLocation
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.DataUtils.timeStr
import me.earzuchan.markdo.utils.ResUtils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPage(duty: DashboardDuty) {
    val recentState by duty.recentItemsState.collectAsState()
    val timelineState by duty.timelineState.collectAsState()

    Scaffold(
        topBar = { TopAppBar({ Text(Res.string.dashboard.t) }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item("t") { TimelineSection(timelineState, duty) }

            item("r") { RecentItemsSection(recentState, duty) }
        }
    }
}

@Composable
fun RecentItemsSection(state: MoodleService.RecentItemsState, duty: DashboardDuty) = OutlinedCard(Modifier.fillMaxWidth()) {
    Column {
        Text(Res.string.recent_items.t, Modifier.padding(16.dp), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)

        when (state) {
            is MoodleService.RecentItemsState.Loading -> LoadingBox()

            is MoodleService.RecentItemsState.Error -> ErrorBox(state.msg) { duty.refetchRecent() }

            is MoodleService.RecentItemsState.Success -> {
                if (state.data.isEmpty()) EmptyBox(Res.string.no_recent_items.t) else Column {
                    state.data.forEach { item ->
                        val itemKey = MoodleTextLocation.key("dashboard", "recent", MoodleTextLocation.seg("item", item.id))

                        ListItem(
                            {
                                MoodleText(
                                    item.name,
                                    context = MoodleTextContext("$itemKey/name"),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            Modifier.clickable {},
                            supportingContent = {
                                MoodleText(
                                    item.courseName,
                                    context = MoodleTextContext("$itemKey/course_name"),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingContent = { TypeIcon(item.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineSection(state: MoodleService.TimelineState, duty: DashboardDuty) = OutlinedCard(Modifier.fillMaxWidth()) {
    Column {
        Text(Res.string.timeline.t, Modifier.padding(16.dp), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)

        when (state) {
            is MoodleService.TimelineState.Loading -> LoadingBox()

            is MoodleService.TimelineState.Error -> ErrorBox(state.msg) { duty.refetchTimeline() }

            is MoodleService.TimelineState.Success -> {
                if (state.data.isEmpty()) EmptyBox(Res.string.empty_timeline.t) else Column {
                    state.data.forEach { item ->
                        val itemKey = MoodleTextLocation.key("dashboard", "timeline", MoodleTextLocation.seg("event", item.id))

                        ListItem(
                            {
                                MoodleText(
                                    item.name,
                                    context = MoodleTextContext("$itemKey/name"),
                                    color = if (item.isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified,
                                )
                            },
                            Modifier.clickable { },
                            supportingContent = {
                                Column {
                                    MoodleText(
                                        item.courseName,
                                        context = MoodleTextContext("$itemKey/course_name"),
                                        style = MaterialTheme.typography.bodySmall,
                                    )

                                    Text(
                                        "${Res.string.due_time.t}${item.deadline.timeStr}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (item.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingContent = {
                                TextButton({ }) {
                                    MoodleText(
                                        item.actionName,
                                        context = MoodleTextContext("$itemKey/action_name"),
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- 辅助小组件 ---

// CHECK：这个没准可以移到更好的位置
@Composable
fun TypeIcon(type: String) {
    val icon = when (type.lowercase()) {
        "assign" -> Res.drawable.ic_task_24px
        "resource", "folder" -> Res.drawable.ic_file_24px
        "quiz" -> Res.drawable.ic_quiz_24px
        else -> Res.drawable.ic_extension_24px
    }

    MIcon(icon, MaterialTheme.colorScheme.secondary)
}

@Composable
fun LoadingBox() = Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), Alignment.Center) {
    CircularProgressIndicator(Modifier.size(32.dp))
}

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) = Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally) {
    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    TextButton(onRetry) { Text(Res.string.retry.t) }
}

@Composable
fun EmptyBox(text: String) = Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), Alignment.Center) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
