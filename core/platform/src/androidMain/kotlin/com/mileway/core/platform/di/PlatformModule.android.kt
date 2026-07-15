package com.mileway.core.platform.di

import com.mileway.core.platform.AndroidAppShortcuts
import com.mileway.core.platform.AndroidBatteryStatusReader
import com.mileway.core.platform.AndroidHaptics
import com.mileway.core.platform.AndroidMotionSensorProvider
import com.mileway.core.platform.AndroidShareSheet
import com.mileway.core.platform.AndroidTrackingPresenceController
import com.mileway.core.platform.AndroidUrlOpener
import com.mileway.core.platform.AppShortcuts
import com.mileway.core.platform.BatteryStatusReader
import com.mileway.core.platform.Haptics
import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.NotificationChannels
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.core.platform.ShakeGestureDetector
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.TrackingPresenceController
import com.mileway.core.platform.UrlOpener
import com.siddharth.kmp.appshell.AndroidLocationTracker
import com.siddharth.kmp.appshell.AndroidNotificationScheduler
import com.siddharth.kmp.appshell.LocationNameResolver
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.appshell.NotificationScheduler
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Android bindings for the platform services.
 * 2.2a: Location, Notification. 2.2c: BackgroundScheduler.
 * (Biometric/DocScan/Permissions → 2.2d.)
 */
actual fun platformModule(): Module =
    module {
        // createdAtStart: NotificationChannels.TRACKING/URGENT are posted to directly by
        // AndroidTrackingPresenceController / the FCM service without going through
        // AndroidNotificationScheduler, so channel creation must not be lazily coupled to that
        // single's own first `get()` — force it up front instead.
        single(createdAtStart = true) { NotificationChannels.ensureChannels(androidContext()) }
        single<LocationTracker> { AndroidLocationTracker(androidContext()) }
        // Reverse geocoding → place names. The offline-first demo binds the deterministic offline
        // resolver (no network) so live tracking shows real-looking Pune waypoint names; the
        // device-backed AndroidLocationNameResolver(Geocoder) remains the production path.
        // PLAN_V24 P10.5: the reverse-geocode source toggle is a named StateFlow bound in core:data
        // (which owns the PluginRegistry). Resolved by qualifier here so core:platform stays free of a
        // core:data compile dependency; getOrNull keeps graphs that omit it on the local-table default.
        single<LocationNameResolver> {
            val remote = getOrNull<StateFlow<Boolean>>(named("reverseGeocodeRemote"))
            OfflineLocationNameResolver(remoteSourceEnabled = { remote?.value ?: false })
        }
        single<NotificationScheduler> {
            AndroidNotificationScheduler(androidContext(), channelId = NotificationChannels.GENERAL, channelName = "General")
        }
        // SH.1: real system-chooser share sheet (LocalManagerProvider resolves it via Koin).
        single<ShareSheet> { AndroidShareSheet(androidContext()) }
        single<UrlOpener> { AndroidUrlOpener(androidContext()) }
        // SH.3: home-screen quick actions → deep links.
        single<AppShortcuts> { AndroidAppShortcuts(androidContext()) }
        // UX.2: haptic feedback.
        single<Haptics> { AndroidHaptics(androidContext()) }
        // O: cross-platform motion sensors (accelerometer + gyroscope).
        single<MotionSensorProvider> { AndroidMotionSensorProvider(androidContext()) }
        // P31.MISC.1: shake-to-report, layered on the same accelerometer stream as motion state.
        single { ShakeGestureDetector(get()) }
        // P-D.2: live presence surface (updates ongoing notification from each snapshot).
        single<TrackingPresenceController> { AndroidTrackingPresenceController(androidContext()) }
        // PLAN_V33 C6: battery preflight gate before a trip can start.
        single<BatteryStatusReader> { AndroidBatteryStatusReader(androidContext()) }
    }
