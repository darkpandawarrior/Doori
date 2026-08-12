// showcase: renders watch SwiftUI screens to PNGs via ImageRenderer — deterministic, no
// home-screen placement or app launch needed. Writes to the host repo's docs/screenshots/.
// Run: xcodebuild test -scheme MilewayWatch -sdk watchsimulator -destination '<watch sim>'
//      -only-testing:MilewayWatchTests/WatchScreenshotTests

import SwiftUI
import XCTest
@testable import MilewayWatch

final class WatchScreenshotTests: XCTestCase {
    // Derived, not hardcoded. This was previously pinned to .../Repos/Mileway/docs/screenshots —
    // a path that does not exist since the repo moved under Repos/Android/. Because the writer
    // calls createDirectory(withIntermediateDirectories: true), every capture silently CREATED
    // that phantom folder and wrote there, so the PNGs never reached the repo and nobody saw a
    // failure. SCREENSHOT_OUT_DIR is set by the test scheme/CI; the fallback is the real path.
    private let outDir = ProcessInfo.processInfo.environment["SCREENSHOT_OUT_DIR"]
        ?? Self.repoScreenshotsDir

    /// Derived from this source file's own location at compile time, so it cannot go stale when the
    /// repo moves — which is exactly what happened before: the path was pinned to
    /// .../Repos/Mileway/..., died when the repo moved under Repos/Android/, and because the writer
    /// calls createDirectory(withIntermediateDirectories: true) every run silently CREATED the
    /// phantom folder and wrote there. No error, no missing file, captures just stopped arriving.
    private static var repoScreenshotsDir: String {
        // <repo>/iosApp/<TestTarget>/<ThisFile>.swift -> up 3 -> <repo>
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("docs/screenshots")
            .path
    }

    @MainActor
    func testCaptureDashboard() throws {
        let model = WatchDashboardModel(
            cachedSnapshot: WatchSnapshot(
                todayDistanceKm: 12.4,
                weekDistanceKm: 58.7,
                weekGoalProgress: 0.587,
                isTracking: true,
                isPaused: false
            )
        )
        let dashboard = WatchDashboardView(model: model).dashboardContent
            .frame(width: 184, height: 224)
            .background(WatchMatrixPalette.canvas)
        try render(dashboard, to: "watchos_app.png")
    }

    @MainActor
    private func render(_ view: some View, to name: String) throws {
        let renderer = ImageRenderer(content: view)
        renderer.scale = 2
        renderer.proposedSize = ProposedViewSize(width: 184, height: 224)
        guard let image = renderer.uiImage, let png = image.pngData() else {
            throw XCTSkip("ImageRenderer produced no image in this environment")
        }
        try FileManager.default.createDirectory(
            atPath: outDir, withIntermediateDirectories: true
        )
        let url = URL(fileURLWithPath: outDir).appendingPathComponent(name)
        try png.write(to: url)
        // Also attach so the image is recoverable from the .xcresult if the host-path write is
        // sandboxed away in some CI setups.
        let attachment = XCTAttachment(data: png, uniformTypeIdentifier: "public.png")
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
