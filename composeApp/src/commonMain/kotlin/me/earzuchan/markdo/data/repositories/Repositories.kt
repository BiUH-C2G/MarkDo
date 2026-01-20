package me.earzuchan.markdo.data.repositories

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.earzuchan.markdo.data.preferences.AppPreferences
import me.earzuchan.markdo.utils.MiscUtils

class AppPreferenceRepository {
    private val dataStore = MiscUtils.buildAppPreferences()

    // --- 通用读写 ---

    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> = dataStore.data.map { preferences ->
        preferences[key] ?: defaultValue
    }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // --- 普通偏好项 ---

    val baseSite: Flow<String> = getPreference(AppPreferences.KEY_BASE_SITE, AppPreferences.DEFAULT_BASE_SITE)
    suspend fun setBaseSite(baseSite: String) = setPreference(AppPreferences.KEY_BASE_SITE, baseSite)

    val username: Flow<String> = getPreference(AppPreferences.KEY_USERNAME, "")
    suspend fun setUsername(username: String) = setPreference(AppPreferences.KEY_USERNAME, username)

    val password: Flow<String> = getPreference(AppPreferences.KEY_PASSWORD, "")
    suspend fun setPassword(password: String) = setPreference(AppPreferences.KEY_PASSWORD, password)
}