package com.mileway.core.data.claim

import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ApprovalStepEntity
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.ReportEntity
import com.siddharth.kmp.offlineoutbox.OpEntry
import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * L2: exercises [ReportRepository]'s local-first read/write path — round-tripping a polymorphic
 * [com.mileway.core.data.domain.claim.ClaimLine] list through Room, bumping `recordVersion` on
 * every [ReportRepository.save] (acceptance #6: this repository is Room-only, so it already
 * "returns local data" regardless of `NetworkBackendFlags.useRealBackend`), and durably queuing the
 * saved report onto the offline-outbox [OpOutbox] (acceptance #5).
 */
class ReportRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun repo(
        reportDao: FakeReportDao = FakeReportDao(),
        claimLineDao: FakeClaimLineDao = FakeClaimLineDao(),
        approvalStepDao: FakeApprovalStepDao = FakeApprovalStepDao(),
        opOutbox: FakeOpOutbox = FakeOpOutbox(),
    ) = ReportRepository(reportDao, claimLineDao, approvalStepDao, opOutbox, json)

    private fun report(id: String = "r1") =
        Report(
            id = id,
            employeeId = "emp1",
            lines =
                listOf(
                    ExpenseLine(id = "l1", amountMinor = 5_000, currency = "INR", merchant = "Cafe", category = "Food"),
                    MileageLine(id = "l2", amountMinor = 1_200, currency = "INR", distanceKm = 12.0, vehicleKey = "twoWheeler"),
                ),
            state = ReportLifecycleState.DRAFT,
            approvalChain =
                ApprovalChain(
                    listOf(
                        ApprovalStep(stepIndex = 0, actedBy = "mgr1", action = ApprovalAction.APPROVE, actedAtMillis = 100L),
                    ),
                ),
        )

    @Test
    fun `save then get round-trips every claim line subtype`() =
        runTest {
            val repository = repo()

            repository.save(report())
            val loaded = requireNotNull(repository.get("r1"))

            assertEquals(2, loaded.lines.size)
            assertTrue(loaded.lines.any { it is ExpenseLine })
            assertTrue(loaded.lines.any { it is MileageLine })
            assertEquals(1, loaded.approvalChain.steps.size)
        }

    @Test
    fun `save bumps recordVersion on every write`() =
        runTest {
            val repository = repo()

            val first = repository.save(report())
            val second = repository.save(first.copy(state = ReportLifecycleState.SUBMITTED))

            assertEquals(1L, first.recordVersion)
            assertEquals(2L, second.recordVersion)
        }

    @Test
    fun `get returns null for an unknown report`() =
        runTest {
            assertNull(repo().get("missing"))
        }

    @Test
    fun `save is Room-only and enqueues the report onto the offline outbox`() =
        runTest {
            val outbox = FakeOpOutbox()
            val repository = repo(opOutbox = outbox)

            repository.save(report())

            val queued = outbox.pending().first()
            assertEquals(1, queued.size)
            assertEquals("report", queued.single().type)
            assertTrue(queued.single().payload.contains("\"id\":\"r1\""))
        }
}

private class FakeReportDao : ReportDao {
    val rows = MutableStateFlow<Map<String, ReportEntity>>(emptyMap())

    override suspend fun get(id: String): ReportEntity? = rows.value[id]

    override fun observe(id: String): Flow<ReportEntity?> = rows.map { it[id] }

    override fun observeByEmployee(employeeId: String): Flow<List<ReportEntity>> = rows.map { it.values.filter { row -> row.employeeId == employeeId } }

    override fun observeAll(): Flow<List<ReportEntity>> = rows.map { it.values.toList() }

    override suspend fun awaitingPayment(): List<ReportEntity> =
        rows.value.values.filter {
            it.state in setOf("APPROVED", "APPROVED_FOR_PAYMENT")
        }

    override suspend fun upsert(entity: ReportEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}

private class FakeClaimLineDao : ClaimLineDao {
    val rows = MutableStateFlow<Map<String, ClaimLineEntity>>(emptyMap())

    override fun observeByReport(reportId: String): Flow<List<ClaimLineEntity>> = rows.map { it.values.filter { row -> row.reportId == reportId } }

    override suspend fun getByReport(reportId: String): List<ClaimLineEntity> = rows.value.values.filter { it.reportId == reportId }

    override suspend fun getBySourceTripId(sourceTripId: String): ClaimLineEntity? = rows.value.values.firstOrNull { it.sourceTripId == sourceTripId }

    override suspend fun insert(entity: ClaimLineEntity) {
        check(entity.sourceTripId == null || rows.value.values.none { it.sourceTripId == entity.sourceTripId }) {
            "UNIQUE constraint failed: claim_lines.sourceTripId"
        }
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun insertIfAbsent(entity: ClaimLineEntity): Long {
        if (rows.value.containsKey(entity.id) || (entity.sourceTripId != null && getBySourceTripId(entity.sourceTripId) != null)) return -1L
        insert(entity)
        return 1L
    }

    override suspend fun insertDraftReport(entity: ReportEntity) = error("unused in this fake")

    override suspend fun upsert(entity: ClaimLineEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}

private class FakeApprovalStepDao : ApprovalStepDao {
    val rows = MutableStateFlow<List<ApprovalStepEntity>>(emptyList())

    override fun observeByReport(reportId: String): Flow<List<ApprovalStepEntity>> = rows.map { list -> list.filter { it.reportId == reportId } }

    override suspend fun getByReport(reportId: String): List<ApprovalStepEntity> = rows.value.filter { it.reportId == reportId }

    override suspend fun insert(entity: ApprovalStepEntity) {
        rows.value = rows.value + entity
    }

    override suspend fun deleteByReport(reportId: String) {
        rows.value = rows.value.filterNot { it.reportId == reportId }
    }
}

/** In-memory [OpOutbox] — mirrors [com.mileway.feature.logging.usecase.FakeSubmitOutbox]'s pattern. */
private class FakeOpOutbox : OpOutbox {
    private val entries = MutableStateFlow<List<OpEntry>>(emptyList())

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enqueue(
        type: String,
        payload: String,
    ): String {
        val id = Uuid.random().toString()
        entries.value =
            entries.value +
            OpEntry(
                id = id,
                type = type,
                payload = payload,
                attempts = 0,
                status = com.siddharth.kmp.offlineoutbox.OpStatus.PENDING,
                lastError = null,
                createdAtMs = 0L,
            )
        return id
    }

    override fun pending(): Flow<List<OpEntry>> = entries.asStateFlow()

    override suspend fun deadLetters(): List<OpEntry> = emptyList()

    override suspend fun replay(
        maxAttempts: Int,
        isPermanent: (Throwable) -> Boolean,
        send: suspend (OpEntry) -> Unit,
    ) = Unit

    override suspend fun requeue(id: String) = Unit
}
