package com.miletracker.core.platform.di

import com.miletracker.core.platform.AndroidAppShortcuts
import com.miletracker.core.platform.AndroidBackgroundScheduler
import com.miletracker.core.platform.AndroidHaptics
import com.miletracker.core.platform.AndroidLocationTracker
import com.miletracker.core.platform.AndroidMotionSensorProvider
import com.miletracker.core.platform.AndroidNotificationScheduler
import com.miletracker.core.platform.AndroidShareSheet
import com.miletracker.core.platform.AndroidTextRecognizer
import com.miletracker.core.platform.AppShortcuts
import com.miletracker.core.platform.BackgroundScheduler
import com.miletracker.core.platform.Haptics
import com.miletracker.core.platform.LocationTracker
import com.miletracker.core.platform.MotionSensorProvider
import com.miletracker.core.platform.NotificationScheduler
import com.miletracker.core.platform.ShareSheet
import com.miletracker.core.platform.TextRecognizer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android bindings for the platform services.
 * 2.2a: Location, Notification. 2.2b: TextRecognizer. 2.2c: BackgroundScheduler.
 * (Biometric/DocScan/Permissions → 2.2d.)
 */
actual fun platformModule(): Module =
    module {
        single<LocationTracker> { AndroidLocationTracker(androidContext()) }
        single<NotificationScheduler> { AndroidNotificationScheduler(androidContext()) }
        single<TextRecognizer> { AndroidTextRecognizer() }
        single<BackgroundScheduler> { AndroidBackgroundScheduler(androidContext()) }
        // SH.1: real system-chooser share sheet (LocalManagerProvider resolves it via Koin).
        single<ShareSheet> { AndroidShareSheet(androidContext()) }
        // SH.3: home-screen quick actions → deep links.
        single<AppShortcuts> { AndroidAppShortcuts(androidContext()) }
        // UX.2: haptic feedback.
        single<Haptics> { AndroidHaptics(androidContext()) }
        // O: cross-platform motion sensors (accelerometer + gyroscope).
        single<MotionSensorProvider> { AndroidMotionSensorProvider(androidContext()) }
    }
