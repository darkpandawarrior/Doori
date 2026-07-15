package com.mileway.core.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Desktop (jvm) platform-service stubs for D.1's dashboard-only Compose Desktop target.
 *
 * There is no on-device location/notification/haptics/share-sheet analogue on a desktop dashboard, so
 * these are deliberate no-ops (mirrors how [OfflineLocationNameResolver] is already the offline demo
 * binding on Android/iOS) — kept minimal to the subset `platformModule()` actually binds
 * (see `PlatformModule.android.kt`), not the full interface surface.
 *
 * LocationTracker / NotificationScheduler have no desktop-specific stub here — :app-shell's own
 * NoOpLocationTracker / NoOpNotificationScheduler are already exactly this (never-emit / no-op), bound
 * directly in PlatformModule.desktop.kt instead of redeclaring them.
 */
class DesktopShareSheet : ShareSheet {
    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) = Unit
}

class DesktopUrlOpener : UrlOpener {
    override fun open(url: String) = Unit
}

class DesktopAppShortcuts : AppShortcuts {
    override fun setDynamicShortcuts(shortcuts: List<AppShortcut>) = Unit
}

class DesktopHaptics : Haptics {
    override fun perform(effect: HapticEffect) = Unit
}

class DesktopMotionSensorProvider : MotionSensorProvider {
    override val readings = MutableSharedFlow<MotionReading>().asSharedFlow()

    override fun start() = Unit

    override fun stop() = Unit
}

class DesktopTrackingPresenceController : TrackingPresenceController {
    override fun start(snapshot: TrackingPresenceSnapshot) = Unit

    override fun update(snapshot: TrackingPresenceSnapshot) = Unit

    override fun stop() = Unit
}
