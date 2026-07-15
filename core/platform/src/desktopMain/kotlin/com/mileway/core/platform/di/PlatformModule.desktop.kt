package com.mileway.core.platform.di

import com.mileway.core.platform.AppShortcuts
import com.mileway.core.platform.BatteryStatusReader
import com.mileway.core.platform.DesktopAppShortcuts
import com.mileway.core.platform.DesktopBatteryStatusReader
import com.mileway.core.platform.DesktopHaptics
import com.mileway.core.platform.DesktopMotionSensorProvider
import com.mileway.core.platform.DesktopShareSheet
import com.mileway.core.platform.DesktopTrackingPresenceController
import com.mileway.core.platform.DesktopUrlOpener
import com.mileway.core.platform.Haptics
import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.core.platform.ShakeGestureDetector
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.TrackingPresenceController
import com.mileway.core.platform.UrlOpener
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.LocationNameResolver
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.appshell.LoggingAnalyticsHelper
import com.siddharth.kmp.appshell.NoOpLocationTracker
import com.siddharth.kmp.appshell.NoOpNotificationScheduler
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.common.CrashReporter
import com.siddharth.kmp.common.NapierCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop (jvm) bindings for the platform services (PLAN_V23 D.1) — a thin dashboard app has no
 * on-device location/notification/haptics hardware, so every binding is a documented no-op stub;
 * same subset [PlatformModule.android.kt] binds (Biometric/DocScan/Permissions/AppUpdate/AppReview
 * are not wired on any platform's dashboard-only path).
 */
actual fun platformModule(): Module =
    module {
        single<LocationTracker> { NoOpLocationTracker }
        single<LocationNameResolver> { OfflineLocationNameResolver() }
        single<NotificationScheduler> { NoOpNotificationScheduler }
        single<ShareSheet> { DesktopShareSheet() }
        single<UrlOpener> { DesktopUrlOpener() }
        single<AppShortcuts> { DesktopAppShortcuts() }
        single<Haptics> { DesktopHaptics() }
        single<MotionSensorProvider> { DesktopMotionSensorProvider() }
        // P31.MISC.1: shake-to-report, layered on the same accelerometer stream as motion state.
        single { ShakeGestureDetector(get()) }
        single<TrackingPresenceController> { DesktopTrackingPresenceController() }
        // CF.2/CF.4: local telemetry (Napier-backed, no real analytics/crash SDK, no network).
        single<AnalyticsHelper> { LoggingAnalyticsHelper() }
        single<CrashReporter> { NapierCrashReporter() }
        // PLAN_V33 C6: battery preflight gate before a trip can start (always unknown -> Ok here).
        single<BatteryStatusReader> { DesktopBatteryStatusReader() }
    }
