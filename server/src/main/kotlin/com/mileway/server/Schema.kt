package com.mileway.server

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * PLAN_V33 B2: real domain tables backing the miles-submission routes. `vehicles` is the source of
 * truth for GET /api/vehicles + GET /api/pricing and for the [com.mileway.core.data.ledger.PolicyRateTable]
 * built in [com.mileway.server.Application]. `location_points`/`events` have no routes yet (B3) —
 * the schema exists now so B3 lands with no migration.
 */
object VehiclesTable : Table("vehicles") {
    val vehicleKey = varchar("vehicle_key", 64)
    val vehicleName = varchar("vehicle_name", 128)
    val ratePerKm = double("rate_per_km")
    override val primaryKey = PrimaryKey(vehicleKey)
}

object TripsTable : Table("trips") {
    val id = long("id").autoIncrement()
    val token = varchar("token", 128)
    val vehicleKey = varchar("vehicle_key", 64)
    val distanceKm = double("distance_km")
    val originalDistanceKm = double("original_distance_km")
    val startTime = long("start_time").nullable()
    val endTime = long("end_time").nullable()
    val status = varchar("status", 32)
    override val primaryKey = PrimaryKey(id)
}

object LocationPointsTable : Table("location_points") {
    val id = long("id").autoIncrement()
    val token = varchar("token", 128)
    val lat = double("lat")
    val lng = double("lng")
    val date = long("date")
    val speed = double("speed").nullable()
    val accuracy = double("accuracy").nullable()
    val isMock = bool("is_mock").default(false)
    val isAbnormal = bool("is_abnormal").default(false)
    val provider = varchar("provider", 32).nullable()
    override val primaryKey = PrimaryKey(id)
}

object EventsTable : Table("events") {
    val id = long("id").autoIncrement()
    val token = varchar("token", 128)
    val event = varchar("event", 64)
    val eventType = varchar("event_type", 64).nullable()
    val time = long("time")
    val lat = double("lat").nullable()
    val lng = double("lng").nullable()
    override val primaryKey = PrimaryKey(id)
}

/**
 * The demo vehicle list copied verbatim from `stub/DemoMockData.kt`'s `vehicles()` (key, display
 * name, ₹/km rate) so GET /api/vehicles from the real backend is byte-identical to :stub's fake.
 */
private val seedVehicleRows =
    listOf(
        Triple("fourWheelerPetrol", "Four Wheeler (Petrol)", 10.0),
        Triple("fourWheelerDiesel", "Four Wheeler (Diesel)", 10.0),
        Triple("fourWheelerCng", "Four Wheeler (CNG)", 10.0),
        Triple("twoWheeler", "Two Wheeler", 16.0),
        Triple("autoRicshaw", "Auto Rickshaw", 8.0),
        Triple("electricCar", "Electric Car", 6.0),
        Triple("electricBikeChargedInsideOffice", "Electric Bike (Office)", 4.0),
        Triple("electricBikeChargedOutsideOffice", "Electric Bike (Own)", 4.0),
        Triple("meterTaxi", "Meter Taxi", 0.0),
        Triple("accompaniedVehicle", "Accompanied Vehicle", 0.0),
        Triple("ownVehicle", "Own Vehicle", 0.0),
    )

/** Inserts [seedVehicleRows] once; a no-op on every restart after the first (table already populated). */
fun seedVehicles() {
    transaction {
        if (VehiclesTable.selectAll().count() == 0L) {
            seedVehicleRows.forEach { (key, name, rate) ->
                VehiclesTable.insert {
                    it[vehicleKey] = key
                    it[vehicleName] = name
                    it[ratePerKm] = rate
                }
            }
        }
    }
}
