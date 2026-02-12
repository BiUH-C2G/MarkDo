package me.earzuchan.markdo.data.models

data class SavedLoginAccount(
    val accountKey: String,
    val baseSite: String,
    val username: String,
    val password: String,
    val lastLoginEpochMs: Long
)

data class LoginDraft(
    val baseSite: String,
    val username: String,
    val password: String
)
