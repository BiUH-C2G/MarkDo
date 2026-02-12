package me.earzuchan.markdo.data.repositories

import lib.fetchmoodle.MoodleCourseInfo
import lib.fetchmoodle.MoodleRecentItem
import lib.fetchmoodle.MoodleTimelineEvent
import lib.fetchmoodle.MoodleUserProfile
import me.earzuchan.markdo.data.dao.MoodleCacheDao
import me.earzuchan.markdo.data.dao.SavedLoginAccountDao
import me.earzuchan.markdo.data.models.CourseInfoCacheEntity
import me.earzuchan.markdo.data.models.LoginDraft
import me.earzuchan.markdo.data.models.RecentItemCacheEntity
import me.earzuchan.markdo.data.models.SavedLoginAccount
import me.earzuchan.markdo.data.models.SavedLoginAccountEntity
import me.earzuchan.markdo.data.models.TimelineEventCacheEntity
import me.earzuchan.markdo.data.models.UserProfileCacheEntity
import me.earzuchan.markdo.data.preferences.AppPreferences

class AppPreferenceRepository {
    fun getDefaultBaseSite(): String = AppPreferences.DEFAULT_BASE_SITE
}

class AccountRepository(private val accountDao: SavedLoginAccountDao) {
    suspend fun getRememberedAccounts(): List<SavedLoginAccount> = accountDao.getAccounts().map { it.toModel() }

    suspend fun hasRememberedAccount(): Boolean = accountDao.hasRememberedAccount()

    suspend fun getActiveAccount(): SavedLoginAccount? = accountDao.getActiveAccount()?.toModel()

    suspend fun getAccountByKey(accountKey: String): SavedLoginAccount? = accountDao.getAccountByKey(accountKey)?.toModel()

    suspend fun getPreferredLoginDraft(): LoginDraft {
        val accounts = getRememberedAccounts()
        val active = getActiveAccount()
        val account = active ?: accounts.maxByOrNull { it.lastLoginEpochMs }

        return if (account != null) LoginDraft(account.baseSite, account.username, account.password)
        else LoginDraft(AppPreferences.DEFAULT_BASE_SITE, "", "")
    }

    suspend fun getLoginDraftByAccountKey(accountKey: String): LoginDraft? {
        val account = getAccountByKey(accountKey) ?: return null
        return LoginDraft(account.baseSite, account.username, account.password)
    }

    suspend fun saveSuccessfulLogin(site: String, username: String, password: String): SavedLoginAccount {
        val normalizedSite = normalizeSite(site)
        val normalizedUsername = username.trim()
        val accountKey = buildAccountKey(normalizedSite, normalizedUsername)
        val now = System.currentTimeMillis()

        val account = SavedLoginAccount(
            accountKey = accountKey,
            baseSite = normalizedSite,
            username = normalizedUsername,
            password = password,
            lastLoginEpochMs = now
        )

        accountDao.upsertAndSetActive(account.toEntity(isActive = true))

        return account
    }

    suspend fun setActiveAccount(accountKey: String): Boolean = accountDao.setActiveAccount(accountKey)

    suspend fun clearActiveAccount() {
        accountDao.clearActive()
    }

    suspend fun removeRememberedAccount(accountKey: String): Boolean {
        val activeAccountKey = accountDao.getActiveAccount()?.accountKey
        if (accountKey == activeAccountKey) return false

        return accountDao.deleteByKey(accountKey) > 0
    }

    suspend fun clearAllAccounts() {
        accountDao.clearAllAccounts()
    }

    companion object {
        fun normalizeSite(site: String): String = site.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trim()
            .trimEnd('/')

        fun buildAccountKey(baseSite: String, username: String): String {
            val sitePart = normalizeSite(baseSite).lowercase()
            val userPart = username.trim().lowercase()
            return "$sitePart|$userPart"
        }
    }
}

private fun SavedLoginAccount.toEntity(isActive: Boolean): SavedLoginAccountEntity = SavedLoginAccountEntity(
    accountKey = accountKey,
    baseSite = baseSite,
    username = username,
    password = password,
    lastLoginEpochMs = lastLoginEpochMs,
    isActive = isActive
)

private fun SavedLoginAccountEntity.toModel(): SavedLoginAccount = SavedLoginAccount(
    accountKey = accountKey,
    baseSite = baseSite,
    username = username,
    password = password,
    lastLoginEpochMs = lastLoginEpochMs
)

