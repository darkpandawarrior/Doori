package com.mileway.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.travel.di.travelModule
import com.mileway.shared.ui.MilewayApp
import com.mileway.stub.di.stubModule
import com.mileway.ui.home.homeModule
import platform.UIKit.UIViewController

/**
 * iOS Compose entry point that renders the **real** Mileway app-shell ([MilewayApp]) — the shared
 * home dashboard + core feature screens under a bottom-tab bar — instead of the old component
 * showcase. Boots the shared Koin graph with every module the shell's screens resolve. Swift's
 * `ContentView` should call `MilewayAppViewControllerKt.MilewayAppViewController()`.
 */
fun MilewayAppViewController(): UIViewController {
    AppLog.init()
    initKoin(
        modules =
            listOf(
                coreDataModule,
                coreUiModule,
                iosAppModule,
                // PLAN_V33 C3: mirrors Android's MilewayApplication module list — trackingModule's
                // get<MilewayNetworkApi>()/get<ConfigProvider>() calls resolve from here.
                stubModule,
                homeModule,
                trackingModule,
                loggingModule,
                travelModule,
            ),
    )
    return ComposeUIViewController {
        LocalManagerProvider {
            AppHost { MilewayApp() }
        }
    }
}
