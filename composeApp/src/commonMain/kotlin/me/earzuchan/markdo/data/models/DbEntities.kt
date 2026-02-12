package me.earzuchan.markdo.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_login_accounts")
data class SavedLoginAccountEntity(
    @PrimaryKey
    val accountKey: String,
    val baseSite: String,
    val username: String,
    val password: String,
    val lastLoginEpochMs: Long,
    val isActive: Boolean
)

@Entity(tableName = "cache_user_profile")
data class UserProfileCacheEntity(
    @PrimaryKey
    val accountKey: String,
    val name: String,
    val avatarUrl: String?
)

@Entity(tableName = "cache_timeline", primaryKeys = ["accountKey", "id"])
data class TimelineEventCacheEntity(
    val accountKey: String,
    val id: Int,
    val title: String,
    val name: String,
    val type: String,
    val description: String,
    val deadline: Long,
    val overdue: Boolean,
    val courseId: Int,
    val courseName: String,
    val iconUrl: String,
    val actionName: String,
    val actionUrl: String,
    val displayOrder: Int
)

@Entity(tableName = "cache_recent_items", primaryKeys = ["accountKey", "id"])
data class RecentItemCacheEntity(
    val accountKey: String,
    val id: Int,
    val type: String,
    val name: String,
    val courseId: Int,
    val courseName: String,
    val courseModuleId: Int,
    val timeAccess: Long,
    val viewUrl: String,
    val rawIconHtml: String,
    val displayOrder: Int
)

@Entity(tableName = "cache_courses", primaryKeys = ["accountKey", "id"])
data class CourseInfoCacheEntity(
    val accountKey: String,
    val id: Int,
    val name: String,
    val category: String,
    val url: String,
    val displayOrder: Int
)
