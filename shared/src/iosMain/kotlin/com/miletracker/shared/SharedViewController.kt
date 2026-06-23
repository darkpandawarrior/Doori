package com.miletracker.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.miletracker.core.common.AppLog
import com.miletracker.core.data.di.coreDataModule
import com.miletracker.core.ui.AppHost
import com.miletracker.core.ui.IosDemoApp
import com.miletracker.core.ui.di.coreUiModule
import com.miletracker.core.ui.di.initKoin
import com.miletracker.core.ui.di.iosAppModule
import com.miletracker.core.ui.platform.LocalManagerProvider
import com.miletracker.feature.agent.iosAgentModule
import com.miletracker.feature.tracking.di.trackingModule
import platform.UIKit.UIViewController

/**
 * Full iOS entry point that wires ALL feature modules into Koin in one call.
 *
 * :shared sits above all features in the dependency graph, so it can see both
 * feature:tracking and feature:agent without creating sibling cross-deps.
 * Xcode should call SharedViewControllerKt.MilwayAppViewController() instead of
 * IosTrackingEntryKt.MilwayViewController() once the agent module is desired.
 */
fun MilwayAppViewController(): UIViewController {
    AppLog.init()
    initKoin(modules = listOf(coreDataModule, coreUiModule, iosAppModule, trackingModule, iosAgentModule))
    return ComposeUIViewController {
        LocalManagerProvider {
            AppHost { IosDemoApp() }
        }
    }
}
