package com.mileway.core.data.claim

import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.ClaimLineEntity
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

    /** Upserts [report], bumping [Report.recordVersion]; returns the saved (version-bumped) report. */
    suspend fun save(report: Report): Report {
        val now = clock.now().toEpochMilliseconds()
        val existing = reportDao.get(report.id)
        val nextVersion = (existing?.recordVersion ?: 0L) + 1
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
        // Whole-report replace of lines/steps — the simplest correct write path for a
        // client-authoritative Draft; line ids are caller-supplied and stable across saves, so
        // this is a delete-then-reinsert, not a churn of new ids each time.
        claimLineDao.getByReport(report.id).forEach { claimLineDao.delete(it.id) }
        report.lines.forEach { claimLineDao.upsert(it.toEntity(report.id, now, json)) }
        approvalStepDao.deleteByReport(report.id)
        report.approvalChain.steps.forEach { approvalStepDao.insert(it.toEntity(report.id)) }

        val saved = report.copy(recordVersion = nextVersion)
        opOutbox.enqueue(type = OP_TYPE_REPORT, payload = json.encodeToString(Report.serializer(), saved))
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
