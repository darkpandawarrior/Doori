// P6.4: ActivityKit contract for the tracking Live Activity + Dynamic Island. Shared (not
// framework-backed, same reasoning as MilewaySyncModels.swift) so both the host app (starts/updates/
// ends the Activity) and the MilewayWidgets extension (renders it) compile the exact same
// `ActivityAttributes` type without linking the KMP `Mileway` framework from the extension process.
import ActivityKit
import Foundation

@available(iOS 16.2, *)
struct MilewayTrackingAttributes: ActivityAttributes {
    /// Fields that change while the trip is tracking — ActivityKit re-renders on every update.
    struct ContentState: Codable, Hashable {
        var distanceKm: Double
        var elapsedSeconds: Int
        var isPaused: Bool
    }

    /// Fixed for the lifetime of one Live Activity (set at `Activity.request`, never updated).
    var tripStartedAtMs: Int64
}

/// Mirrors the title copy `TrackingNotificationMapper.fromSnapshot()` (Kotlin, shared, platform-
/// neutral) produces for its `ACTIVE`/`PAUSED` branches — see
/// `feature/tracking/src/commonMain/kotlin/com/mileway/feature/tracking/service/
/// TrackingNotificationContent.kt`. That mapper is the single source of truth the Android
/// notification, Wear and Glance surfaces already render from; a Live Activity's `ContentState`
/// only ever carries `isPaused` (no GPS/permission flags), so `active`/`paused` are the only two
/// mapper branches this surface can represent.
///
/// The widget extension deliberately does not link `Mileway.framework` (see
/// `MilewaySyncModels.swift`'s doc comment for the App-Group-process reasoning), so calling the
/// mapper directly isn't an option here — these two strings are mirrored BY HAND instead.
/// **If `TrackingNotificationMapper`'s "Tracking active" / "Tracking paused" titles ever change,
/// update these two constants in the same commit — this is the one place iOS copy can silently
/// drift from Android/Wear/Glance.**
enum TrackingActivityCopy {
    static let active = "Tracking active"
    static let paused = "Tracking paused"

    static func title(isPaused: Bool) -> String {
        isPaused ? paused : active
    }
}
