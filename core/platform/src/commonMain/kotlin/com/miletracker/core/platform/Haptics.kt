package com.miletracker.core.platform

/** Haptic feedback intensities / semantic cues (UX.2). */
enum class HapticEffect { LIGHT, MEDIUM, HEAVY, SUCCESS, WARNING, ERROR }

/**
 * Platform haptic feedback (UX.2). Android: Vibrator / VibrationEffect; iOS: UIFeedbackGenerator
 * (impact + notification). Bound per platform through `platformModule()`, the same interface-plus-actual-
 * binding pattern the other context-backed services use (a bare expect/actual fun can't reach the Android
 * Context / Vibrator cleanly).
 */
interface Haptics {
    fun perform(effect: HapticEffect)
}
