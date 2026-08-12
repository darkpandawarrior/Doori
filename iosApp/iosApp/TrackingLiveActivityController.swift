// P6.4: host-app-side ActivityKit lifecycle (start/update/end) for the tracking Live Activity.
// Driven from the same `MilewaySyncPayload` the watch sync bridge already pushes on every trip
// change (see PhoneWatchSyncBridge.swift) — no separate tracking-state source, same "one snapshot,
// many readers" shape as P6.1's SnapshotCache.
import ActivityKit
import Foundation

@available(iOS 16.2, *)
final class TrackingLiveActivityController {
    static let shared = TrackingLiveActivityController()

    /// Floor on how often `activity.update()` is called. `apply` can be driven by
    /// `SnapshotPublisher` as often as once per GPS fix (core/data's `PhoneSnapshotSync`);
    /// ActivityKit's own guidance is not to update more than once a second, so this is a cheap
    /// client-side budget on top of whatever the system itself throttles.
    private static let minimumUpdateInterval: TimeInterval = 1

    /// How far past "now" each update's `staleDate` sits. If tracking stops without a clean
    /// `end()` landing (app killed, GPS silently lost mid-trip) no further `apply()` calls arrive,
    /// so without this the Lock Screen/Dynamic Island would show a frozen-but-still-"live" elapsed
    /// time forever. Past this window iOS renders its own stale-content (dimmed) treatment instead.
    private static let staleAfter: TimeInterval = 60

    private var currentActivity: Activity<MilewayTrackingAttributes>?
    private var lastUpdateAt: Date?

    /// Call on every snapshot update (mirrors `PhoneWatchSyncBridge.push`): starts the Activity on
    /// the tracking-off→on edge, updates it while tracking, ends it on the on→off edge. A no-op if
    /// Live Activities are disabled by the user (`areActivitiesEnabled`) or unsupported.
    func apply(_ payload: MilewaySyncPayload) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        if !payload.isTracking {
            end()
            return
        }

        // A force-quit mid-trip kills this class's in-memory state but not the OS-owned Activity —
        // adopt any pre-existing one for this attributes type on relaunch instead of starting a
        // duplicate (ActivityKit is meant to carry exactly one live tracking Activity at a time).
        if currentActivity == nil {
            currentActivity = Activity<MilewayTrackingAttributes>.activities.first
        }

        if let activity = currentActivity {
            let now = Date()
            if let lastUpdateAt, now.timeIntervalSince(lastUpdateAt) < Self.minimumUpdateInterval { return }
            lastUpdateAt = now
            let content = ActivityContent(state: contentState(for: payload), staleDate: now.addingTimeInterval(Self.staleAfter))
            Task { await activity.update(content) }
        } else {
            let now = Date()
            lastUpdateAt = now
            let attributes = MilewayTrackingAttributes(tripStartedAtMs: payload.updatedAtMs)
            let content = ActivityContent(state: contentState(for: payload), staleDate: now.addingTimeInterval(Self.staleAfter))
            currentActivity = try? Activity.request(attributes: attributes, content: content)
        }
    }

    private func contentState(for payload: MilewaySyncPayload) -> MilewayTrackingAttributes.ContentState {
        let elapsedSeconds = max(0, Int((Date().timeIntervalSince1970 * 1000 - Double(payload.updatedAtMs)) / 1000))
        return MilewayTrackingAttributes.ContentState(
            distanceKm: payload.todayKm,
            elapsedSeconds: elapsedSeconds,
            isPaused: payload.isPaused
        )
    }

    /// Ends the tracked Activity — and, defensively, every other Activity of this type ActivityKit
    /// still has running (the force-quit case above: a fresh process has no `currentActivity`, but
    /// the killed process's Activity is still alive server-side until something ends it). This runs
    /// automatically the next time a real "not tracking" snapshot arrives, so no extra app-launch
    /// wiring is needed to clean up an orphaned Activity after a kill.
    private func end() {
        let toEnd = currentActivity.map { [$0] } ?? Array(Activity<MilewayTrackingAttributes>.activities)
        currentActivity = nil
        lastUpdateAt = nil
        for activity in toEnd {
            Task { await activity.end(nil, dismissalPolicy: .immediate) }
        }
    }
}
