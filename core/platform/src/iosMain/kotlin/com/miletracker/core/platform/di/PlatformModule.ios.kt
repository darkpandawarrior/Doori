package com.miletracker.core.platform.di

import com.miletracker.core.platform.IosAppReviewManager
import com.miletracker.core.platform.IosAppUpdateManager
import com.miletracker.core.platform.IosBackgroundScheduler
import com.miletracker.core.platform.IosBiometricAuthenticator
import com.miletracker.core.platform.IosDocumentScanner
import com.miletracker.core.platform.IosLocationTracker
import com.miletracker.core.platform.IosNotificationScheduler
import com.miletracker.core.platform.IosPermissionsProvider
import com.miletracker.core.platform.IosTextRecognizer
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<com.miletracker.core.platform.LocationTracker> { IosLocationTracker() }
        single<com.miletracker.core.platform.TextRecognizer> { IosTextRecognizer() }
        single<com.miletracker.core.platform.DocumentScanner> { IosDocumentScanner() }
        single<com.miletracker.core.platform.NotificationScheduler> { IosNotificationScheduler() }
        single<com.miletracker.core.platform.BiometricAuthenticator> { IosBiometricAuthenticator() }
        single<com.miletracker.core.platform.BackgroundScheduler> { IosBackgroundScheduler() }
        single<com.miletracker.core.platform.PermissionsProvider> { IosPermissionsProvider() }
        // V15 UP.3: iOS in-app update (iTunes Lookup API). Picked up by LocalManagerProvider when iOS
        // Koin is started; falls back to no-op until then.
        single<com.miletracker.core.platform.AppUpdateManager> { IosAppUpdateManager() }
        // V15 RV.3: iOS in-app review (SKStoreReviewController window-scene variant).
        single<com.miletracker.core.platform.AppReviewManager> { IosAppReviewManager() }
        // SH.1: iOS share via UIActivityViewController (LocalManagerProvider resolves it via Koin).
        single<com.miletracker.core.platform.ShareSheet> { com.miletracker.core.platform.IosShareSheet() }
        // SH.3: app shortcuts contract (iOS impl is a documented no-op — see IosAppShortcuts).
        single<com.miletracker.core.platform.AppShortcuts> { com.miletracker.core.platform.IosAppShortcuts() }
        // UX.2: haptic feedback via UIFeedbackGenerator.
        single<com.miletracker.core.platform.Haptics> { com.miletracker.core.platform.IosHaptics() }
        // O: cross-platform motion sensors via CoreMotion.
        single<com.miletracker.core.platform.MotionSensorProvider> { com.miletracker.core.platform.IosMotionSensorProvider() }
    }
