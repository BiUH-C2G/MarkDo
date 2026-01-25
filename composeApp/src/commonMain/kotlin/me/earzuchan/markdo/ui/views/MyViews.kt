package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.markdo.duties.GradesDuty
import me.earzuchan.markdo.duties.MyDuty
import me.earzuchan.markdo.duties.SettingsDuty
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.ResUtils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPage(duty: MyDuty) = Children(duty.navStack, Modifier.fillMaxSize()) { created ->
    when (val ins = created.instance) {
        is MyDuty -> OverviewPage(ins)

        is GradesDuty -> GradesPage(ins)

        is SettingsDuty -> SettingsPage(ins)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPage(duty: MyDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.my.t) })
}) { padding ->
    var showWhetherLogout by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val userName by duty.userName.collectAsState()

    if (showWhetherLogout) MAlertDialog(
        Res.string.sure_logout.t,
        Res.drawable.ic_logout_24px,
        null,
        listOf(DialogActionItem(Res.string.confirm.t) { duty.logout() }, DialogActionItem(Res.string.cancel.t)),
        true,
        { showWhetherLogout = false })
    if (showAbout) MAlertDialog(Res.string.about.t, Res.drawable.ic_info_24px, Res.string.about_desc.t, listOf(DialogActionItem(Res.string.ok.t)), true, { showAbout = false })

    LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        item("name") {
            Text(userName, Modifier.padding(16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }

        item("hello") {
            Text(Res.string.hello.t, Modifier.padding(bottom = 16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        item("grades") {
            ListItem({ Text(Res.string.grades.t) }, Modifier.clickable { duty.navGrades() }, leadingContent = { MIcon(Res.drawable.ic_list_24px) })
        }

        item("trans_man") {
            ListItem({ Text(Res.string.trans_man.t) }, Modifier.clickable { }, leadingContent = { MIcon(Res.drawable.ic_translate_24px) })
        }

        item("settings") {
            ListItem({ Text(Res.string.settings.t) }, Modifier.clickable { duty.navSettings() }, leadingContent = { MIcon(Res.drawable.ic_settings_24px) })
        }

        item("about") {
            ListItem({ Text(Res.string.about.t) }, Modifier.clickable { showAbout = true }, leadingContent = { MIcon(Res.drawable.ic_info_24px) })
        }

        item("logout") {
            ListItem({ Text(Res.string.logout.t) }, Modifier.clickable { showWhetherLogout = true }, leadingContent = { MIcon(Res.drawable.ic_logout_24px) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesPage(duty: GradesDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.grades.t) }, navigationIcon = { IconButton({ duty.navBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
}) { padding ->
    val state by duty.state.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        when (val s = state) {
            is GradesDuty.UIState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is GradesDuty.UIState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.msg, color = MaterialTheme.colorScheme.error)
                Button({ duty.loadGrades() }) { Text(Res.string.retry.t) }
            }

            is GradesDuty.UIState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                items(s.data) {
                    ListItem({ Text(it.name) }, trailingContent = { Text(it.grade, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) })
                }
            }
        }
    }
}

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(duty: SettingsDuty) = Scaffold(topBar = {
    TopAppBar({ Text(Res.string.settings.t) }, navigationIcon = { IconButton({ duty.navBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } })
}) { padding -> }