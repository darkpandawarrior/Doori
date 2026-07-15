package com.mileway.feature.tracking.map

import com.siddharth.kmp.appshell.PlaceName

/**
 * Pure helpers behind the live-map address chip and heading indicator (Wave 3 live-map polish).
 * No Compose/platform imports — kept separate from [MapRouteBuilder] since these operate on a
 * single current fix rather than the whole route.
 */
object LiveMapOverlayData {
    /** Chip text for [place], or null when there is nothing worth showing (screen renders nothing). */
    fun addressChipText(place: PlaceName?): String? = place?.displayLabel?.takeIf { it.isNotBlank() }

    /**
     * Normalizes a raw GPS/device [bearingDegrees] (any real value, including negative or > 360
     * from sensor noise) to a `[0, 360)` rotation angle for the heading-arrow indicator.
     */
    fun headingRotationDegrees(bearingDegrees: Float): Float {
        val wrapped = bearingDegrees % 360f
        return if (wrapped < 0f) wrapped + 360f else wrapped
    }
}
