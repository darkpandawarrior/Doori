package com.miletracker.feature.tracking.debug

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
     */
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
        } catch (e: Exception) {
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }
    }

    fun isDebugRestart(intent: Intent?): Boolean {
        return intent?.getBooleanExtra("debug_restart", false) == true
    }
}
