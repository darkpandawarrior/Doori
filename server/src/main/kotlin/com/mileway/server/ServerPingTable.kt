package com.mileway.server

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * ponytail: one throwaway table proving the Exposed wiring end-to-end (connect + schema create +
 * a query from [Application.module]'s `/health` handler). No real domain table yet — that lands
 * with the miles/expense routes in a later PLAN_V33 task.
 */
object ServerPingTable : Table("server_ping") {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
}

/**
 * Connects Exposed to a JDBC database configured entirely from the environment, defaulting to an
 * in-memory H2 instance so local runs and tests need no external database. Point JDBC_URL (plus
 * JDBC_DRIVER/JDBC_USER/JDBC_PASSWORD) at Postgres — see docker-compose.yml — for a real deploy.
 * No URL/credentials are ever hardcoded here beyond the H2 dev default.
 */
fun connectDatabase() {
    Database.connect(
        // PLAN_V33 B3: MODE=MySQL is required for Exposed's insertIgnore on H2 (the dialect throws
        // UnsupportedByDialectException without it — see LocationEventRoutes.kt's dedup insert).
        // A real deploy points JDBC_URL at Postgres (see docker-compose.yml), which supports
        // ON CONFLICT DO NOTHING natively and never hits this default.
        url = System.getenv("JDBC_URL") ?: "jdbc:h2:mem:mileway;MODE=MySQL;DB_CLOSE_DELAY=-1",
        driver = System.getenv("JDBC_DRIVER") ?: "org.h2.Driver",
        user = System.getenv("JDBC_USER") ?: "sa",
        password = System.getenv("JDBC_PASSWORD") ?: "",
    )
    transaction {
        SchemaUtils.create(
            ServerPingTable,
            VehiclesTable,
            TripsTable,
            LocationPointsTable,
            EventsTable,
            CheckInsTable,
            GeoTypesTable,
            LogMilesServicesTable,
            TaggedExpensesTable,
        )
    }
    seedVehicles()
    seedGeoTypes()
    seedLogMilesServices()
    seedTaggedExpenses()
}
