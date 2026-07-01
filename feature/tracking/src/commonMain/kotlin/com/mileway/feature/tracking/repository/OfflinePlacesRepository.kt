package com.mileway.feature.tracking.repository

/**
 * Returns human-readable demo address strings keyed deterministically on lat/lng.
 * No network calls, all strings are baked in so the demo works fully offline.
 */
object OfflinePlacesRepository {
    private val PUNE_ADDRESSES =
        listOf(
            "Baner Road, Pune, Maharashtra 411045",
            "Koregaon Park, Pune, Maharashtra 411001",
            "Shivajinagar, Pune, Maharashtra 411005",
            "Hadapsar, Pune, Maharashtra 411028",
            "Viman Nagar, Pune, Maharashtra 411014",
            "Hinjewadi IT Park, Pune, Maharashtra 411057",
            "Wakad, Pune, Maharashtra 411057",
            "Kothrud, Pune, Maharashtra 411038",
            "Aundh, Pune, Maharashtra 411007",
            "Kharadi, Pune, Maharashtra 411014",
            "Magarpatta City, Hadapsar, Pune 411013",
            "Deccan Gymkhana, Pune, Maharashtra 411004",
            "Swargate, Pune, Maharashtra 411037",
            "Pimpri-Chinchwad, Maharashtra 411018",
            "Yerwada, Pune, Maharashtra 411006",
        )

    /**
     * Returns a demo address for the given coordinates.
     * The same lat/lng always returns the same address (deterministic).
     */
    fun addressFor(
        lat: Double,
        lng: Double,
    ): String {
        val fingerprint = kotlin.math.abs((lat * 100).toInt() * 31 + (lng * 100).toInt())
        return PUNE_ADDRESSES[fingerprint % PUNE_ADDRESSES.size]
    }
}
