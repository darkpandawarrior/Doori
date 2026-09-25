package com.mileway.core.data.domain.claim

import com.mileway.core.data.domain.payout.PaymentStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Golden-gate hardening (backend-validation-matrix.md C02/C04/C06): every enum this slice puts on
 * the wire is asserted against its literal `@SerialName` string, not just round-tripped — a plain
 * round trip can't catch a rename because both encode and decode sides change together. C02's
 * stated risk is real: [ReportLifecycleState]/[ApprovalAction]/[PaymentStatus] values get persisted
 * in Room rows on real devices (a later `:core:data` lane), so the wire form is one-way once that
 * ships. C04 proves an unrecognized [ClaimLine] discriminator fails loudly. C06 proves money never
 * touches a floating-point wire representation.
 */
class WireStabilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    // ── C02: enum wire values survive a Kotlin symbol rename ─────────────────

    @Test
    fun reportLifecycleStateWireValuesAreStable() {
        val expected =
            mapOf(
                ReportLifecycleState.DRAFT to "\"draft\"",
                ReportLifecycleState.SUBMITTED to "\"submitted\"",
                ReportLifecycleState.APPROVED to "\"approved\"",
                ReportLifecycleState.SENT_BACK to "\"sent_back\"",
                ReportLifecycleState.PAID to "\"paid\"",
            )
        expected.forEach { (state, wireForm) ->
            assertEquals(wireForm, json.encodeToString(ReportLifecycleState.serializer(), state))
            assertEquals(state, json.decodeFromString(ReportLifecycleState.serializer(), wireForm))
        }
    }

    @Test
    fun approvalActionWireValuesAreStable() {
        val expected =
            mapOf(
                ApprovalAction.APPROVE to "\"approve\"",
                ApprovalAction.SEND_BACK to "\"send_back\"",
            )
        expected.forEach { (action, wireForm) ->
            assertEquals(wireForm, json.encodeToString(ApprovalAction.serializer(), action))
            assertEquals(action, json.decodeFromString(ApprovalAction.serializer(), wireForm))
        }
    }

    @Test
    fun paymentStatusWireValuesAreStable() {
        val expected =
            mapOf(
                PaymentStatus.PENDING to "\"pending\"",
                PaymentStatus.PAID to "\"paid\"",
            )
        expected.forEach { (status, wireForm) ->
            assertEquals(wireForm, json.encodeToString(PaymentStatus.serializer(), status))
            assertEquals(status, json.decodeFromString(PaymentStatus.serializer(), wireForm))
        }
    }

    @Test
    fun claimLineDiscriminatorValuesAreStable() {
        // Re-asserted here alongside the other enum/discriminator wire values (ClaimLineSerializationTest
        // covers the full-object literal JSON; this is the discriminator-string half of C02 specifically).
        val expense: ClaimLine = ExpenseLine(id = "l1", amountMinor = 100, currency = "INR", merchant = "m", category = "c")
        val mileage: ClaimLine = MileageLine(id = "l2", amountMinor = 100, currency = "INR", distanceKm = 1.0, vehicleKey = "v")

        assertTrue(json.encodeToString(expense).contains("\"type\":\"expense\""))
        assertTrue(json.encodeToString(mileage).contains("\"type\":\"mileage\""))
    }

    // ── C04: an unrecognized ClaimLine discriminator fails explicitly, never a silent drop ─────

    @Test
    fun unknownClaimLineDiscriminatorFailsExplicitly() {
        val badJson = """{"type":"per_diem","id":"l3","amountMinor":100,"currency":"INR"}"""

        assertFailsWith<SerializationException> {
            json.decodeFromString<ClaimLine>(badJson)
        }
    }

    @Test
    fun unknownClaimLineDiscriminatorInsideAReportFailsExplicitlyRatherThanDroppingTheLine() {
        val badJson =
            """{"id":"r1","employeeId":"emp-1","lines":[{"type":"advance","id":"l3","amountMinor":100,"currency":"INR"}],""" +
                """"state":"draft","approvalChain":{"steps":[]},"recordVersion":0}"""

        assertFailsWith<SerializationException> {
            json.decodeFromString<Report>(badJson)
        }
    }

    // ── C06: money is integer minor units on the wire, never a float ───────────────────────────

    @Test
    fun amountMinorSerializesAsAnIntegerNeverADecimal() {
        val line: ClaimLine = ExpenseLine(id = "l1", amountMinor = 45000, currency = "INR", merchant = "Cafe", category = "MEALS")

        val encoded = json.encodeToString(line)

        assertTrue(encoded.contains("\"amountMinor\":45000") && !encoded.contains("45000.0"))
    }
}
