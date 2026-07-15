package com.mileway.core.platform.di

import com.mileway.core.platform.IosBiometricAuthenticator
import com.mileway.core.platform.IosTrackingPresenceController
import com.mileway.core.platform.OfflineLocationNameResolver
import com.siddharth.kmp.appshell.AppReviewManager
import com.siddharth.kmp.appshell.AppUpdateManager
import com.siddharth.kmp.appshell.DocumentScanner
import com.siddharth.kmp.appshell.IosAppReviewManager
import com.siddharth.kmp.appshell.IosAppUpdateManager
import com.siddharth.kmp.appshell.IosDocumentScanner
import com.siddharth.kmp.appshell.IosLocationTracker
import com.siddharth.kmp.appshell.IosNotificationScheduler
import com.siddharth.kmp.appshell.IosPermissionsProvider
import com.siddharth.kmp.appshell.LocationNameResolver
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.PermissionsProvider
import com.siddharth.kmp.common.NapierCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<LocationTracker> { IosLocationTracker() }
        // Reverse geocoding → place names. Offline-first demo binds the deterministic offline
        // resolver (no network); com.siddharth.kmp.appshell.IosLocationNameResolver(CLGeocoder) is
        // the device-backed path.
        single<LocationNameResolver> {
            OfflineLocationNameResolver()
        }
        single<DocumentScanner> { IosDocumentScanner() }
        single<NotificationScheduler> { IosNotificationScheduler() }
        single<com.mileway.core.platform.BiometricAuthenticator> { IosBiometricAuthenticator() }
        single<PermissionsProvider> { IosPermissionsProvider() }
        // V15 UP.3: iOS in-app update (iTunes Lookup API). Picked up by LocalManagerProvider when iOS
        // Koin is started; falls back to no-op until then.
        single<AppUpdateManager> { IosAppUpdateManager() }
        // V15 RV.3: iOS in-app review (SKStoreReviewController window-scene variant).
        single<AppReviewManager> { IosAppReviewManager() }
        // SH.1: iOS share via UIActivityViewController (LocalManagerProvider resolves it via Koin).
        single<com.mileway.core.platform.ShareSheet> { com.mileway.core.platform.IosShareSheet() }
        single<com.mileway.core.platform.UrlOpener> { com.mileway.core.platform.IosUrlOpener() }
        // SH.3: app shortcuts contract (iOS impl is a documented no-op, see IosAppShortcuts).
        single<com.mileway.core.platform.AppShortcuts> { com.mileway.core.platform.IosAppShortcuts() }
        // UX.2: haptic feedback via UIFeedbackGenerator.
        single<com.mileway.core.platform.Haptics> { com.mileway.core.platform.IosHaptics() }
        // O: cross-platform motion sensors via CoreMotion.
        single<com.mileway.core.platform.MotionSensorProvider> { com.mileway.core.platform.IosMotionSensorProvider() }
        // P31.MISC.1: shake-to-report, layered on the same accelerometer stream as motion state.
        single { com.mileway.core.platform.ShakeGestureDetector(get()) }
        // P-D.2: live presence surface (drives ActivityKit Live Activity + Dynamic Island).
        single<com.mileway.core.platform.TrackingPresenceController> { IosTrackingPresenceController() }
        // CF.4: local crash reporter (Napier-backed, no real crash SDK, no network). AnalyticsHelper
        // is already bound in iosAppModule (core/ui) — not duplicated here.
        single<com.siddharth.kmp.common.CrashReporter> { NapierCrashReporter() }
    }
