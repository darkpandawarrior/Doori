package com.mileway.core.platform

/**
 * OEM battery-optimization onboarding hint (Wave 3): several Android manufacturers kill background
 * location with their own aggressive battery managers on top of stock Doze, so the permission-onboarding
 * flow surfaces manufacturer-specific guidance after the background-location tier. Pure data — the mapping
 * itself needs no platform API and is fully unit-testable; only reading *which* manufacturer the device
 * reports is platform-specific (see [currentDeviceManufacturer]).
 */
object OemBatteryHints {
    private val hints =
        mapOf(
            "xiaomi" to "Xiaomi may stop background tracking. Enable Autostart for Doori in Settings > Apps > Permissions > Autostart.",
            "redmi" to "Xiaomi may stop background tracking. Enable Autostart for Doori in Settings > Apps > Permissions > Autostart.",
            "poco" to "Xiaomi may stop background tracking. Enable Autostart for Doori in Settings > Apps > Permissions > Autostart.",
            "samsung" to "Samsung may put Doori to sleep. Turn off 'Put unused apps to sleep' and disable battery optimization for Doori.",
            "oneplus" to "OnePlus may restrict background activity. Set Doori's battery usage to 'Don't optimize' in Settings > Battery.",
            "oppo" to "Oppo/ColorOS may restrict background activity. Allow Doori to run in the background in Settings > Battery > App battery management.",
            "vivo" to "Vivo/FuntouchOS may restrict background activity. Add Doori to the background app whitelist in iManager.",
            "huawei" to "Huawei's Protected Apps list can stop background tracking. Add Doori to Protected Apps in Battery settings.",
        )

    /** Hint copy for [manufacturer], or null when there's no known OEM-specific guidance (stock/unknown). */
    fun hintFor(manufacturer: String): String? = hints[manufacturer.trim().lowercase()]
}

/** Reads the device's manufacturer for [OemBatteryHints]. Android: `Build.MANUFACTURER`; iOS: always null (no OEM skinning). */
expect fun currentDeviceManufacturer(): String?
