package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.earzuchan.markdo.data.models.MoodleTextContext
import me.earzuchan.markdo.data.models.MoodleTextLocation
import me.earzuchan.markdo.data.models.TextTransformRule
import me.earzuchan.markdo.data.models.TextTransformRuleDraft
import me.earzuchan.markdo.data.models.TextTransformRuleType
import me.earzuchan.markdo.resources.*
import me.earzuchan.markdo.services.TextTransformService
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.utils.ResUtils.t
import org.koin.compose.koinInject

@Composable
fun TextTransformRuleType.toDisplayName(): String = when (this) {
    TextTransformRuleType.KEYWORD -> Res.string.trans_rule_type_keyword.t
    TextTransformRuleType.REGEX -> Res.string.trans_rule_type_regex.t
    TextTransformRuleType.LOCATION -> Res.string.trans_rule_type_location.t
}

fun TextTransformRule.toDraft(): TextTransformRuleDraft = TextTransformRuleDraft(
    type = type,
    matcher = matcher,
    replacement = replacement,
    ignoreCase = ignoreCase,
    note = note,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodleText(
    rawText: String,
    context: MoodleTextContext,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val textTransformService: TextTransformService = koinInject()
    val rules by textTransformService.rules.collectAsState()
    val enabled by textTransformService.enabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val createRuleFromCurrentText = Res.string.trans_create_rule_from_current.t
    val viewEffectiveRulesText = Res.string.trans_view_effective_rules.t
    val viewRawTextText = Res.string.trans_view_raw_text.t
    val originalTextTitleText = Res.string.trans_original_text.t
    val ruleCreatedText = Res.string.trans_rule_created.t
    val createRuleFailedText = Res.string.trans_rule_create_failed.t
    val ruleUpdatedText = Res.string.trans_rule_updated.t
    val ruleUpdateFailedText = Res.string.trans_rule_update_failed.t
    val copyText = Res.string.trans_copy.t

    var showMenu by remember { mutableStateOf(false) }
    var showRuleCreator by remember { mutableStateOf(false) }
    var showEffectiveRules by remember { mutableStateOf(false) }
    var showRawText by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<TextTransformRule?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val transformedText = remember(rawText, context.locationKey, rules, enabled) {
        textTransformService.transform(rawText, context)
    }
    val effectiveRules = remember(rawText, context.locationKey, rules, enabled) {
        textTransformService.getEffectiveRules(rawText, context)
    }

    Box {
        Text(
            text = transformedText,
            modifier = modifier.longPressContextMenu { showMenu = true },
            color = color,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
        )

        DropdownMenu(showMenu, { showMenu = false }) {
            DropdownMenuItem({ Text(createRuleFromCurrentText) }, {
                showMenu = false
                showRuleCreator = true
            })

            if (effectiveRules.isNotEmpty()) DropdownMenuItem({ Text(viewEffectiveRulesText) }, {
                showMenu = false
                showEffectiveRules = true
            })

            DropdownMenuItem({ Text(viewRawTextText) }, {
                showMenu = false
                showRawText = true
            })
        }
    }

    if (showRuleCreator) TextTransformRuleEditDialog(
        createRuleFromCurrentText, TextTransformRuleDraft(
            type = TextTransformRuleType.KEYWORD,
            matcher = rawText,
            replacement = rawText,
            ignoreCase = true,
            note = context.locationLabel,
        ), context, { showRuleCreator = false }, { draft ->
            coroutineScope.launch {
                runCatching { textTransformService.addRule(draft) }.onSuccess {
                    notice = ruleCreatedText
                    showRuleCreator = false
                }.onFailure {
                    notice = createRuleFailedText
                }
            }
        }
    )

    if (showEffectiveRules) MAlertDialog(
        title = viewEffectiveRulesText,
        actions = listOf(DialogActionItem(Res.string.ok.t)),
        onDismissRequest = { showEffectiveRules = false },
        actionsInColumn = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            effectiveRules.forEach { rule ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rule.type.toDisplayName(), style = MaterialTheme.typography.labelLarge)
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            androidx.compose.material3.TextButton({
                                showEffectiveRules = false
                                editingRule = rule
                            }) { Text(Res.string.edit.t) }
                        }
                    }
                }
            }
        }
    }

    editingRule?.let { rule ->
        TextTransformRuleEditDialog(
            title = Res.string.trans_edit_rule.t,
            initialDraft = rule.toDraft(),
            locationHint = if (rule.type == TextTransformRuleType.LOCATION) context else null,
            onDismiss = { editingRule = null },
            onConfirm = { draft ->
                coroutineScope.launch {
                    runCatching { textTransformService.updateRule(rule.id, draft) }.onSuccess {
                        notice = ruleUpdatedText
                        editingRule = null
                    }.onFailure {
                        notice = ruleUpdateFailedText
                    }
                }
            },
        )
    }

    if (showRawText) MAlertDialog(
        originalTextTitleText, null, rawText, listOf(
            DialogActionItem(copyText, false) { clipboardManager.setText(AnnotatedString(rawText)) },
            DialogActionItem(Res.string.confirm.t),
        ), false, { showRawText = false }
    )

    notice?.let { message ->
        MAlertDialog(
            title = Res.string.trans_notice.t,
            description = message,
            actions = listOf(DialogActionItem(Res.string.ok.t)),
            onDismissRequest = { notice = null },
            actionsInColumn = false,
        )
    }
}

