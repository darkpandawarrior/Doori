import UIKit
import UserNotifications
import MileTracker

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
        // RF.3: capture a deferred referral left on the pasteboard at first launch.
        ReferralBridge.shared.captureDeferred()
        return true
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
            let url = path.contains("://") ? path : "miletracker://\(path)"
            DeepLinkBridge.shared.handle(url: url)
        }
        completionHandler()
    }
}
