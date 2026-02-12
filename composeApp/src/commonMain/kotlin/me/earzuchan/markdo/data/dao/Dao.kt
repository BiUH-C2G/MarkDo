package me.earzuchan.markdo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import me.earzuchan.markdo.data.models.CourseInfoCacheEntity
import me.earzuchan.markdo.data.models.RecentItemCacheEntity
import me.earzuchan.markdo.data.models.SavedLoginAccountEntity
import me.earzuchan.markdo.data.models.TimelineEventCacheEntity
import me.earzuchan.markdo.data.models.UserProfileCacheEntity

@Dao
interface SavedLoginAccountDao {
    @Query("SELECT * FROM saved_login_accounts ORDER BY isActive DESC, lastLoginEpochMs DESC")
    suspend fun getAccounts(): List<SavedLoginAccountEntity>

    @Query("SELECT * FROM saved_login_accounts WHERE accountKey = :accountKey LIMIT 1")
    suspend fun getAccountByKey(accountKey: String): SavedLoginAccountEntity?

    @Query("SELECT * FROM saved_login_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): SavedLoginAccountEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_login_accounts)")
    suspend fun hasRememberedAccount(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedLoginAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SavedLoginAccountEntity>)

    @Query("UPDATE saved_login_accounts SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE saved_login_accounts SET isActive = 1 WHERE accountKey = :accountKey")
    suspend fun markActive(accountKey: String): Int

    @Query("DELETE FROM saved_login_accounts")
    suspend fun clearAllAccounts()

    @Query("DELETE FROM saved_login_accounts WHERE accountKey = :accountKey")
    suspend fun deleteByKey(accountKey: String): Int

    @Transaction
    suspend fun setActiveAccount(accountKey: String): Boolean {
        clearActive()
        return markActive(accountKey) > 0
    }

    @Transaction
    suspend fun upsertAndSetActive(entity: SavedLoginAccountEntity): Boolean {
        upsert(entity.copy(isActive = false))
        return setActiveAccount(entity.accountKey)
    }
}

@Dao
interface MoodleCacheDao {
    @Query("SELECT * FROM cache_user_profile WHERE accountKey = :accountKey LIMIT 1")
    suspend fun getCachedUserProfile(accountKey: String): UserProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(entity: UserProfileCacheEntity)

    @Query("DELETE FROM cache_user_profile WHERE accountKey = :accountKey")
    suspend fun clearUserProfile(accountKey: String)

    @Query("SELECT * FROM cache_timeline WHERE accountKey = :accountKey ORDER BY displayOrder ASC")
    suspend fun getCachedTimeline(accountKey: String): List<TimelineEventCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeline(entities: List<TimelineEventCacheEntity>)

    @Query("DELETE FROM cache_timeline WHERE accountKey = :accountKey")
    suspend fun clearTimeline(accountKey: String)

    @Transaction
    suspend fun replaceTimeline(accountKey: String, entities: List<TimelineEventCacheEntity>) {
        clearTimeline(accountKey)
        if (entities.isNotEmpty()) upsertTimeline(entities)
    }

    @Query("SELECT * FROM cache_recent_items WHERE accountKey = :accountKey ORDER BY displayOrder ASC")
    suspend fun getCachedRecentItems(accountKey: String): List<RecentItemCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentItems(entities: List<RecentItemCacheEntity>)

    @Query("DELETE FROM cache_recent_items WHERE accountKey = :accountKey")
    suspend fun clearRecentItems(accountKey: String)

    @Transaction
    suspend fun replaceRecentItems(accountKey: String, entities: List<RecentItemCacheEntity>) {
        clearRecentItems(accountKey)
        if (entities.isNotEmpty()) upsertRecentItems(entities)
    }

    @Query("SELECT * FROM cache_courses WHERE accountKey = :accountKey ORDER BY displayOrder ASC")
    suspend fun getCachedCourses(accountKey: String): List<CourseInfoCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(entities: List<CourseInfoCacheEntity>)

    @Query("DELETE FROM cache_courses WHERE accountKey = :accountKey")
    suspend fun clearCourses(accountKey: String)

    @Transaction
    suspend fun replaceCourses(accountKey: String, entities: List<CourseInfoCacheEntity>) {
        clearCourses(accountKey)
        if (entities.isNotEmpty()) upsertCourses(entities)
    }

    @Query(
        """
        SELECT 
            EXISTS(SELECT 1 FROM cache_user_profile WHERE accountKey = :accountKey) OR
            EXISTS(SELECT 1 FROM cache_timeline WHERE accountKey = :accountKey) OR
            EXISTS(SELECT 1 FROM cache_recent_items WHERE accountKey = :accountKey) OR
            EXISTS(SELECT 1 FROM cache_courses WHERE accountKey = :accountKey)
        """
    )
    suspend fun hasAnyCacheForAccount(accountKey: String): Boolean

    @Transaction
    suspend fun clearAccountCaches(accountKey: String) {
        clearUserProfile(accountKey)
        clearTimeline(accountKey)
        clearRecentItems(accountKey)
        clearCourses(accountKey)
    }

    @Query(
        """
        SELECT 
            EXISTS(SELECT 1 FROM cache_user_profile) OR
            EXISTS(SELECT 1 FROM cache_timeline) OR
            EXISTS(SELECT 1 FROM cache_recent_items) OR
            EXISTS(SELECT 1 FROM cache_courses)
        """
    )
    suspend fun hasAnyCache(): Boolean

    @Transaction
    suspend fun clearAllCaches() {
        clearAllUserProfile()
        clearAllTimeline()
        clearAllRecentItems()
        clearAllCourses()
    }

    @Query("DELETE FROM cache_user_profile")
    suspend fun clearAllUserProfile()

    @Query("DELETE FROM cache_timeline")
    suspend fun clearAllTimeline()

    @Query("DELETE FROM cache_recent_items")
    suspend fun clearAllRecentItems()

    @Query("DELETE FROM cache_courses")
    suspend fun clearAllCourses()
}
