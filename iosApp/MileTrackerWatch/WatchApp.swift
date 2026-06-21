// MileTrackerWatch — watchOS companion stub
// Presents today's tracked distance on the wrist. Wired to WatchGraph which holds
// a single copy of the KMP shared framework and exposes pre-computed snapshots.
// TODO ios: link the real shared KMP framework (compileKotlinWatchosArm64 target)
//           once the watchOS KMP target is added to :core modules.

import SwiftUI

@main
struct MileTrackerWatchApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
