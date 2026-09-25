package com.mileway.server

import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalActionRequest
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.IllegalReportTransitionException
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import com.mileway.core.data.domain.payout.PaymentStatus
import com.mileway.core.data.domain.payout.PendingPaymentJournal
import com.mileway.core.data.domain.payout.SimulatedPayoutBackend
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * The expense-report lifecycle slice: submit -> approve/send-back -> reimburse. Every route
 * applies [ReportLifecycleStateMachine.transition] before touching a table, so an illegal move
 * (e.g. approving a Draft report, reimbursing twice) is rejected with 409 before any write —
 * the state machine is the single source of truth for what's legal, not a per-route `if`.
 */
private val payoutBackend = SimulatedPayoutBackend()

fun Route.reportRoutes() {
    post("/api/reports/submit") {
        val incoming = call.receive<Report>()
        val current = loadReport(incoming.id)
        val fromState = current?.state ?: ReportLifecycleState.DRAFT
        val event = if (fromState == ReportLifecycleState.SENT_BACK) ReportLifecycleEvent.RESUBMIT else ReportLifecycleEvent.SUBMIT
        respondWithTransition(call, fromState, event) { nextState ->
            val saved = incoming.copy(state = nextState, approvalChain = current?.approvalChain ?: incoming.approvalChain)
            persistReport(saved)
            saved
        }
    }

    post("/api/reports/{id}/approve") {
        approveOrSendBack(call, ApprovalAction.APPROVE, ReportLifecycleEvent.APPROVE)
    }

    post("/api/reports/{id}/send-back") {
        approveOrSendBack(call, ApprovalAction.SEND_BACK, ReportLifecycleEvent.SEND_BACK)
    }

    post("/api/reports/{id}/reimburse") {
        val id = call.parameters["id"]
        val stored = id?.let { loadReport(it) }
        if (stored == null) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }
        respondWithTransition(call, stored.state, ReportLifecycleEvent.REIMBURSE) { nextState ->
            // Journal to the pending table BEFORE calling the (simulator) backend, so a crash
            // between the two leaves a recoverable PENDING row rather than a lost payout.
            val journal =
                PendingPaymentJournal(
                    reportId = stored.id,
                    amountMinor = stored.totalAmountMinor(),
                    currency = stored.currency(),
                    status = PaymentStatus.PENDING,
                    createdAtMillis = System.currentTimeMillis(),
                )
            persistJournal(journal)
            val paid = payoutBackend.payout(journal)
            persistJournal(paid)

            val saved = stored.copy(state = nextState)
            persistReport(saved)
            saved
        }
    }
}

private suspend fun approveOrSendBack(
    call: ApplicationCall,
    action: ApprovalAction,
    event: ReportLifecycleEvent,
) {
    val id = call.parameters["id"]
    val stored = id?.let { loadReport(it) }
    if (stored == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }
    val request = call.receive<ApprovalActionRequest>()
    respondWithTransition(call, stored.state, event) { nextState ->
        val step =
            ApprovalStep(
                stepIndex = stored.approvalChain.steps.size,
                actedBy = request.actedBy,
                action = action,
                comment = request.comment,
                actedAtMillis = System.currentTimeMillis(),
            )
        val saved = stored.copy(state = nextState, approvalChain = ApprovalChain(steps = stored.approvalChain.steps + step))
        persistReport(saved)
        insertApprovalStep(saved.id, step)
        saved
    }
}

/**
 * Applies [ReportLifecycleStateMachine.transition] from [fromState] and either responds 409 on an
 * [IllegalReportTransitionException] or runs [onLegal] with the resolved next state and responds
 * with its result.
 */
private suspend fun respondWithTransition(
    call: ApplicationCall,
    fromState: ReportLifecycleState,
    event: ReportLifecycleEvent,
    onLegal: (ReportLifecycleState) -> Report,
) {
    val nextState =
        try {
            ReportLifecycleStateMachine.transition(fromState, event)
        } catch (_: IllegalReportTransitionException) {
            call.respond(HttpStatusCode.Conflict, "Cannot apply $event to a report in state $fromState")
            return
        }
    call.respond(onLegal(nextState))
}

// ── Persistence ──────────────────────────────────────────────────────────────

