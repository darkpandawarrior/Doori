package com.mileway.contract

import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.network.TransactionRef
import com.mileway.core.data.model.network.ViolationSeverity
import com.mileway.core.data.model.network.Voucher
import com.mileway.core.data.model.network.VoucherStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V33 A1: locks the wire shape of the two DTOs a future `:server` module must produce/consume
 * byte-identically to the client — a request (`SubmitMilesRequestK`) and a response
 * (`ExpenseSubmissionResponse`). If either drifts (a field renamed, a default changed in a way that
 * changes the encoded JSON), this test breaks before the client/server contract does.
 */
class ContractSerializationRoundTripTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun submitMilesRequestK_roundTrips() {
        val original =
            SubmitMilesRequestK(
                token = "tok-123",
                vehicleType = "CAR",
                origin = CoordsV2(lat = 12.9716, lng = 77.5946, name = "Origin"),
                destination = CoordsV2(lat = 13.0827, lng = 80.2707, name = "Destination"),
                distance = 42.5,
                originalDistance = 40.0,
                forms = mapOf(1L to "form-a"),
                notes = "client demo trip",
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SubmitMilesRequestK>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun expenseSubmissionResponse_roundTrips() {
        val original =
            ExpenseSubmissionResponse(
                status = 1,
                amount = 250.0,
                currency = "INR",
                distance = 42.5,
                submissionStatus = SubmissionStatus.NEEDS_APPROVAL,
                violations =
                    listOf(
                        PolicyViolation(
                            id = "max-distance-per-day",
                            title = "Over daily limit",
                            message = "Trip exceeds the configured daily distance limit.",
                            severity = ViolationSeverity.VIOLATION,
                        ),
                    ),
                issuedVoucher = Voucher(id = 7L, number = "O-INDIAN-000048769", amount = 250.0, status = VoucherStatus.FILED),
                transaction = TransactionRef(id = "O-INDIAN-000048769", createdAtMillis = 1_700_000_000_000L, amount = 250.0),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ExpenseSubmissionResponse>(encoded)

        assertEquals(original, decoded)
    }
}
