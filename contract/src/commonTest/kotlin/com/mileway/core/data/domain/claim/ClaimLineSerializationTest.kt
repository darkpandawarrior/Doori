package com.mileway.core.data.domain.claim

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the `"type"` discriminator's wire value for each [ClaimLine] subtype — a plain
 * encode-then-decode round trip can't catch a `@SerialName` rename (both sides change together),
 * so this asserts the literal JSON the way ContractSerializationRoundTripTest does for the other
 * wire DTOs.
 */
class ClaimLineSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun expenseLineDiscriminatorIsExpense() {
        val line: ClaimLine = ExpenseLine(id = "line-1", amountMinor = 45000, currency = "INR", merchant = "Cafe", category = "MEALS")
        val expectedJson =
            """{"type":"expense","id":"line-1","amountMinor":45000,"currency":"INR","merchant":"Cafe","category":"MEALS"}"""

        assertEquals(expectedJson, json.encodeToString(line))
        assertEquals(line, json.decodeFromString<ClaimLine>(expectedJson))
    }

    @Test
    fun mileageLineDiscriminatorIsMileage() {
        val line: ClaimLine = MileageLine(id = "line-2", amountMinor = 12000, currency = "INR", distanceKm = 12.5, vehicleKey = "twoWheeler")
        val expectedJson =
            """{"type":"mileage","id":"line-2","amountMinor":12000,"currency":"INR","distanceKm":12.5,"vehicleKey":"twoWheeler"}"""

        assertEquals(expectedJson, json.encodeToString(line))
        assertEquals(line, json.decodeFromString<ClaimLine>(expectedJson))
    }

    @Test
    fun reportRoundTripsMixedLines() {
        val report =
            Report(
                id = "report-1",
                employeeId = "emp-1",
                lines =
                    listOf(
                        ExpenseLine(id = "line-1", amountMinor = 45000, currency = "INR", merchant = "Cafe", category = "MEALS"),
                        MileageLine(id = "line-2", amountMinor = 12000, currency = "INR", distanceKm = 12.5, vehicleKey = "twoWheeler"),
                    ),
            )

        val encoded = json.encodeToString(report)
        val decoded = json.decodeFromString<Report>(encoded)

        assertEquals(report, decoded)
        assertEquals(57000L, report.totalAmountMinor())
    }
}
