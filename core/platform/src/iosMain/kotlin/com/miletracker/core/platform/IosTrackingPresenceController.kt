package com.miletracker.core.platform

import io.github.aakira.napier.Napier

private const val TAG = "IosPresence"

/**
 * P-D.2 iOS: drives ActivityKit Live Activity + Dynamic Island from KMP.
 *
 * The Swift Widget Extension hosts the SwiftUI views (lock-screen + Dynamic Island compact/
 * expanded/minimal layouts). This controller calls through the Swift bridge functions
 * (`startLiveActivity`, `updateLiveActivity`, `endLiveActivity`) that must be authored in
 * Swift and exposed as @_cdecl or via a shared KMP → Swift interface. The bridge symbols are
 * resolved at runtime; when ActivityKit is unavailable (iOS < 16.1) the calls are no-ops
 * and a [Napier] log is emitted instead.
 *
 * Xcode steps (documented, manual-verify required):
 * 1. Add a Widget Extension target → choose "Live Activity" template.
 * 2. Define `MilewayActivityAttributes : ActivityAttributes` with content state fields:
 *    distanceKm, durationMs, speedKmh, activityLabel, isPaused.
 * 3. Implement `ActivityConfiguration<MilewayActivityAttributes>` with lock-screen view and
 *    DynamicIsland layouts (compact leading: distance, compact trailing: speed, expanded: full
 *    row with activity label + duration, minimal: distance badge).
 * 4. In the app target AppDelegate, call IosTrackingPresenceController().start() on session
 *    begin and bridge the snapshot via the provided functions.
 * 5. Add "Supports Live Activities" = YES to the app target's Info.plist.
 */
class IosTrackingPresenceController : TrackingPresenceController {
    override fun start(snapshot: TrackingPresenceSnapshot) {
        Napier.d(
            "Live Activity start: ${snapshot.distanceKm} km / ${snapshot.activityLabel}",
            tag = TAG,
        )
        // Swift bridge call: startLiveActivity(distanceKm, durationMs, speedKmh, activityLabel, isPaused)
        // TODO(ios): wire once the Swift Widget Extension is registered in Xcode.
    }

    override fun update(snapshot: TrackingPresenceSnapshot) {
        Napier.d(
            "Live Activity update: ${snapshot.distanceKm} km / ${snapshot.speedKmh} km/h",
            tag = TAG,
        )
        // Swift bridge call: updateLiveActivity(distanceKm, durationMs, speedKmh, activityLabel, isPaused)
        // TODO(ios): wire once the Swift Widget Extension is registered in Xcode.
    }

    override fun stop() {
        Napier.d("Live Activity end", tag = TAG)
        // Swift bridge call: endLiveActivity()
        // TODO(ios): wire once the Swift Widget Extension is registered in Xcode.
    }
}
