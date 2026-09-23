// P6.3: interactive App-Intent button (iOS 17+ `Button(intent:)` inside widget views). Runs in the
// widget-extension process, so it can only flip the shared snapshot's `isTracking` flag and ask
// WidgetKit to reload — the actual start/stop side-effect (GPS, Room writes) happens in the host
// app process via P4.5/P2.10's `WatchFacade`/sync bridge, same as the watch's own start/stop
// command path. Full app-side wiring (host app observing this flag flip and driving
// TrackingController) is a P6.3 follow-up once the app target adopts this shared payload as its
// live tracking source — this task's acceptance is the widget compiling/rendering + the intent
// round-tripping the flag through the App-Group store, not launching real GPS tracking from the
// extension process (which App Intents deliberately keep out-of-process for).

import AppIntents
import WidgetKit

@available(iOS 17.0, *)
struct ToggleTrackingIntent: AppIntent {
    static var title: LocalizedStringResource = "Toggle Doori Tracking"
    static var description = IntentDescription("Starts or stops mileage tracking from the widget.")

    func perform() async throws -> some IntentResult {
        var payload = MileageSnapshotStore.read()
        payload.isTracking.toggle()
        payload.isPaused = false
        MileageSnapshotStore.write(payload)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}
