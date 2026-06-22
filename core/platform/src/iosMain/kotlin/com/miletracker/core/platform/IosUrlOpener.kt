package com.miletracker.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/** iOS: open a URL via UIApplication.shared.open (the system routes to Safari or the registered handler). */
class IosUrlOpener : UrlOpener {
    override fun open(url: String) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
