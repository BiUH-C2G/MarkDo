package me.earzuchan.markdo.data.models

import kotlinx.serialization.Serializable
import me.earzuchan.markdo.utils.PlatformFunctions

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
    val createdAtEpochMs: Long = PlatformFunctions.currentTimeMillis(),
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

    fun course(courseId: Any?): String = key("course", seg("course", courseId))

    fun courseName(courseId: Any?): String = key(course(courseId), "name")

    fun courseCategory(courseId: Any?): String = key(course(courseId), "category")

    fun courseSection(courseId: Any?, sectionId: Any?): String = key(course(courseId), seg("section", sectionId))

    fun canonical(rawKey: String): String {
        val segments = rawKey.split("/").filter { it.isNotBlank() }
        if (segments.size < 3) return rawKey

        if (segments[0] == "course" && (segments[1] == "all" || segments[1] == "detail") && segments[2].startsWith("course:")) {
            val tail = segments.drop(3).toMutableList()
            if (tail.firstOrNull() == "title") tail[0] = "name"
            return key("course", segments[2], *tail.toTypedArray())
        }

        return rawKey
    }

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
