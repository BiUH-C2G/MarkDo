package me.earzuchan.markdo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import me.earzuchan.markdo.duties.AppDuty
import me.earzuchan.markdo.duties.LoginDuty
import me.earzuchan.markdo.duties.MainDuty
import me.earzuchan.markdo.duties.SplashDuty
import me.earzuchan.markdo.ui.models.AppToastModel
import me.earzuchan.markdo.ui.screens.LoginScreen
import me.earzuchan.markdo.ui.screens.MainScreen
import me.earzuchan.markdo.ui.screens.SplashScreen
import me.earzuchan.markdo.ui.themes.MarkDoTheme
import me.earzuchan.markdo.ui.views.MToast
import me.earzuchan.markdo.utils.PlatformFunctions

@Composable
fun MarkDoApp(appDuty: AppDuty) {
    val TAG = "MarkDoApp"

    val toastState by appDuty.toast.collectAsState()

    // 关键：记住上一个非空的 Toast
    var lastToast by remember { mutableStateOf<AppToastModel?>(null) }

    // 当 toastState 更新时，如果非空，就更新 lastToast
    LaunchedEffect(toastState) { if (toastState != null) lastToast = toastState }

    DisposableEffect(Unit) {
        PlatformFunctions.setupApp()

        onDispose { PlatformFunctions.stopApp() }
    }

    MarkDoTheme {
        Surface {
            Box(Modifier.fillMaxSize()) {
                Children(appDuty.navStack, Modifier.fillMaxSize()) {
                    when (val ins = it.instance) {
                        is MainDuty -> MainScreen(ins)

                        is LoginDuty -> LoginScreen(ins)

                        is SplashDuty -> SplashScreen()
                    }
                }

                AnimatedVisibility(
                    toastState != null, Modifier.fillMaxSize(), fadeIn(), fadeOut()
                ) { lastToast?.let { MToast(it.text) } }
            }
        }
    }
}