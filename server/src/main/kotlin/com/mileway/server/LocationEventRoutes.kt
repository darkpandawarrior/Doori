package com.mileway.server

import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.EventPayloadV2
import com.mileway.core.data.model.network.EventRequestV2
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.LocationPayloadV2
import com.mileway.core.data.model.network.LocationRequestV2
import com.mileway.core.data.model.network.LocationResponseV2
import com.mileway.core.data.model.network.SuccessResponseV2
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * PLAN_V33 B3: offline-sync upload/read endpoints — no auth (deferred, see PLAN_V33.1). Dedup is by
 * `opId`: [org.jetbrains.exposed.v1.jdbc.insertIgnore] against the UNIQUE index on
 * `LocationPointsTable.opId`/`EventsTable.opId` (Schema.kt) turns a replayed POST of the same opId
 * into a no-op — H2 (like Postgres) allows unlimited NULLs in a unique index, so rows without an
 * opId always insert.
 */
fun Route.locationEventRoutes() {
    post("/api/location/batch") {
        val request = call.receive<BulkLocationRequestV2>()
        transaction { request.data.forEach(::insertLocationRow) }
        call.respond(SuccessResponseV2())
    }
    post("/api/location") {
        val request = call.receive<LocationRequestV2>()
        transaction { insertLocationRow(request.data) }
        call.respond(SuccessResponseV2())
    }
    get("/api/location") { call.respond(locationResponse(call)) }

    post("/api/events/batch") {
        val request = call.receive<BulkEventRequestV2>()
        transaction { request.data.forEach(::insertEventRow) }
        call.respond(SuccessResponseV2())
    }
    post("/api/events") {
        val request = call.receive<EventRequestV2>()
        transaction { insertEventRow(request.data) }
        call.respond(SuccessResponseV2())
    }
    get("/api/events") { call.respond(eventResponse(call)) }
}

private fun insertLocationRow(payload: LocationPayloadV2) {
    LocationPointsTable.insertIgnore {
        it[token] = payload.token
        it[lat] = payload.lat
        it[lng] = payload.lng
        it[date] = payload.date
        it[speed] = payload.speed.toDouble()
        it[accuracy] = payload.accuracy.toDouble()
        it[isMock] = payload.isMock
        it[isAbnormal] = payload.isAbnormal
        it[provider] = payload.provider
        it[opId] = payload.opId
    }
}

private fun insertEventRow(payload: EventPayloadV2) {
    EventsTable.insertIgnore {
        it[token] = payload.token
        it[event] = payload.event
        it[eventType] = payload.eventType
        it[time] = payload.time
        it[lat] = payload.lat
        it[lng] = payload.lng
        it[opId] = payload.opId
    }
}

private fun locationRowToPayload(row: ResultRow) =
    LocationPayloadV2(
        lat = row[LocationPointsTable.lat],
        lng = row[LocationPointsTable.lng],
        token = row[LocationPointsTable.token],
        date = row[LocationPointsTable.date],
        speed = row[LocationPointsTable.speed]?.toFloat() ?: 0f,
        accuracy = row[LocationPointsTable.accuracy]?.toFloat() ?: 0f,
        isMock = row[LocationPointsTable.isMock],
        isAbnormal = row[LocationPointsTable.isAbnormal],
        provider = row[LocationPointsTable.provider],
        opId = row[LocationPointsTable.opId],
    )

private fun eventRowToPayload(row: ResultRow) =
    EventPayloadV2(
        token = row[EventsTable.token],
        event = row[EventsTable.event],
        time = row[EventsTable.time],
        eventType = row[EventsTable.eventType],
        lat = row[EventsTable.lat],
        lng = row[EventsTable.lng],
        opId = row[EventsTable.opId],
    )

/** Reads `token` (required, empty string default) and `start`/`end` millis (default full range). */
private fun rangeParams(call: ApplicationCall): Triple<String, Long, Long> =
    Triple(
        call.request.queryParameters["token"] ?: "",
        call.request.queryParameters["start"]?.toLongOrNull() ?: Long.MIN_VALUE,
        call.request.queryParameters["end"]?.toLongOrNull() ?: Long.MAX_VALUE,
    )

private fun locationResponse(call: ApplicationCall): LocationResponseV2 {
    val (token, start, end) = rangeParams(call)
    return transaction {
        val rows =
            LocationPointsTable.selectAll()
                .where {
                    (LocationPointsTable.token eq token) and
                        (LocationPointsTable.date greaterEq start) and
                        (LocationPointsTable.date lessEq end)
                }
                .orderBy(LocationPointsTable.date to SortOrder.ASC)
                .map(::locationRowToPayload)
        LocationResponseV2(status = 200, count = rows.size, data = rows)
    }
}

private fun eventResponse(call: ApplicationCall): EventResponseV2 {
    val (token, start, end) = rangeParams(call)
    return transaction {
        val rows =
            EventsTable.selectAll()
                .where {
                    (EventsTable.token eq token) and
                        (EventsTable.time greaterEq start) and
                        (EventsTable.time lessEq end)
                }
                .orderBy(EventsTable.time to SortOrder.ASC)
                .map(::eventRowToPayload)
        EventResponseV2(status = 200, count = rows.size, data = rows)
    }
}
