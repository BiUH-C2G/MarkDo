package me.earzuchan.markdo

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import me.earzuchan.markdo.di.appModule
import me.earzuchan.markdo.duties.AppDuty
import org.koin.compose.KoinApplication
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    KoinApplication(application = { modules(appModule) }) {
        val appDuty = remember { AppDuty(DefaultComponentContext(LifecycleRegistry())) }

        MarkDoApp(appDuty)
    }
}