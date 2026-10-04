package com.mileway.core.data.claim

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.dao.StatementImportDao
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
import com.mileway.core.data.model.db.StatementImportEntity
import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val mutex = Mutex()

    suspend fun get(id: String): Report? {
        val entity = reportDao.get(id) ?: return null
        return entity.toDomain(
            lines = claimLineDao.getByReport(id).map { it.toDomain(json) },
            steps = approvalStepDao.getByReport(id).filter { it.claimLineId == null }.map { it.toDomain() },
        )
    }

    fun observe(id: String): Flow<Report?> =
        combine(
            reportDao.observe(id),
            claimLineDao.observeByReport(id),
            approvalStepDao.observeByReport(id),
        ) { reportEntity, lines, steps ->
            reportEntity?.toDomain(lines.map { it.toDomain(json) }, steps.filter { it.claimLineId == null }.map { it.toDomain() })
        }

    fun observeByEmployee(employeeId: String): Flow<List<Report>> =
        reportDao.observeByEmployee(employeeId).map { reports ->
            reports.map { entity ->
                entity.toDomain(
                    lines = claimLineDao.getByReport(entity.id).map { it.toDomain(json) },
                    steps = approvalStepDao.getByReport(entity.id).filter { it.claimLineId == null }.map { it.toDomain() },
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

    /** Checks the persisted batch receipt without exposing Room to feature modules. */
    suspend fun wasStatementImported(id: String): Boolean = requireNotNull(database).statementImportDao().get(id) != null

    /** Saves the import receipt and all matches in one Room transaction, with version checks. */
    suspend fun saveStatementMatches(
        batch: StatementImportEntity,
        snapshots: List<Report>,
        matches: Map<String, ExpenseLine>,
        imports: StatementImportDao = requireNotNull(database).statementImportDao(),
    ): Boolean {
        val saved =
            atomic {
                if (imports.get(batch.id) != null) return@atomic null
                require(matches.keys.all { id -> snapshots.any { report -> report.lines.any { it.id == id } } })
                val used = imports.matchedTransactionIds().toSet()
                require(
                    matches.values
                        .map { it.cardMatchId }
                        .distinct()
                        .size == matches.size,
                )
                require(matches.values.all { it.cardMatchId != null && it.cardMatchId !in used }) { "Statement transaction already matched" }
                val changed =
                    snapshots.filter { report -> report.lines.any { it.id in matches } }.map { report ->
                        require(report.state in setOf(ReportLifecycleState.DRAFT, ReportLifecycleState.SENT_BACK, ReportLifecycleState.RECALLED))
                        report.lines.forEach { original -> matches[original.id]?.let { validateStatementMatch(original, it) } }
                        write(report.copy(lines = report.lines.map { matches[it.id] ?: it }))
                    }
                imports.upsert(batch)
                changed
            } ?: return false
        saved.forEach { enqueue(it) }
        return true
    }

    /** Includes booking metadata and scoped line actions without changing the report wire contract. */
    suspend fun review(id: String): ApprovalReview? {
        val report = get(id) ?: return null
        val entity = requireNotNull(reportDao.get(id))
        val violations = database?.policyViolationDao()?.getByReport(id).orEmpty()
        val blocked =
            report.lines
                .filter { it.hasHardViolation() }
                .map { it.id }
                .toMutableSet()
        violations.filter { isHardPolicyCode(it.code) }.forEach { violation ->
            if (violation.claimLineId == null) blocked.addAll(report.lines.map { it.id }) else blocked.add(violation.claimLineId)
        }
        return ApprovalReview(report, approvalStepDao.getByReport(id), entity.submittedAtMs, entity.accountingPeriodKey, blocked)
    }

    /** The same persisted queue drives single-report and bulk review. */
    fun observeReviewQueue(): Flow<List<ApprovalReview>> =
        combine(observeAll(), database?.policyViolationDao()?.observeAll() ?: flowOf(emptyList())) { reports, _ ->
            reports.filter { it.state == ReportLifecycleState.SUBMITTED }.mapNotNull { review(it.id) }
        }

    /** Route evidence from the trip which produced a mileage claim; manual lines have no route. */
    suspend fun mileageRoute(line: MileageLine): List<com.mileway.core.data.model.db.LocationData> =
        line.sourceTripId?.let { database?.locationDao()?.getLocationsByTokenOnce(it) }.orEmpty()

    /** Active, time-bounded delegation choices for this report and current step. */
    suspend fun delegates(
        reportId: String,
        actor: String,
    ): List<com.mileway.core.data.model.db.DelegateAssignmentEntity> {
        val review = requireNotNull(review(reportId))
        val now = clock.now().toEpochMilliseconds()
        return database?.delegateAssignmentDao()?.getActiveForDelegate(actor).orEmpty().filter {
            it.isActive &&
                it.startsAtMs <= now &&
                now < it.expiresAtMs &&
                it.delegatorAccountId != review.report.employeeId &&
                delegationScopeMatches(it.scope, reportId, review.nextRole)
        }
    }

    /** Records the manager then final FINANCE approval on one ordered chain. */
    suspend fun act(
        reportId: String,
        expectedVersion: Long,
        actedBy: String,
        action: ApprovalAction,
        comment: String,
        role: String = MANAGER_ROLE,
        onBehalfOf: String? = null,
    ): Report = actScoped(reportId, expectedVersion, actedBy, action, comment, role, onBehalfOf, null)

    /** A scoped rejection retains the line and history while leaving the other lines approvable. */
    suspend fun reviewLine(
        reportId: String,
        expectedVersion: Long,
        actedBy: String,
        action: ApprovalAction,
        comment: String,
        lineId: String,
        onBehalfOf: String? = null,
    ): Report = actScoped(reportId, expectedVersion, actedBy, action, comment, MANAGER_ROLE, onBehalfOf, lineId)

    private suspend fun actScoped(
        reportId: String,
        expectedVersion: Long,
        actedBy: String,
        action: ApprovalAction,
        comment: String,
        role: String,
        onBehalfOf: String?,
        lineId: String?,
    ): Report {
        require(actedBy.isNotBlank()) { "An approver identity is required" }
        require(comment.isNotBlank()) { "A comment is required" }
        val saved =
            atomic {
                val review = requireNotNull(review(reportId)) { "Report not found" }
                val report = review.report
                require(report.recordVersion == expectedVersion) { "Report changed; reload before acting" }
                require(report.state == ReportLifecycleState.SUBMITTED) { "Report is not awaiting review" }
                require(actedBy != report.employeeId && onBehalfOf != report.employeeId) { "Self approval is not allowed" }
                require(role == review.nextRole) { "Review requires the ${review.nextRole} step" }
                if (onBehalfOf != null) {
                    require(onBehalfOf != actedBy && delegates(reportId, actedBy).any { it.delegatorAccountId == onBehalfOf }) {
                        "No active delegation for this report and role"
                    }
                }
                if (lineId != null) {
                    require(report.lines.any { it.id == lineId }) { "Claim line not found" }
                    require(action != ApprovalAction.SEND_BACK) { "Send back applies to the whole report" }
                }
                if (action == ApprovalAction.APPROVE) {
                    require(if (lineId == null) review.canBulkApprove else lineId !in review.blockedLineIds) { "Hard policy violation blocks approval" }
                }
                val row =
                    ApprovalStepEntity(
                        reportId = reportId,
                        stepIndex = (review.actions.maxOfOrNull { it.stepIndex } ?: -1) + 1,
                        role = role,
                        thresholdMinor = null,
                        actedBy = actedBy,
                        onBehalfOf = onBehalfOf,
                        action = action.name,
                        comment = comment.trim(),
                        actedAtMillis = clock.now().toEpochMilliseconds(),
                        claimLineId = lineId,
                    )
                val next =
                    when {
                        lineId != null || (action == ApprovalAction.APPROVE && role == MANAGER_ROLE) -> report.state
                        else -> ReportLifecycleStateMachine.transition(report.state, action.event())
                    }
                approvalStepDao.insert(row)
                val chain = if (lineId == null) ApprovalChain(report.approvalChain.steps + row.toDomain()) else report.approvalChain
                write(report.copy(state = next, approvalChain = chain), approvalAction = row)
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
                require(event != ReportLifecycleEvent.RECALL || approvalStepDao.getByReport(reportId).isEmpty()) {
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

    private suspend fun write(
        report: Report,
        approvalAction: ApprovalStepEntity? = null,
    ): Report {
        val now = clock.now().toEpochMilliseconds()
        val existing = reportDao.get(report.id)
        require(report.recordVersion == (existing?.recordVersion ?: 0L)) { "Report changed; reload before saving" }
        require((existing?.recordVersion ?: 0L) < Long.MAX_VALUE) { "Report version exhausted" }
        val nextVersion = (existing?.recordVersion ?: 0L) + 1
        validateReviewHistory(report, existing, approvalAction, approvalStepDao.getByReport(report.id))
        val from = existing?.let { ReportLifecycleState.valueOf(it.state) } ?: ReportLifecycleState.DRAFT
        if (from in
            setOf(ReportLifecycleState.SUBMITTED, ReportLifecycleState.APPROVED, ReportLifecycleState.APPROVED_FOR_PAYMENT, ReportLifecycleState.PAID)
        ) {
            require(report.lines == get(report.id)?.lines && report.employeeId == existing?.employeeId) {
                "Submitted claim lines and employee are immutable"
            }
        }
        require(
            from == report.state ||
                ReportLifecycleEvent.entries.any {
                    runCatching { ReportLifecycleStateMachine.transition(from, it) }.getOrNull() == report.state
                },
        ) { "Illegal report state change" }
        val priorLines = claimLineDao.getByReport(report.id)
        priorLines.map { it.toDomain(json) }.filter { it.cardMatchId != null }.forEach { prior ->
            val next = report.lines.find { it.id == prior.id }
            require(next == null || (next.amountMinor == prior.amountMinor && next.currency == prior.currency && next.cardMatchId == prior.cardMatchId)) {
                "Card-matched amount and currency are locked"
            }
        }
        val newlySubmitted = report.state == ReportLifecycleState.SUBMITTED && from != ReportLifecycleState.SUBMITTED
        val submittedAt = existing?.submittedAtMs ?: if (newlySubmitted) now else null
        val period = existing?.accountingPeriodKey ?: submittedAt?.let { at -> nextOpenPeriod(at) { database?.periodLockDao()?.get(it) } }
        reportDao.upsert(
            ReportEntity(
                id = report.id,
                employeeId = report.employeeId,
                state = report.state.name,
                recordVersion = nextVersion,
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                submittedAtMs = submittedAt,
                accountingPeriodKey = period,
            ),
        )
        priorLines.forEach { claimLineDao.delete(it.id) }
        report.lines.forEach { line ->
            val createdAt = priorLines.find { it.id == line.id }?.createdAtMs ?: now
            claimLineDao.upsert(line.toEntity(report.id, createdAt, json))
        }
        if (existing == null) report.approvalChain.steps.forEach { approvalStepDao.insert(it.toEntity(report.id)) }
        val saved = report.copy(recordVersion = nextVersion)
        notifyAndJournal(from, saved, now, approvalAction)
        return saved
    }

    /** Checks the same source-trip identity used by the legacy backfill. */
    suspend fun hasSourceTrip(sourceTripId: String): Boolean = claimLineDao.getBySourceTripId(sourceTripId) != null

    /** Creates exactly one draft per tracked trip without replacing any existing claim or report. */
    suspend fun createMileageDraft(
        employeeId: String,
        line: MileageLine,
    ): Report? {
        require(employeeId.isNotBlank()) { "A mileage draft requires its trip owner" }
        val tripId = requireNotNull(line.sourceTripId)
        require(tripId.isNotBlank()) { "A mileage draft requires its source trip" }
        val now = clock.now().toEpochMilliseconds()
        val report = Report(id = "mileage_$tripId", employeeId = employeeId, lines = listOf(line), recordVersion = 1L)
        val inserted =
            claimLineDao.insertMileageDraft(
                ReportEntity(report.id, employeeId, report.state.name, report.recordVersion, now, now),
                line.toEntity(report.id, now, json),
            )
        if (!inserted) return null
        opOutbox.enqueue(type = OP_TYPE_REPORT, payload = json.encodeToString(Report.serializer(), report))
        return report
    }

    private suspend fun notifyAndJournal(
        from: ReportLifecycleState,
        saved: Report,
        now: Long,
        approvalAction: ApprovalStepEntity? = null,
    ) {
        database?.let { db ->
            val notification =
                ReportLifecycleNotifier.map(from, saved, now)
                    ?: approvalAction?.let { action ->
                        // A substep keeps SUBMITTED; reuse the notifier's stable id and deep link,
                        // then label the review event rather than pretending the lifecycle changed.
                        ReportLifecycleNotifier.map(ReportLifecycleState.DRAFT, saved, now)?.copy(
                            title = if (action.claimLineId != null) "Claim line ${action.action.lowercase()}" else "Finance review required",
                            body = "Report ${saved.id}: ${action.claimLineId ?: action.role} reviewed by ${action.actedBy}",
                        )
                    }
            notification?.let { row ->
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
                val payableLines = requireNotNull(review(saved.id)).payableLines
                require(payableLines.isNotEmpty() && payableLines.all { it.currency == payableLines.first().currency }) {
                    "Payout needs a nonempty single-currency report"
                }
                val amount =
                    payableLines.fold(0L) { sum, line ->
                        require(line.amountMinor >= 0 && sum <= Long.MAX_VALUE - line.amountMinor) { "Invalid payout amount" }
                        sum + line.amountMinor
                    }
                require(amount > 0) { "Payout must be positive" }
                val currency = payableLines.first().currency
                require(currency.matches(Regex("[A-Z]{3}"))) { "Currency must be an ISO code" }
                val dao = db.pendingPaymentJournalDao()
                val prior = dao.getByReport(saved.id).singleOrNull { it.id == payoutJournalId(saved.id) }
                if (prior == null) {
                    dao.upsert(
                        PendingPaymentJournalEntity(
                            id = payoutJournalId(saved.id),
                            reportId = saved.id,
                            amountMinor = amount,
                            currency = currency,
                            glAccountCode = null,
                            status = PaymentStatus.PENDING.name,
                            createdAtMs = now,
                        ),
                    )
                } else {
                    require(prior.amountMinor == amount && prior.currency == currency) { "Journal is immutable" }
                }
            }
        }
    }

    private suspend fun enqueue(report: Report) {
        opOutbox.enqueue(type = OP_TYPE_REPORT, payload = json.encodeToString(Report.serializer(), report))
    }

    private suspend fun <T> atomic(block: suspend () -> T): T =
        mutex.withLock {
            if (database == null) block() else database.useWriterConnection { connection -> connection.immediateTransaction { block() } }
        }

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

private fun ApprovalAction.event(): ReportLifecycleEvent =
    when (this) {
        ApprovalAction.APPROVE -> ReportLifecycleEvent.APPROVE
        ApprovalAction.REJECT -> ReportLifecycleEvent.REJECT
        ApprovalAction.SEND_BACK -> ReportLifecycleEvent.SEND_BACK
    }

private fun delegationScopeMatches(
    scope: String,
    reportId: String,
    role: String,
): Boolean = scope in setOf("approvals", role, "report:$reportId")

private fun validateReviewHistory(
    report: Report,
    existing: ReportEntity?,
    approvalAction: ApprovalStepEntity?,
    priorActions: List<ApprovalStepEntity>,
) {
    require(
        approvalAction != null || report.approvalChain.steps == priorActions.filter { it.claimLineId == null }.map { it.toDomain() } || existing == null,
    ) {
        "Approval history may only change through review actions"
    }
    if (report.state == ReportLifecycleState.APPROVED && existing?.state != ReportLifecycleState.APPROVED.name) {
        require(approvalAction?.role == FINANCE_ROLE && approvalAction.claimLineId == null && approvalAction.action == ApprovalAction.APPROVE.name) {
            "Final finance approval is required"
        }
    }
    require(report.state != ReportLifecycleState.RECALLED || priorActions.isEmpty()) { "A reviewed report cannot be recalled" }
}
