// WatchGraph: single composition root for the watchOS target.
// Builds the dependency graph once (actor-isolated) and exposes SurfaceSnapshots
// to the SwiftUI layer so views stay pure value types.
// TODO ios: replace DemoSnapshot with real data from the KMP shared framework.

import SwiftUI

struct SurfaceSnapshot {
    let distanceKm: Double
    let isTracking: Bool
    let formattedDistance: String
}

@MainActor
final class WatchGraph: ObservableObject {
    @Published private(set) var snapshot = SurfaceSnapshot(
        distanceKm: 0.0,
        isTracking: false,
        formattedDistance: "0.0 km",
    )

    func refresh() {
        // TODO ios: fetch from shared KMP framework via WatchConnectivity or DataLayer equivalent.
        snapshot = SurfaceSnapshot(
            distanceKm: snapshot.distanceKm,
            isTracking: snapshot.isTracking,
            formattedDistance: String(format: "%.1f km", snapshot.distanceKm),
        )
    }
}
