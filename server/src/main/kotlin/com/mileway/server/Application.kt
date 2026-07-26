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
import io.ktor.server.auth.authenticate
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
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

@Serializable
data class VersionResponse(
    val fingerprint: String,
)

private const val DEFAULT_PORT = 8080

/**
 * Wave-2 §A: FINGERPRINT (YYYY.0M.0W.MILESTONE.commitCount), baked into the jar at build time by
 * `generateVersionResource` (server/build.gradle.kts) from the same computed value :app/:wear use.
 * "unknown" is a local `./gradlew run` from source without that generated resource on the
 * classpath, not a production condition — the packaged distribution always has it.
 */
private fun readFingerprint(): String =
    object {}.javaClass.getResourceAsStream("/version.properties")?.use { stream ->
        java.util.Properties().apply { load(stream) }.getProperty("fingerprint")
    } ?: "unknown"

fun main() {
    embeddedServer(Netty, port = envPort(), module = Application::module).start(wait = true)
}

private fun envPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

fun Application.module() {
    connectDatabase()

    install(ContentNegotiation) { json(serverJson) }
    configureAuth()

    routing {
        get("/health") { call.respond(healthResponse()) }
        get("/version") { call.respond(VersionResponse(fingerprint = readFingerprint())) }
        authRoutes()
        // PLAN_V34 P2/B2: every other /api/* route needs a bearer token — the routes underneath
        // never took a token dependency (PLAN_V33.1's AUTH-DEFERRED), so this single wrapper is the
        // whole restructure.
        authenticate("jwt") {
            post("/api/echo") { call.respond(call.receive<SubmitMilesRequestK>()) }
            get("/api/vehicles") { call.respond(vehiclesResponse()) }
            get("/api/pricing") { call.respond(ApprovedVehiclePricingResponse(data = loadRateTable().rates)) }
            post("/api/miles/submit") { call.respond(submitMiles(call.receive())) }
            locationEventRoutes()
            milesExtraRoutes()
            checkInRoutes()
        }
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
    val stored = persistTrip(request)
    val result = PolicyRateEngine(loadRateTable()).reimbursement(stored.vehicleKey, stored.distanceKm)

    return ExpenseSubmissionResponse(
        status = 1,
        reimbursableAmount = result.cappedAmount.toDouble(),
        amount = result.cappedAmount.toDouble(),
        distance = stored.distanceKm,
        transId = submitTransId(stored),
        message = "Journey submitted successfully",
    )
}

/** The trip row a submit resolved to — a freshly inserted one, or the one a replayed opId already stored. */
private data class StoredTrip(
    val token: String,
    val vehicleKey: String,
    val distanceKm: Double,
)

/**
 * The client-supplied idempotency key for a submit. The offline outbox replays the *same*
 * [SubmitMilesRequestK] after a lost response (feature/tracking's `MilesSubmitSyncer`), and
 * `SubmitMilesRequestBuilder` puts the trip's routeId in `token` — one trip, one stable key — so
 * that is what dedups until [SubmitMilesRequestK] carries an explicit `opId` of its own. A blank
 * token yields null, which (like `location_points`/`events`) always inserts.
 */
private fun submitOpId(request: SubmitMilesRequestK): String? = request.token?.takeIf { it.isNotBlank() }

/**
 * `insertIgnore` against the UNIQUE `trips.op_id` (Schema.kt) — the same dedup shape as
 * [locationEventRoutes] — then reads the stored row back, so a replay returns the *already-stored*
 * trip rather than re-pricing whatever the retry happened to carry.
 */
private fun persistTrip(request: SubmitMilesRequestK): StoredTrip {
    val requested =
        StoredTrip(
            token = request.token ?: "",
            vehicleKey = request.vehicleType ?: "NONE",
            distanceKm = request.distance,
        )
    val key = submitOpId(request)
    return transaction {
        TripsTable.insertIgnore {
            it[token] = requested.token
            it[vehicleKey] = requested.vehicleKey
            it[distanceKm] = requested.distanceKm
            it[originalDistanceKm] = request.originalDistance
            it[startTime] = request.startTime
            it[endTime] = request.endTime
            it[status] = "SUBMITTED"
            it[opId] = key
        }
        if (key == null) {
            requested
        } else {
            TripsTable.selectAll().where { TripsTable.opId eq key }.first().let { row ->
                StoredTrip(
                    token = row[TripsTable.token],
                    vehicleKey = row[TripsTable.vehicleKey],
                    distanceKm = row[TripsTable.distanceKm],
                )
            }
        }
    }
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
 * on this dispatcher-free JVM target); derived entirely from the *stored* trip so retries of the
 * same submission produce the same id. A blank token normalises to `anon` so the id reads
 * `TXN-anon-…` rather than `TXN--…`.
 */
private fun submitTransId(trip: StoredTrip): String = "TXN-${trip.token.ifBlank { "anon" }}-${trip.vehicleKey}-${trip.distanceKm}"
