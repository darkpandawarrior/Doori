package com.miletracker.core.platform

/**
 * iOS document scanning (F).
 *
 * Unlike the other six platform services — all headless and now implemented against their iOS frameworks
 * (CoreLocation, Vision, LocalAuthentication, UNUserNotificationCenter, BGTaskScheduler, AVCaptureDevice) —
 * VisionKit's `VNDocumentCameraViewController` is **inherently a UI flow**: it must be *presented modally*
 * from a live `UIViewController` and delivers its pages through a delegate, so it cannot be driven from a
 * background service object that has no presentation context (the Android side is the same — ML Kit's
 * document scanner needs an Activity, which is why `DocumentScanner` is not bound in the Android
 * `platformModule()` either).
 *
 * The correct home for this is the Compose UI layer (the `ComposeUIViewController` host can present the
 * scanner and pump pages back), mirroring how camera capture and maps live in the UI layer rather than as
 * services. Until that UI hook exists this returns no pages — a truthful no-op rather than fragile,
 * untestable view-controller-presentation interop.
 */
class IosDocumentScanner : DocumentScanner {
    override suspend fun scan(maxPages: Int): List<ByteArray> = emptyList()
}
