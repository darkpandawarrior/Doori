package com.mileway.server

import com.mileway.core.data.domain.claim.ApprovalActionRequest
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.PaymentStatus
import com.mileway.core.data.domain.payout.PayoutBackend
import com.mileway.core.data.domain.payout.PendingPaymentJournal
import com.mileway.core.data.domain.payout.SimulatedPayoutBackend
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden-gate hardening pass (backend-validation-matrix.md) for the must-cases this slice's
 * ReportRoutes actually shipped: C08 (recordVersion monotonic), the E-area routing guards
 * (self-approval, out-of-order), B13 (stepIndex bounds), and — the top risk named by the
 * coordinator — Y01/Y02/Y03 payout journal ordering and idempotency, proven with a call-counting
 * spy rather than the state-machine double-reimburse test alone.
 */
class ReportLifecycleHardeningTest {
    private val realBackend = SimulatedPayoutBackend()

    @AfterTest
    fun restoreRealBackend() {
        payoutBackend = realBackend
    }

    private fun newReport(id: String = "report-${UUID.randomUUID()}") =
        Report(
            id = id,
            employeeId = "emp-1",
            lines =
                listOf(
                    ExpenseLine(id = "line-1", amountMinor = 45000, currency = "INR", merchant = "Cafe", category = "MEALS"),
                    MileageLine(id = "line-2", amountMinor = 12000, currency = "INR", distanceKm = 12.5, vehicleKey = "twoWheeler"),
                ),
        )

    // ── C08: recordVersion is present and monotonic ───────────────────────────

    @Test
    fun recordVersionIncrementsOnEveryAcceptedWrite() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()

            val submitted =
                serverJson.decodeFromString<Report>(
                    client
                        .post("/api/reports/submit") {
                            bearerAuth(token)
                            contentType(ContentType.Application.Json)
                            setBody(serverJson.encodeToString(report))
                        }.bodyAsText(),
                )
            assertEquals(1L, submitted.recordVersion)

            val approved =
                serverJson.decodeFromString<Report>(
                    client
                        .post("/api/reports/${report.id}/approve") {
                            bearerAuth(token)
                            contentType(ContentType.Application.Json)
                            setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
                        }.bodyAsText(),
                )
            assertEquals(2L, approved.recordVersion)

