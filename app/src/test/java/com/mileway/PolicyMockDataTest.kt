package com.mileway

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.network.model.ViolationSeverity
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.PolicyMockData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the deterministic policy-engine mock: bucket boundaries, determinism,
 * violation gating, and id format stability.
 */
class PolicyMockDataTest {

    private val transactionIdFormat = Regex("^O-INDIAN-\\d{9}$")
    private val voucherNumberFormat = Regex("^VCH-\\d{6}$")
    private val gstinFormat = Regex("^\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")

    // ── Bucket boundaries ─────────────────────────────────────────────────────

    @Test
    fun `outcome buckets map distances to documented statuses`() {
        assertEquals(SubmissionStatus.SUCCESS, PolicyMockData.outcomeFor(0.0))
        assertEquals(SubmissionStatus.SUCCESS, PolicyMockData.outcomeFor(4.99))
        assertEquals(SubmissionStatus.REIMBURSABLE_ADJUSTED, PolicyMockData.outcomeFor(5.0))
        assertEquals(SubmissionStatus.REIMBURSABLE_ADJUSTED, PolicyMockData.outcomeFor(9.99))
        assertEquals(SubmissionStatus.POLICY_VIOLATION, PolicyMockData.outcomeFor(10.0))
        assertEquals(SubmissionStatus.POLICY_VIOLATION, PolicyMockData.outcomeFor(14.99))
        assertEquals(SubmissionStatus.NEEDS_APPROVAL, PolicyMockData.outcomeFor(15.0))
        assertEquals(SubmissionStatus.NEEDS_APPROVAL, PolicyMockData.outcomeFor(40.0))
        assertEquals(SubmissionStatus.HARD_STOP, PolicyMockData.outcomeFor(40.01))
        assertEquals(SubmissionStatus.HARD_STOP, PolicyMockData.outcomeFor(120.0))
    }

