package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.markdo.data.models.MoodleTextContext
import me.earzuchan.markdo.data.models.MoodleTextLocation
import me.earzuchan.markdo.data.models.SavedLoginAccount
import me.earzuchan.markdo.data.models.TextTransformRule
import me.earzuchan.markdo.data.models.TextTransformRuleDraft
import me.earzuchan.markdo.data.models.TextTransformRuleType
import me.earzuchan.markdo.duties.GradesDuty
import me.earzuchan.markdo.duties.MyDuty
import me.earzuchan.markdo.duties.SettingsDuty
import me.earzuchan.markdo.duties.TextTranslationManageDuty
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.ui.widgets.MIcon
import me.earzuchan.markdo.utils.PlatformFunctions
import me.earzuchan.markdo.utils.ResUtils.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPage(duty: MyDuty) = Children(duty.navStack, Modifier.fillMaxSize()) { created ->
    when (val ins = created.instance) {
        is MyDuty -> OverviewPage(ins)
        is TextTranslationManageDuty -> TextTranslationManagePage(ins)
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
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var pendingDeleteAccount by remember { mutableStateOf<SavedLoginAccount?>(null) }

    val userName by duty.userName.collectAsState()
    val rememberedAccounts by duty.rememberedAccounts.collectAsState()
    val activeAccountKey by duty.activeAccountKey.collectAsState()
    val switchingAccount by duty.switchingAccount.collectAsState()
    val connectionState by duty.loginConnectionState.collectAsState()
    val loginStatusText = when (connectionState) {
        is MoodleService.LoginConnectionState.Onlined -> Res.string.login_status_online.t
        is MoodleService.LoginConnectionState.Offlined -> Res.string.login_status_offline_cached.t
        is MoodleService.LoginConnectionState.Onlining -> Res.string.login_status_onlining.t
        is MoodleService.LoginConnectionState.Unknown -> Res.string.login_status_unknown.t
    }

    if (showWhetherLogout) MAlertDialog(
        Res.string.sure_logout.t,
        Res.drawable.ic_logout_24px,
        null,
        listOf(DialogActionItem(Res.string.confirm.t) { duty.logout() }, DialogActionItem(Res.string.cancel.t)),
        true,
        { showWhetherLogout = false })

    if (showAbout) MAlertDialog(Res.string.about.t, Res.drawable.ic_info_24px, Res.string.about_desc.t, listOf(DialogActionItem(Res.string.ok.t)), true, { showAbout = false })

    if (showSwitchAccountDialog) MAlertDialog(Res.string.account_switch.t, null, null, listOf(DialogActionItem(Res.string.close.t)), false, { showSwitchAccountDialog = false }, {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
            rememberedAccounts.forEach { account ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.username, style = MaterialTheme.typography.bodyLarge)

                            Text(account.baseSite, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (account.accountKey == activeAccountKey) Text(Res.string.current_account.t, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        else Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                {
                                    duty.switchAccount(account.accountKey)
                                    showSwitchAccountDialog = false
                                }, enabled = !switchingAccount
                            ) { Text(Res.string.switch_account.t) }
                            TextButton({ pendingDeleteAccount = account }, enabled = !switchingAccount) { Text(Res.string.delete_account.t) }
                        }
                    }
                }
            }
        }
    })

    pendingDeleteAccount?.let { account ->
        MAlertDialog(
            Res.string.confirm_delete_account.t, Res.drawable.ic_block_24px, "${Res.string.delete_account_desc.t}\n${account.username}@${account.baseSite}", listOf(
                DialogActionItem(Res.string.cancel.t), DialogActionItem(Res.string.confirm.t) { duty.removeRememberedAccount(account.accountKey) }
            ), false, { pendingDeleteAccount = null })
    }

    LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Top))) {
        item("name") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = userName,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item("login_status") {
            Text(loginStatusText, Modifier.padding(bottom = 16.dp).fillMaxWidth(), MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        item("grades") {
            ListItem({ Text(Res.string.grades.t) }, Modifier.clickable { duty.navGrades() }, leadingContent = { MIcon(Res.drawable.ic_list_24px) })
        }

        item("trans_man") {
            ListItem({ Text(Res.string.trans_man.t) }, Modifier.clickable { duty.navTransMan() }, leadingContent = { MIcon(Res.drawable.ic_translate_24px) })
        }

        item("settings") {
            ListItem({ Text(Res.string.settings.t) }, Modifier.clickable { duty.navSettings() }, leadingContent = { MIcon(Res.drawable.ic_settings_24px) })
        }

        item("about") {
            ListItem({ Text(Res.string.about.t) }, Modifier.clickable { showAbout = true }, leadingContent = { MIcon(Res.drawable.ic_info_24px) })
        }

        if (rememberedAccounts.size > 1) item("account_switch") {
            ListItem({ Text(Res.string.account_switch.t) }, Modifier.clickable { showSwitchAccountDialog = true }, leadingContent = { MIcon(Res.drawable.ic_switch_account_24px) })
        }

        if (connectionState is MoodleService.LoginConnectionState.Offlined) item("relogin") {
            ListItem(
                { Text(Res.string.relogin.t) },
                Modifier.clickable { duty.retryLogin() },
                leadingContent = { MIcon(Res.drawable.ic_refresh_24px) }
            )
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
                items(s.data) { grade ->
                    val gradeItemKey = MoodleTextLocation.key("grades", MoodleTextLocation.seg("item", MoodleTextLocation.hash(grade.url)))

                    ListItem(
                        {
                            MoodleText(
                                rawText = grade.name,
                                context = MoodleTextContext("$gradeItemKey/name"),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        trailingContent = {
                            MoodleText(
                                rawText = grade.grade,
                                context = MoodleTextContext("$gradeItemKey/grade"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
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
}) { padding ->
    val enabled by duty.textTransformEnabled.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        item("text_transform_switch") {
            ListItem(
                { Text(Res.string.text_transform_switch.t) }, Modifier.clickable { duty.setTextTransformEnabled(!enabled) },
                supportingContent = { Text(Res.string.text_transform_switch_desc.t) }, trailingContent = { Switch(enabled, { duty.setTextTransformEnabled(it) }) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslationManagePage(duty: TextTranslationManageDuty) {
    val rules by duty.rules.collectAsState()
    val exportedJson by duty.exportedJson.collectAsState()
    val notice by duty.notice.collectAsState()
    val exportDefaultFileName = Res.string.trans_export_default_file_name.t
    val exportSuccessText = Res.string.trans_file_export_success.t
    val exportFailedText = Res.string.trans_file_export_failed.t
    val pickerNotReadyText = Res.string.trans_file_picker_not_ready.t
    val importFailedText = Res.string.trans_file_import_failed.t
    val clipboardEmptyText = Res.string.trans_clipboard_empty.t
    val copyText = Res.string.trans_copy.t
    val pasteText = Res.string.trans_paste.t
    val exportToFileText = Res.string.trans_export_to_file.t
    val exportToTextText = Res.string.trans_export_to_text.t
    val exportRulesTitleText = Res.string.trans_export_rules.t
    val clipboardManager = LocalClipboardManager.current

    var showCreate by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<TextTransformRule?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var importPayload by remember { mutableStateOf("") }
    var pendingDeleteRule by remember { mutableStateOf<TextTransformRule?>(null) }
    var pendingImportPayload by remember { mutableStateOf<String?>(null) }
    var pendingExportAction by remember { mutableStateOf<String?>(null) }
    var exportedTextDialogContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exportedJson, pendingExportAction) {
        val content = exportedJson ?: return@LaunchedEffect
        val action = pendingExportAction ?: return@LaunchedEffect

        if (action == "text") {
            exportedTextDialogContent = content
            pendingExportAction = null
            duty.clearExportedJson()
            return@LaunchedEffect
        }

        if (action == "file") {
            pendingExportAction = null
            PlatformFunctions.exportTextToFile(exportDefaultFileName, content) { success, error ->
                when {
                    success -> duty.showNotice(exportSuccessText)
                    error == "picker_not_ready" -> duty.showNotice(pickerNotReadyText)
                    error != null -> duty.showNotice("$exportFailedText: $error")
                }
                duty.clearExportedJson()
            }
        }
    }

    if (showCreate) TextTransformRuleEditDialog(
        title = Res.string.trans_create_rule.t,
        initialDraft = TextTransformRuleDraft(
            type = TextTransformRuleType.KEYWORD,
            matcher = "",
            replacement = "",
            ignoreCase = true,
            note = "",
        ),
        onDismiss = { showCreate = false },
        onConfirm = { draft ->
            duty.createRule(draft)
            showCreate = false
        },
    )

    editingRule?.let { rule ->
        TextTransformRuleEditDialog(
            title = Res.string.trans_edit_rule.t,
            initialDraft = rule.toDraft(),
            locationHint = if (rule.type == TextTransformRuleType.LOCATION) MoodleTextContext(rule.matcher, rule.note.ifBlank { rule.matcher }) else null,
            onDismiss = { editingRule = null },
            onConfirm = { draft ->
                duty.updateRule(rule.id, draft)
                editingRule = null
            },
        )
    }

    if (showImportDialog) MAlertDialog(
        title = Res.string.trans_import_rules.t,
        description = Res.string.trans_import_rules_desc.t,
        actions = listOf(
            DialogActionItem(pasteText, false) {
                val text = clipboardManager.getText()?.text
                if (text.isNullOrBlank()) duty.showNotice(clipboardEmptyText)
                else importPayload = text
            },
            DialogActionItem(Res.string.cancel.t),
            DialogActionItem(Res.string.confirm.t) {
                pendingImportPayload = importPayload
                showImportDialog = false
            },
        ),
        actionsInColumn = false,
        onDismissRequest = { showImportDialog = false },
    ) {
        OutlinedTextField(
            value = importPayload,
            onValueChange = { importPayload = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).heightIn(120.dp, 320.dp),
            placeholder = { Text("{ \"version\": 1, \"rules\": [] }") },
        )
    }

    pendingDeleteRule?.let { rule ->
        MAlertDialog(
            title = Res.string.trans_confirm_delete_title.t,
            description = Res.string.trans_confirm_delete_desc.t,
            actions = listOf(
                DialogActionItem(Res.string.cancel.t),
                DialogActionItem(Res.string.confirm.t) {
                    duty.deleteRule(rule.id)
                    pendingDeleteRule = null
                },
            ),
            actionsInColumn = false,
            onDismissRequest = { pendingDeleteRule = null },
        )
    }

    pendingImportPayload?.let { payload ->
        MAlertDialog(
            title = Res.string.trans_import_overwrite_title.t,
            description = Res.string.trans_import_overwrite_desc.t,
            actions = listOf(
                DialogActionItem(Res.string.cancel.t) { pendingImportPayload = null },
                DialogActionItem(Res.string.trans_confirm_import_overwrite.t) {
                    duty.importRules(payload)
                    pendingImportPayload = null
                },
            ),
            actionsInColumn = false,
            onDismissRequest = { pendingImportPayload = null },
        )
    }

    notice?.let { msg ->
        MAlertDialog(
            title = Res.string.trans_notice.t,
            description = msg,
            actions = listOf(DialogActionItem(Res.string.ok.t) { duty.clearNotice() }),
            actionsInColumn = false,
            onDismissRequest = { duty.clearNotice() },
        )
    }

    exportedTextDialogContent?.let { content ->
        MAlertDialog(
            title = exportRulesTitleText,
            actions = listOf(
                DialogActionItem(copyText, false) { clipboardManager.setText(AnnotatedString(content)) },
                DialogActionItem(Res.string.ok.t) { exportedTextDialogContent = null },
            ),
            actionsInColumn = false,
            onDismissRequest = { exportedTextDialogContent = null },
        ) {
            OutlinedTextField(
                content, {},
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).heightIn(140.dp, 320.dp),
                readOnly = true,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Res.string.trans_man.t) },
                navigationIcon = { IconButton({ duty.navBack() }) { MIcon(Res.drawable.ic_arrow_back_24px) } },
                actions = {
                    Box {
                        IconButton({ showImportMenu = true }) { MIcon(Res.drawable.ic_import_24px) }
                        DropdownMenu(showImportMenu, { showImportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(Res.string.trans_import_from_text.t) },
                                onClick = {
                                    showImportMenu = false
                                    showImportDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(Res.string.trans_import_from_file.t) },
                                onClick = {
                                    showImportMenu = false
                                    PlatformFunctions.importTextFromFile { content, error ->
                                        when {
                                            content != null -> pendingImportPayload = content
                                            error == "picker_not_ready" -> duty.showNotice(pickerNotReadyText)
                                            error != null -> duty.showNotice("$importFailedText: $error")
                                        }
                                    }
                                },
                            )
                        }
                    }

                    Box {
                        IconButton({ showExportMenu = true }) { MIcon(Res.drawable.ic_export_24px) }
                        DropdownMenu(showExportMenu, { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(exportToFileText) },
                                onClick = {
                                    showExportMenu = false
                                    pendingExportAction = "file"
                                    duty.exportRules()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(exportToTextText) },
                                onClick = {
                                    showExportMenu = false
                                    pendingExportAction = "text"
                                    duty.exportRules()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton({ showCreate = true }) { MIcon(Res.drawable.ic_add_24px) }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(Res.string.trans_empty_rules.t, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rules, key = { it.id }) { rule ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(rule.type.toDisplayName(), style = MaterialTheme.typography.titleMedium)
                            Switch(rule.enabled, { duty.setRuleEnabled(rule, it) })
                        }

                        Text(
                            Res.string.trans_match_preview.t(rule.matcher),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            Res.string.trans_replace_preview.t(rule.replacement),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (rule.note.isNotBlank()) Text(
                            Res.string.trans_note_preview.t(rule.note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton({ editingRule = rule }) { Text(Res.string.edit.t) }
                            TextButton({ pendingDeleteRule = rule }) { Text(Res.string.delete.t, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
