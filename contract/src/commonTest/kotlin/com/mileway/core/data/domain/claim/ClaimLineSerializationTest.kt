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
    fun typedExpenseDetailsRoundTripAndOldPayloadDefaultsStayEmpty() {
        val oldJson = """{"type":"expense","id":"old","amountMinor":1001,"currency":"INR","merchant":"Cafe","category":"FOOD"}"""
        val old = json.decodeFromString<ClaimLine>(oldJson) as ExpenseLine
        assertEquals(null, old.incurredOn)
        assertEquals(emptyList(), old.splits)
        assertEquals(emptyList(), old.attendees)
        assertEquals(emptyList(), old.itemized)
        val detailed: ClaimLine =
            old.copy(
                splits = listOf(CostSplit(SplitTarget.PROJECT, "A", 10000, 1001)),
                incurredOn = "2026-09-25",
                attendees = listOf(Attendee("Alex")),
                itemized = listOf(ItemizedLine("Meals", 1001)),
            )
        assertEquals(detailed, json.decodeFromString<ClaimLine>(json.encodeToString(detailed)))
        val report = Report("r", "employee", listOf(detailed))
        assertEquals(report, json.decodeFromString<Report>(json.encodeToString(report)))
    }

    @Test
    fun fxPinRoundTripsThroughTheSameDetailsJsonAndOldRowsHaveNoPin() {
        val old: ClaimLine = ExpenseLine("old", 1000, "USD", merchant = "Cafe", category = "FOOD")
        val decodedOld = json.decodeFromString<ClaimLine>(json.encodeToString(old)) as ExpenseLine
        assertEquals(null, decodedOld.fxRate)
        assertEquals(null, decodedOld.fxRatePinnedAt)
        val pinned: ClaimLine = decodedOld.copy(fxRate = FxRate(95.82, "USD", sourceDate = "2026-09-25"), fxRatePinnedAt = 123L)
        assertEquals(pinned, json.decodeFromString<ClaimLine>(json.encodeToString(pinned)))
        val report = Report("fx-report", "employee", listOf(pinned))
        assertEquals(report, json.decodeFromString<Report>(json.encodeToString(report)))
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
