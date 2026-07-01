package com.mileway.core.platform

import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

/**
 * iOS in-app review (full parity with the Android Play-review path). Uses the iOS 14+ window-scene variant
 * of [SKStoreReviewController]; if no foreground-active window scene is found it simply does nothing
 * (matches the "host may decline" semantics of the Android impl). Never crashes.
 */
class IosAppReviewManager : AppReviewManager {
    override suspend fun promptForReview() {
        val scene =
            UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        if (scene != null) {
            SKStoreReviewController.requestReviewInScene(scene)
        }
    }
}
