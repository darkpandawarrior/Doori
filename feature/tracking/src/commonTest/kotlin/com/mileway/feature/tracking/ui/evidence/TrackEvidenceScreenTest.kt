package com.mileway.feature.tracking.ui.evidence

import com.mileway.core.ui.detail.DetailField
import com.siddharth.kmp.common.UiText
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun UiText.text(): String = (this as UiText.Dynamic).value

private fun assertApprox(
    expected: Double,
    actual: Double,
    tolerance: Double = 0.001,
) {
    assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")
}

class TrackEvidenceScreenTest {
    @Test
    fun `spec carries every required section id in order`() {
        val spec = trackEvidenceDetailSpec(sampleEvidenceTrack())

        assertEquals(
            listOf("ledger", "provenance", "capture_quality", "evidence", "edit_history", "compliance"),
            spec.sections.map { it.id },
        )
    }

    @Test
    fun `ledger figures map from the real SavedTrack distance columns, not invented ones`() {
        val track =
            sampleEvidenceTrack().copy(
                originalDistance = 13_100.0,
                cleanedDistance = 12_400.0,
                distance = 12_400.0,
                mockDistance = 200.0,
                abnormalDistance = 400.0,
                spikeDistance = 100.0,
                odometerDistance = 12_600.0,
                useOdometer = true,
            )

        val ledger = track.toDistanceLedger()

        assertApprox(13.1, ledger.rawKm)
        assertApprox(12.4, ledger.cleanedKm)
        assertApprox(12.4, ledger.claimedKm)
        assertApprox(0.2, ledger.mockKm)
        assertApprox(0.4, ledger.abnormalKm)
        assertApprox(0.1, ledger.spikeKm)
        assertApprox(12.6, ledger.odometerKm ?: -1.0)
    }

    @Test
    fun `odometer cross-check is omitted when useOdometer is false, even if a stale reading exists`() {
        val track = sampleEvidenceTrack().copy(useOdometer = false, odometerDistance = 12_600.0)

        assertEquals(null, track.toDistanceLedger().odometerKm)
    }

    @Test
    fun `cleaned falls back to the claimed distance when cleanedDistance was never computed`() {
        val track = sampleEvidenceTrack().copy(cleanedDistance = 0.0, distance = 9_000.0)

        assertApprox(9.0, track.toDistanceLedger().cleanedKm)
    }

    @Test
    fun `a zero-distance track builds without crashing and reports a zero ledger`() {
        val track =
            sampleEvidenceTrack().copy(
                distance = 0.0,
                originalDistance = 0.0,
                cleanedDistance = 0.0,
                mockDistance = 0.0,
                abnormalDistance = 0.0,
                spikeDistance = 0.0,
                odometerDistance = 0.0,
                useOdometer = false,
            )

        val spec = trackEvidenceDetailSpec(track)

        assertEquals(0.0, track.toDistanceLedger().claimedKm)
        assertTrue(spec.sections.isNotEmpty())
    }

    @Test
    fun `nullable journeyDate falls back to createdAt without crashing`() {
        val track = sampleEvidenceTrack().copy(journeyDate = null, createdAt = 1_754_000_000_000L)

        val spec = trackEvidenceDetailSpec(track)

        val dateField = spec.sections.single { it.id == "compliance" }.fields.single { it.id == "compliance_date" } as DetailField.DateField
        assertTrue(dateField.value.text() != "—")
    }

    @Test
    fun `edit history is always rendered honestly rather than invented`() {
        val spec = trackEvidenceDetailSpec(sampleEvidenceTrack())

        val row = spec.sections.single { it.id == "edit_history" }.fields.single() as DetailField.KeyValue
        assertTrue(row.value.text().contains("No edits recorded"))
    }

    @Test
    fun `attachments are hidden and an honest none-captured row shown when no odometer photos exist`() {
        val track = sampleEvidenceTrack().copy(odometerStartUrl = "", odometerEndUrl = "")

        val spec = trackEvidenceDetailSpec(track)
        val evidenceFields = spec.sections.single { it.id == "evidence" }.fields

        val attachmentsField = evidenceFields.single { it.id == "evidence_attachments" }
        assertFalse(attachmentsField.visible)
        val noneField = evidenceFields.single { it.id == "evidence_none" }
        assertTrue(noneField.visible)
    }

    @Test
    fun `attachments render one entry per non-blank odometer url`() {
        val spec = trackEvidenceDetailSpec(sampleEvidenceTrack())

        val attachmentsField =
            spec.sections.single { it.id == "evidence" }.fields.single { it.id == "evidence_attachments" } as DetailField.AttachmentList
        assertEquals(2, attachmentsField.attachments.size)
    }
}
