import SwiftUI

struct ContentView: View {
    @StateObject private var graph = WatchGraph()

    var body: some View {
        VStack(spacing: 8) {
            Text(graph.snapshot.formattedDistance)
                .font(.system(size: 28, weight: .bold, design: .rounded))

            Text(graph.snapshot.isTracking ? "Tracking" : "Idle")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .onAppear { graph.refresh() }
    }
}
