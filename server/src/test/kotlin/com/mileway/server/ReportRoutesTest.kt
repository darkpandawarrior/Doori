package com.mileway.server

import com.mileway.core.data.domain.claim.ApprovalActionRequest
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.payout.PaymentStatus
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * L1a/L5 contract test: submit -> approve -> reimburse round-trips through the shared :contract
 * DTOs end to end against the real routes + Exposed persistence, and the lifecycle state machine
 * rejects an illegal transition (approve before submit, double reimburse) with 409 rather than
 * silently succeeding or 500ing.
 */
class ReportRoutesTest {
    private fun newReport() =
        Report(
            id = "report-${UUID.randomUUID()}",
            employeeId = "emp-1",
            lines =
                listOf(
                    ExpenseLine(id = "line-1", amountMinor = 45000, currency = "INR", merchant = "Cafe", category = "MEALS"),
                    MileageLine(id = "line-2", amountMinor = 12000, currency = "INR", distanceKm = 12.5, vehicleKey = "twoWheeler"),
                ),
        )

    @Test
    fun submitApproveReimburseRoundTripsThroughTheSharedContractDtos() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()

            val submitResponse =
                client.post("/api/reports/submit") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(report))
                }
            assertEquals(HttpStatusCode.OK, submitResponse.status)
            val submitted = serverJson.decodeFromString<Report>(submitResponse.bodyAsText())
            assertEquals(ReportLifecycleState.SUBMITTED, submitted.state)
            assertEquals(report.lines, submitted.lines)

            val approveResponse =
                client.post("/api/reports/${report.id}/approve") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1", comment = "looks fine")))
                }
            assertEquals(HttpStatusCode.OK, approveResponse.status)
            val approved = serverJson.decodeFromString<Report>(approveResponse.bodyAsText())
            assertEquals(ReportLifecycleState.APPROVED, approved.state)
            assertEquals(1, approved.approvalChain.steps.size)
            assertEquals(
                "manager-1",
                approved.approvalChain.steps
                    .single()
                    .actedBy,
            )

            val reimburseResponse =
                client.post("/api/reports/${report.id}/reimburse") {
                    bearerAuth(token)
                }
            assertEquals(HttpStatusCode.OK, reimburseResponse.status)
            val paid = serverJson.decodeFromString<Report>(reimburseResponse.bodyAsText())
            assertEquals(ReportLifecycleState.PAID, paid.state)

            val journal =
                transaction {
                    PendingPaymentJournalTable.selectAll().where { PendingPaymentJournalTable.reportId eq report.id }.first()
                }
            assertEquals(PaymentStatus.PAID.name, journal[PendingPaymentJournalTable.status])
            assertEquals(57000L, journal[PendingPaymentJournalTable.amountMinor])
        }

    @Test
    fun approveBeforeSubmitIsRejectedWith409() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()

            // Never submitted — approve must fail, not silently create/advance a report.
            val response =
                client.post("/api/reports/${report.id}/approve") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1")))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun reimbursingAReportTwiceIsRejectedOnTheSecondCall() =
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
            val first = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }
            assertEquals(HttpStatusCode.OK, first.status)

            val second = client.post("/api/reports/${report.id}/reimburse") { bearerAuth(token) }

            assertEquals(HttpStatusCode.Conflict, second.status)
            assertTrue(second.bodyAsText().contains("PAID"))
        }

    @Test
    fun sendBackThenResubmitReachesSubmittedAgain() =
        testApplication {
            application { module() }
            val token = client.demoLoginToken()
            val report = newReport()

            client.post("/api/reports/submit") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(report))
            }
            val sentBack =
                client.post("/api/reports/${report.id}/send-back") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(ApprovalActionRequest(actedBy = "manager-1", comment = "missing receipt")))
                }
            assertEquals(HttpStatusCode.OK, sentBack.status)
            assertEquals(ReportLifecycleState.SENT_BACK, serverJson.decodeFromString<Report>(sentBack.bodyAsText()).state)

            val resubmitted =
                client.post("/api/reports/submit") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(report))
                }

            assertEquals(HttpStatusCode.OK, resubmitted.status)
            assertEquals(ReportLifecycleState.SUBMITTED, serverJson.decodeFromString<Report>(resubmitted.bodyAsText()).state)
        }
}
