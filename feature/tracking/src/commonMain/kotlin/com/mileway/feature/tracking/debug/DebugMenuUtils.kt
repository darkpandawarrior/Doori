package com.mileway.feature.tracking.debug

/**
 * Utility class for debug menu operations that need to be shared between implementations
 */
object DebugMenuUtils {
    /**
     * Determine the initial origin override based on debug settings
     * Returns:
     * 0: none
     * 1: UAT
     * 2: Prod
     * 3: Custom
     * 4: Dev
     */
    fun determineInitialOriginOverride(debugSettings: Map<String, Boolean>): Int =
        when {
            debugSettings["Force UAT"] == true -> 1
            debugSettings["Force Prod"] == true -> 2
            debugSettings["Force Custom Origin"] == true -> 3
            debugSettings["Force Dev Environment"] == true -> 4
            else -> 0
        }
}
