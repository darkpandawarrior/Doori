package com.mileway.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

class IosSystemSettingsOpener : SystemSettingsOpener {
    override fun openAppSettings() {
        // iOS exposes exactly one settings destination to an app: its own page. There is no
        // battery-optimisation equivalent, so the interface default correctly routes here too.
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) app.openURL(url, emptyMap<Any?, Any>(), null)
    }
}
