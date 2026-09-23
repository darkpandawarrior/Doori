// P4.4: the watch dashboard — today/week distance + tracking pill, rendered from
// WatchDashboardModel's published WatchSnapshot. Colors mirror core:ui's Ember token spec
// (T.2: canvas #0B0806, amber accent #F5A623, red live #FF453A) natively in SwiftUI — Compose
// tokens don't cross the KMP/SwiftUI boundary, so the palette is re-declared here, not imported.
//
// P8.2 VoiceOver checklist (manually walked on Apple Watch Simulator, VoiceOver on):
//  - [x] "Distance today" reads as one stop with a spoken-out value ("12.5 kilometers"), not two
//        fragments ("12.5 km" then "today").
//  - [x] "This week" reads as one stop with a spoken-out value, same pattern.
//  - [x] Tracking pill (Z.5d: now tappable to start/stop) announces as a button with a label
//        stating the current state and a hint naming the resulting action.
//  - [x] "Trips" NavigationLink keeps its default button trait + a hint describing the destination.
//  - [x] Trip list rows each read as one stop ("Commute, 8.2 kilometers") via SwiftUI's built-in
//        NavigationLink button trait plus the merged label/value.
//  - [x] Trip detail screen reads as one stop with label + value.
//  - [x] Dynamic Type: headline/value text uses `.minimumScaleFactor` + `.lineLimit` so long
//        localized numbers/labels compress rather than clip on the 40mm watch face.

import SwiftUI

/// The Ember palette's watch-relevant subset, hand-mirrored from
/// `core/ui/.../theme/MilewayThemes.kt`'s `EmberSpec` (T.2: amber accent `#F5A623`, red live
/// `#FF453A`, canvas `#0B0806`).
enum WatchMatrixPalette {
    static let canvas = Color(red: 0x0B / 255, green: 0x08 / 255, blue: 0x06 / 255)
    static let accent = Color(red: 0xF5 / 255, green: 0xA6 / 255, blue: 0x23 / 255)
    static let live = Color(red: 0xFF / 255, green: 0x45 / 255, blue: 0x3A / 255)
    static let text = Color(red: 0xF7 / 255, green: 0xEF / 255, blue: 0xE3 / 255)
    static let textMuted = Color(red: 0xC9 / 255, green: 0xB9 / 255, blue: 0xA3 / 255)
}

struct WatchDashboardView: View {
    @ObservedObject var model: WatchDashboardModel

    var body: some View {
        ScrollView { dashboardContent }
            .background(WatchMatrixPalette.canvas)
            .navigationTitle("Doori")
            .task { await model.refresh() }
    }

    /// The scroll-independent body content, factored out so a screenshot test can host-render it
    /// with `ImageRenderer` (which does not render `ScrollView`-virtualized content).
    @ViewBuilder var dashboardContent: some View {
        VStack(spacing: 14) {
            VStack(spacing: 6) {
                Text(String(format: "%.1f km", model.snapshot.todayDistanceKm))
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(WatchMatrixPalette.accent)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                Text("today")
                    .font(.caption2)
                    .foregroundStyle(WatchMatrixPalette.textMuted)
            }
            // P8.2: today's distance + its "today" caption read as one VoiceOver stop, with a
            // clear value, rather than two separate stops the user has to piece together.
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Distance today")
            .accessibilityValue(String(format: "%.1f kilometers", model.snapshot.todayDistanceKm))

            VStack(spacing: 6) {
                Text(String(format: "%.1f km this week", model.snapshot.weekDistanceKm))
                    .font(.caption)
                    .foregroundStyle(WatchMatrixPalette.text)
                    .minimumScaleFactor(0.7)
                    .lineLimit(2)
                    .accessibilityLabel("This week")
                    .accessibilityValue(String(format: "%.1f kilometers", model.snapshot.weekDistanceKm))

                trackingPill

                NavigationLink("Trips") {
                    WatchTripListView(trips: model.trips)
                }
                .padding(.top, 4)
                .accessibilityHint("Opens your recent trips list")
            }
            .padding()
        }
    }

    private var trackingPill: some View {
        // Z.5d: tapping sends a watch->phone start/stop command over WCSession
        // (`WatchTrackingCommandSender`) — the phone dispatches it through the same
        // `IosIntentEntry` seam its own App Intents use. The pill's displayed state still comes
        // from the synced `SurfaceSnapshot`, not from this tap's local effect.
        Button(action: model.toggleTracking) {
            Text(pillLabel)
                .font(.caption2.weight(.semibold))
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(pillColor.opacity(0.2))
                .foregroundStyle(pillColor)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        // P8.2: the pill is now an action — expose it as a button with a label naming the current
        // state and a hint naming the tap's effect.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tracking status, \(pillLabel)")
        .accessibilityHint(model.snapshot.isTracking ? "Stops the current trip" : "Starts a new trip")
        .accessibilityAddTraits(.isButton)
    }

    private var pillLabel: String {
        if model.snapshot.isTracking {
            return model.snapshot.isPaused ? "Paused" : "Tracking"
        }
        return "Idle"
    }

    private var pillColor: Color {
        model.snapshot.isTracking && !model.snapshot.isPaused ? WatchMatrixPalette.live : WatchMatrixPalette.textMuted
    }
}

#Preview {
    NavigationStack {
        WatchDashboardView(model: WatchDashboardModel())
    }
}
