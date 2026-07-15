package com.mileway.server

import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.DistanceResponseV2
import com.mileway.core.data.model.network.EmptyRequest
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesRequestV2
import com.mileway.core.data.model.network.LogMilesResponseV2
import com.mileway.core.data.model.network.LogMilesRoutesResponse
import com.mileway.core.data.model.network.LogMilesServiceDto
import com.mileway.core.data.model.network.LogMilesServicesResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.MapResponse
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.SuccessResponseV2
import com.mileway.core.data.model.network.TrackMileageStatusResponse
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

// ponytail: no policy-configured log-miles cap exists yet (no config table) — a fixed 50km/month
// fixture stands in. Upgrade to a real per-tenant limit once log-miles policy config lands.
private const val DEFAULT_LOG_MILES_LIMIT_KM = 50.0

/**
 * PLAN_V33 B4: the remaining `miles`/`log-miles`/`distance`/`map` routes marked "route pending B4"
 * in KtorMilewayNetworkApi.kt. No auth (PLAN_V33.1) — every route below is open.
 */
fun Route.milesExtraRoutes() {
    post("/api/miles/event") {
        val request = call.receive<PostMileageEventRequestK>()
        transaction { insertMilesEventRow(request, event = request.eventType ?: "MILES_EVENT") }
        call.respond(SuccessResponseV2())
    }

    post("/api/miles/discard") {
        val request = call.receive<PostMileageEventRequestK>()
        transaction { insertMilesEventRow(request, event = "DISCARD") }
        call.respond(SuccessResponseV2())
    }

    get("/api/miles/status") {
        call.respond(trackMileageStatus(call.request.queryParameters["token"] ?: ""))
    }

    post("/api/miles/reset/{contactId}") {
        call.receive<EmptyRequest>()
        call.respond(SuccessResponseV2(permissionId = call.parameters["contactId"]?.toLongOrNull()))
    }

    post("/api/distance") { call.respond(distanceResponse(call.receive())) }

    get("/api/map") { call.respond(mapResponse(call.request.queryParameters["lat"], call.request.queryParameters["lng"])) }

    post("/api/miles/log/limit") {
        call.receive<LogMilesRequestV2>()
        call.respond(LogMilesResponseV2(limit = DEFAULT_LOG_MILES_LIMIT_KM, limitPeriod = "MONTHLY"))
    }

    post("/api/miles/log") { call.respond(submitLogMiles(call.receive())) }

    get("/api/log-miles/services") { call.respond(logMilesServicesResponse()) }

    get("/api/log-miles/routes") { call.respond(LogMilesRoutesResponse()) }
}

private fun insertMilesEventRow(
    request: PostMileageEventRequestK,
    event: String,
) {
    EventsTable.insert {
        it[token] = request.token
        it[EventsTable.event] = event
        it[eventType] = request.tag
        it[time] = request.timestamp
        it[lat] = request.latitude
        it[lng] = request.longitude
    }
}

/**
 * Derives active/deactivated from the token's most recent [EventsTable] row — there's no
 * dedicated tracking-session table yet, so "status" is inferred from the event log itself. No
 * history for the token defaults to active, matching `stub/DemoMockData.kt`'s always-active demo.
 */
private fun trackMileageStatus(token: String): TrackMileageStatusResponse {
    val latestEvent =
        transaction {
            EventsTable.selectAll()
                .where { EventsTable.token eq token }
                .orderBy(EventsTable.time to SortOrder.DESC)
                .limit(1)
                .map { it[EventsTable.event] }
                .firstOrNull()
        }
    val stopped = latestEvent?.let { it.contains("STOP", ignoreCase = true) || it.contains("DISCARD", ignoreCase = true) } ?: false
    return if (stopped) {
        TrackMileageStatusResponse(statusCode = 504, description = "Deactivated by user")
    } else {
        TrackMileageStatusResponse(statusCode = 200, description = "Active")
    }
}

/** Same rate math as POST /api/miles/submit (see Application.kt's `submitMiles`) — one policy engine, two entry points. */
private fun submitLogMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse {
    val vehicleKey = request.vehicleType ?: "NONE"
    val result = transaction { PolicyRateEngine(loadRateTable()).reimbursement(vehicleKey, request.distance) }
    return ExpenseSubmissionResponse(
        status = 1,
        reimbursableAmount = result.cappedAmount.toDouble(),
        amount = result.cappedAmount.toDouble(),
        distance = request.distance,
        transId = "LOGMILES-$vehicleKey-${request.distance}",
        message = "Miles logged successfully",
    )
}

private fun logMilesServicesResponse(): LogMilesServicesResponse =
    transaction {
        LogMilesServicesResponse(
            services =
                LogMilesServicesTable.selectAll().map { row ->
                    LogMilesServiceDto(
                        id = row[LogMilesServicesTable.id],
                        name = row[LogMilesServicesTable.name],
                        glCode = row[LogMilesServicesTable.glCode],
                    )
                },
        )
    }

private fun distanceResponse(request: DistanceRequestV2): DistanceResponseV2 {
    val coords = request.coords.filter { it.lat != null && it.lng != null }
    val totalKm = coords.zipWithNext { a, b -> haversineKm(a, b) }.sum()
    return DistanceResponseV2(distance = totalKm, unit = "km")
}

/** Great-circle distance in km between two [CoordsV2] points. */
private fun haversineKm(
    a: CoordsV2,
    b: CoordsV2,
): Double {
    val lat1 = Math.toRadians(a.lat ?: 0.0)
    val lat2 = Math.toRadians(b.lat ?: 0.0)
    val dLat = Math.toRadians((b.lat ?: 0.0) - (a.lat ?: 0.0))
    val dLng = Math.toRadians((b.lng ?: 0.0) - (a.lng ?: 0.0))
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * EARTH_RADIUS_KM * atan2(sqrt(h), sqrt(1 - h))
}

// ponytail: no geocoder is wired up — returns the input coordinates plus a placeholder address
// string. Swap for a real reverse-geocoding call (or an offline tile-based lookup) when one
// exists; this is enough to unblock the client's map-preview screen without a live dependency.
private fun mapResponse(
    lat: String?,
    lng: String?,
): MapResponse =
    MapResponse(
        address = "Unknown address ($lat, $lng) — no geocoder configured",
        lat = lat?.toDoubleOrNull(),
        lng = lng?.toDoubleOrNull(),
    )
