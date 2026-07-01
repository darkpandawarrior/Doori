package com.mileway.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.IosDemoApp
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.feature.agent.iosAgentModule
import com.mileway.feature.tracking.di.trackingModule
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
