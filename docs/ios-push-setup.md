# iOS push (APNs + Firebase) setup — FCM.4

The Swift + KMP wiring is in place; the only manual step is adding the Firebase SPM package to the Xcode
project (cannot be scripted safely from the repo).

## 1. Add the Firebase SPM package
In Xcode: **File → Add Package Dependencies…** → `https://github.com/firebase/firebase-ios-sdk` →
add the **FirebaseMessaging** product to the `iosApp` target.

## 2. Drop in the real config
Replace `iosApp/iosApp/GoogleService-Info.plist` (a placeholder) with the real file, or materialize it in CI
from `GOOGLE_SERVICES_IOS_B64` (see `docs/RELEASE.md`).

## 3. Switch APNs → FCM token (optional, once Firebase is linked)
In `AppDelegate.swift`:
- `import FirebaseCore` + `FirebaseApp.configure()` in `didFinishLaunchingWithOptions`.
- `import FirebaseMessaging`, set `Messaging.messaging().apnsToken = deviceToken`, and forward
  `Messaging.messaging().fcmToken` (via `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)`) to
  `PushBridge.shared.setToken(token:)` instead of the raw APNs hex.

## Already wired (no action needed)
- `AppDelegate` registers for remote notifications, forwards the token to `PushBridge`, and routes
  notification taps (`userInfo["path"]`) through `DeepLinkBridge` → shared `DeepLinkRouter`.
- `iosApp.entitlements`: `aps-environment` (set `production` for App Store).
- `Info.plist`: `remote-notification` background mode.
- KMP: `PushBridge` (exported in the MileTracker framework) backs `PushTokenStore` / `PushMessaging`.