@Composable
fun TextTransformRuleEditDialog(
    title: String,
    initialDraft: TextTransformRuleDraft,
    locationHint: MoodleTextContext? = null,
    onDismiss: () -> Unit,
    onConfirm: (TextTransformRuleDraft) -> Unit,
) {
    var type by remember(initialDraft.type) { mutableStateOf(initialDraft.type) }

    var keywordMatcher by remember(initialDraft.matcher, initialDraft.type) {
        mutableStateOf(if (initialDraft.type != TextTransformRuleType.LOCATION) initialDraft.matcher else "")
    }

    var regexMatcher by remember(initialDraft.matcher, initialDraft.type) {
        mutableStateOf(if (initialDraft.type != TextTransformRuleType.LOCATION) initialDraft.matcher else "")
    }

    var replacement by remember(initialDraft.replacement) { mutableStateOf(initialDraft.replacement) }
    var ignoreCase by remember(initialDraft.ignoreCase) { mutableStateOf(initialDraft.ignoreCase) }
    var note by remember(initialDraft.note) { mutableStateOf(initialDraft.note) }

    val locationKey = MoodleTextLocation.canonical(locationHint?.locationKey.orEmpty())
    var locationMatcher by remember(initialDraft.matcher, initialDraft.type, locationKey) {
        mutableStateOf(
            when {
                initialDraft.type == TextTransformRuleType.LOCATION -> MoodleTextLocation.canonical(initialDraft.matcher)
                locationKey.isNotBlank() -> locationKey
                else -> ""
            }
        )
    }

    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
        MAlertDialog(
            title, actionsInColumn = false,
            onDismissRequest = onDismiss,
            actions = listOf(
                DialogActionItem(Res.string.cancel.t),
                DialogActionItem(Res.string.confirm.t) {
                    val finalMatcher = when (type) {
                        TextTransformRuleType.KEYWORD -> keywordMatcher
                        TextTransformRuleType.REGEX -> regexMatcher
                        TextTransformRuleType.LOCATION -> locationMatcher
                    }

                    onConfirm(TextTransformRuleDraft(type, finalMatcher, replacement, ignoreCase, note))
                },
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(Res.string.trans_rule_type.t, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.labelLarge)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextTransformRuleType.entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth().selectable(selected = type == entry, role = Role.RadioButton, onClick = {
                                    type = entry
                                    if (entry == TextTransformRuleType.LOCATION && locationKey.isNotBlank() && locationMatcher.isBlank()) {
                                        locationMatcher = locationKey
                                    }
                                }).height(48.dp).padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = type == entry, onClick = null)
                            Text(entry.toDisplayName(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = when (type) {
                        TextTransformRuleType.KEYWORD -> keywordMatcher
                        TextTransformRuleType.REGEX -> regexMatcher
                        TextTransformRuleType.LOCATION -> locationMatcher
                    },
                    onValueChange = {
                        when (type) {
                            TextTransformRuleType.KEYWORD -> keywordMatcher = it
                            TextTransformRuleType.REGEX -> regexMatcher = it
                            TextTransformRuleType.LOCATION -> locationMatcher = it
                        }
                    },
                    label = {
                        Text(
                            when (type) {
                                TextTransformRuleType.KEYWORD -> Res.string.trans_match_keyword.t
                                TextTransformRuleType.REGEX -> Res.string.trans_match_regex.t
                                TextTransformRuleType.LOCATION -> Res.string.trans_match_location.t
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = type != TextTransformRuleType.REGEX,
                )

                if (type == TextTransformRuleType.LOCATION && locationHint != null) Text(
                    Res.string.trans_current_location.t(locationHint.locationLabel),
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text(Res.string.trans_replacement.t) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )

                if (type != TextTransformRuleType.LOCATION) Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { ignoreCase = !ignoreCase },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
                    Text(Res.string.trans_ignore_case.t)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Res.string.trans_note_optional.t) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.longPressContextMenu(onLongPress: () -> Unit): Modifier = pointerInput(onLongPress) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id)
        if (longPress != null) {
            longPress.consume()
            onLongPress()
            waitForUpOrCancellation()?.consume()
        }
    }
}
