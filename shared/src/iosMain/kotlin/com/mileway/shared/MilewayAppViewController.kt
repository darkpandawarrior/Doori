package com.mileway.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.core.ui.platform.LocalReducedMotion
import com.mileway.feature.advances.di.advancesModule
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.service.AppSyncTrigger
import com.mileway.feature.travel.di.travelModule
import com.mileway.shared.ui.MilewayApp
import com.mileway.stub.di.stubModule
import com.mileway.ui.home.homeModule
import org.koin.mp.KoinPlatform
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIApplicationDidBecomeActiveNotification
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
                advancesModule,
                trackingModule,
                loggingModule,
                travelModule,
            ),
    )
    // PLAN_V34 P1: app-scoped outbox flush — connectivity edges for the process lifetime plus a
    // drain on every return to the foreground, mirroring Android's MilewayApplication hook.
    val appSyncTrigger = KoinPlatform.getKoin().get<AppSyncTrigger>()
    appSyncTrigger.start()
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> appSyncTrigger.onAppForeground() }
    return ComposeUIViewController {
        // PLAN_V36 P6: one-shot read of the OS-level "Reduce Motion" accessibility toggle,
        // mirroring the Android root's Settings.Global.ANIMATOR_DURATION_SCALE read — see
        // LocalReducedMotion's KDoc for why this isn't observed live.
        CompositionLocalProvider(LocalReducedMotion provides UIAccessibilityIsReduceMotionEnabled()) {
            LocalManagerProvider {
                AppHost { MilewayApp() }
            }
        }
    }
}
