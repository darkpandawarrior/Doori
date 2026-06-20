package com.miletracker.core.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android haptics (UX.2) via the system Vibrator. Maps each [HapticEffect] to a short one-shot vibration
 * (default amplitude on API 26+). Resolves the vibrator through VibratorManager on API 31+, falling back to
 * the deprecated VIBRATOR_SERVICE below it. Requires the (normal, no-prompt) VIBRATE permission.
 */
class AndroidHaptics(private val context: Context) : Haptics {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun perform(effect: HapticEffect) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val ms =
            when (effect) {
                HapticEffect.LIGHT -> 15L
                HapticEffect.MEDIUM -> 30L
                HapticEffect.HEAVY -> 50L
                HapticEffect.SUCCESS -> 20L
                HapticEffect.WARNING -> 35L
                HapticEffect.ERROR -> 60L
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }
}
