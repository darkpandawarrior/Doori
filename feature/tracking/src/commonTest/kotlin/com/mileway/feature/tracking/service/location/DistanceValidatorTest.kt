package com.mileway.feature.tracking.service.location

import com.mileway.feature.tracking.service.location.DistanceValidator.DistanceMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DistanceValidatorTest {
    private fun metrics(
        total: Double = 1000.0,
        cleaned: Double = 900.0,
        mock: Double = 50.0,
        abnormal: Double = 50.0,
        spike: Double = 0.0,
        odometer: Double? = null,
    ) = DistanceMetrics(total, cleaned, mock, abnormal, spike, odometer)

    @Test
    fun `consistent metrics pass with no errors or warnings`() {
        val result = DistanceValidator.validate(metrics())
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertTrue(DistanceValidator.isValid(metrics()))
    }

    @Test
    fun `component mismatch fails when cleaned does not equal total minus mock and abnormal`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 800.0, mock = 50.0, abnormal = 50.0))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it is DistanceValidator.ValidationError.ComponentMismatch })
    }

    @Test
    fun `cleaned exceeding total fails`() {
        val result = DistanceValidator.validate(metrics(total = 500.0, cleaned = 600.0, mock = 0.0, abnormal = -100.0))
        assertTrue(result.errors.any { it is DistanceValidator.ValidationError.CleanedExceedsTotal })
    }

    @Test
    fun `negative distance fails`() {
        val result = DistanceValidator.validate(metrics(mock = -10.0, cleaned = 960.0))
        assertTrue(result.errors.any { it is DistanceValidator.ValidationError.NegativeDistance && it.field == "mock" })
    }

    @Test
    fun `spike is excluded from the cleaned relationship`() {
        // A large spike must not perturb cleaned = total - (mock + abnormal).
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 900.0, mock = 50.0, abnormal = 50.0, spike = 5000.0))
        assertFalse(result.errors.any { it is DistanceValidator.ValidationError.ComponentMismatch })
        assertTrue(result.isValid)
    }

    @Test
    fun `mock ratio warning triggers above 50 percent`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 400.0, mock = 600.0, abnormal = 0.0))
        assertTrue(result.warnings.any { it is DistanceValidator.ValidationWarning.UnusualRatio && it.field == "mock" })
    }

    @Test
    fun `abnormal ratio warning triggers above 50 percent`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 400.0, mock = 0.0, abnormal = 600.0))
        assertTrue(result.warnings.any { it is DistanceValidator.ValidationWarning.UnusualRatio && it.field == "abnormal" })
    }

    @Test
    fun `spike ratio warning triggers above 30 percent of tracked movement`() {
        // spikeRatio = spike / (total + spike) > 0.3
        val result = DistanceValidator.validate(metrics(total = 700.0, cleaned = 600.0, mock = 50.0, abnormal = 50.0, spike = 500.0))
        assertTrue(result.warnings.any { it is DistanceValidator.ValidationWarning.UnusualRatio && it.field == "spike" })
    }

    @Test
    fun `odometer discrepancy warning triggers above 30 percent`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 900.0, mock = 50.0, abnormal = 50.0, odometer = 1500.0))
        assertTrue(result.warnings.any { it is DistanceValidator.ValidationWarning.LargeDiscrepancy })
    }

    @Test
    fun `odometer within tolerance does not warn`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 900.0, mock = 50.0, abnormal = 50.0, odometer = 1100.0))
        assertTrue(result.warnings.none { it is DistanceValidator.ValidationWarning.LargeDiscrepancy })
    }

    @Test
    fun `isValid is false when there are blocking errors`() {
        assertFalse(DistanceValidator.isValid(metrics(mock = -1.0, cleaned = 950.0)))
    }

    @Test
    fun `floating point tolerance does not false-positive`() {
        val result = DistanceValidator.validate(metrics(total = 1000.0, cleaned = 900.00000001, mock = 50.0, abnormal = 50.0))
        assertEquals(0, result.errors.size)
    }
}
