import UIKit
import UserNotifications
import BackgroundTasks
import Mileway

/// FCM.4: APNs registration + token/tap forwarding into the KMP shared layer.
///
/// Firebase is added via SPM (see docs/RELEASE.md "iOS push setup"). Once `FirebaseMessaging` is linked,
/// swap the raw APNs token below for `Messaging.messaging().fcmToken` and set the APNs token on Messaging.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
            DispatchQueue.main.async { application.registerForRemoteNotifications() }
        }
        // V26 AI: register the Foundation Models Swift bridges (EXPERIMENTAL, xcodebuild-gated —
        // see FoundationModelsDocumentAnalyzer.swift / FoundationModelsTextGenerator.swift). Both
        // seams degrade to "unavailable" if this line is ever removed — nothing else depends on it.
        FoundationModelsBridge.shared.seam.analyzer = FoundationModelsDocumentAnalyzer()
        FoundationModelsTextGeneratorBridge.shared.seam.generator = FoundationModelsTextGenerator()
        // RF.3: capture a deferred referral left on the pasteboard at first launch.
        ReferralBridge.shared.captureDeferred()
        // P4.5: activate the WatchConnectivity session so a paired watch gets the latest
        // snapshot via applicationContext as soon as one is available.
        PhoneWatchSyncBridge.shared.activate()
        // P-C.2: CoreLocation relaunched a terminated app because significant-change monitoring
        // saw movement (see IosLocationTracker.startMonitoringSignificantLocationChanges) — mark
        // the pending relaunch and resume the active track, if any.
        if launchOptions?[.location] != nil {
            TrackingRelaunchBridge.shared.onSystemRelaunch()
        }
        // Register BGTaskScheduler handlers (P-F.1/P-F.3).
        // The identifier strings must match BGTaskSchedulerPermittedIdentifiers in Info.plist.
        // IosBgTaskDispatcher (feature:tracking/iosMain) maps the identifier to the matching
        // kmpworkmanager Worker (maintenance purge / auto-discard) via MilewayWorkerFactory and runs it.
        registerBgTasks()
        return true
    }

    private func registerBgTasks() {
        // 90-day stale-row purge — runs as a BGProcessingTask (longer runtime, no network required).
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.mileway.maintenance",
            using: nil
        ) { task in
            IosBgTaskDispatcher.shared.runTask(taskId: task.identifier) { success in
                task.setTaskCompleted(success: success.boolValue)
            }
            // Reschedule next weekly run.
            let req = BGProcessingTaskRequest(identifier: "com.mileway.maintenance")
            req.earliestBeginDate = Date(timeIntervalSinceNow: 7 * 24 * 60 * 60)
            req.requiresNetworkConnectivity = false
            try? BGTaskScheduler.shared.submit(req)
        }

        // Auto-discard check — runs as a BGAppRefreshTask (daily cutoff policy check).
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.mileway.autodiscard",
            using: nil
        ) { task in
            IosBgTaskDispatcher.shared.runTask(taskId: task.identifier) { success in
                task.setTaskCompleted(success: success.boolValue)
            }
            // Reschedule next daily check.
            let req = BGAppRefreshTaskRequest(identifier: "com.mileway.autodiscard")
            req.earliestBeginDate = Date(timeIntervalSinceNow: 24 * 60 * 60)
            try? BGTaskScheduler.shared.submit(req)
        }
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        PushBridge.shared.setToken(token: token)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // No-op: push simply stays unavailable (offline-tolerant).
    }

    // Foreground presentation.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    // Notification tap → route the data "path" through the shared DeepLinkRouter.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let path = response.notification.request.content.userInfo["path"] as? String {
            let url = path.contains("://") ? path : "mileway://\(path)"
            DeepLinkBridge.shared.handle(url: url)
        }
        completionHandler()
    }
}
