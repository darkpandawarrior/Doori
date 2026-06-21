package com.miletracker.feature.logging.ui.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Category for a POI — drives the icon shown in the search result row. */
enum class PoiCategory { OFFICE, CLIENT, RESTAURANT, HOME, TRANSIT, LANDMARK, OTHER }

/**
 * A single searchable place the user can pick as a travelled stop.
 *
 * The demo ships an offline catalogue of Indian cities/landmarks (see
 * [CityCatalog]) so that location search works with no network and no map
 * provider. Each entry carries real-ish coordinates so the great-circle
 * distance between two stops is plausible.
 */
data class LocationEntry(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val category: PoiCategory = PoiCategory.OTHER,
)

/**
 * An ordered stop in the journey itinerary. [order] is the 1-based position the
 * user sees; [entry] is the underlying place. Stops are kept in a list inside the
 * UI state so reordering, insertion and removal are simple list operations.
 */
data class LocationStop(
    val id: Long,
    val entry: LocationEntry,
)

/** Offline catalogue of 30+ POIs (Pune-focused) used by the location-search sheet. */
object CityCatalog {
    val all: List<LocationEntry> =
        listOf(
            // ── Pune landmarks & transit ──────────────────────────────────────────────
            LocationEntry("Pune Railway Station", "Agarkar Nagar, Pune 411001", 18.5286, 73.8743, PoiCategory.TRANSIT),
            LocationEntry("Pune International Airport (PNQ)", "Lohegaon, Pune 411032", 18.5793, 73.9089, PoiCategory.TRANSIT),
            LocationEntry("Pune Bus Stand (Swargate)", "Swargate, Pune 411037", 18.4978, 73.8587, PoiCategory.TRANSIT),
            LocationEntry("Shivajinagar Metro Station", "Shivajinagar, Pune 411005", 18.5300, 73.8450, PoiCategory.TRANSIT),
            // ── Pune offices / IT parks ───────────────────────────────────────────────
            LocationEntry("Hinjewadi IT Park Phase 1", "Hinjewadi, Pune 411057", 18.5912, 73.7389, PoiCategory.OFFICE),
            LocationEntry("Hinjewadi IT Park Phase 2", "Hinjewadi, Pune 411057", 18.5970, 73.7320, PoiCategory.OFFICE),
            LocationEntry("Kharadi IT Park (EON)", "Kharadi, Pune 411014", 18.5514, 73.9384, PoiCategory.OFFICE),
            LocationEntry("Magarpatta Cybercity", "Hadapsar, Pune 411013", 18.5089, 73.9260, PoiCategory.OFFICE),
            LocationEntry("Viman Nagar Business Hub", "Viman Nagar, Pune 411014", 18.5670, 73.9110, PoiCategory.OFFICE),
            LocationEntry("Baner Road Office Complex", "Baner, Pune 411045", 18.5590, 73.7850, PoiCategory.OFFICE),
            // ── Client sites ─────────────────────────────────────────────────────────
            LocationEntry("Speedline Transport Co.", "Koregaon Park, Pune 411001", 18.5364, 73.8933, PoiCategory.CLIENT),
            LocationEntry("Metro Cargo Movers", "Pimpri-Chinchwad 411018", 18.6298, 73.8000, PoiCategory.CLIENT),
            LocationEntry("CityLink Telecom Services", "Kothrud, Pune 411038", 18.5074, 73.8078, PoiCategory.CLIENT),
            LocationEntry("Eastern Freight Lines", "Hadapsar, Pune 411028", 18.5020, 73.9350, PoiCategory.CLIENT),
            LocationEntry("Westgate Fleet Services", "Baner, Pune 411045", 18.5610, 73.7800, PoiCategory.CLIENT),
            // ── Home / residential ───────────────────────────────────────────────────
            LocationEntry("Aundh Residential Colony", "Aundh, Pune 411007", 18.5590, 73.8140, PoiCategory.HOME),
            LocationEntry("Wakad Housing Society", "Wakad, Pune 411057", 18.5983, 73.7640, PoiCategory.HOME),
            LocationEntry("Kharadi Housing Complex", "Kharadi, Pune 411014", 18.5490, 73.9430, PoiCategory.HOME),
            // ── Restaurants / food ───────────────────────────────────────────────────
            LocationEntry("Cafe Goodluck", "Deccan Gymkhana, Pune 411004", 18.5165, 73.8497, PoiCategory.RESTAURANT),
            LocationEntry("Vohuman Cafe", "Sassoon Road, Pune 411001", 18.5218, 73.8752, PoiCategory.RESTAURANT),
            LocationEntry("Malaka Spice", "Koregaon Park, Pune 411001", 18.5390, 73.8962, PoiCategory.RESTAURANT),
            // ── Other cities ─────────────────────────────────────────────────────────
            LocationEntry("Mumbai Central Station", "Mumbai 400008", 18.9696, 72.8195, PoiCategory.TRANSIT),
            LocationEntry("Nashik Road Railway Station", "Nashik 422101", 19.9975, 73.7898, PoiCategory.TRANSIT),
            LocationEntry("Bengaluru Airport (BLR)", "Devanahalli, Bengaluru 562300", 13.1986, 77.7066, PoiCategory.TRANSIT),
            LocationEntry("Hyderabad Airport (HYD)", "Shamshabad, Hyderabad 501218", 17.2403, 78.4294, PoiCategory.TRANSIT),
            LocationEntry("Mumbai, Maharashtra", "Maharashtra, India", 19.0760, 72.8777, PoiCategory.OTHER),
            LocationEntry("Nashik, Maharashtra", "Maharashtra, India", 19.9975, 73.7898, PoiCategory.OTHER),
            LocationEntry("Nagpur, Maharashtra", "Maharashtra, India", 21.1458, 79.0882, PoiCategory.OTHER),
            LocationEntry("Bengaluru, Karnataka", "Karnataka, India", 12.9716, 77.5946, PoiCategory.OTHER),
            LocationEntry("New Delhi", "Delhi, India", 28.6139, 77.2090, PoiCategory.OTHER),
        )

    /** Current-location placeholder — the "Your Location" shortcut in the sheet. */
    val currentLocation =
        LocationEntry(
            name = "Your current location",
            subtitle = "Baner Road, Pune 411045",
            lat = 18.5590,
            lng = 73.7850,
            category = PoiCategory.HOME,
        )

    /** Case-insensitive substring search over name, subtitle, and category. */
    fun search(query: String): List<LocationEntry> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return all.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.category.name.contains(q, ignoreCase = true)
        }
    }
}

/**
 * Great-circle (haversine) distance in kilometres between two coordinates.
 * Used as the default "Calculated" distance before the user verifies/overrides it.
 */
fun haversineKm(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val earthRadiusKm = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

/**
 * Total great-circle distance across an ordered list of stops (sum of consecutive
 * leg distances). Round trips add the return leg back to the first stop.
 */
fun totalRouteKm(
    stops: List<LocationStop>,
    roundTrip: Boolean,
): Double {
    if (stops.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until stops.size - 1) {
        val a = stops[i].entry
        val b = stops[i + 1].entry
        total += haversineKm(a.lat, a.lng, b.lat, b.lng)
    }
    if (roundTrip) {
        val first = stops.first().entry
        val last = stops.last().entry
        total += haversineKm(last.lat, last.lng, first.lat, first.lng)
    }
    return total
}
