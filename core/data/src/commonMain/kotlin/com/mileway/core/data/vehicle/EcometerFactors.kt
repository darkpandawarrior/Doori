package com.mileway.core.data.vehicle

/**
 * PLAN_V24 P11.4 — the seeded per-vehicle-type emission/fuel factor table for the Ecometer, keyed by
 * the same [VehicleCatalog] vehicle keys P11.1 uses. Modelled on the reference app's ecometer
 * (CO₂ / fuel / distance / ride-count), rebuilt as local KMP data computed from real trip distances.
 *
 * [co2GramsPerKm] is tailpipe CO₂ per km; [fuelInrPerKm] is running fuel/energy cost per km. The
 * Ecometer reports **savings vs a petrol-car baseline** ([CAR_BASELINE]) — i.e. how much CO₂ and fuel
 * cost a greener vehicle avoids relative to driving a car for the same distance.
 */
data class EmissionFactor(
    val co2GramsPerKm: Double,
    val fuelInrPerKm: Double,
)

/** The petrol-car baseline every other vehicle is compared against (own/car keys resolve to this). */
val CAR_BASELINE: EmissionFactor = EmissionFactor(co2GramsPerKm = 170.0, fuelInrPerKm = 7.0)

private val ECOMETER_FACTORS: Map<String, EmissionFactor> =
    mapOf(
        "twoWheeler" to EmissionFactor(55.0, 2.5),
        "fourWheelerPetrol" to CAR_BASELINE,
        "fourWheelerDiesel" to EmissionFactor(160.0, 6.5),
        "fourWheelerCng" to EmissionFactor(120.0, 4.5),
        "electricBike" to EmissionFactor(20.0, 0.4),
        "electricBikeChargedInsideOffice" to EmissionFactor(20.0, 0.4),
        "electricBikeChargedOutsideOffice" to EmissionFactor(20.0, 0.4),
        "autoRicshaw" to EmissionFactor(110.0, 4.0),
        "electricCar" to EmissionFactor(50.0, 1.5),
        "hybridCar" to EmissionFactor(110.0, 4.5),
        "meterTaxi" to CAR_BASELINE,
        "bus" to EmissionFactor(80.0, 1.5),
        "ownVehicle" to CAR_BASELINE,
        "accompaniedVehicle" to CAR_BASELINE,
    )

/** Factor for [key]; an unknown key resolves to the car baseline (0 savings — never negative). */
fun emissionFactorFor(key: String): EmissionFactor = ECOMETER_FACTORS[key] ?: CAR_BASELINE

/** One completed trip's contribution to the Ecometer: which vehicle, how far (km). */
data class EcoTrip(
    val vehicleKey: String,
    val distanceKm: Double,
)

/** Aggregated Ecometer totals shown on the dashboard. */
data class EcometerTotals(
    val trips: Int = 0,
    val distanceKm: Double = 0.0,
    val co2SavedKg: Double = 0.0,
    val fuelSavedInr: Double = 0.0,
)

/**
 * PLAN_V24 P11.4 — the pure Ecometer aggregation. For each trip, savings = `(baseline − vehicle) ×
 * km`, floored at 0 so a car trip (or unknown key) contributes zero rather than a negative. CO₂ is
 * converted g → kg. Negative distances are ignored. Unit-tested once; the repository just feeds it
 * real [SavedTrack][com.mileway.core.data.model.db.SavedTrack] distances.
 */
fun computeEcometer(trips: List<EcoTrip>): EcometerTotals {
    var distanceKm = 0.0
    var co2SavedG = 0.0
    var fuelSavedInr = 0.0
    var count = 0
    for (trip in trips) {
        val km = trip.distanceKm
        if (km <= 0.0) continue
        count++
        distanceKm += km
        val factor = emissionFactorFor(trip.vehicleKey)
        co2SavedG += (CAR_BASELINE.co2GramsPerKm - factor.co2GramsPerKm).coerceAtLeast(0.0) * km
        fuelSavedInr += (CAR_BASELINE.fuelInrPerKm - factor.fuelInrPerKm).coerceAtLeast(0.0) * km
    }
    return EcometerTotals(
        trips = count,
        distanceKm = distanceKm,
        co2SavedKg = co2SavedG / 1000.0,
        fuelSavedInr = fuelSavedInr,
    )
}
