package com.mileway.core.platform.di

import com.mileway.core.platform.IosAppReviewManager
import com.mileway.core.platform.IosAppUpdateManager
import com.mileway.core.platform.IosBiometricAuthenticator
import com.mileway.core.platform.IosDocumentScanner
import com.mileway.core.platform.IosLocationTracker
import com.mileway.core.platform.IosNotificationScheduler
import com.mileway.core.platform.IosPermissionsProvider
import com.mileway.core.platform.IosTrackingPresenceController
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<com.mileway.core.platform.LocationTracker> { IosLocationTracker() }
        // Reverse geocoding → place names. Offline-first demo binds the deterministic offline
        // resolver (no network); IosLocationNameResolver(CLGeocoder) is the device-backed path.
        single<com.mileway.core.platform.LocationNameResolver> {
            com.mileway.core.platform.OfflineLocationNameResolver()
        }
        single<com.mileway.core.platform.DocumentScanner> { IosDocumentScanner() }
        single<com.mileway.core.platform.NotificationScheduler> { IosNotificationScheduler() }
        single<com.mileway.core.platform.BiometricAuthenticator> { IosBiometricAuthenticator() }
        single<com.mileway.core.platform.PermissionsProvider> { IosPermissionsProvider() }
        // V15 UP.3: iOS in-app update (iTunes Lookup API). Picked up by LocalManagerProvider when iOS
        // Koin is started; falls back to no-op until then.
        single<com.mileway.core.platform.AppUpdateManager> { IosAppUpdateManager() }
        // V15 RV.3: iOS in-app review (SKStoreReviewController window-scene variant).
        single<com.mileway.core.platform.AppReviewManager> { IosAppReviewManager() }
        // SH.1: iOS share via UIActivityViewController (LocalManagerProvider resolves it via Koin).
        single<com.mileway.core.platform.ShareSheet> { com.mileway.core.platform.IosShareSheet() }
        single<com.mileway.core.platform.UrlOpener> { com.mileway.core.platform.IosUrlOpener() }
        // SH.3: app shortcuts contract (iOS impl is a documented no-op, see IosAppShortcuts).
        single<com.mileway.core.platform.AppShortcuts> { com.mileway.core.platform.IosAppShortcuts() }
        // UX.2: haptic feedback via UIFeedbackGenerator.
        single<com.mileway.core.platform.Haptics> { com.mileway.core.platform.IosHaptics() }
        // O: cross-platform motion sensors via CoreMotion.
        single<com.mileway.core.platform.MotionSensorProvider> { com.mileway.core.platform.IosMotionSensorProvider() }
        // P-D.2: live presence surface (drives ActivityKit Live Activity + Dynamic Island).
        single<com.mileway.core.platform.TrackingPresenceController> { IosTrackingPresenceController() }
        // CF.4: local crash reporter (Napier-backed, no real crash SDK, no network). AnalyticsHelper
        // is already bound in iosAppModule (core/ui) — not duplicated here.
        single<com.mileway.core.platform.CrashReporter> { com.mileway.core.platform.NapierCrashReporter() }
    }
