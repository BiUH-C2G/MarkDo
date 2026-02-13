package me.earzuchan.markdo.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.earzuchan.markdo.data.models.MoodleTextContext
import me.earzuchan.markdo.data.models.TextTransformRule
import me.earzuchan.markdo.data.models.TextTransformRuleBundle
import me.earzuchan.markdo.data.models.TextTransformRuleDraft
import me.earzuchan.markdo.data.models.TextTransformRuleType
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.data.repositories.TextTransformRepository
import me.earzuchan.markdo.utils.MiscUtils.ioDispatcherLaunch

class TextTransformService(private val repo: TextTransformRepository, private val appPrefsRepo: AppPreferenceRepository) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _rules = MutableStateFlow<List<TextTransformRule>>(emptyList())
    val rules: StateFlow<List<TextTransformRule>> = _rules.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    init {
        ioDispatcherLaunch {
            repo.rulesFlow.collect { loaded ->
                _rules.value = loaded
            }
        }

        ioDispatcherLaunch {
            appPrefsRepo.textTransformEnabledFlow().collect { enabled ->
                _enabled.value = enabled
            }
        }
    }

    fun transform(rawText: String, context: MoodleTextContext): String {
        if (rawText.isEmpty() || !_enabled.value) return rawText

        val enabledRules = _rules.value.filter { it.enabled }
        val locationRule = enabledRules.firstOrNull { it.type == TextTransformRuleType.LOCATION && it.matcher == context.locationKey }
        if (locationRule != null) return locationRule.replacement

        var transformed = rawText
        enabledRules.forEach { rule ->
            when (rule.type) {
                TextTransformRuleType.KEYWORD -> {
                    if (rule.matcher.isBlank()) return@forEach
                    transformed = transformed.replace(rule.matcher, rule.replacement, ignoreCase = rule.ignoreCase)
                }

                TextTransformRuleType.REGEX -> {
                    if (rule.matcher.isBlank()) return@forEach
                    val options = if (rule.ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    transformed = runCatching { Regex(rule.matcher, options).replace(transformed, rule.replacement) }.getOrElse { transformed }
                }

                TextTransformRuleType.LOCATION -> Unit
            }
        }

        return transformed
    }

    suspend fun addRule(draft: TextTransformRuleDraft): TextTransformRule {
        validateDraft(draft)

        val rule = TextTransformRule(
            id = buildRuleId(),
            type = draft.type,
            matcher = draft.matcher.trim(),
            replacement = draft.replacement,
            ignoreCase = draft.ignoreCase,
            note = draft.note.trim(),
            enabled = true,
            createdAtEpochMs = System.currentTimeMillis(),
        )

        repo.upsertRule(rule)
        return rule
    }

    suspend fun updateRule(ruleId: String, draft: TextTransformRuleDraft) {
        validateDraft(draft)

        val current = _rules.value.firstOrNull { it.id == ruleId } ?: return

        repo.upsertRule(
            current.copy(
                type = draft.type,
                matcher = draft.matcher.trim(),
                replacement = draft.replacement,
                ignoreCase = draft.ignoreCase,
                note = draft.note.trim(),
            )
        )
    }

    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        val current = _rules.value.firstOrNull { it.id == ruleId } ?: return
        repo.upsertRule(current.copy(enabled = enabled))
    }

    suspend fun deleteRule(ruleId: String) = repo.deleteRule(ruleId)

    fun exportRulesJson(): String = json.encodeToString(TextTransformRuleBundle(rules = _rules.value))

    suspend fun importRulesJson(raw: String): Int {
        if (raw.isBlank()) throw IllegalArgumentException("导入内容为空")

        val importedRules = runCatching {
            json.decodeFromString<TextTransformRuleBundle>(raw).rules
        }.recoverCatching {
            json.decodeFromString<List<TextTransformRule>>(raw)
        }.getOrElse { throw IllegalArgumentException("导入失败：JSON 格式不正确") }

        repo.replaceAllRules(importedRules)
        return importedRules.size
    }

    fun buildDraftFromRule(rule: TextTransformRule): TextTransformRuleDraft = TextTransformRuleDraft(
        rule.type, rule.matcher, rule.replacement, rule.ignoreCase, rule.note
    )

    private fun validateDraft(draft: TextTransformRuleDraft) {
        if (draft.matcher.isBlank()) throw IllegalArgumentException("匹配内容不能为空")
        if (draft.type == TextTransformRuleType.REGEX) {
            val options = if (draft.ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            runCatching { Regex(draft.matcher, options) }.getOrElse {
                throw IllegalArgumentException("正则表达式无效：${it.message ?: "未知错误"}")
            }
        }
    }

    private fun buildRuleId(): String = "rule_${System.currentTimeMillis()}_${(1000..9999).random()}"
}
