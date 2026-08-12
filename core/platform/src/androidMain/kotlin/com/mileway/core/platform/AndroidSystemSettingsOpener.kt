package com.mileway.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AndroidSystemSettingsOpener(private val context: Context) : SystemSettingsOpener {
    override fun openAppSettings() {
        // ACTION_APPLICATION_DETAILS_SETTINGS with a package: URI — not ACTION_VIEW, which cannot
        // resolve this and is why UrlOpener could not be reused here.
        start(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    override fun openBatteryOptimisationSettings() {
        // The per-app exemption dialog needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which is
        // Play-policy sensitive. The battery-settings LIST needs no permission and lands the user
        // one tap away, so it is the honest default: it always resolves, and it never asks the app
        // to hold a permission it may not be allowed to justify.
        if (!start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            openAppSettings()
        }
    }

    /** Returns whether the intent actually resolved, so callers can fall back rather than no-op. */
    private fun start(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
