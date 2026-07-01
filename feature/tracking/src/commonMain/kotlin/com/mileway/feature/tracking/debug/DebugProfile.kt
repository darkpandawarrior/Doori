package com.mileway.feature.tracking.debug

/**
 * Represents a debug configuration profile that can be applied for testing purposes.
 */
data class DebugProfile(
    val name: String,
    val description: String,
    val options: Map<String, Boolean>,
    val customValues: Map<String, String> = mapOf(),
)

/**
 * Repository of predefined debug profiles for testing different configurations
 */
object DebugProfiles {
    val DEVELOPMENT =
        DebugProfile(
            name = "Development",
            description = "Development environment with UAT and logging enabled",
            options =
                mapOf(
                    "Force UAT" to true,
                    "Enable Logging For Release" to true,
                    "Skip OTP Pin Screen" to true,
                    "Allow Mock Locations" to true,
                    "Force Track Miles V2 UI" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "Development Mode Active",
                ),
        )

    val QA_TESTING =
        DebugProfile(
            name = "QA Testing",
            description = "Testing configuration with mock locations and UAT",
            options =
                mapOf(
                    "Force UAT" to true,
                    "Allow Mock Locations" to true,
                    "Skip OTP Pin Screen" to true,
                    "Enable Location Dump Creation" to true,
                    "Force Track Miles V2 UI" to true,
                    "Enable Logging For Release" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "QA Testing Mode",
                ),
        )

    val PERFORMANCE_TESTING =
        DebugProfile(
            name = "Performance Testing",
            description = "Configuration for performance testing with minimal overhead",
            options =
                mapOf(
                    "Force Prod" to true,
                    "Force Track Miles V2 UI" to true,
                    "Bypass Battery Level Check" to true,
                    "Bypass Battery Optimization Check" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "Performance Testing Mode",
                ),
        )

    val TRACK_MILES_DEBUGGING =
        DebugProfile(
            name = "Track Miles Debugging",
            description = "Configuration for debugging track miles functionality",
            options =
                mapOf(
                    "Force UAT" to true,
                    "Enable Logging For Release" to true,
                    "Enable Location Dump Creation" to true,
                    "Force Track Miles V2 UI" to true,
                    "Enable Tracking Overlay" to true,
                    "Allow Mock Locations" to true,
                    "Bypass Battery Level Check" to true,
                    "Bypass Battery Optimization Check" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "Track Miles Debug Mode",
                ),
        )

    val MENA_TESTING =
        DebugProfile(
            name = "MENA Testing",
            description = "Configuration for Middle East/North Africa testing",
            options =
                mapOf(
                    "Force MENA Login" to true,
                    "Force UAT" to true,
                    "Skip OTP Pin Screen" to true,
                    "Enable Logging For Release" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "MENA Testing Mode",
                ),
        )

    val PRODUCTION_VALIDATION =
        DebugProfile(
            name = "Production Validation",
            description = "Production environment with minimal debug features",
            options =
                mapOf(
                    "Force Prod" to true,
                    "Force Track Miles V2 UI" to true,
                    "Enable Custom Banner" to true,
                ),
            customValues =
                mapOf(
                    "Custom Banner" to "Production Validation Mode",
                ),
        )

    // Provide a list of all available profiles for UI display
    val allProfiles =
        listOf(
            DEVELOPMENT,
            QA_TESTING,
            PERFORMANCE_TESTING,
            TRACK_MILES_DEBUGGING,
            MENA_TESTING,
            PRODUCTION_VALIDATION,
        )
}
