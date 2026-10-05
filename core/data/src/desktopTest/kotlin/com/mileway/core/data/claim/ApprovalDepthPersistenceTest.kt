package com.mileway.core.data.claim

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.DelegateAssignmentEntity
import com.mileway.core.data.model.db.PeriodLockEntity
import com.siddharth.kmp.offlineoutbox.OutboxDatabase
import com.siddharth.kmp.offlineoutbox.RoomOpOutbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ApprovalDepthPersistenceTest {
    private val instant = Instant.parse("2026-09-29T10:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = instant
        }

    private fun open(file: File): MilewayDatabase =
        Room
            .databaseBuilder<MilewayDatabase>(name = file.path)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

    private fun withStore(block: suspend (MilewayDatabase, ReportRepository) -> Unit) =
        runBlocking {
            val file = File.createTempFile("l11-review", ".db")
            val outboxFile = File.createTempFile("l11-outbox", ".db")
            val db = open(file)
            val outbox =
                Room
                    .databaseBuilder<OutboxDatabase>(name = outboxFile.path)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.Default)
                    .build()
            try {
                val reports = ReportRepository(db.reportDao(), db.claimLineDao(), db.approvalStepDao(), RoomOpOutbox(outbox.opOutboxDao()), Json, clock, db)
                block(db, reports)
            } finally {
                db.close()
                outbox.close()
                file.delete()
                outboxFile.delete()
            }
        }

    private suspend fun submit(
        reports: ReportRepository,
        flags: List<String> = emptyList(),
    ): Report {
        val draft =
            reports.save(
                Report(
                    "report",
                    "employee",
                    listOf(
                        ExpenseLine("first", 5000, "INR", policyFlags = flags, merchant = "Cafe", category = "Meals"),
                        ExpenseLine("second", 1000, "INR", merchant = "Hotel", category = "Travel"),
                    ),
                ),
            )
        return reports.save(draft.copy(state = ReportLifecycleState.SUBMITTED))
    }

    @Test
    fun `delegate action records both identities and rejects expired revoked and wrong scope grants`() =
        withStore { db, reports ->
            val submitted = submit(reports)
            val grant =
                DelegateAssignmentEntity(
                    "grant",
                    "manager-1",
                    "proxy",
                    MANAGER_ROLE,
                    instant.toEpochMilliseconds() - 1,
                    instant.toEpochMilliseconds() + 1,
                    true,
                    1,
                )
            for (invalid in listOf(
                grant.copy(scope = FINANCE_ROLE),
                grant.copy(isActive = false),
                grant.copy(expiresAtMs = instant.toEpochMilliseconds()),
                grant.copy(startsAtMs = instant.toEpochMilliseconds() + 1),
            )) {
                db.delegateAssignmentDao().upsert(invalid)
                assertFailsWith<IllegalArgumentException> {
                    reports.act(submitted.id, submitted.recordVersion, "proxy", ApprovalAction.APPROVE, "Checked", onBehalfOf = "manager-1")
                }
            }
            db.delegateAssignmentDao().upsert(grant)
            val acted = reports.act(submitted.id, submitted.recordVersion, "proxy", ApprovalAction.APPROVE, "Checked", onBehalfOf = "manager-1")
            val row = db.approvalStepDao().getByReport(acted.id).single()
            assertEquals("proxy", row.actedBy)
            assertEquals("manager-1", row.onBehalfOf)
            assertEquals(
                row.onBehalfOf,
                reports
                    .get(acted.id)!!
                    .approvalChain.steps
                    .single()
                    .onBehalfOf,
            )
            assertEquals(ReportLifecycleState.SUBMITTED, acted.state)
        }

    @Test
    fun `manager and finance share one chain and payout cannot release while finance is pending`() =
        withStore { db, reports ->
            val submitted = submit(reports)
            assertFailsWith<IllegalArgumentException> {
                reports.act(submitted.id, submitted.recordVersion, "finance", ApprovalAction.APPROVE, "Early", FINANCE_ROLE)
            }
            val manager = reports.act(submitted.id, submitted.recordVersion, "manager", ApprovalAction.APPROVE, "Checked")
            val payouts = ReportPayoutProcessor(reports, db.reportDao(), db.pendingPaymentJournalDao())
            assertEquals(ReportLifecycleState.SUBMITTED, manager.state)
            assertFailsWith<IllegalArgumentException> { payouts.pay(manager.id) }
            assertTrue(payouts.recover().isEmpty())
            assertTrue(db.pendingPaymentJournalDao().getByReport(manager.id).isEmpty())
            assertFailsWith<IllegalArgumentException> { reports.save(manager.copy(state = ReportLifecycleState.APPROVED)) }
            assertFailsWith<IllegalArgumentException> {
                reports.act(manager.id, submitted.recordVersion, "finance", ApprovalAction.APPROVE, "Stale", FINANCE_ROLE)
            }
            val finance = reports.act(manager.id, manager.recordVersion, "finance", ApprovalAction.APPROVE, "Ready", FINANCE_ROLE)
            assertEquals(listOf(MANAGER_ROLE, FINANCE_ROLE), finance.approvalChain.steps.map { it.role })
            assertEquals(listOf(0, 1), finance.approvalChain.steps.map { it.stepIndex })
            assertEquals(ReportLifecycleState.PAID, payouts.pay(finance.id).state)
            val rows =
                db
                    .notificationDao()
                    .observeAll()
                    .first()
                    .filter { it.id.startsWith("report:report:") }
            assertEquals(5, rows.size)
            assertTrue(rows.any { it.title == "Finance review required" })
        }

    @Test
    fun `locked submission redirects to next open period and preserves original date after unlock`() =
        withStore { db, reports ->
            db.periodLockDao().upsert(PeriodLockEntity("2026-09", 1, "finance"))
            db.periodLockDao().upsert(PeriodLockEntity("2026-10", 1, "finance"))
            val submitted = submit(reports)
            val before = requireNotNull(reports.review(submitted.id))
            assertEquals("2026-11", before.accountingPeriodKey)
            assertEquals(instant.toEpochMilliseconds(), before.submittedAtMs)
            db.periodLockDao().unlock("2026-09")
            reports.act(submitted.id, submitted.recordVersion, "manager", ApprovalAction.SEND_BACK, "Fix receipt")
            reports.transition(submitted.id, ReportLifecycleEvent.RESUBMIT)
            val after = requireNotNull(reports.review(submitted.id))
            assertEquals(before.accountingPeriodKey, after.accountingPeriodKey)
            assertEquals(before.submittedAtMs, after.submittedAtMs)
            assertEquals(before.submittedAtMs, db.reportDao().get(submitted.id)?.submittedAtMs)
        }

    @Test
    fun `per line reject keeps remaining report approvable and rejected amount out of payout`() =
        withStore { db, reports ->
            val submitted = submit(reports)
            val rejected = reports.reviewLine(submitted.id, submitted.recordVersion, "manager", ApprovalAction.REJECT, "Duplicate", "first")
            assertEquals(ReportLifecycleState.SUBMITTED, rejected.state)
            assertEquals(2, rejected.lines.size)
            assertEquals(setOf("first"), reports.review(rejected.id)?.rejectedLineIds)
            assertFailsWith<IllegalArgumentException> { reports.transition(rejected.id, ReportLifecycleEvent.RECALL) }
            val manager = reports.act(rejected.id, rejected.recordVersion, "manager", ApprovalAction.APPROVE, "Approve rest")
            val finance = reports.act(manager.id, manager.recordVersion, "finance", ApprovalAction.APPROVE, "Ready", FINANCE_ROLE)
            val paid = ReportPayoutProcessor(reports, db.reportDao(), db.pendingPaymentJournalDao()).pay(finance.id)
            assertEquals(ReportLifecycleState.PAID, paid.state)
            assertEquals(
                1000L,
                db
                    .pendingPaymentJournalDao()
                    .getByReport(paid.id)
                    .single()
                    .amountMinor,
            )
            assertEquals(
                "first",
                db
                    .approvalStepDao()
                    .getByReport(paid.id)
                    .first()
                    .claimLineId,
            )
            assertEquals(2, reports.get(paid.id)?.lines?.size)
        }

    @Test
    fun `hard violation cannot be approved through repository even when selection is stale`() =
        withStore { _, reports ->
            val submitted = submit(reports, listOf("EXPENSE_OVER_MAX"))
            assertEquals(false, reports.review(submitted.id)?.canBulkApprove)
            assertFailsWith<IllegalArgumentException> {
                reports.act(submitted.id, submitted.recordVersion, "manager", ApprovalAction.APPROVE, "Bulk")
            }
            assertFailsWith<IllegalArgumentException> {
                reports.reviewLine(submitted.id, submitted.recordVersion, "manager", ApprovalAction.APPROVE, "Line", "first")
            }
            val rejected = reports.reviewLine(submitted.id, submitted.recordVersion, "manager", ApprovalAction.REJECT, "Hard violation", "first")
            assertEquals(true, reports.review(rejected.id)?.canBulkApprove)
            assertEquals(ReportLifecycleState.SUBMITTED, reports.act(rejected.id, rejected.recordVersion, "manager", ApprovalAction.APPROVE, "Rest").state)
        }
}
