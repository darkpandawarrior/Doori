package com.mileway.core.data.claim

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.PaymentStatus
import com.mileway.core.data.domain.payout.PendingPaymentJournal
import com.mileway.core.data.domain.payout.SimulatedPayoutBackend
import com.siddharth.kmp.offlineoutbox.OpOutbox
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

class ReportPayoutRecoveryTest {
    private fun open(file: File): MilewayDatabase =
        Room.databaseBuilder<MilewayDatabase>(name = file.path)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

    private fun repository(db: MilewayDatabase, outbox: OpOutbox) =
        ReportRepository(db.reportDao(), db.claimLineDao(), db.approvalStepDao(), outbox, Json, database = db)

    @Test
    fun `kill before call or after simulation then relaunch loses no payout and creates no second receipt`() = runBlocking {
        val reportFile = File.createTempFile("l5-reports", ".db")
        val outboxFile = File.createTempFile("l5-outbox", ".db")
        val outboxDb = Room.databaseBuilder<OutboxDatabase>(name = outboxFile.path)
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default).build()
        val outbox = RoomOpOutbox(outboxDb.opOutboxDao())
        try {
            for (afterCall in listOf(false, true)) {
                val id = if (afterCall) "expense" else "mileage"
                val firstProcess = open(reportFile)
                val reports = repository(firstProcess, outbox)
                val line = if (afterCall) {
                    ExpenseLine("line-$id", 5000, "INR", merchant = "Cafe", category = "Meals")
                } else {
                    MileageLine("line-$id", 5000, "INR", distanceKm = 12.0, vehicleKey = "car")
                }
                val draft = reports.save(Report(id, "employee", lines = listOf(line)))
                val submitted = reports.save(draft.copy(state = ReportLifecycleState.SUBMITTED))
                reports.act(id, submitted.recordVersion, "manager", ApprovalAction.APPROVE, "Checked")
                reports.transition(id, ReportLifecycleEvent.RELEASE_FOR_PAYMENT)
                val pending = firstProcess.pendingPaymentJournalDao().getByReport(id).single()
                assertEquals(PaymentStatus.PENDING.name, pending.status) // committed BEFORE any call
                val request = PendingPaymentJournal(id, pending.amountMinor, pending.currency, createdAtMillis = pending.createdAtMs)
                val interruptedReceipt = if (afterCall) SimulatedPayoutBackend().payout(request) else null
                firstProcess.close() // kill at either side of the call, before acknowledgment

                val secondProcess = open(reportFile)
                val restored = repository(secondProcess, outbox)
                val payouts = ReportPayoutProcessor(restored, secondProcess.reportDao(), secondProcess.pendingPaymentJournalDao())
                assertTrue(payouts.recover().isEmpty())
                payouts.pay(id) // duplicate retry after paid must also be a no-op
                assertEquals(ReportLifecycleState.PAID, restored.get(id)?.state)
                val paid = secondProcess.pendingPaymentJournalDao().getByReport(id).single()
                assertEquals(pending.id, paid.id)
                assertEquals(PaymentStatus.PAID.name, paid.status)
                assertEquals(5000L, paid.amountMinor)
                if (interruptedReceipt != null) assertEquals(interruptedReceipt, SimulatedPayoutBackend().payout(request))
                val notifications = secondProcess.notificationDao().observeAll().first().filter { it.id.startsWith("report:$id:") }
                assertEquals(4, notifications.size) // submitted, approved, ready, paid, once each
                val step = restored.get(id)!!.approvalChain.steps.single()
                assertEquals("manager", step.actedBy)
                assertEquals("Checked", step.comment)
                secondProcess.close()
            }
        } finally {
            outboxDb.close()
            reportFile.delete()
            outboxFile.delete()
        }
    }

    @Test
    fun `reject send back recall and stale approval are persisted without extra notifications`() = runBlocking {
        val reportFile = File.createTempFile("l5-actions", ".db")
        val outboxFile = File.createTempFile("l5-outbox", ".db")
        val db = open(reportFile)
        val outboxDb = Room.databaseBuilder<OutboxDatabase>(name = outboxFile.path)
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default).build()
        try {
            val reports = repository(db, RoomOpOutbox(outboxDb.opOutboxDao()))
            for (action in listOf(ApprovalAction.SEND_BACK, ApprovalAction.REJECT)) {
                val draft = reports.save(Report(action.name, "employee"))
                val submitted = reports.save(draft.copy(state = ReportLifecycleState.SUBMITTED))
                assertFailsWith<IllegalArgumentException> { reports.act(draft.id, submitted.recordVersion, "employee", action, "Self") }
                val acted = reports.act(draft.id, submitted.recordVersion, "manager", action, "Explain")
                assertEquals(action, acted.approvalChain.steps.single().action)
                assertEquals("Explain", acted.approvalChain.steps.single().comment)
                assertFailsWith<IllegalArgumentException> { reports.act(draft.id, submitted.recordVersion, "manager", action, "Duplicate") }
                assertFailsWith<IllegalArgumentException> { reports.transition(draft.id, ReportLifecycleEvent.RECALL) }
                assertEquals(2, db.notificationDao().observeAll().first().count { it.id.startsWith("report:${draft.id}:") })
            }
            val draft = reports.save(Report("recall", "employee"))
            reports.save(draft.copy(state = ReportLifecycleState.SUBMITTED))
            reports.transition("recall", ReportLifecycleEvent.RECALL)
            assertEquals(ReportLifecycleState.RECALLED, reports.get("recall")?.state)
            assertEquals(2, db.notificationDao().observeAll().first().count { it.id.startsWith("report:recall:") })
        } finally {
            db.close()
            outboxDb.close()
            reportFile.delete()
            outboxFile.delete()
        }
    }
}
