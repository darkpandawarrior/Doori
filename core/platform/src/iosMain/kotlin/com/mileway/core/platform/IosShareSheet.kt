package com.mileway.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * iOS share via UIActivityViewController (SH.1), the counterpart to Android's ACTION_SEND chooser. Builds
 * the activity items (text + an optional file URL) and presents the controller from the top-most view
 * controller. Compiles + links against the simulator framework.
 */
class IosShareSheet : ShareSheet {
    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) {
        val items = mutableListOf<Any>(text)
        fileUri?.let { NSURL.URLWithString(it)?.let { url -> items.add(url) } }
        val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }
}
