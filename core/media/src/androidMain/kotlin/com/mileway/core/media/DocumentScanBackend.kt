package com.mileway.core.media

import android.app.Activity
import android.content.Intent
import android.content.IntentSender

/** Flavor-bound scanner; FOSS builds use the existing system document picker. */
interface DocumentScanBackend {
    fun start(
        activity: Activity,
        pageLimit: Int,
        onReady: (IntentSender) -> Unit,
        onUnavailable: () -> Unit,
    )

    fun pages(intent: Intent?): List<String>
}
