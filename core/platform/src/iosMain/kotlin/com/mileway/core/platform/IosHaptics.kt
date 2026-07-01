package com.mileway.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/**
 * iOS haptics (UX.2) via UIFeedbackGenerator, impact styles for LIGHT/MEDIUM/HEAVY and notification types
 * for SUCCESS/WARNING/ERROR (the Taptic Engine's semantic cues). Generators are prepared before firing for
 * lower latency. Compiles + links against the simulator framework.
 */
class IosHaptics : Haptics {
    override fun perform(effect: HapticEffect) {
        when (effect) {
            HapticEffect.LIGHT -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
            HapticEffect.MEDIUM -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
            HapticEffect.HEAVY -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
            HapticEffect.SUCCESS -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            HapticEffect.WARNING -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            HapticEffect.ERROR -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeError)
        }
    }

    private fun impact(style: UIImpactFeedbackStyle) {
        val generator = UIImpactFeedbackGenerator(style)
        generator.prepare()
        generator.impactOccurred()
    }

    private fun notify(type: UINotificationFeedbackType) {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(type)
    }
}
