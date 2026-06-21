import SwiftUI
import MileTracker

/// Root view: embeds the Compose Multiplatform UI rendered via Skia.
struct ContentView: View {
    var body: some View {
        ComposeViewControllerRepresentable()
            .ignoresSafeArea(.all)
    }
}

/// Bridges Compose Multiplatform's UIViewController into SwiftUI.
private struct ComposeViewControllerRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
