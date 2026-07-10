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
 */
class DesktopLocationTracker : LocationTracker {
    override val updates = MutableSharedFlow<GeoPoint>().asSharedFlow()

    override suspend fun current(): GeoPoint? = null

    override fun start() = Unit

    override fun stop() = Unit
}

class DesktopNotificationScheduler : NotificationScheduler {
    override suspend fun ensurePermission(): Boolean = false

    override fun notify(
        id: Int,
        title: String,
        body: String,
    ) = Unit

    override fun cancel(id: Int) = Unit
}

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
