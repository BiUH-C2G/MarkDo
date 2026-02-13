package me.earzuchan.markdo.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.earzuchan.markdo.data.models.TextTransformRule
import me.earzuchan.markdo.data.models.TextTransformRuleBundle

class TextTransformRepository(private val dataStore: DataStore<Preferences>) {
    private val storedJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val storageKey = stringPreferencesKey("text_transform_rules_json")

    val rulesFlow: Flow<List<TextTransformRule>> = dataStore.data.map { prefs ->
        val raw = prefs[storageKey]
        if (raw.isNullOrBlank()) return@map emptyList()

        runCatching { storedJson.decodeFromString<TextTransformRuleBundle>(raw).rules }
            .recoverCatching { storedJson.decodeFromString<List<TextTransformRule>>(raw) }
            .getOrElse { emptyList() }
    }

    suspend fun getRules(): List<TextTransformRule> = rulesFlow.first()

    suspend fun upsertRule(rule: TextTransformRule) {
        val current = getRules()
        val existingIndex = current.indexOfFirst { it.id == rule.id }
        val updated = if (existingIndex >= 0) current.toMutableList().also { it[existingIndex] = rule }
        else current + rule

        replaceAllRules(updated)
    }

    suspend fun deleteRule(ruleId: String) {
        val updated = getRules().filterNot { it.id == ruleId }
        replaceAllRules(updated)
    }

    suspend fun replaceAllRules(rules: List<TextTransformRule>) {
        dataStore.edit { prefs ->
            val payload = TextTransformRuleBundle(rules = rules)
            prefs[storageKey] = storedJson.encodeToString(payload)
        }
    }
}
