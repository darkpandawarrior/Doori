package com.miletracker.core.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.miletracker.core.common.AppLog
import com.miletracker.core.data.di.coreDataModule
import com.miletracker.core.ui.di.coreUiModule
import com.miletracker.core.ui.di.initKoin
import com.miletracker.core.ui.di.iosAppModule
import com.miletracker.core.ui.platform.LocalManagerProvider
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
