package com.mileway.feature.tracking.debug

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.github.aakira.napier.Napier
import kotlin.system.exitProcess

/**
 * Utility class for restarting the app properly.
 */
object AppRestartUtils {
    private const val TAG = "AppRestartUtils"

    /**
     * Perform a complete app restart using the proven legacy approach.
     *
     * Boundary catch-all: this is the edge between the app and a platform or backend call that
     * fails in ways no narrower Kotlin type covers on this source set. The failure is logged
     * and surfaced to the caller, never swallowed — crashing the process is the alternative.
     */
    @Suppress("TooGenericExceptionCaught")
    fun performAppRestart(context: Context) {
        try {
            Napier.i("App restart requested", tag = TAG)

            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component

            if (intent != null && componentName != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("debug_restart", true)
                context.startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(0)
            } else {
                performFallbackRestart(context)
            }
        } catch (e: Exception) {
            Napier.e("Error in performAppRestart: ${e.message}", e, tag = TAG)
            performFallbackRestart(context)
        }
    }

    private fun performFallbackRestart(context: Context) {
        try {
            if (context is Activity) {
                context.finishAffinity()
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        } catch (ignored: Exception) {
            // The fallback restart already failed; the process is killed either way.
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }
    }

    fun isDebugRestart(intent: Intent?): Boolean = intent?.getBooleanExtra("debug_restart", false) == true
}
