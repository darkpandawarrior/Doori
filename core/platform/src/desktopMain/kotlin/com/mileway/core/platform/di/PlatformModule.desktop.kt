package com.mileway.core.platform.di

import com.mileway.core.platform.AnalyticsHelper
import com.mileway.core.platform.AppShortcuts
import com.mileway.core.platform.CrashReporter
import com.mileway.core.platform.DesktopAppShortcuts
import com.mileway.core.platform.DesktopHaptics
import com.mileway.core.platform.DesktopLocationTracker
import com.mileway.core.platform.DesktopMotionSensorProvider
import com.mileway.core.platform.DesktopNotificationScheduler
import com.mileway.core.platform.DesktopShareSheet
import com.mileway.core.platform.DesktopTrackingPresenceController
import com.mileway.core.platform.DesktopUrlOpener
import com.mileway.core.platform.Haptics
import com.mileway.core.platform.LocationNameResolver
import com.mileway.core.platform.LocationTracker
import com.mileway.core.platform.LoggingAnalyticsHelper
import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.NapierCrashReporter
import com.mileway.core.platform.NotificationScheduler
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.core.platform.ShakeGestureDetector
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.TrackingPresenceController
import com.mileway.core.platform.UrlOpener
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
        single<LocationTracker> { DesktopLocationTracker() }
        single<LocationNameResolver> { OfflineLocationNameResolver() }
        single<NotificationScheduler> { DesktopNotificationScheduler() }
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
    }