    @Test
    fun `negative distance falls into success bucket`() {
        assertEquals(SubmissionStatus.SUCCESS, PolicyMockData.outcomeFor(-1.0))
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same input twice produces identical voucher and transaction`() {
        val v1 = PolicyMockData.voucherFor(12.4, "token-alpha")
        val v2 = PolicyMockData.voucherFor(12.4, "token-alpha")
        assertEquals(v1, v2)

        val t1 = PolicyMockData.transactionFor(12.4, "token-alpha")
        val t2 = PolicyMockData.transactionFor(12.4, "token-alpha")
        assertEquals(t1, t2)
    }

    @Test
    fun `different tokens produce different ids`() {
        val alpha = PolicyMockData.transactionFor(12.4, "token-alpha")
        val beta = PolicyMockData.transactionFor(12.4, "token-beta")
        assertNotNull(alpha)
        assertNotNull(beta)
        assertNotEquals(alpha.id, beta.id)
    }

    @Test
    fun `different distances produce different ids for same token`() {
        val short = PolicyMockData.transactionFor(2.0, "token-alpha")
        val long = PolicyMockData.transactionFor(25.0, "token-alpha")
        assertNotNull(short)
        assertNotNull(long)
        assertNotEquals(short.id, long.id)
    }

    @Test
    fun `null and blank token fall back to the same default seed`() {
        assertEquals(
            PolicyMockData.transactionFor(7.0, null),
            PolicyMockData.transactionFor(7.0, "")
        )
    }

    // ── Violation gating ──────────────────────────────────────────────────────

    @Test
    fun `violations populated only for violation statuses`() {
        val samples = listOf(1.0, 4.99, 5.0, 7.5, 10.0, 12.4, 14.99, 15.0, 40.0, 40.01, 99.0)
        for (distance in samples) {
            val status = PolicyMockData.outcomeFor(distance)
            val violations = PolicyMockData.violationsFor(distance)
            val isViolationStatus =
                status == SubmissionStatus.POLICY_VIOLATION || status == SubmissionStatus.HARD_STOP
            assertEquals(
                isViolationStatus,
                violations.isNotEmpty(),
                "distance=$distance status=$status should ${if (isViolationStatus) "" else "not "}carry violations"
            )
        }
    }

    @Test
    fun `policy violation bucket carries max-distance-per-day violation`() {
        val violations = PolicyMockData.violationsFor(12.4)
        assertEquals(1, violations.size)
        assertEquals("max-distance-per-day", violations.first().id)
        assertEquals(ViolationSeverity.VIOLATION, violations.first().severity)
        assertTrue(violations.first().title.isNotBlank())
        assertTrue(violations.first().message.isNotBlank())
    }

    @Test
    fun `hard stop bucket carries hardstop severity violation`() {
        val violations = PolicyMockData.violationsFor(50.0)
        assertEquals(1, violations.size)
        assertEquals("max-trip-distance-hard-stop", violations.first().id)
        assertEquals(ViolationSeverity.HARDSTOP, violations.first().severity)
    }

    // ── Voucher / transaction factories ───────────────────────────────────────

    @Test
    fun `hard stop produces no voucher and no transaction`() {
        assertNull(PolicyMockData.voucherFor(50.0, "token"))
        assertNull(PolicyMockData.transactionFor(50.0, "token"))
    }

    @Test
    fun `non hard stop buckets produce voucher and transaction`() {
        for (distance in listOf(2.0, 7.0, 12.4, 25.0)) {
            assertNotNull(PolicyMockData.voucherFor(distance, "token"), "voucher for $distance km")
            assertNotNull(PolicyMockData.transactionFor(distance, "token"), "transaction for $distance km")
        }
    }

    @Test
    fun `id formats are stable`() {
        val transaction = PolicyMockData.transactionFor(12.4, "token-alpha")
        assertNotNull(transaction)
        assertTrue(
            transactionIdFormat.matches(transaction.id),
            "transaction id '${transaction.id}' should match $transactionIdFormat"
        )

        val voucher = PolicyMockData.voucherFor(12.4, "token-alpha")
        assertNotNull(voucher)
        assertTrue(
            voucherNumberFormat.matches(voucher.number),
            "voucher number '${voucher.number}' should match $voucherNumberFormat"
        )
        assertTrue(voucher.id > 0L)
    }

    @Test
    fun `amounts are capped at the daily policy ceiling`() {
        val withinCap = PolicyMockData.voucherFor(4.0, "token")
        assertNotNull(withinCap)
        assertEquals(4.0 * PolicyMockData.RATE_PER_KM, withinCap.amount, 0.001)

        val aboveCap = PolicyMockData.voucherFor(25.0, "token")
        assertNotNull(aboveCap)
        assertEquals(
            PolicyMockData.MAX_REIMBURSABLE_KM_PER_DAY * PolicyMockData.RATE_PER_KM,
            aboveCap.amount,
            0.001
        )
    }

    @Test
    fun `transaction timestamp is derived from the fixed demo epoch`() {
        val transaction = PolicyMockData.transactionFor(7.0, "token")
        assertNotNull(transaction)
        assertTrue(transaction.createdAtMillis >= PolicyMockData.DEMO_EPOCH_MILLIS)
        assertTrue(transaction.createdAtMillis < PolicyMockData.DEMO_EPOCH_MILLIS + 86_400_000L)
    }

    // ── Offices & entities ────────────────────────────────────────────────────

    @Test
    fun `offices expose the four reference codes across two cities`() {
        val offices = PolicyMockData.offices()
        assertEquals(4, offices.size)
        assertEquals(setOf("1345", "1347", "1349", "5356"), offices.map { it.code }.toSet())
        assertEquals(2, offices.count { it.address.contains("Pune") })
        assertEquals(2, offices.count { it.address.contains("Mumbai") })
        for (office in offices) {
            assertTrue(office.name.isNotBlank())
            assertTrue(
                gstinFormat.matches(office.gstin),
                "gstin '${office.gstin}' should match $gstinFormat"
            )
        }
    }

    @Test
    fun `business entities cover at least six entities with mixed currencies`() {
        val entities = PolicyMockData.businessEntities()
        assertTrue(entities.size >= 6)
        assertTrue(entities.map { it.currencySymbol }.toSet().size >= 4)
        assertTrue(entities.all { it.name.isNotBlank() && it.country.isNotBlank() })
    }

    @Test
    fun `voucher declaration requires acknowledgement`() {
        val declaration = PolicyMockData.voucherDeclaration()
        assertTrue(declaration.text.isNotBlank())
        assertTrue(declaration.requiresAcknowledgement)
    }

    // ── Response enrichment ───────────────────────────────────────────────────

    @Test
    fun `enrich preserves all base fields and populates policy fields`() {
        val base = ExpenseSubmissionResponse(
            status = 1,
            reimbursableAmount = 124.0,
            distance = 12.4,
            message = "Journey submitted successfully",
            transId = "TXN-FIXED"
        )
        val enriched = PolicyMockData.enrich(base, distanceKm = 12.4, token = "token-alpha")

        assertEquals(base.status, enriched.status)
        assertEquals(base.reimbursableAmount, enriched.reimbursableAmount)
        assertEquals(base.distance, enriched.distance)
        assertEquals(base.message, enriched.message)
        assertEquals(base.transId, enriched.transId)

        assertEquals(SubmissionStatus.POLICY_VIOLATION, enriched.submissionStatus)
        assertTrue(enriched.violations.isNotEmpty())
        assertNotNull(enriched.issuedVoucher)
        assertNotNull(enriched.transaction)
    }

    @Test
    fun `enrich is deterministic for the same input`() {
        val base = ExpenseSubmissionResponse(status = 1, distance = 7.0)
        val first = PolicyMockData.enrich(base, distanceKm = 7.0, token = "token")
        val second = PolicyMockData.enrich(base, distanceKm = 7.0, token = "token")
        assertEquals(first, second)
    }

    // ── Fake API wiring ───────────────────────────────────────────────────────

    @Test
    fun `submitMiles enriches response by distance bucket`() = runTest {
        val api = FakeTrackingNetworkApi()

        val success = api.submitMiles(SubmitMilesRequestK(token = "tok", distance = 2.0))
        assertEquals(SubmissionStatus.SUCCESS, success.submissionStatus)
        assertNotNull(success.issuedVoucher)
        assertNotNull(success.transaction)
        assertTrue(transactionIdFormat.matches(success.transaction!!.id))

        val hardStop = api.submitMiles(SubmitMilesRequestK(token = "tok", distance = 50.0))
        assertEquals(SubmissionStatus.HARD_STOP, hardStop.submissionStatus)
        assertTrue(hardStop.violations.isNotEmpty())
        assertNull(hardStop.issuedVoucher)
        assertNull(hardStop.transaction)
    }

    @Test
    fun `logMiles enriches response by distance bucket`() = runTest {
        val api = FakeTrackingNetworkApi()
        val adjusted = api.logMiles(LogMilesSubmitRequestV2(vehicleType = "twoWheeler", distance = 7.0))
        assertEquals(SubmissionStatus.REIMBURSABLE_ADJUSTED, adjusted.submissionStatus)
        assertTrue(adjusted.violations.isEmpty())
        assertNotNull(adjusted.issuedVoucher)
        // Existing behaviour untouched: legacy fields still populated by DemoMockData.
        assertEquals(1, adjusted.status)
        assertEquals(7.0, adjusted.distance, 0.001)
    }
}
