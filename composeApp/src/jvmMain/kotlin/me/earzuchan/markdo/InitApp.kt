package me.earzuchan.markdo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import me.earzuchan.markdo.di.appModule
import me.earzuchan.markdo.duties.AppDuty
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.ic_lawnchair
import me.earzuchan.markdo.utils.DesktopUtils
import me.earzuchan.markdo.utils.PlatformFunctions
import me.earzuchan.markdo.utils.ResUtils.p
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinApplication

fun main() = application {
    KoinApplication({ modules(appModule) }) {
        val lifecycle = LifecycleRegistry()

        val appDuty = DesktopUtils.runOnUiThread { AppDuty(DefaultComponentContext(lifecycle)) }

        PlatformFunctions.exitAppMethod = ::exitApplication

        val windowState = rememberWindowState(size = DpSize(400.dp, 800.dp))
        LifecycleController(lifecycle, windowState)

        Window(PlatformFunctions::stopApp, windowState, title = "MarkDo", icon = Res.drawable.ic_lawnchair.p) {
            MarkDoApp(appDuty)
        }
    }
}