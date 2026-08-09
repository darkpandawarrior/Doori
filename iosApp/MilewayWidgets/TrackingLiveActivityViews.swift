// P6.4 / showcase: the Live Activity's presentation content factored out of the
// `ActivityConfiguration` closure so it renders from a plain `ContentState` — both the real
// ActivityKit config (TrackingLiveActivity.swift) and a screenshot test host these, since an
// `ActivityViewContext` can't be constructed off-device.

import SwiftUI

private func liveActivityElapsed(_ seconds: Int) -> String {
    String(format: "%d:%02d", seconds / 60, seconds % 60)
}

/// Lock Screen / banner presentation for an in-progress trip.
@available(iOS 16.2, *)
struct TrackingLockScreenView: View {
    let state: MilewayTrackingAttributes.ContentState

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(TrackingActivityCopy.title(isPaused: state.isPaused))
                    .font(.caption2)
                    .foregroundStyle(WidgetMatrixPalette.textMuted)
                Text(String(format: "%.1f km", state.distanceKm))
                    .font(.title2.bold())
                    .foregroundStyle(WidgetMatrixPalette.accent)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
            }
            Spacer()
            Text(liveActivityElapsed(state.elapsedSeconds))
                .font(.title3.monospacedDigit())
                .foregroundStyle(WidgetMatrixPalette.text)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
                .accessibilityLabel("Elapsed time")
        }
        .padding()
        // P8.2: one VoiceOver stop for the whole banner — status, distance, elapsed time.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(TrackingActivityCopy.title(isPaused: state.isPaused))
        .accessibilityValue(
            "\(String(format: "%.1f kilometers", state.distanceKm)), "
                + "\(liveActivityElapsed(state.elapsedSeconds)) elapsed"
        )
    }
}

/// A screenshot-only approximation of the Dynamic Island expanded presentation (the real
/// `DynamicIslandExpandedRegion` layout only renders inside the system Dynamic Island, which can't
/// be host-rendered) — same content (distance / elapsed / status), same Ember palette.
@available(iOS 16.2, *)
struct TrackingDynamicIslandExpandedView: View {
    let state: MilewayTrackingAttributes.ContentState

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(String(format: "%.1f km", state.distanceKm))
                    .font(.headline)
                    .foregroundStyle(WidgetMatrixPalette.accent)
                Spacer()
                Text(liveActivityElapsed(state.elapsedSeconds))
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(WidgetMatrixPalette.text)
            }
            Text(TrackingActivityCopy.title(isPaused: state.isPaused))
                .font(.caption)
                .foregroundStyle(WidgetMatrixPalette.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(Capsule().fill(Color.black))
    }
}
