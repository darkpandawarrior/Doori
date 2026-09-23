// P7.1: iOS App Intent — Siri/Spotlight/Shortcuts entry point for stopping the active mileage
// trip. See StartTrackingIntent.swift for the shared-bridge rationale. A no-op (never throws) if
// no trip is currently active, mirroring `WatchFacade.stopTracking()`'s own semantics.

import AppIntents
import Mileway

struct StopTrackingIntent: AppIntent {
    static var title: LocalizedStringResource = "Stop Doori Trip"
    static var description = IntentDescription("Stops the currently active mileage trip in Doori.")

    @MainActor
    func perform() async throws -> some IntentResult {
        try await IosIntentEntry.shared.stopTracking()
        return .result()
    }
}
