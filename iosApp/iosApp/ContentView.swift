import SwiftUI
import Mileway

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
        // Renders the real shared app-shell (home dashboard + core features) — full KMP shell parity.
        MilewayAppViewControllerKt.MilewayAppViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
