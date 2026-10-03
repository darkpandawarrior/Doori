package com.mileway.core.data.domain.claim

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deserializes the golden fixtures committed at `commonTest/resources/golden/` — the same files
 * `:server:test`'s GoldenFixtureTest reads (server/build.gradle.kts wires this module's
 * commonTest resources onto :server's test classpath, no copy) — and proves L1a's wire-stability
 * claim two ways: (1) every discriminator/enum value is asserted as a literal substring against
 * the frozen fixture text itself, so a Kotlin symbol rename can never silently change the wire
 * form (only editing the fixture can), and (2) decode -> encode -> decode is lossless.
 */
class GoldenFixtureRoundTripTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun claimLineFixtureDecodesAllFourDiscriminatorsAndRoundTrips() {
        val raw = readGoldenFixture("claimline.json")
        val lines = json.decodeFromString<List<ClaimLine>>(raw)

        assertEquals(4, lines.size)
        assertTrue(lines[0] is ExpenseLine)
        assertTrue(lines[1] is MileageLine)
        assertTrue(lines[2] is PerDiemLine)
        assertTrue(lines[3] is AdvanceLine)

        assertTrue(raw.contains("\"type\": \"expense\""))
        assertTrue(raw.contains("\"type\": \"mileage\""))
        assertTrue(raw.contains("\"type\": \"per_diem\""))
        assertTrue(raw.contains("\"type\": \"advance\""))

        val reEncoded = json.encodeToString(lines)
        assertEquals(lines, json.decodeFromString<List<ClaimLine>>(reEncoded))
    }

    @Test
    fun approvalStepFixtureDecodesAndRoundTrips() {
        val raw = readGoldenFixture("approvalstep.json")
        val steps = json.decodeFromString<List<ApprovalStep>>(raw)

        assertEquals(2, steps.size)
        assertEquals("manager", steps[0].role)
        assertEquals("finance", steps[1].role)
        assertEquals("emp-fin-1", steps[1].onBehalfOf)
        assertTrue(raw.contains("\"action\": \"approve\""))

        val reEncoded = json.encodeToString(steps)
        assertEquals(steps, json.decodeFromString<List<ApprovalStep>>(reEncoded))
    }

    @Test
    fun reportFixtureDecodesAndRoundTrips() {
        val raw = readGoldenFixture("report.json")
        val report = json.decodeFromString<Report>(raw)

        assertEquals("report-golden-1", report.id)
        assertEquals(4, report.lines.size)
        assertEquals(2, report.approvalChain.steps.size)
        assertEquals(ReportLifecycleState.APPROVED, report.state)
        assertTrue(raw.contains("\"state\": \"approved\""))

        val reEncoded = json.encodeToString(report)
        assertEquals(report, json.decodeFromString<Report>(reEncoded))
    }
}