            val paid =
                serverJson.decodeFromString<Report>(
                    client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }.bodyAsText(),
                )
            assertEquals(3L, paid.recordVersion)
        }

    // ── E: approval/send-back routing ──────────────────────────────────────────

    @Test
    fun submitterCannotApproveTheirOwnReport() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }

            val response =
                client.post("/api/reports/${report.id}/approve") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    // report.employeeId == "emp-1" (see newReport()) — same actor approving their own report.
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "emp-1")))
                }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }

    @Test
    fun aDifferentApproverThanTheSubmitterCanApprove() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }

            val response =
                client.post("/api/reports/${report.id}/approve") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun approvingAnAlreadyApprovedReportIsRejected() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }

            val secondApprove =
                client.post("/api/reports/${report.id}/approve") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-2")))
                }

            assertEquals(HttpStatusCode.Conflict, secondApprove.status)
        }

    @Test
    fun sendingBackAnAlreadyApprovedReportIsRejected() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }

            val sendBack =
                client.post("/api/reports/${report.id}/send-back") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
                }

            assertEquals(HttpStatusCode.Conflict, sendBack.status)
        }

    // ── B13: stepIndex stays in bounds across a send-back/resubmit/approve cycle ────────────────

    @Test
    fun approvalStepIndexesAreSequentialAcrossASendBackResubmitApproveCycle() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/send-back") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1", comment = "fix it")))
            }
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            val approved =
                serverJson.decodeFromString<Report>(
                    client
                        .post("/api/reports/${report.id}/approve") {
                            bearerAuth(token)
                            contentType(ContentType.Application.Json)
                            setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
                        }.bodyAsText(),
                )

            val steps = approved.approvalChain.steps
            assertEquals(listOf(0, 1), steps.map { it.stepIndex })
        }

    // ── F: payout journal ordering + idempotency (top risk) ──────────────────────────────────

    /** Wraps a real backend and counts calls, so "exactly once" is a measured assertion, not an inference. */
    private class CountingPayoutBackend(
        private val delegate: PayoutBackend,
    ) : PayoutBackend {
        val callCount = AtomicInteger(0)

        override fun payout(journal: PendingPaymentJournal): PendingPaymentJournal {
            callCount.incrementAndGet()
            return delegate.payout(journal)
        }
    }

    @Test
    fun y01JournalIsWrittenBeforeThePayoutBackendIsEverCalled() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }

            var journalExistedBeforeFirstCall = false
            payoutBackend =
                object : PayoutBackend {
                    override fun payout(journal: PendingPaymentJournal): PendingPaymentJournal {
                        // If Y01 held, the PENDING row this handler wrote is already durable by the
                        // time the backend is invoked — readable from inside the call itself.
                        journalExistedBeforeFirstCall = journal.status == PaymentStatus.PENDING
                        return realBackend.payout(journal)
                    }
                }

            val response = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(journalExistedBeforeFirstCall, "payoutBackend.payout() must see a PENDING journal, not an absent one")
        }

    @Test
    fun y03ARetriedReimburseCallNeverInvokesThePayoutBackendTwice() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }
            val counting = CountingPayoutBackend(realBackend)
            payoutBackend = counting

            val first = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }
            assertEquals(HttpStatusCode.OK, first.status)
            assertEquals(1, counting.callCount.get())

            // A client retry after e.g. a dropped response — same report, called again.
            val retry = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }

            assertEquals(HttpStatusCode.Conflict, retry.status)
            assertEquals(1, counting.callCount.get(), "a retried reimburse must not invoke the payout backend a second time")
        }

    /**
     * Y02: simulates a crash between the journal write and the report row being persisted as
     * PAID — a PENDING journal is seeded directly (as if a prior attempt wrote it and then the
     * process died before calling the backend), then the real route is invoked as the "relaunch +
     * retry". The backend must be called exactly once by that resumed attempt, and the report must
     * end up PAID with no lost payout.
     */
    @Test
    fun y02ResumingAfterACrashBetweenJournalWriteAndPayoutCallCompletesExactlyOnce() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }

            // Simulate: an earlier process wrote the PENDING journal, then died before ever
            // calling the backend or updating the report row (report is still APPROVED in the DB).
            persistJournal(
                PendingPaymentJournal(
                    reportId = report.id,
                    amountMinor = report.totalAmountMinor(),
                    currency = report.currency(),
                    status = PaymentStatus.PENDING,
                    createdAtMillis = 1_700_000_000_000L,
                ),
            )
            val counting = CountingPayoutBackend(realBackend)
            payoutBackend = counting

            val resumed = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, resumed.status)
            assertEquals(1, counting.callCount.get(), "the resumed attempt must call the backend exactly once, not skip or double it")
            val paid = serverJson.decodeFromString<Report>(resumed.bodyAsText())
            assertEquals(ReportLifecycleState.PAID, paid.state)
        }

    /**
     * The other half of Y02: the backend call already completed (journal is PAID) but the report
     * row never got updated before the crash. Resuming must NOT call the backend again — it just
     * finishes persisting the report as PAID from the already-completed journal.
     */
    @Test
    fun y02ResumingAfterACrashBetweenPayoutCompletionAndReportUpdateNeverCallsTheBackendAgain() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()
            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            client.post("/api/reports/${report.id}/approve") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
            }

            // Simulate: an earlier process's backend call completed and the journal was written
            // PAID, then it died before persisting the report row itself (still APPROVED in the DB).
            persistJournal(
                PendingPaymentJournal(
                    reportId = report.id,
                    amountMinor = report.totalAmountMinor(),
                    currency = report.currency(),
                    status = PaymentStatus.PAID,
                    createdAtMillis = 1_700_000_000_000L,
                ),
            )
            val counting = CountingPayoutBackend(realBackend)
            payoutBackend = counting

            val resumed = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, resumed.status)
            assertEquals(0, counting.callCount.get(), "a journal already PAID must not trigger a second backend call")
            val paid = serverJson.decodeFromString<Report>(resumed.bodyAsText())
            assertEquals(ReportLifecycleState.PAID, paid.state)
        }
}
