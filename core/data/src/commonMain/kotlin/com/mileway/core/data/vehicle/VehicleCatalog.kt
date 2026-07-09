package com.mileway.core.data.vehicle

/**
 * PLAN_V24 P11.1 — the canonical vehicle catalog: the single commonMain source of truth for which
 * vehicle keys exist, their default per-km policy rate (₹/km), and an abstract icon-family key so
 * the render layer can map key → icon without this data-layer type depending on Compose. Modelled
 * on the reference app's static vehicle-type table (key → rate), rebuilt here as plain KMP data.
 *
 * Rates are the DEFAULT/policy layer; the actual rate a persona sees is gated by
 * [com.mileway.core.data.vehicle.VehicleRateRepository] (per-km rates OFF ⇒ no rate at all).
 */
object VehicleCatalog {
    /** One vehicle type: gating key, default policy rate (₹/km), and an abstract icon family. */
    data class Entry(
        val key: String,
        val ratePerKm: Double,
        val iconKey: String,
    )

    // Icon families (abstract; the Compose layer maps these to ImageVectors).
    const val ICON_BIKE = "bike"
    const val ICON_CAR = "car"
    const val ICON_AUTO = "auto"
    const val ICON_TAXI = "taxi"
    const val ICON_BUS = "bus"

    /**
     * Default policy rates. Keys match the seeded vehicle list so a lookup by
     * [com.mileway.core.data.model.network.ApprovedVehicle.vehicleKey] resolves directly. Zero-rate
     * keys (own/accompanied) are non-reimbursable by policy.
     */
    val entries: List<Entry> =
        listOf(
            Entry("twoWheeler", 2.50, ICON_BIKE),
            Entry("fourWheelerPetrol", 5.00, ICON_CAR),
            Entry("fourWheelerDiesel", 5.00, ICON_CAR),
            Entry("fourWheelerCng", 5.00, ICON_CAR),
            Entry("electricBike", 2.50, ICON_BIKE),
            Entry("electricBikeChargedInsideOffice", 2.50, ICON_BIKE),
            Entry("electricBikeChargedOutsideOffice", 2.50, ICON_BIKE),
            Entry("autoRicshaw", 8.00, ICON_AUTO),
            Entry("electricCar", 5.00, ICON_CAR),
            Entry("hybridCar", 5.00, ICON_CAR),
            Entry("meterTaxi", 8.00, ICON_TAXI),
            Entry("bus", 10.00, ICON_BUS),
            Entry("ownVehicle", 0.00, ICON_CAR),
            Entry("accompaniedVehicle", 0.00, ICON_CAR),
        )

    private val byKey: Map<String, Entry> = entries.associateBy { it.key }

    /** Default policy rate for [key], or 0.0 for an unknown/non-reimbursable key. */
    fun rateFor(key: String): Double = byKey[key]?.ratePerKm ?: 0.0

    /** Abstract icon family for [key], defaulting to [ICON_CAR]. */
    fun iconKeyFor(key: String?): String = key?.let { byKey[it]?.iconKey } ?: ICON_CAR
}

/**
 * PLAN_V24 P11.1 — the reimbursable-amount rule, extracted as a pure function so it is unit-tested
 * once and reused wherever a trip amount is shown (live tracking, submission). Amount = rate ×
 * distance, where distance is the odometer delta when the persona's config computes expenses via
 * odometer AND a positive odometer delta exists, otherwise the GPS distance. Negative inputs are
 * floored to zero so a bad reading can never produce a negative payout.
 */
fun reimbursableAmount(
    ratePerKm: Double,
    gpsDistanceKm: Double,
    odometerDistanceKm: Double = 0.0,
    viaOdometer: Boolean = false,
): Double {
    val distance =
        if (viaOdometer && odometerDistanceKm > 0.0) odometerDistanceKm else gpsDistanceKm
    return ratePerKm.coerceAtLeast(0.0) * distance.coerceAtLeast(0.0)
}
