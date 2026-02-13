package me.earzuchan.markdo.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class TextTransformRuleType {
    KEYWORD,
    REGEX,
    LOCATION,
}

@Serializable
data class TextTransformRule(
    val id: String,
    val type: TextTransformRuleType,
    val matcher: String,
    val replacement: String,
    val enabled: Boolean = true,
    val ignoreCase: Boolean = true,
    val note: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Serializable
data class TextTransformRuleBundle(
    val version: Int = 1,
    val rules: List<TextTransformRule> = emptyList(),
)

data class TextTransformRuleDraft(
    val type: TextTransformRuleType,
    val matcher: String,
    val replacement: String,
    val ignoreCase: Boolean = true,
    val note: String = "",
)

data class MoodleTextContext(
    val locationKey: String,
    val locationLabel: String = locationKey,
)

object MoodleTextLocation {
    fun key(vararg segments: String): String = segments.filter { it.isNotBlank() }.joinToString("/")

    fun seg(name: String, value: Any?): String = "$name:${sanitize(value?.toString() ?: "")}".trimEnd(':')

    fun hash(raw: String): String = raw.hashCode().toString()

    private fun sanitize(raw: String): String {
        if (raw.isBlank()) return ""

        val builder = StringBuilder(raw.length)
        raw.forEach { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') builder.append(ch)
            else builder.append('_')
        }

        return builder.toString().trim('_').ifBlank { "_" }
    }
}
