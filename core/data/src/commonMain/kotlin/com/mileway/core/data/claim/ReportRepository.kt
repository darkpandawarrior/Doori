package com.mileway.core.data.claim

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import com.mileway.core.data.domain.notify.ReportLifecycleNotifier
import com.mileway.core.data.domain.payout.PaymentStatus
import com.mileway.core.data.domain.payout.PendingPaymentJournal
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.core.data.model.db.PendingPaymentJournalEntity
import com.mileway.core.data.model.db.ReportEntity
import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * L2: the local-first store for the whole claim domain (a [Report], its [ClaimLine]s and its
 * [ApprovalStep] chain), backed by [ReportDao]/[ClaimLineDao]/[ApprovalStepDao].
 *
 * ponytail: no real backend client is wired for reports yet — that is a later lane's job (see
 * [com.mileway.core.network.api.NetworkBackendFlags.useRealBackend], which this repository does not
 * branch on). Every read/write here always goes through Room, which already satisfies "returns
 * local data when useRealBackend=false" since there is no other path to diverge from.
 *
 * Every [save] bumps [Report.recordVersion] by one and durably queues the saved report onto the
 * kmp-toolkit [OpOutbox] (`op_outbox`, type `"report"`) — the same FIFO operation log
 * [com.mileway.core.data.watch]/WhatsNew already use for non-mileage writes, now covering the claim
 * domain's record types alongside it.
 */
