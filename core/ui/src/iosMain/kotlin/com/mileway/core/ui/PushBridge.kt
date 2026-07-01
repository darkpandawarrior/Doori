package com.mileway.core.ui

import com.mileway.core.platform.InMemoryPushTokenStore
import com.mileway.core.platform.LocalPushMessaging
import com.mileway.core.platform.PushMessaging
import com.mileway.core.platform.PushTokenStore

/**
 * FCM.4: Swift → KMP push bridge, exported in the Mileway framework.
 *
 * `AppDelegate.swift` forwards the APNs/FCM token (from `didRegisterForRemoteNotificationsWithDeviceToken`,
 * or `Messaging.messaging().fcmToken` once the Firebase SPM package is added) to [setToken]; shared code
 * reads it through [messaging]/[tokenStore]. Notification taps are routed via [DeepLinkBridge].
 */
object PushBridge {
    val tokenStore: PushTokenStore = InMemoryPushTokenStore()
    val messaging: PushMessaging = LocalPushMessaging(tokenStore)

    fun setToken(token: String) {
        tokenStore.setToken(token)
    }
}
