// P6.4: ActivityKit widget — Lock Screen banner + Dynamic Island (compact/minimal/expanded)
// presentations for an in-progress trip. Reads only the ContentState ActivityKit hands it (pushed
// by the host app's TrackingLiveActivityController) — no snapshot-store read needed here, unlike
// the home/lock-screen widgets in MileageWidgetProvider.swift, since ActivityKit itself delivers
// state updates to this process.

import ActivityKit
import SwiftUI
import WidgetKit

private func formattedElapsed(_ seconds: Int) -> String {
    let m = seconds / 60
    let s = seconds % 60
    return String(format: "%d:%02d", m, s)
}

@available(iOS 16.2, *)
struct TrackingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MilewayTrackingAttributes.self) { context in
            // Lock Screen / banner presentation (factored into TrackingLockScreenView so it's
            // screenshot-testable off a plain ContentState).
            TrackingLockScreenView(state: context.state)
                .activityBackgroundTint(WidgetMatrixPalette.canvas)
                .activitySystemActionForegroundColor(WidgetMatrixPalette.text)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(String(format: "%.1f km", context.state.distanceKm))
                        .font(.headline)
                        .foregroundStyle(WidgetMatrixPalette.accent)
                        .minimumScaleFactor(0.7)
                        .lineLimit(1)
                        .accessibilityLabel("Distance")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(formattedElapsed(context.state.elapsedSeconds))
                        .font(.headline.monospacedDigit())
                        .foregroundStyle(WidgetMatrixPalette.text)
                        .minimumScaleFactor(0.7)
                        .lineLimit(1)
                        .accessibilityLabel("Elapsed time")
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(TrackingActivityCopy.title(isPaused: context.state.isPaused))
                        .font(.caption)
                        .foregroundStyle(WidgetMatrixPalette.textMuted)
                }
            } compactLeading: {
                Image(systemName: "location.fill")
                    .foregroundStyle(WidgetMatrixPalette.accent)
                    .accessibilityLabel(TrackingActivityCopy.title(isPaused: context.state.isPaused))
            } compactTrailing: {
                Text(formattedElapsed(context.state.elapsedSeconds))
                    .font(.caption2.monospacedDigit())
                    .foregroundStyle(WidgetMatrixPalette.text)
                    .accessibilityLabel("Elapsed time")
            } minimal: {
                Image(systemName: "location.fill")
                    .foregroundStyle(WidgetMatrixPalette.accent)
                    .accessibilityLabel(TrackingActivityCopy.title(isPaused: context.state.isPaused))
            }
        }
    }
}