class ReportRepository(
    private val reportDao: ReportDao,
    private val claimLineDao: ClaimLineDao,
    private val approvalStepDao: ApprovalStepDao,
    private val opOutbox: OpOutbox,
    private val json: Json,
    private val clock: Clock = Clock.System,
    private val database: MilewayDatabase? = null,
) {
    suspend fun get(id: String): Report? {
        val entity = reportDao.get(id) ?: return null
        return entity.toDomain(
            lines = claimLineDao.getByReport(id).map { it.toDomain(json) },
            steps = approvalStepDao.getByReport(id).map { it.toDomain() },
        )
    }

    fun observe(id: String): Flow<Report?> =
        combine(
            reportDao.observe(id),
            claimLineDao.observeByReport(id),
            approvalStepDao.observeByReport(id),
        ) { reportEntity, lines, steps ->
            reportEntity?.toDomain(lines.map { it.toDomain(json) }, steps.map { it.toDomain() })
        }

    fun observeByEmployee(employeeId: String): Flow<List<Report>> =
        reportDao.observeByEmployee(employeeId).map { reports ->
            reports.map { entity ->
                entity.toDomain(
                    lines = claimLineDao.getByReport(entity.id).map { it.toDomain(json) },
                    steps = approvalStepDao.getByReport(entity.id).map { it.toDomain() },
                )
            }
        }

    /** All persisted reports, used by the local approval queue. */
    fun observeAll(): Flow<List<Report>> = reportDao.observeAll().map { rows -> rows.mapNotNull { get(it.id) } }

    /** Atomically writes the report, history, lifecycle inbox row and (when ready) payout journal. */
    suspend fun save(report: Report): Report {
        val saved = atomic { write(report) }
        enqueue(saved)
        return saved
    }

    /** Records an approver action and comment with optimistic concurrency and self-approval protection. */
    suspend fun act(
        reportId: String,
        expectedVersion: Long,
        actedBy: String,
        action: ApprovalAction,
        comment: String,
        role: String = "manager",
        onBehalfOf: String? = null,
    ): Report {
        require(actedBy.isNotBlank()) { "An approver identity is required" }
        require(comment.isNotBlank()) { "A comment is required" }
        val saved =
            atomic {
                val report = requireNotNull(get(reportId)) { "Report not found" }
                require(report.recordVersion == expectedVersion) { "Report changed; reload before acting" }
                require(actedBy != report.employeeId && onBehalfOf != report.employeeId) { "Self approval is not allowed" }
                val event =
                    when (action) {
                        ApprovalAction.APPROVE -> ReportLifecycleEvent.APPROVE
                        ApprovalAction.SEND_BACK -> ReportLifecycleEvent.SEND_BACK
                        ApprovalAction.REJECT -> ReportLifecycleEvent.REJECT
                    }
                val step =
                    ApprovalStep(
                        stepIndex = report.approvalChain.steps.size,
                        role = role,
                        actedBy = actedBy,
                        onBehalfOf = onBehalfOf,
                        action = action,
                        comment = comment.trim(),
                        actedAtMillis = clock.now().toEpochMilliseconds(),
                    )
                write(
                    report.copy(
                        state = ReportLifecycleStateMachine.transition(report.state, event),
                        approvalChain = ApprovalChain(report.approvalChain.steps + step),
                    ),
                )
            }
        enqueue(saved)
        return saved
    }

    /** Advances a lifecycle event; a recall is refused once an approver has acted. */
    suspend fun transition(
        reportId: String,
        event: ReportLifecycleEvent,
    ): Report {
        val saved =
            atomic {
                val report = requireNotNull(get(reportId)) { "Report not found" }
                require(event != ReportLifecycleEvent.RECALL || report.approvalChain.steps.isEmpty()) {
                    "A report with an approval action cannot be recalled"
                }
                write(report.copy(state = ReportLifecycleStateMachine.transition(report.state, event)))
            }
        enqueue(saved)
        return saved
    }

    /** Commits the simulated receipt, paid report and notification in the same Room transaction. */
    suspend fun completePayment(receipt: PendingPaymentJournal): Report {
        val db = requireNotNull(database) { "Payout requires Room" }
        val saved =
            atomic {
                val report = requireNotNull(get(receipt.reportId)) { "Report not found" }
                val journal =
                    requireNotNull(
                        db
                            .pendingPaymentJournalDao()
                            .getByReport(report.id)
                            .singleOrNull { it.id == payoutJournalId(report.id) },
                    ) { "Payout was not journaled" }
                require(
                    receipt.status == PaymentStatus.PAID &&
                        receipt.amountMinor == journal.amountMinor &&
                        receipt.currency == journal.currency &&
                        receipt.createdAtMillis == journal.createdAtMs,
                ) {
                    "Receipt differs from the journal"
                }
                if (report.state == ReportLifecycleState.PAID) return@atomic report
                require(report.state == ReportLifecycleState.APPROVED_FOR_PAYMENT) { "Report is not payable" }
                db.pendingPaymentJournalDao().upsert(journal.copy(status = PaymentStatus.PAID.name))
                write(report.copy(state = ReportLifecycleStateMachine.transition(report.state, ReportLifecycleEvent.REIMBURSE)))
            }
        enqueue(saved)
        return saved
    }

    private suspend fun write(report: Report): Report {
        val now = clock.now().toEpochMilliseconds()
        val existing = reportDao.get(report.id)
        require(report.recordVersion == (existing?.recordVersion ?: 0L)) { "Report changed; reload before saving" }
        require((existing?.recordVersion ?: 0L) < Long.MAX_VALUE) { "Report version exhausted" }
        val nextVersion = (existing?.recordVersion ?: 0L) + 1
        val from = existing?.let { ReportLifecycleState.valueOf(it.state) } ?: ReportLifecycleState.DRAFT
        if (from in setOf(ReportLifecycleState.APPROVED, ReportLifecycleState.APPROVED_FOR_PAYMENT, ReportLifecycleState.PAID)) {
            require(report.lines == get(report.id)?.lines && report.employeeId == existing?.employeeId) {
                "Approved claim lines and employee are immutable"
            }
        }
        require(
            from == report.state ||
                ReportLifecycleEvent.entries.any {
                    runCatching { ReportLifecycleStateMachine.transition(from, it) }.getOrNull() == report.state
                },
        ) { "Illegal report state change" }
        require(report.state != ReportLifecycleState.RECALLED || report.approvalChain.steps.isEmpty()) {
            "A report with an approval action cannot be recalled"
        }
        reportDao.upsert(
            ReportEntity(
                id = report.id,
                employeeId = report.employeeId,
                state = report.state.name,
                recordVersion = nextVersion,
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
            ),
        )
        claimLineDao.getByReport(report.id).forEach { claimLineDao.delete(it.id) }
        report.lines.forEach { claimLineDao.upsert(it.toEntity(report.id, now, json)) }
        approvalStepDao.deleteByReport(report.id)
        report.approvalChain.steps.forEach { approvalStepDao.insert(it.toEntity(report.id)) }
        val saved = report.copy(recordVersion = nextVersion)
        notifyAndJournal(from, saved, now)
        return saved
    }

    private suspend fun notifyAndJournal(
        from: ReportLifecycleState,
        saved: Report,
        now: Long,
    ) {
        database?.let { db ->
            ReportLifecycleNotifier.map(from, saved, now)?.let { row ->
                db.notificationDao().upsertAll(
                    listOf(
                        NotificationEntity(
                            id = row.id,
                            title = row.title,
                            body = row.body,
                            relativeTime = "Just now",
                            isUnread = true,
                            type = row.type,
                            createdAtMs = row.createdAtMs,
                            deeplink = row.deeplink,
                        ),
                    ),
                )
            }
            if (saved.state == ReportLifecycleState.APPROVED_FOR_PAYMENT) {
                require(saved.lines.isNotEmpty() && saved.lines.all { it.currency == saved.currency() }) {
                    "Payout needs a nonempty single-currency report"
                }
                val amount =
                    saved.lines.fold(0L) { sum, line ->
                        require(line.amountMinor >= 0 && sum <= Long.MAX_VALUE - line.amountMinor) { "Invalid payout amount" }
                        sum + line.amountMinor
                    }
                require(amount > 0) { "Payout must be positive" }
                require(saved.currency().matches(Regex("[A-Z]{3}"))) { "Currency must be an ISO code" }
                val dao = db.pendingPaymentJournalDao()
                val prior = dao.getByReport(saved.id).singleOrNull { it.id == payoutJournalId(saved.id) }
                if (prior == null) {
                    dao.upsert(
                        PendingPaymentJournalEntity(
                            id = payoutJournalId(saved.id),
                            reportId = saved.id,
                            amountMinor = amount,
                            currency = saved.currency(),
                            glAccountCode = null,
                            status = PaymentStatus.PENDING.name,
                            createdAtMs = now,
                        ),
                    )
                } else {
                    require(prior.amountMinor == amount && prior.currency == saved.currency()) { "Journal is immutable" }
                }
            }
        }
    }

    private suspend fun enqueue(report: Report) {
        opOutbox.enqueue(type = OP_TYPE_REPORT, payload = json.encodeToString(Report.serializer(), report))
    }

    private suspend fun <T> atomic(block: suspend () -> T): T =
        if (database == null) block() else database.useWriterConnection { connection -> connection.immediateTransaction { block() } }

    private companion object {
        const val OP_TYPE_REPORT = "report"
    }
}

