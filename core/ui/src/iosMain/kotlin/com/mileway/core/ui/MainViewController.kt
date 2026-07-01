package com.mileway.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    AppLog.init()
    // KOIN.1: start the shared Koin graph on iOS (platformModule() + core data/ui + the iOS app module)
    // before any Compose host reads it, so LocalManagerProvider / PushBridge resolve real iOS managers.
    initKoin(modules = listOf(coreDataModule, coreUiModule, iosAppModule))
    return ComposeUIViewController {
        // PF.4: seed the UIViewController-scoped platform managers at the iOS root.
        LocalManagerProvider {
            AppHost { IosDemoApp() }
        }
    }
}
