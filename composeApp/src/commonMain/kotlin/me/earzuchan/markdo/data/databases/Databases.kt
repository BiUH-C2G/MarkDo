package me.earzuchan.markdo.data.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import me.earzuchan.markdo.data.dao.MoodleCacheDao
import me.earzuchan.markdo.data.dao.SavedLoginAccountDao
import me.earzuchan.markdo.data.models.CourseInfoCacheEntity
import me.earzuchan.markdo.data.models.RecentItemCacheEntity
import me.earzuchan.markdo.data.models.SavedLoginAccountEntity
import me.earzuchan.markdo.data.models.TimelineEventCacheEntity
import me.earzuchan.markdo.data.models.UserProfileCacheEntity

@Database(
    entities = [SavedLoginAccountEntity::class, UserProfileCacheEntity::class, TimelineEventCacheEntity::class, RecentItemCacheEntity::class, CourseInfoCacheEntity::class],
    version = 3, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedLoginAccountDao(): SavedLoginAccountDao

    abstract fun moodleCacheDao(): MoodleCacheDao
}