private fun loadReport(id: String): Report? =
    transaction {
        val reportRow = ReportsTable.selectAll().where { ReportsTable.id eq id }.firstOrNull() ?: return@transaction null
        val lines = ClaimLinesTable.selectAll().where { ClaimLinesTable.reportId eq id }.map(::claimLineRowToDomain)
        val steps =
            ApprovalStepsTable
                .selectAll()
                .where { ApprovalStepsTable.reportId eq id }
                .sortedBy { it[ApprovalStepsTable.stepIndex] }
                .map(::approvalStepRowToDomain)
        Report(
            id = reportRow[ReportsTable.id],
            employeeId = reportRow[ReportsTable.employeeId],
            lines = lines,
            state = ReportLifecycleState.valueOf(reportRow[ReportsTable.state]),
            approvalChain = ApprovalChain(steps = steps),
            recordVersion = reportRow[ReportsTable.recordVersion],
        )
    }

/** Overwrites the report row and its claim lines — delete-then-insert, same upsert idiom as every other table here. */
private fun persistReport(report: Report) {
    transaction {
        ReportsTable.deleteWhere { ReportsTable.id eq report.id }
        ReportsTable.insert {
            it[id] = report.id
            it[employeeId] = report.employeeId
            it[state] = report.state.name
            it[recordVersion] = report.recordVersion + 1
        }
        ClaimLinesTable.deleteWhere { ClaimLinesTable.reportId eq report.id }
        report.lines.forEach { line -> insertClaimLine(report.id, line) }
    }
}

private fun insertClaimLine(
    reportId: String,
    line: ClaimLine,
) {
    ClaimLinesTable.insert {
        it[id] = line.id
        it[ClaimLinesTable.reportId] = reportId
        it[amountMinor] = line.amountMinor
        it[currency] = line.currency
        when (line) {
            is ExpenseLine -> {
                it[discriminator] = "expense"
                it[merchant] = line.merchant
                it[category] = line.category
            }
            is MileageLine -> {
                it[discriminator] = "mileage"
                it[distanceKm] = line.distanceKm
                it[vehicleKey] = line.vehicleKey
            }
        }
    }
}

private fun claimLineRowToDomain(row: ResultRow): ClaimLine =
    when (row[ClaimLinesTable.discriminator]) {
        "expense" ->
            ExpenseLine(
                id = row[ClaimLinesTable.id],
                amountMinor = row[ClaimLinesTable.amountMinor],
                currency = row[ClaimLinesTable.currency],
                merchant = row[ClaimLinesTable.merchant].orEmpty(),
                category = row[ClaimLinesTable.category].orEmpty(),
            )
        "mileage" ->
            MileageLine(
                id = row[ClaimLinesTable.id],
                amountMinor = row[ClaimLinesTable.amountMinor],
                currency = row[ClaimLinesTable.currency],
                distanceKm = row[ClaimLinesTable.distanceKm] ?: 0.0,
                vehicleKey = row[ClaimLinesTable.vehicleKey].orEmpty(),
            )
        else -> error("Unknown claim line discriminator: ${row[ClaimLinesTable.discriminator]}")
    }

private fun insertApprovalStep(
    reportId: String,
    step: ApprovalStep,
) {
    transaction {
        ApprovalStepsTable.insert {
            it[ApprovalStepsTable.reportId] = reportId
            it[stepIndex] = step.stepIndex
            it[actedBy] = step.actedBy
            it[action] = step.action.name
            it[comment] = step.comment
            it[actedAtMillis] = step.actedAtMillis
        }
    }
}

private fun approvalStepRowToDomain(row: ResultRow): ApprovalStep =
    ApprovalStep(
        stepIndex = row[ApprovalStepsTable.stepIndex],
        actedBy = row[ApprovalStepsTable.actedBy],
        action = ApprovalAction.valueOf(row[ApprovalStepsTable.action]),
        comment = row[ApprovalStepsTable.comment],
        actedAtMillis = row[ApprovalStepsTable.actedAtMillis],
    )

private fun persistJournal(journal: PendingPaymentJournal) {
    transaction {
        PendingPaymentJournalTable.deleteWhere { PendingPaymentJournalTable.reportId eq journal.reportId }
        PendingPaymentJournalTable.insert {
            it[reportId] = journal.reportId
            it[amountMinor] = journal.amountMinor
            it[currency] = journal.currency
            it[status] = journal.status.name
            it[createdAtMillis] = journal.createdAtMillis
        }
    }
}
