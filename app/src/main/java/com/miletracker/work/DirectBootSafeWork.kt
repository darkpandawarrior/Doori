package com.miletracker.work

import android.content.Context
import android.os.Build
import android.os.UserManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager

/**
 * Direct-boot-safe WorkManager scheduling (UX.4).
 *
 * WorkManager's database lives in *credential-encrypted* storage, so `WorkManager.getInstance()` is unsafe
 * before the user has unlocked the device after a reboot (direct-boot window). Touching it then throws /
 * corrupts state. This helper guards every enqueue on [isUserUnlocked]; when the device is still locked it
 * returns `false` so the caller defers, the canonical pattern is a manifest receiver listening for
 * `ACTION_LOCKED_BOOT_COMPLETED` that does nothing but register for `ACTION_USER_UNLOCKED`, then re-runs the
 * enqueue once the credential storage is available.
 */
object DirectBootSafeWork {
    /** True on pre-N (no direct boot) or once the user has unlocked credential-encrypted storage. */
    fun isUserUnlocked(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        return userManager?.isUserUnlocked ?: true
    }

    /**
     * Enqueue [request] as unique periodic work, but only if credential storage is available. Returns true
     * when it was enqueued, false when deferred because the device is still in the direct-boot window.
     */
    fun enqueueUniquePeriodicWhenUnlocked(
        context: Context,
        uniqueName: String,
        policy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Boolean {
        if (!isUserUnlocked(context)) return false
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(uniqueName, policy, request)
        return true
    }
}
