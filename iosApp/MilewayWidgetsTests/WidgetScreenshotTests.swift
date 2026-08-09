// showcase: renders the iOS WidgetKit widget views to PNGs via ImageRenderer — fixed-layout
// widget views render cleanly (no ScrollView), so no home-screen placement is needed. Writes to
// the host repo's docs/screenshots/. Run: xcodebuild test -scheme MilewayWidgetsTests
//      -sdk iphonesimulator -only-testing:MilewayWidgetsTests/WidgetScreenshotTests
// NB: no `@testable import MilewayWidgets` — the MilewayWidgetsTests target compiles the widget
// view sources directly into itself (app-extension targets can't be linked into a test bundle,
// see project.yml), so the types below are already in this module.

import SwiftUI
import WidgetKit
import XCTest

final class WidgetScreenshotTests: XCTestCase {
    // Derived, not hardcoded. This was previously pinned to .../Repos/Mileway/docs/screenshots —
    // a path that does not exist since the repo moved under Repos/Android/. Because the writer
    // calls createDirectory(withIntermediateDirectories: true), every capture silently CREATED
    // that phantom folder and wrote there, so the PNGs never reached the repo and nobody saw a
    // failure. SCREENSHOT_OUT_DIR is set by the test scheme/CI; the fallback is the real path.
    private let outDir = ProcessInfo.processInfo.environment["SCREENSHOT_OUT_DIR"]
        ?? "/Users/darkpandawarrior/Repos/Android/Mileway/docs/screenshots"

    private var mockEntry: MileageWidgetEntry {
        MileageWidgetEntry(
            date: Date(timeIntervalSince1970: 1_700_000_000),
            payload: MilewaySyncPayload(
                todayKm: 12.4,
                weekKm: 58.7,
                tripCount: 4,
                isTracking: true,
                weekGoalProgress: 0.587,
                lastTripLabel: "Commute"
            )
        )
    }

    @MainActor
    func testCaptureHomeWidget() throws {
        let view = MileageHomeWidgetView(entry: mockEntry)
            .frame(width: 329, height: 155)
        try render(view, to: "widget_ios_home.png")
    }

    @MainActor
    func testCaptureLockScreenWidget() throws {
        let view = MileageAccessoryRectangularView(entry: mockEntry)
            .frame(width: 160, height: 72)
        try render(view, to: "widget_ios_lockscreen.png")
    }

    @MainActor
    @available(iOS 16.2, *)
    func testCaptureLiveActivity() throws {
        let state = MilewayTrackingAttributes.ContentState(
            distanceKm: 12.4, elapsedSeconds: 754, isPaused: false
        )
        let canvas = Color(red: 0x0B / 255, green: 0x08 / 255, blue: 0x06 / 255)
        try render(
            TrackingLockScreenView(state: state).frame(width: 360, height: 84).background(canvas),
            to: "live_activity.png"
        )
        try render(
            TrackingDynamicIslandExpandedView(state: state).frame(width: 360).padding(24)
                .background(Color.black),
            to: "live_activity_dynamic_island.png"
        )
    }

    @MainActor
    private func render(_ view: some View, to name: String) throws {
        let renderer = ImageRenderer(content: view)
        renderer.scale = 3
        guard let image = renderer.uiImage, let png = image.pngData() else {
            throw XCTSkip("ImageRenderer produced no image in this environment")
        }
        try FileManager.default.createDirectory(atPath: outDir, withIntermediateDirectories: true)
        let url = URL(fileURLWithPath: outDir).appendingPathComponent(name)
        try png.write(to: url)
        let attachment = XCTAttachment(data: png, uniformTypeIdentifier: "public.png")
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
