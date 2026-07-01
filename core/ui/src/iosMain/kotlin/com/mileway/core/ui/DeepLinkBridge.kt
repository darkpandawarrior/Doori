package com.mileway.core.ui

import com.mileway.core.platform.DeepLinkHandler
import com.mileway.core.platform.DefaultDeepLinkHandler

/**
 * DL.3: Swift → KMP deep-link bridge, exported in the Mileway framework.
 *
 * `iOSApp.swift` calls `DeepLinkBridge.shared.handle(url:)` from `.onOpenURL` and the
 * `NSUserActivityTypeBrowsingWeb` continuation; the shared Compose nav (DL.4) observes [handler]'s
 * `incoming` flow and routes each URI through the shared `DeepLinkRouter`.
 */
object DeepLinkBridge {
    val handler: DeepLinkHandler = DefaultDeepLinkHandler()

    fun handle(url: String) {
        handler.handle(url)
    }
}
