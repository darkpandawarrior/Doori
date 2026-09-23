// P6.3: widget view family — home screen (systemSmall/systemMedium) + Lock Screen accessories
// (accessoryRectangular/accessoryCircular). Palette hand-mirrored from core:ui's Ember token spec
// (T.2), same approach WatchDashboardView.swift already takes for the watch target (Compose
// tokens don't cross the KMP/SwiftUI boundary).

import SwiftUI
import WidgetKit

enum WidgetMatrixPalette {
    static let canvas = Color(red: 0x0B / 255, green: 0x08 / 255, blue: 0x06 / 255)
    static let accent = Color(red: 0xF5 / 255, green: 0xA6 / 255, blue: 0x23 / 255)
    static let text = Color(red: 0xF7 / 255, green: 0xEF / 255, blue: 0xE3 / 255)
    static let textMuted = Color(red: 0xC9 / 255, green: 0xB9 / 255, blue: 0xA3 / 255)
}

private func formattedKm(_ value: Double) -> String {
    String(format: "%.1f km", value)
}

// MARK: - Home screen (systemSmall / systemMedium)

struct MileageHomeWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: MileageWidgetEntry

    var body: some View {
        ZStack {
            WidgetMatrixPalette.canvas
            VStack(alignment: .leading, spacing: 4) {
                Group {
                    Text(formattedKm(entry.payload.todayKm))
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .foregroundStyle(WidgetMatrixPalette.accent)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    Text("today")
                        .font(.caption2)
                        .foregroundStyle(WidgetMatrixPalette.textMuted)
                }
                // P8.2: today's distance + its caption read as one VoiceOver stop with a clear value.
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Distance today")
                .accessibilityValue(formattedKm(entry.payload.todayKm))

                if family == .systemMedium {
                    Text("\(formattedKm(entry.payload.weekKm)) this week · \(entry.payload.tripCount) trips")
                        .font(.caption)
                        .foregroundStyle(WidgetMatrixPalette.text)
                        .minimumScaleFactor(0.8)
                        .lineLimit(2)
                        .accessibilityLabel(
                            "\(formattedKm(entry.payload.weekKm)) this week, \(entry.payload.tripCount) trips"
                        )
                }

                Spacer(minLength: 4)

                if #available(iOS 17.0, *) {
                    Button(intent: ToggleTrackingIntent()) {
                        Text(entry.payload.isTracking ? "Stop" : "Start")
                            .font(.caption2.weight(.semibold))
                    }
                    .tint(WidgetMatrixPalette.accent)
                    .accessibilityLabel(entry.payload.isTracking ? "Stop tracking" : "Start tracking")
                } else {
                    Text(entry.payload.isTracking ? "Tracking" : "Idle")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(WidgetMatrixPalette.textMuted)
                        .accessibilityLabel("Tracking status")
                        .accessibilityValue(entry.payload.isTracking ? "Tracking" : "Idle")
                }
            }
            .padding()
        }
    }
}

// MARK: - Lock Screen accessories

struct MileageAccessoryRectangularView: View {
    let entry: MileageWidgetEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(formattedKm(entry.payload.todayKm))
                .font(.headline)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text(entry.payload.isTracking ? "Tracking" : "Idle")
                .font(.caption2)
        }
        // P8.2: Lock Screen accessories are read as a single VoiceOver stop by convention.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Doori distance today")
        .accessibilityValue(
            "\(formattedKm(entry.payload.todayKm)), \(entry.payload.isTracking ? "tracking" : "idle")"
        )
    }
}

struct MileageAccessoryCircularView: View {
    let entry: MileageWidgetEntry

    var body: some View {
        Gauge(value: min(entry.payload.weekGoalProgress, 1.0)) {
            Text("km")
        } currentValueLabel: {
            Text(String(format: "%.0f", entry.payload.todayKm))
        }
        .gaugeStyle(.accessoryCircular)
        .accessibilityLabel("Weekly distance goal progress")
        .accessibilityValue(
            "\(Int(min(entry.payload.weekGoalProgress, 1.0) * 100)) percent, \(formattedKm(entry.payload.todayKm)) today"
        )
    }
}

// MARK: - Widgets

struct MileageHomeWidget: Widget {
    let kind = "MileageHomeWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MileageWidgetProvider()) { entry in
            if #available(iOS 17.0, *) {
                MileageHomeWidgetView(entry: entry)
                    .containerBackground(WidgetMatrixPalette.canvas, for: .widget)
            } else {
                MileageHomeWidgetView(entry: entry)
                    .background(WidgetMatrixPalette.canvas)
            }
        }
        .configurationDisplayName("Doori")
        .description("Today's and this week's distance, with quick start/stop.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct MileageAccessoryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: MileageWidgetEntry

    var body: some View {
        switch family {
        case .accessoryCircular:
            MileageAccessoryCircularView(entry: entry)
        default:
            MileageAccessoryRectangularView(entry: entry)
        }
    }
}

struct MileageLockScreenWidget: Widget {
    let kind = "MileageLockScreenWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MileageWidgetProvider()) { entry in
            MileageAccessoryView(entry: entry)
        }
        .configurationDisplayName("Doori Today")
        .description("Today's distance on your Lock Screen.")
        .supportedFamilies([.accessoryRectangular, .accessoryCircular])
    }
}
