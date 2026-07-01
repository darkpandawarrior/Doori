package com.mileway.feature.tracking

import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.IosDemoApp
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.feature.tracking.di.trackingModule
import platform.UIKit.UIViewController

/**
 * P-B.3: Full iOS entry point that includes the tracking Koin module.
 *
 * Swift callers (ContentView.swift) should use this instead of MainViewController():
 *   IosTrackingEntryKt.MilwayViewController()
 *
 * We can't add trackingModule to core/ui's MainViewController because feature:tracking depends on
 * core:ui (adding the inverse dep would create a cycle). This file lives in feature:tracking/iosMain
 * so it can see both coreDataModule/coreUiModule and the iosMain trackingModule.
 */
fun MilwayViewController(): UIViewController {
    AppLog.init()
    initKoin(modules = listOf(coreDataModule, coreUiModule, iosAppModule, trackingModule))
    return ComposeUIViewController {
        LocalManagerProvider {
            AppHost { IosDemoApp() }
        }
    }
}
