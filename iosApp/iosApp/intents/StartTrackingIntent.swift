// P7.1: iOS App Intent — Siri/Spotlight/Shortcuts entry point for starting a mileage trip.
// Calls straight through to the shared KMP `IosIntentEntry.startTracking()` (feature:logging/
// iosMain), the same offline `WatchFacade` seam Android's AppFunctions (P7.5) and the Wear OS
// tile already bind to. No network — Room/DataStore only, per CLAUDE.md.

import AppIntents
import Mileway

struct StartTrackingIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Doori Trip"
    static var description = IntentDescription("Starts tracking a new mileage trip in Doori.")

    @MainActor
    func perform() async throws -> some IntentResult {
        try await IosIntentEntry.shared.startTracking()
        return .result()
    }
}