class DataCacheRepository(private val cacheDao: MoodleCacheDao) {
    suspend fun readCachedUserProfile(accountKey: String): MoodleUserProfile? = cacheDao.getCachedUserProfile(accountKey)?.toModel()

    suspend fun readCachedTimeline(accountKey: String): List<MoodleTimelineEvent> = cacheDao.getCachedTimeline(accountKey).map { it.toModel() }

    suspend fun readCachedRecentItems(accountKey: String): List<MoodleRecentItem> = cacheDao.getCachedRecentItems(accountKey).map { it.toModel() }

    suspend fun readCachedCourses(accountKey: String): List<MoodleCourseInfo> = cacheDao.getCachedCourses(accountKey).map { it.toModel() }

    suspend fun replaceUserProfile(accountKey: String, model: MoodleUserProfile) {
        cacheDao.upsertUserProfile(UserProfileCacheEntity(accountKey = accountKey, name = model.name, avatarUrl = model.avatarUrl))
    }

    suspend fun replaceTimeline(accountKey: String, models: List<MoodleTimelineEvent>) = cacheDao.replaceTimeline(accountKey, models.mapIndexed { index, model -> model.toEntity(accountKey, index) })

    suspend fun replaceRecentItems(accountKey: String, models: List<MoodleRecentItem>) = cacheDao.replaceRecentItems(accountKey, models.mapIndexed { index, model -> model.toEntity(accountKey, index) })

    suspend fun replaceCourses(accountKey: String, models: List<MoodleCourseInfo>) = cacheDao.replaceCourses(accountKey, models.mapIndexed { index, model -> model.toEntity(accountKey, index) })

    suspend fun hasAnyCache(): Boolean = cacheDao.hasAnyCache()

    suspend fun hasAnyCacheForAccount(accountKey: String): Boolean = cacheDao.hasAnyCacheForAccount(accountKey)

    suspend fun clearAccountCaches(accountKey: String) = cacheDao.clearAccountCaches(accountKey)

    suspend fun clearAllCaches() = cacheDao.clearAllCaches()
}

private fun UserProfileCacheEntity.toModel(): MoodleUserProfile = MoodleUserProfile(name = name, avatarUrl = avatarUrl)

private fun TimelineEventCacheEntity.toModel(): MoodleTimelineEvent = MoodleTimelineEvent(
    id = id,
    title = title,
    name = name,
    type = type,
    description = description,
    deadline = deadline,
    isOverdue = overdue,
    courseId = courseId,
    courseName = courseName,
    iconUrl = iconUrl,
    actionName = actionName,
    actionUrl = actionUrl
)

private fun MoodleTimelineEvent.toEntity(accountKey: String, order: Int): TimelineEventCacheEntity = TimelineEventCacheEntity(
    accountKey = accountKey,
    id = id,
    title = title,
    name = name,
    type = type,
    description = description,
    deadline = deadline,
    overdue = isOverdue,
    courseId = courseId,
    courseName = courseName,
    iconUrl = iconUrl,
    actionName = actionName,
    actionUrl = actionUrl,
    displayOrder = order
)

private fun RecentItemCacheEntity.toModel(): MoodleRecentItem = MoodleRecentItem(
    id = id,
    type = type,
    name = name,
    courseId = courseId,
    courseName = courseName,
    courseModuleId = courseModuleId,
    timeAccess = timeAccess,
    viewUrl = viewUrl,
    rawIconHtml = rawIconHtml
)

private fun MoodleRecentItem.toEntity(accountKey: String, order: Int): RecentItemCacheEntity = RecentItemCacheEntity(
    accountKey = accountKey,
    id = id,
    type = type,
    name = name,
    courseId = courseId,
    courseName = courseName,
    courseModuleId = courseModuleId,
    timeAccess = timeAccess,
    viewUrl = viewUrl,
    rawIconHtml = rawIconHtml,
    displayOrder = order
)

private fun CourseInfoCacheEntity.toModel(): MoodleCourseInfo = MoodleCourseInfo(id = id, name = name, category = category, url = url)

private fun MoodleCourseInfo.toEntity(accountKey: String, order: Int): CourseInfoCacheEntity = CourseInfoCacheEntity(
    accountKey = accountKey,
    id = id,
    name = name,
    category = category,
    url = url,
    displayOrder = order
)
