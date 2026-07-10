package com.mileway.core.data.vehicle

import com.mileway.core.data.model.db.VehicleEntity

/**
 * PLAN_V24 P11.2: the domain view of a garage vehicle (the decoded [VehicleEntity]). [services] is
 * the set of trip-purpose keys this vehicle may be used for; [availability] is the optional
 * gig-driver P2P availability window.
 */
data class GarageVehicle(
    val id: String,
    val brand: String,
    val model: String,
    val registrationNumber: String,
    val year: Int,
    val color: String,
    val seats: Int,
    val vehicleTypeKey: String,
    val photoUri: String,
    val isActive: Boolean,
    val services: Set<String>,
    val availability: AvailabilityWindow?,
    val createdAtMs: Long,
) {
    val displayName: String get() = "$brand $model".trim()
}

/** A gig-driver availability window: minute-of-day start/end + ₹/hour. */
data class AvailabilityWindow(
    val startMinute: Int,
    val endMinute: Int,
    val ratePerHour: Double,
)

/** Trip-purpose keys a vehicle can be enabled for (the reference service-set checkbox list). */
object VehicleServices {
    const val COMMUTE = "commute"
    const val BUSINESS = "business"
    const val DELIVERY = "delivery"

    /** All known purposes, in display order. */
    val all: List<String> = listOf(COMMUTE, BUSINESS, DELIVERY)
}

internal fun VehicleEntity.toDomain(): GarageVehicle =
    GarageVehicle(
        id = id,
        brand = brand,
        model = model,
        registrationNumber = registrationNumber,
        year = year,
        color = color,
        seats = seats,
        vehicleTypeKey = vehicleTypeKey,
        photoUri = photoUri,
        isActive = isActive,
        services = servicesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        availability =
            if (availabilityStartMinute >= 0 && availabilityEndMinute >= 0) {
                AvailabilityWindow(availabilityStartMinute, availabilityEndMinute, availabilityRatePerHour.coerceAtLeast(0.0))
            } else {
                null
            },
        createdAtMs = createdAtMs,
    )
