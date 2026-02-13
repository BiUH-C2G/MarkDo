package me.earzuchan.markdo.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

object AppPreferences {
    val KEY_TEXT_TRANSFORM_ENABLED = booleanPreferencesKey("text_transform_enabled")

    const val DEFAULT_TEXT_TRANSFORM_ENABLED = true
}
