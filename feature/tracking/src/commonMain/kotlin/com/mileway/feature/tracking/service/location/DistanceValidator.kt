package com.mileway.feature.tracking.service.location

import kotlin.math.abs

/**
 * P-C.2: Cross-checks the distance-metrics invariant on `SavedTrack`
 * (`originalDistance`, `mockDistance`, `abnormalDistance`, `spikeDistance`, `cleanedDistance`,
 * `odometerDistance`) before submission. Nothing enforced this relationship before.
 *
 * Invariant: `cleaned = total - (mock + abnormal)`. Spike is tracked separately (it's never
 * folded into total) and is deliberately EXCLUDED from this relationship.
 *
 * Pure Kotlin, no Android dependency, fully JVM-unit-testable.
 */
object DistanceValidator {
    /** Distance metrics snapshot (metres), mirroring the relevant `SavedTrack` fields. */
    data class DistanceMetrics(
        val total: Double,
        val cleaned: Double,
        val mock: Double,
        val abnormal: Double,
        val spike: Double,
        val odometer: Double? = null,
    )

    sealed class ValidationError(val message: String) {
        data class NegativeDistance(val field: String, val value: Double) :
            ValidationError("Negative distance not allowed: $field = $value")

        data class ComponentMismatch(val expected: Double, val actual: Double, val difference: Double) :
            ValidationError("Distance components don't sum to total: expected=$expected, actual=$actual, diff=$difference")

        data class CleanedExceedsTotal(val cleaned: Double, val total: Double) :
            ValidationError("Cleaned distance ($cleaned) exceeds total distance ($total)")
    }

    sealed class ValidationWarning(val message: String) {
        data class UnusualRatio(val field: String, val ratio: Double) :
            ValidationWarning("$field is ${(ratio * 100).toInt()}% of total")

        data class LargeDiscrepancy(val field: String, val ratio: Double) :
            ValidationWarning("$field discrepancy is ${(ratio * 100).toInt()}%")
    }

    data class ValidationResult(
        val errors: List<ValidationError> = emptyList(),
        val warnings: List<ValidationWarning> = emptyList(),
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    private const val TOLERANCE_METRES = 0.1 // floating-point slack, not a real discrepancy
    private const val MOCK_ABNORMAL_RATIO_THRESHOLD = 0.5
    private const val SPIKE_RATIO_THRESHOLD = 0.3
    private const val ODOMETER_DISCREPANCY_THRESHOLD = 0.3

    fun validate(metrics: DistanceMetrics): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        listOf(
            "total" to metrics.total,
            "cleaned" to metrics.cleaned,
            "mock" to metrics.mock,
            "abnormal" to metrics.abnormal,
            "spike" to metrics.spike,
        ).forEach { (field, value) ->
            if (value < 0) errors.add(ValidationError.NegativeDistance(field, value))
        }

        // CRITICAL: spike is excluded here — it's never folded into `total`, so it isn't
        // subtracted back out of `cleaned` either.
        val expectedCleaned = metrics.total - (metrics.mock + metrics.abnormal)
        val diff = abs(metrics.cleaned - expectedCleaned)
        if (diff > TOLERANCE_METRES) {
            errors.add(ValidationError.ComponentMismatch(expectedCleaned, metrics.cleaned, diff))
        }

        if (metrics.cleaned > metrics.total) {
            errors.add(ValidationError.CleanedExceedsTotal(metrics.cleaned, metrics.total))
        }

        if (metrics.total > 0) {
            val mockRatio = metrics.mock / metrics.total
            if (mockRatio > MOCK_ABNORMAL_RATIO_THRESHOLD) {
                warnings.add(ValidationWarning.UnusualRatio("mock", mockRatio))
            }
            val abnormalRatio = metrics.abnormal / metrics.total
            if (abnormalRatio > MOCK_ABNORMAL_RATIO_THRESHOLD) {
                warnings.add(ValidationWarning.UnusualRatio("abnormal", abnormalRatio))
            }
            val spikeRatio = metrics.spike / (metrics.total + metrics.spike)
            if (spikeRatio > SPIKE_RATIO_THRESHOLD) {
                warnings.add(ValidationWarning.UnusualRatio("spike", spikeRatio))
            }
        }

        metrics.odometer?.let { odometer ->
            if (odometer > 0 && metrics.total > 0) {
                val discrepancyRatio = abs(odometer - metrics.total) / odometer
                if (discrepancyRatio > ODOMETER_DISCREPANCY_THRESHOLD) {
                    warnings.add(ValidationWarning.LargeDiscrepancy("odometer-vs-gps", discrepancyRatio))
                }
            }
        }

        return ValidationResult(errors, warnings)
    }

    /** Quick gate: true if `metrics` has no blocking errors. */
    fun isValid(metrics: DistanceMetrics): Boolean = validate(metrics).isValid
}
