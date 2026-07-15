package com.mileway.server

import com.mileway.core.data.model.network.AllTaggedExpenseResponse
import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInItem
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.SubmittedCheckInResponseV2
import com.mileway.core.data.model.network.SuccessResponseV2
import com.mileway.core.data.model.network.TaggedExpenseItem
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
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * PLAN_V33 B4: check-in + expense-tagging routes marked "route pending B4" in
 * KtorMilewayNetworkApi.kt. No auth (PLAN_V33.1) — [CheckInRequestV2] carries no token, so
 * persisted checkins aren't attributed to a user yet; GET /api/checkin/submitted accepts the
 * `token` query param for API compatibility but returns every persisted row (nothing to filter on
 * until auth lands and a token column has real meaning).
 */
fun Route.checkInRoutes() {
    post("/api/checkin") { call.respond(insertCheckIn(call.receive())) }
    post("/api/checkin/center") { call.respond(insertCheckIn(call.receive())) }

    get("/api/checkin/types") { call.respond(allGeoTypes()) }
    get("/api/checkin/types/{typeId}") { call.respond(geoTypeById(call.parameters["typeId"]?.toLongOrNull())) }
    get("/api/checkin/submitted") { call.respond(submittedCheckIns()) }

    get("/api/expenses/tagged") { call.respond(taggedExpenses(call, pending = false)) }
    get("/api/expenses/pending") { call.respond(taggedExpenses(call, pending = true)) }
}

private fun insertCheckIn(request: CheckInRequestV2): SuccessResponseV2 {
    val newId =
        transaction {
            val statement =
                CheckInsTable.insert {
                    it[lat] = request.lat
                    it[lng] = request.lng
                    it[typeId] = request.typeId
                    it[time] = System.currentTimeMillis()
                }
            statement[CheckInsTable.id]
        }
    return SuccessResponseV2(id = newId)
}

private fun allGeoTypes(): AllTypesResponseV2 =
    transaction {
        AllTypesResponseV2(types = GeoTypesTable.selectAll().map(::geoTypeRowToResponse))
    }

private fun geoTypeById(typeId: Long?): CheckInDetailsResponseV2 =
    transaction {
        GeoTypesTable.selectAll()
            .where { GeoTypesTable.id eq (typeId ?: -1L) }
            .map(::geoTypeRowToResponse)
            .firstOrNull() ?: CheckInDetailsResponseV2()
    }

private fun geoTypeRowToResponse(row: ResultRow) =
    CheckInDetailsResponseV2(
        id = row[GeoTypesTable.id],
        name = row[GeoTypesTable.name],
        radius = row[GeoTypesTable.radius],
        lat = row[GeoTypesTable.lat],
        lng = row[GeoTypesTable.lng],
        title = row[GeoTypesTable.name],
        type = row[GeoTypesTable.type],
    )

private fun submittedCheckIns(): SubmittedCheckInResponseV2 =
    transaction {
        SubmittedCheckInResponseV2(
            checkIns =
                CheckInsTable.selectAll()
                    .orderBy(CheckInsTable.time to SortOrder.DESC)
                    .map { CheckInItem(id = it[CheckInsTable.id], time = it[CheckInsTable.time]) },
        )
    }

private fun taggedExpenses(
    call: ApplicationCall,
    pending: Boolean,
): AllTaggedExpenseResponse {
    val start = call.request.queryParameters["start"]?.toLongOrNull() ?: Long.MIN_VALUE
    val end = call.request.queryParameters["end"]?.toLongOrNull() ?: Long.MAX_VALUE
    return transaction {
        AllTaggedExpenseResponse(
            data =
                TaggedExpensesTable.selectAll()
                    .where {
                        (TaggedExpensesTable.pending eq pending) and
                            (TaggedExpensesTable.submittedAt greaterEq start) and
                            (TaggedExpensesTable.submittedAt lessEq end)
                    }
                    .map { row ->
                        TaggedExpenseItem(
                            id = row[TaggedExpensesTable.id],
                            title = row[TaggedExpensesTable.title],
                            amount = row[TaggedExpensesTable.amount],
                        )
                    },
        )
    }
}
