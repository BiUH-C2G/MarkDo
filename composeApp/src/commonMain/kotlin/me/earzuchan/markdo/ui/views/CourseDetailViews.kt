package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lib.fetchmoodle.CourseModule
import lib.fetchmoodle.CourseModuleAvailability
import lib.fetchmoodle.SectionLike
import me.earzuchan.markdo.duties.CourseDetailDuty
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.ic_arrow_back_24px
import me.earzuchan.markdo.resources.ic_block_24px
import me.earzuchan.markdo.resources.ic_extension_24px
import me.earzuchan.markdo.resources.ic_file_24px
import me.earzuchan.markdo.resources.ic_forum_24px
import me.earzuchan.markdo.resources.ic_quiz_24px
import me.earzuchan.markdo.resources.ic_task_24px
import me.earzuchan.markdo.utils.ResUtils.vector
import org.jetbrains.compose.resources.DrawableResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailPage(duty: CourseDetailDuty) {
    val state by duty.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar({
            Text(
                when (val s = state) {
                    is CourseDetailDuty.UIState.Success -> s.data.name
                    else -> "课程详情"
                }
            )
        }, navigationIcon = { IconButton({ duty.naviBack() }) { Icon(Res.drawable.ic_arrow_back_24px.vector, "返回") } })
    }) { padding ->
        val state by duty.state.collectAsState()

        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
            when (val s = state) {
                is CourseDetailDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is CourseDetailDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.msg, color = MaterialTheme.colorScheme.error)
                    Button({ duty.loadCourse() }) { Text("重试") }
                }

                is CourseDetailDuty.UIState.Success -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        s.data.sections.forEach { section -> item(section.id) { SectionView(section, duty) } }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionView(section: SectionLike, duty: CourseDetailDuty): Unit = Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large).padding(vertical = 16.dp)) {
    SectionHeader(section)

    section.modules.forEach { module -> ModuleItemDispatcher(module, duty) }
}

@Composable
fun ModuleItemDispatcher(module: CourseModule, duty: CourseDetailDuty) = when (module) {
    is CourseModule.SubSection -> SectionView(module, duty)

    is CourseModule.Label -> LabelView(module)

    is CourseModule.Resource -> ResourceView(module) { /* duty.openResource(it) */ }

    is CourseModule.Assignment -> AssignmentView(module) { /* duty.openAssign(it) */ }

    is CourseModule.Forum -> SimpleModuleView(module, Res.drawable.ic_forum_24px) { /* duty.openForum(it) */ }

    is CourseModule.Quiz -> QuizView(module) {/* duty.openQuiz(it) */ }

    else -> SimpleModuleView(module, Res.drawable.ic_extension_24px) {}
}

@Composable
fun SectionHeader(section: SectionLike) = Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
    val modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)

    Text(section.name, modifier, MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)

    section.summary?.let { if (it.isNotBlank()) Text(it, modifier, MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun ResourceView(resource: CourseModule.Resource, onClick: (CourseModule.Resource) -> Unit) = ListItem(
    { Text(resource.name) }, Modifier.clickable { onClick(resource) },
    supportingContent = {
        val info = listOfNotNull(resource.fileSize, resource.uploadDate, resource.availability?.description).joinToString(" · ")
        if (info.isNotBlank()) Text(info)
    },
    leadingContent = { Icon(Res.drawable.ic_file_24px.vector, "文件", tint = MaterialTheme.colorScheme.primary) }, trailingContent = { resource.availability?.let { RestrictionBadge(it) } }
)

@Composable
fun AssignmentView(assign: CourseModule.Assignment, onClick: () -> Unit) = ListItem(
    { Text(assign.name) }, Modifier.clickable { onClick() }, leadingContent = { Icon(Res.drawable.ic_task_24px.vector, "作业", tint = MaterialTheme.colorScheme.tertiary) },
    supportingContent = { Text(listOfNotNull("开始日期：${assign.openDate}", "截止日期：${assign.dueDate}", assign.description).joinToString("\n"), color = MaterialTheme.colorScheme.error) }
)

@Composable
fun LabelView(label: CourseModule.Label) {
    // TODO：简单处理 HTML，以后用库
    val plainText = remember(label.contentHtml) { label.contentHtml.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim() }

    if (plainText.isNotEmpty()) Text(plainText, Modifier.padding(horizontal = 16.dp), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SimpleModuleView(module: CourseModule, icon: DrawableResource, onClick: () -> Unit) = ListItem(
    { Text(module.name) }, Modifier.clickable { onClick() }, leadingContent = { Icon(icon.vector, null) }
)

@Composable
fun RestrictionBadge(availability: CourseModuleAvailability) {
    if (availability.isRestricted) Icon(Res.drawable.ic_block_24px.vector, "受限")
}

@Composable
fun QuizView(quiz: CourseModule.Quiz, onClick: () -> Unit) = ListItem(
    { Text(quiz.name) }, Modifier.clickable { onClick() }, leadingContent = { Icon(Res.drawable.ic_quiz_24px.vector, "测试", tint = MaterialTheme.colorScheme.tertiary) },
    supportingContent = { Text(listOfNotNull("开始日期：${quiz.openDate}", "截止日期：${quiz.closeDate}", quiz.description, quiz.availability?.description).joinToString("\n"), color = MaterialTheme.colorScheme.error) },
    trailingContent = { quiz.availability?.let { RestrictionBadge(it) } }
)