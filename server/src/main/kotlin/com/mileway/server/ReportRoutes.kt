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
import com.mileway.core.data.domain.payout.PayoutBackend
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
 *
 * `internal var`, not `private val`: ReportRoutesTest swaps in a call-counting wrapper around a
 * real [SimulatedPayoutBackend] to prove Y01-Y03 (journal-before-call, crash recovery,
 * exactly-once) without a second production seam.
 */
internal var payoutBackend: PayoutBackend = SimulatedPayoutBackend()

fun Route.reportRoutes() {
    post("/api/reports/submit") {
        val incoming = call.receive<Report>()
        val current = loadReport(incoming.id)
        val fromState = current?.state ?: ReportLifecycleState.DRAFT
        val event = if (fromState == ReportLifecycleState.SENT_BACK) ReportLifecycleEvent.RESUBMIT else ReportLifecycleEvent.SUBMIT
        respondWithTransition(call, fromState, event) { nextState ->
            val saved = incoming.copy(state = nextState, approvalChain = current?.approvalChain ?: incoming.approvalChain)
            persistReport(saved)
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
            completePayout(stored)
            persistReport(stored.copy(state = nextState))
        }
    }
}

/**
 * Journal-before-call (Y01), and idempotent against a crash landing between the journal write and
 * the report row being persisted as PAID (Y02/Y03): a retried reimburse call for the same report
 * first reads back whatever journal state already exists.
 *
 * - No journal yet -> fresh attempt: write PENDING, call [payoutBackend] once, write PAID.
 * - Existing PENDING (crash before the backend call ran, or before its result was written) ->
 *   resume: reuse that same row (not a new one — one row per report, `reportId` is its PK) and
 *   call [payoutBackend] once.
 * - Existing PAID (the backend call already completed in a prior attempt that crashed before the
 *   *report* row was updated to PAID) -> resume WITHOUT calling [payoutBackend] again. This is the
 *   guard that makes a retry never complete twice (Y03): the journal's own persisted status, not
 *   the report's lifecycle state, is what decides whether the backend gets called.
 *
 * ponytail: exactly-once against a REAL payment rail needs the backend's own idempotency key, not
 * just journal-state inspection — that gap doesn't exist here because [SimulatedPayoutBackend] is
 * synchronous in-process (no network hop for a crash to land inside mid-call). Upgrade this if a
 * real rail is ever wired in behind [PayoutBackend].
 */
private fun completePayout(report: Report): PendingPaymentJournal {
    val existing = loadJournal(report.id)
    if (existing?.status == PaymentStatus.PAID) return existing

    val journal =
        existing ?: PendingPaymentJournal(
            reportId = report.id,
            amountMinor = report.totalAmountMinor(),
            currency = report.currency(),
            status = PaymentStatus.PENDING,
            createdAtMillis = System.currentTimeMillis(),
        )
    persistJournal(journal)
    val paid = payoutBackend.payout(journal)
    persistJournal(paid)
    return paid
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
    // A04: the submitter cannot approve their own report. Scoped to APPROVE only — sending a
    // report back to its own submitter isn't the same hazard this guard exists for.
    if (action == ApprovalAction.APPROVE && request.actedBy == stored.employeeId) {
        call.respond(HttpStatusCode.Conflict, "Cannot approve your own report")
        return
    }
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
        val persisted = persistReport(saved)
        insertApprovalStep(saved.id, step)
        persisted
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

/**
 * Overwrites the report row and its claim lines — delete-then-insert, same upsert idiom as every
 * other table here — and returns what was actually persisted (C08: `recordVersion` bumped by
 * exactly one on every accepted write). Callers must respond with THIS return value, not their own
 * pre-increment `report`: the in-memory object passed in still carries the OLD version number —
 * responding with it instead of the persisted row was a real bug caught by
 * ReportLifecycleHardeningTest.recordVersionIncrementsOnEveryAcceptedWrite.
 */
private fun persistReport(report: Report): Report {
    val persisted = report.copy(recordVersion = report.recordVersion + 1)
    transaction {
        ReportsTable.deleteWhere { ReportsTable.id eq persisted.id }
        ReportsTable.insert {
            it[id] = persisted.id
            it[employeeId] = persisted.employeeId
            it[state] = persisted.state.name
            it[recordVersion] = persisted.recordVersion
        }
        ClaimLinesTable.deleteWhere { ClaimLinesTable.reportId eq persisted.id }
        persisted.lines.forEach { line -> insertClaimLine(persisted.id, line) }
    }
    return persisted
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

/** `internal`, not `private`: ReportRoutesTest seeds a pre-existing journal row directly to simulate a crash mid-reimburse (Y02). */
internal fun persistJournal(journal: PendingPaymentJournal) {
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

private fun loadJournal(reportId: String): PendingPaymentJournal? =
    transaction {
        PendingPaymentJournalTable
            .selectAll()
            .where { PendingPaymentJournalTable.reportId eq reportId }
            .firstOrNull()
            ?.let { row ->
                PendingPaymentJournal(
                    reportId = row[PendingPaymentJournalTable.reportId],
                    amountMinor = row[PendingPaymentJournalTable.amountMinor],
                    currency = row[PendingPaymentJournalTable.currency],
                    status = PaymentStatus.valueOf(row[PendingPaymentJournalTable.status]),
                    createdAtMillis = row[PendingPaymentJournalTable.createdAtMillis],
                )
            }
    }
