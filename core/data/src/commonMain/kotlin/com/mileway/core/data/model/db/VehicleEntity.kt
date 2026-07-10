package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P11.2: one vehicle in the garage. Multi-row table (unlike the single-row
 * [VehicleDetailsEntity] linked-vehicle store, which P6.2 shipped) — the garage owns 0..N vehicles
 * with one [isActive] at a time. Modelled on the reference app's driver-vehicle record
 * (brand/model/number/seats/doors/type + an active-vehicle pointer) and its per-vehicle
 * service-set + availability-window shapes, rebuilt as local Room data.
 *
 * [vehicleTypeKey] ties a garage vehicle to the P11.1 [com.mileway.core.data.vehicle.VehicleCatalog]
 * (drives the per-km rate + the tracking vehicle default). [servicesCsv] is the comma-separated set
 * of trip-purpose keys this vehicle may be used for. The `availability*` columns are the gig-driver
 * P2P availability window (start/end minute-of-day + ₹/hour); -1 means "not set".
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: String,
    val brand: String,
    val model: String,
    val registrationNumber: String,
    val year: Int,
    val color: String,
    val seats: Int,
    val vehicleTypeKey: String,
    val photoUri: String = "",
    val isActive: Boolean = false,
    val servicesCsv: String = "",
    val availabilityStartMinute: Int = -1,
    val availabilityEndMinute: Int = -1,
    val availabilityRatePerHour: Double = -1.0,
    val createdAtMs: Long = 0L,
)
