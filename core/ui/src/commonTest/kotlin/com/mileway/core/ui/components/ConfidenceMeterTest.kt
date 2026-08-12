package com.mileway.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ConfidenceMeterTest {
    @Test
    fun unknown_confidence_renders_as_not_measured_never_as_zero_or_a_hundred_percent() {
        val state = Confidence.Unknown.toMeterState()
        assertNull(state.fraction, "unknown must not carry a fraction that a bar could draw as a real measurement")
        assertEquals("Not measured", state.label)
        assertEquals(StatusTone.Neutral, state.tone)
    }

    @Test
    fun a_real_zero_measurement_is_distinct_from_unknown() {
        // Confidence.Known(0f) is a real measurement that happened to come out at zero — it must
        // read differently from "we never measured this".
        val state = Confidence.Known(0f).toMeterState()
        assertEquals(0f, state.fraction)
        assertEquals("0%", state.label)
    }

    @Test
    fun a_low_known_confidence_is_percentage_labelled_and_toned_as_error() {
        val state = Confidence.Known(0.2f).toMeterState()
        assertEquals(0.2f, state.fraction)
        assertEquals("20%", state.label)
        assertEquals(StatusTone.Error, state.tone)
    }

    @Test
    fun a_mid_known_confidence_is_toned_as_warning() {
        assertEquals(StatusTone.Warning, Confidence.Known(0.5f).toMeterState().tone)
    }

    @Test
    fun a_high_known_confidence_is_toned_as_success() {
        val state = Confidence.Known(0.9f).toMeterState()
        assertEquals("90%", state.label)
        assertEquals(StatusTone.Success, state.tone)
    }

    @Test
    fun a_confidence_outside_0_to_1_is_rejected_rather_than_silently_clamped() {
        assertFailsWith<IllegalArgumentException> { Confidence.Known(1.5f) }
        assertFailsWith<IllegalArgumentException> { Confidence.Known(-0.1f) }
    }
}
