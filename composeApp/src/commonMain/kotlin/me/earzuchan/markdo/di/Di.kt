package me.earzuchan.markdo.di

import lib.fetchmoodle.MoodleFetcher
import me.earzuchan.markdo.data.databases.AppDatabase
import me.earzuchan.markdo.data.repositories.AccountRepository
import me.earzuchan.markdo.data.repositories.AppPreferenceRepository
import me.earzuchan.markdo.data.repositories.DataCacheRepository
import me.earzuchan.markdo.data.repositories.TextTransformRepository
import me.earzuchan.markdo.services.MoodleService
import me.earzuchan.markdo.services.TextTransformService
import me.earzuchan.markdo.utils.MiscUtils
import org.koin.dsl.module

val appModule = module {
    single { MiscUtils.buildAppDatabase() }
    single { get<AppDatabase>().savedLoginAccountDao() }
    single { get<AppDatabase>().moodleCacheDao() }

    single { MiscUtils.buildAppPreferences() }

    single { AppPreferenceRepository(get()) }
    single { AccountRepository(get()) }
    single { DataCacheRepository(get()) }
    single { TextTransformRepository(get()) }

    single { MoodleFetcher() }

    single { MoodleService(get(), get(), get()) }
    single { TextTransformService(get(), get()) }
}
