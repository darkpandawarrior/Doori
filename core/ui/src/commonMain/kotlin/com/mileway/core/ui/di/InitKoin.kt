package com.mileway.core.ui.di

import com.mileway.core.platform.di.platformModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

/**
 * Shared Koin bootstrap for both platforms (KOIN.1).
 *
 * Always wires the per-platform service graph ([platformModule]), the `expect`/`actual` module that binds
 * `LocationTracker` / `NotificationScheduler` on Android and the full service +
 * `AppUpdateManager`/`AppReviewManager` set on iOS, then appends the caller's [modules].
 *
 * - Android: [com.mileway.MilewayApplication] passes the full feature/app/flavor list plus the
 *   `androidContext()` / `androidLogger()` setup via [appDeclaration].
 * - iOS: `MainViewController` passes `coreDataModule` + `coreUiModule` + the iOS app module so
 *   `LocalManagerProvider` resolves the real iOS managers instead of the [PlatformBindings] no-op.
 *
 * Koin definition override is enabled in this project (a few graphs intentionally re-bind a service, e.g.
 * `NotificationScheduler` in `trackingModule` and again here via [platformModule]); the last definition
 * wins and both bind the same Android impl, so the duplicate is benign.
 */
fun initKoin(
    modules: List<Module>,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication {
    // Re-entrancy guard: Android Application.onCreate and the iOS view-controller factory may both run.
    val alreadyStarted = runCatching { KoinPlatform.getKoin() }.isSuccess
    if (alreadyStarted) stopKoin()
    return startKoin {
        appDeclaration()
        modules(
            buildList {
                add(platformModule())
                addAll(modules)
            },
        )
    }
}
