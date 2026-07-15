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

    // PLAN_V33 B3: client-generated idempotency key. Nullable (rows without one always insert) but
    // unique when present, so `insertIgnore` makes a replayed POST of the same opId a no-op.
    val opId = varchar("op_id", 64).nullable().uniqueIndex()
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
    val opId = varchar("op_id", 64).nullable().uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

// PLAN_V33 B4: check-in + log-miles + expense-tagging tables. checkins is what
// POST /api/checkin persists (no token in CheckInRequestV2 — AUTH-DEFERRED, so rows aren't
// attributed to a user yet); geo_types/log_miles_services/tagged_expenses are read-mostly
// reference data seeded once below.
object CheckInsTable : Table("checkins") {
    val id = long("id").autoIncrement()
    val lat = double("lat")
    val lng = double("lng")
    val typeId = long("type_id").nullable()
    val time = long("time")
    override val primaryKey = PrimaryKey(id)
}

/** Backs GET /api/checkin/types(/{id}) — mirrors [com.mileway.core.data.model.network.CheckInDetailsResponseV2]. */
object GeoTypesTable : Table("geo_types") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val type = varchar("type", 64)
    val lat = double("lat")
    val lng = double("lng")
    val radius = double("radius")
    override val primaryKey = PrimaryKey(id)
}

/** Backs GET /api/log-miles/services — mirrors [com.mileway.core.data.model.network.LogMilesServiceDto]. */
object LogMilesServicesTable : Table("log_miles_services") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val glCode = varchar("gl_code", 64)
    override val primaryKey = PrimaryKey(id)
}

/** Backs GET /api/expenses/{tagged,pending} — mirrors [com.mileway.core.data.model.network.TaggedExpenseItem]. */
object TaggedExpensesTable : Table("tagged_expenses") {
    val id = long("id").autoIncrement()
    val title = varchar("title", 128)
    val amount = double("amount")
    val submittedAt = long("submitted_at")
    val pending = bool("pending").default(false)
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

/**
 * Geo-fence check-in locations, copied verbatim from `stub/DemoMockData.kt`'s
 * `checkInLocations()` (name, type, lat/lng, per-location radius override — the 100 m default
 * from that file's own doc comment when a location doesn't override it).
 */
private data class SeedGeoType(
    val name: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double,
)

private const val DEFAULT_CHECKIN_RADIUS_METERS = 100.0

private val seedGeoTypeRows =
    listOf(
        SeedGeoType("Head Office", "OFFICE", 18.5204, 73.8567, DEFAULT_CHECKIN_RADIUS_METERS),
        SeedGeoType("Warehouse / Supply Center", "SUPPLY_CENTER", 18.5480, 73.8718, 150.0),
        SeedGeoType("North Job Site", "JOB_SITE", 18.5601, 73.8234, 200.0),
        SeedGeoType("East Distribution Hub", "DISTRIBUTION_HUB", 18.5120, 73.9012, 120.0),
        SeedGeoType("South Service Point", "SERVICE_POINT", 18.4890, 73.8350, 80.0),
    )

/** Inserts [seedGeoTypeRows] once. */
fun seedGeoTypes() {
    transaction {
        if (GeoTypesTable.selectAll().count() == 0L) {
            seedGeoTypeRows.forEach { row ->
                GeoTypesTable.insert {
                    it[name] = row.name
                    it[type] = row.type
                    it[lat] = row.lat
                    it[lng] = row.lng
                    it[radius] = row.radiusMeters
                }
            }
        }
    }
}

/** Copied verbatim from `stub/DemoMockData.kt`'s `logMilesServices()` (id, name, GL code). */
private val seedLogMilesServiceRows =
    listOf(
        Triple(1L, "Own Car", "CONV-001"),
        Triple(2L, "Company Car", "CONV-002"),
        Triple(3L, "Taxi / Cab", "CONV-003"),
        Triple(4L, "Public Transport", "CONV-004"),
        Triple(5L, "Auto Rickshaw", "CONV-005"),
        Triple(6L, "Two Wheeler", "CONV-006"),
    )

/** Inserts [seedLogMilesServiceRows] once. */
fun seedLogMilesServices() {
    transaction {
        if (LogMilesServicesTable.selectAll().count() == 0L) {
            seedLogMilesServiceRows.forEach { (rowId, name, glCode) ->
                LogMilesServicesTable.insert {
                    it[id] = rowId
                    it[LogMilesServicesTable.name] = name
                    it[LogMilesServicesTable.glCode] = glCode
                }
            }
        }
    }
}

// ponytail: stub/DemoMockData.kt has no tagged-expense fixture to mirror (unlike vehicles/
// check-ins/log-miles-services above) — these two rows are a small sensible fixture invented for
// B4 so GET /api/expenses/{tagged,pending} has something real to return. Replace with real data
// once expense tagging has its own submission flow.
private data class SeedTaggedExpense(
    val title: String,
    val amount: Double,
    val submittedAt: Long,
    val pending: Boolean,
)

private val seedTaggedExpenseRows =
    listOf(
        SeedTaggedExpense("Client Lunch", 450.0, 1_700_000_000_000L, pending = false),
        SeedTaggedExpense("Taxi Fare", 220.0, 1_700_000_600_000L, pending = true),
    )

/** Inserts [seedTaggedExpenseRows] once. */
fun seedTaggedExpenses() {
    transaction {
        if (TaggedExpensesTable.selectAll().count() == 0L) {
            seedTaggedExpenseRows.forEach { row ->
                TaggedExpensesTable.insert {
                    it[title] = row.title
                    it[amount] = row.amount
                    it[submittedAt] = row.submittedAt
                    it[pending] = row.pending
                }
            }
        }
    }
}
