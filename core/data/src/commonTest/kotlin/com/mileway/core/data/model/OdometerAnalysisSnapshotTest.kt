package com.mileway.core.data.model

import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.model.validator.OdometerError
import com.mileway.core.data.model.validator.OdometerValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OdometerAnalysisSnapshotTest {
    @Test
    fun `snapshot round-trips through json`() {
        val snapshot =
            OdometerAnalysisSnapshot(
                reading = 48213,
                source = OdometerReadingSource.DEVICE_OCR,
                computedDistance = 0,
                rolledOver = false,
                synthetic = false,
                validationError = null,
                analyzedAtMs = 1_000L,
            )

        val json = OdometerAnalysisSnapshot.encode(snapshot)
        val decoded = OdometerAnalysisSnapshot.decode(json)

        assertEquals(snapshot, decoded)
    }

    @Test
    fun `decode returns null for corrupt json`() {
        assertNull(OdometerAnalysisSnapshot.decode("not-json"))
    }

    @Test
    fun `decode returns null for absent json`() {
        assertNull(OdometerAnalysisSnapshot.decode(null))
    }

    @Test
    fun `fromReading builds a valid snapshot from an in-bounds reading`() {
        val snapshot =
            OdometerAnalysisSnapshot.fromReading(reading = 48213, source = OdometerReadingSource.MANUAL, analyzedAtMs = 500L)

        assertEquals(48213, snapshot.reading)
        assertEquals(OdometerReadingSource.MANUAL, snapshot.source)
        assertEquals(0, snapshot.computedDistance)
        assertEquals(false, snapshot.rolledOver)
        assertEquals(false, snapshot.synthetic)
        assertNull(snapshot.validationError)
        assertEquals(500L, snapshot.analyzedAtMs)
    }

    @Test
    fun `fromReading carries the synthetic flag for an agent-stub source`() {
        val snapshot =
            OdometerAnalysisSnapshot.fromReading(reading = 100, source = OdometerReadingSource.AGENT_STUB, analyzedAtMs = 0L)

        assertEquals(true, snapshot.synthetic)
        assertNull(snapshot.validationError)
    }

    @Test
    fun `fromReading surfaces an OdometerValidator invalid reason and no distance`() {
        val snapshot =
            OdometerAnalysisSnapshot.fromReading(
                reading = OdometerValidator.MAX_ODOMETER + 1,
                source = OdometerReadingSource.MANUAL,
                analyzedAtMs = 0L,
            )

        assertNull(snapshot.computedDistance)
        assertEquals(OdometerError.ABOVE_BOUNDS.name, snapshot.validationError)
    }
}
