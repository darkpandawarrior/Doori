package com.mileway.feature.tracking.debug

/**
 * Utility class for debug menu operations that need to be shared between implementations
 */
object DebugMenuUtils {
    /** Origin-override codes, as returned by [determineInitialOriginOverride]. */
    private const val ORIGIN_NONE = 0
    private const val ORIGIN_UAT = 1
    private const val ORIGIN_PROD = 2
    private const val ORIGIN_CUSTOM = 3
    private const val ORIGIN_DEV = 4

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
            debugSettings["Force UAT"] == true -> ORIGIN_UAT
            debugSettings["Force Prod"] == true -> ORIGIN_PROD
            debugSettings["Force Custom Origin"] == true -> ORIGIN_CUSTOM
            debugSettings["Force Dev Environment"] == true -> ORIGIN_DEV
            else -> ORIGIN_NONE
        }
}
