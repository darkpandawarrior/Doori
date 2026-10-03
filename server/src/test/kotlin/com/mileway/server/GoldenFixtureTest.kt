package com.mileway.server

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * L1a's wire-drift guard: `:server` deserializes the SAME golden fixture files
 * `:contract`'s GoldenFixtureRoundTripTest reads — wired onto this module's test classpath via
 * `sourceSets.test.resources.srcDir` in server/build.gradle.kts pointing straight at
 * `contract/src/commonTest/resources`, not a copy. If a future change anywhere breaks the shared
 * wire contract, both sides fail together, not just the client.
 */
class GoldenFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun readFixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/golden/$name")) { "Missing golden fixture: $name" }
            .bufferedReader()
            .readText()

    @Test
    fun serverDecodesTheSameClaimLineFixtureAsTheClient() {
        val lines = json.decodeFromString<List<ClaimLine>>(readFixture("claimline.json"))

        assertEquals(4, lines.size)
        assertTrue(lines[0] is ExpenseLine)
        assertTrue(lines[1] is MileageLine)
        assertTrue(lines[2] is PerDiemLine)
        assertTrue(lines[3] is AdvanceLine)
    }

    @Test
    fun serverDecodesTheSameApprovalStepFixtureAsTheClient() {
        val steps = json.decodeFromString<List<ApprovalStep>>(readFixture("approvalstep.json"))

        assertEquals(2, steps.size)
        assertEquals("manager", steps[0].role)
        assertEquals("finance", steps[1].role)
    }

    @Test
    fun serverDecodesTheSameReportFixtureAsTheClient() {
        val report = json.decodeFromString<Report>(readFixture("report.json"))

        assertEquals("report-golden-1", report.id)
        assertEquals(ReportLifecycleState.APPROVED, report.state)
        assertEquals(4, report.lines.size)
        assertEquals(2, report.approvalChain.steps.size)
    }
}
