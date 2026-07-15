package com.mileway.server

import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.data.model.network.SubmitMilesRequestK
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Mirrors the client's Ktor JSON config exactly (see external/kmp-toolkit
 * network/HttpClientFactory.kt's `networkJson`) so request/response bodies round-trip
 * byte-identically on both sides of the wire.
 */
val serverJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val dbOk: Boolean = true,
)

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(Netty, port = envPort(), module = Application::module).start(wait = true)
}

private fun envPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

fun Application.module() {
    connectDatabase()

    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") { call.respond(healthResponse()) }
        post("/api/echo") { call.respond(call.receive<SubmitMilesRequestK>()) }
        get("/api/vehicles") { call.respond(vehiclesResponse()) }
        get("/api/pricing") { call.respond(ApprovedVehiclePricingResponse(data = loadRateTable().rates)) }
        post("/api/miles/submit") { call.respond(submitMiles(call.receive())) }
        locationEventRoutes()
        milesExtraRoutes()
        checkInRoutes()
    }
}

private fun healthResponse(): HealthResponse {
    val dbOk = runCatching { transaction { ServerPingTable.selectAll().count() } }.isSuccess
    return HealthResponse(dbOk = dbOk)
}

private fun vehiclesResponse(): PolicyApprovedVehiclesResponse {
    val vehicles =
        transaction {
            VehiclesTable.selectAll().map { row ->
                ApprovedVehicle(
                    vehicleKey = row[VehiclesTable.vehicleKey],
                    vehicleName = row[VehiclesTable.vehicleName],
                    vehiclePricing = row[VehiclesTable.ratePerKm],
                )
            }
        }
    return PolicyApprovedVehiclesResponse(vehicles = vehicles)
}

/** Persists the trip and computes the reimbursement via the shared [PolicyRateEngine]. */
private fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse {
    val vehicleKey = request.vehicleType ?: "NONE"
    val result = PolicyRateEngine(loadRateTable()).reimbursement(vehicleKey, request.distance)

    transaction {
        TripsTable.insert {
            it[token] = request.token ?: ""
            it[TripsTable.vehicleKey] = vehicleKey
            it[distanceKm] = request.distance
            it[originalDistanceKm] = request.originalDistance
            it[startTime] = request.startTime
            it[endTime] = request.endTime
            it[status] = "SUBMITTED"
        }
    }

    return ExpenseSubmissionResponse(
        status = 1,
        reimbursableAmount = result.cappedAmount.toDouble(),
        amount = result.cappedAmount.toDouble(),
        distance = request.distance,
        transId = submitTransId(request, vehicleKey),
        message = "Journey submitted successfully",
    )
}

/**
 * Builds the live [PolicyRateTable] from [VehiclesTable] so server and client use the same rates.
 * Internal (not private) — PLAN_V33 B4's `/api/miles/log` route reuses this same rate math.
 */
internal fun loadRateTable(): PolicyRateTable =
    transaction {
        PolicyRateTable(
            rates = VehiclesTable.selectAll().associate { it[VehiclesTable.vehicleKey] to it[VehiclesTable.ratePerKm] },
        )
    }

/**
 * Deterministic transaction id — no Math.random/Date (non-deterministic, and Date isn't available
 * on this dispatcher-free JVM target); derived entirely from request fields so retries of the same
 * submission produce the same id.
 */
private fun submitTransId(
    request: SubmitMilesRequestK,
    vehicleKey: String,
): String = "TXN-${request.token ?: "anon"}-$vehicleKey-${request.distance}"
