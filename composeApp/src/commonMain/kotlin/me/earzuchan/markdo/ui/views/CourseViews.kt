package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import lib.fetchmoodle.CourseModule
import lib.fetchmoodle.CourseModuleAvailability
import lib.fetchmoodle.CourseSection
import lib.fetchmoodle.SectionLike
import me.earzuchan.markdo.data.models.MoodleTextContext
import me.earzuchan.markdo.data.models.MoodleTextLocation
import me.earzuchan.markdo.duties.CourseDetailDuty
import me.earzuchan.markdo.duties.CourseDuty
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.ResUtils.t
import org.jetbrains.compose.resources.DrawableResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursePage(duty: CourseDuty) = Children(duty.navStack, Modifier.fillMaxSize()) { created ->
    when (val ins = created.instance) {
        is CourseDuty -> AllCoursesPage(ins)

        is CourseDetailDuty -> CourseDetailPage(ins)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCoursesPage(duty: CourseDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.courses.t) })
}) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        when (val s = state) {
            is MoodleService.CoursesState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is MoodleService.CoursesState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.refetchCourses() }) { Text(Res.string.retry.t) }
            }

            is MoodleService.CoursesState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data) { course ->
                    val courseKey = MoodleTextLocation.key("course", "all", MoodleTextLocation.seg("course", course.id))
                    ListItem(
                        {
                            MoodleText(
                                course.name,
                                context = MoodleTextContext("$courseKey/name"),
                                onClick = { duty.navCourseDetail(course.id) },
                            )
                        },
                        Modifier.clickable { duty.navCourseDetail(course.id) },
                        trailingContent = {
                            MoodleText(
                                course.category,
                                context = MoodleTextContext("$courseKey/category"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onClick = { duty.navCourseDetail(course.id) },
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailPage(duty: CourseDetailDuty) {
    val state by duty.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar({
            when (val s = state) {
                is CourseDetailDuty.UIState.Success -> {
                    val courseKey = MoodleTextLocation.key("course", "detail", MoodleTextLocation.seg("course", s.data.id))
                    MoodleText(
                        rawText = s.data.name,
                        context = MoodleTextContext("$courseKey/title"),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                else -> Text(Res.string.course_detail.t)
            }
        }, navigationIcon = { IconButton({ duty.naviBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
    }) { padding ->
        val state by duty.state.collectAsState()

        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
            when (val s = state) {
                is CourseDetailDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                is CourseDetailDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.msg, color = MaterialTheme.colorScheme.error)
                    Button({ duty.loadCourse() }) { Text(Res.string.retry.t) }
                }

                is CourseDetailDuty.UIState.Success -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        s.data.sections.forEach { section -> item(section.id) { SectionView(section, duty, s.data.id) } }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionView(section: SectionLike, duty: CourseDetailDuty, courseId: Int): Unit = Column(
    Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
        .padding(vertical = 16.dp)
) {
    val sectionKey = remember(section, courseId) { buildSectionKey(courseId, section) }

    // 1. 头部永远显示
    SectionHeader(section, sectionKey)

    // 2. 遍历模块，将“上一个模块”的信息传给分发器
    section.modules.forEachIndexed { index, module ->
        val prevModule = if (index > 0) section.modules[index - 1] else null

        ModuleItemDispatcher(module, prevModule, index == 0, duty, courseId, sectionKey)
    }
}

@Composable
fun ModuleItemDispatcher(module: CourseModule, prevModule: CourseModule?, isFirst: Boolean, duty: CourseDetailDuty, courseId: Int, sectionKey: String) {
    // 判断当前模块和上一个模块是否都是 ListItem 类型
    val currentIsFull = isFullWidth(module)
    val prevIsFull = prevModule?.let { isFullWidth(it) } ?: false

    // 间距逻辑：
    // 1. 如果是第一个模块，且 Header 存在，必须有 16dp Gap
    // 2. 如果当前或前一个是 Inset 类型，必须有 16dp Gap
    // 3. 只有连续两个 FullWidth 之间是 0 Gap
    val needTopGap = isFirst || !currentIsFull || !prevIsFull

    Column(Modifier.fillMaxWidth()) {
        if (needTopGap) Spacer(Modifier.height(16.dp))

        // 统一处理左右内边距：如果是 FullWidth 类型则为 0，否则为 16.dp
        val horizontalPadding = if (currentIsFull) 0.dp else 16.dp

        Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
            val moduleKey = MoodleTextLocation.key(sectionKey, MoodleTextLocation.seg("module", module.id))
            when (module) {
                is CourseModule.SubSection -> SectionView(module, duty, courseId) // 递归
                is CourseModule.Label -> LabelView(module, moduleKey)
                is CourseModule.Resource -> ResourceView(module, moduleKey) { /*...*/ }
                is CourseModule.Assignment -> AssignmentView(module, moduleKey) { /*...*/ }
                is CourseModule.Forum -> SimpleModuleView(module, Res.drawable.ic_forum_24px, moduleKey) { /*...*/ }
                is CourseModule.Quiz -> QuizView(module, moduleKey) { /*...*/ }
                else -> SimpleModuleView(module, Res.drawable.ic_extension_24px, moduleKey) { /*...*/ }
            }
        }
    }
}

// 提取判断逻辑
private fun isFullWidth(module: CourseModule): Boolean =
    module !is CourseModule.Label && module !is CourseModule.SubSection

@Composable
fun SectionHeader(section: SectionLike, sectionKey: String) = Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(16.dp)) {

    MoodleText(
        section.name,
        context = MoodleTextContext("$sectionKey/name"),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
    )

    section.summary?.let {
        if (it.isNotBlank()) MoodleText(
            it,
            context = MoodleTextContext("$sectionKey/summary"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun ResourceView(resource: CourseModule.Resource, moduleKey: String, onClick: (CourseModule.Resource) -> Unit) = ListItem(
    {
        MoodleText(
            resource.name,
            context = MoodleTextContext("$moduleKey/name"),
            onClick = { onClick(resource) },
        )
    },
    Modifier.clickable { onClick(resource) },
    supportingContent = {
        val info = listOfNotNull(resource.fileSize, resource.uploadDate, resource.availability?.description).joinToString(" · ")
        if (info.isNotBlank()) MoodleText(
            info,
            context = MoodleTextContext("$moduleKey/info"),
            onClick = { onClick(resource) },
        )
    },
    leadingContent = { MIcon(Res.drawable.ic_file_24px, MaterialTheme.colorScheme.primary) }, trailingContent = { resource.availability?.let { RestrictionBadge(it) } }
)

@Composable
fun AssignmentView(assign: CourseModule.Assignment, moduleKey: String, onClick: () -> Unit) = ListItem(
    {
        MoodleText(
            assign.name,
            context = MoodleTextContext("$moduleKey/name"),
            onClick = onClick,
        )
    },
    Modifier.clickable { onClick() },
    leadingContent = { MIcon(Res.drawable.ic_task_24px, MaterialTheme.colorScheme.tertiary) },
    supportingContent = {
        listOfNotNull(assign.openDate?.let { "${Res.string.open_date.t}$it" }, assign.dueDate?.let { "${Res.string.due_time.t}$it" }, assign.description).takeIf { it.isNotEmpty() }
            ?.let {
                MoodleText(
                    it.joinToString("\n"),
                    context = MoodleTextContext("$moduleKey/info"),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onClick,
                )
            }
    }
)

@Composable
fun LabelView(label: CourseModule.Label, moduleKey: String) {
    // TODO：简单处理 HTML，以后用库
    val plainText = remember(label.contentHtml) { label.contentHtml.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim() }

    if (plainText.isNotEmpty()) MoodleText(
        plainText,
        context = MoodleTextContext("$moduleKey/content"),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun SimpleModuleView(module: CourseModule, icon: DrawableResource, moduleKey: String, onClick: () -> Unit) = ListItem(
    {
        MoodleText(
            module.name,
            context = MoodleTextContext("$moduleKey/name"),
            onClick = onClick,
        )
    },
    Modifier.clickable { onClick() },
    leadingContent = { MIcon(icon) }
)

@Composable
fun RestrictionBadge(availability: CourseModuleAvailability) {
    if (availability.isRestricted) MIcon(Res.drawable.ic_block_24px)
}

@Composable
fun QuizView(quiz: CourseModule.Quiz, moduleKey: String, onClick: () -> Unit) = ListItem(
    {
        MoodleText(
            quiz.name,
            context = MoodleTextContext("$moduleKey/name"),
            onClick = onClick,
        )
    },
    Modifier.clickable { onClick() },
    leadingContent = { MIcon(Res.drawable.ic_quiz_24px, MaterialTheme.colorScheme.tertiary) },
    supportingContent = {
        listOfNotNull(quiz.openDate?.let { "${Res.string.open_date.t}$it" }, quiz.closeDate?.let { "${Res.string.close_time.t}$it" }, quiz.description, quiz.availability?.description).takeIf { it.isNotEmpty() }
            ?.let {
                MoodleText(
                    it.joinToString("\n"),
                    context = MoodleTextContext("$moduleKey/info"),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onClick,
                )
            }
    },
    trailingContent = { quiz.availability?.let { RestrictionBadge(it) } }
)

private fun buildSectionKey(courseId: Int, section: SectionLike): String {
    val sectionId = when (section) {
        is CourseSection -> section.id
        is CourseModule.SubSection -> section.id
        else -> section.name.hashCode()
    }

    return MoodleTextLocation.key("course", "detail", MoodleTextLocation.seg("course", courseId), MoodleTextLocation.seg("section", sectionId))
}