private fun ReportEntity.toDomain(
    lines: List<ClaimLine>,
    steps: List<ApprovalStep>,
): Report =
    Report(
        id = id,
        employeeId = employeeId,
        lines = lines,
        state = ReportLifecycleState.valueOf(state),
        approvalChain = ApprovalChain(steps),
        recordVersion = recordVersion,
    )

private fun ClaimLine.wireType(): String =
    when (this) {
        is ExpenseLine -> "expense"
        is MileageLine -> "mileage"
        is PerDiemLine -> "per_diem"
        is AdvanceLine -> "advance"
    }

internal fun ClaimLine.toEntity(
    reportId: String,
    createdAtMs: Long,
    json: Json,
): ClaimLineEntity =
    ClaimLineEntity(
        id = id,
        reportId = reportId,
        type = wireType(),
        amountMinor = amountMinor,
        currency = currency,
        fxRatePinnedAt = fxRatePinnedAt,
        policyFlagsCsv = policyFlags.joinToString(","),
        cardMatchId = cardMatchId,
        sourceTripId = sourceTripId,
        detailsJson = json.encodeToString(ClaimLine.serializer(), this),
        createdAtMs = createdAtMs,
    )

internal fun ClaimLineEntity.toDomain(json: Json): ClaimLine = json.decodeFromString(ClaimLine.serializer(), detailsJson)

private fun ApprovalStep.toEntity(reportId: String): ApprovalStepEntity =
    ApprovalStepEntity(
        reportId = reportId,
        stepIndex = stepIndex,
        role = role,
        thresholdMinor = thresholdMinor,
        actedBy = actedBy,
        onBehalfOf = onBehalfOf,
        action = action.name,
        comment = comment,
        actedAtMillis = actedAtMillis,
    )

private fun ApprovalStepEntity.toDomain(): ApprovalStep =
    ApprovalStep(
        stepIndex = stepIndex,
        role = role,
        thresholdMinor = thresholdMinor,
        actedBy = actedBy,
        onBehalfOf = onBehalfOf,
        action = ApprovalAction.valueOf(action),
        comment = comment,
        actedAtMillis = actedAtMillis,
    )

/** One immutable reimbursement journal per report; retries never allocate a new payout. */
internal fun payoutJournalId(reportId: String): String = "report-payout:$reportId"
