package com.mileway.core.data.model.validator

import com.mileway.core.data.model.display.OdometerReadingSource

/**
 * Result of validating an odometer start/end pair (parity spec §2.4): bounds, rollover
 * (999999→0), decrement detection, and a carried-through synthetic (GPS-derived) flag.
 * Readings are plain odometer units (not km) — [Valid.distance] is `end - start` in those
 * same units, computed across the wrap when [Valid.rolledOver] is true.
 */
sealed interface OdometerValidation {
    data class Valid(
        val distance: Int,
        val rolledOver: Boolean,
        val synthetic: Boolean,
    ) : OdometerValidation

    data class Invalid(val reason: OdometerError) : OdometerValidation
}

enum class OdometerError { BELOW_BOUNDS, ABOVE_BOUNDS, DECREMENT, IMPLAUSIBLE_JUMP }

/** Pure commonMain odometer validator — no Android/Java imports. */
object OdometerValidator {
    /** Odometers in this fleet are 6-digit displays; matches `core:media`'s `OcrAggregator` 1_000..999_999 extraction range. */
    const val MAX_ODOMETER: Int = 999_999

    /**
     * ponytail: a genuine trip won't rack up more than a few thousand km between start and end
     * capture; anything past this (whether same-direction or wrapped) is an implausible jump
     * rather than a real rollover. Bump if a real fleet use case needs longer single trips.
     */
    const val MAX_PLAUSIBLE_TRIP: Int = 5_000

    fun validate(
        start: Int,
        end: Int,
        source: OdometerReadingSource,
    ): OdometerValidation {
        if (start < 0 || end < 0) return OdometerValidation.Invalid(OdometerError.BELOW_BOUNDS)
        if (start > MAX_ODOMETER || end > MAX_ODOMETER) return OdometerValidation.Invalid(OdometerError.ABOVE_BOUNDS)

        val synthetic = source == OdometerReadingSource.AGENT_STUB

        if (end >= start) {
            val distance = end - start
            return if (distance > MAX_PLAUSIBLE_TRIP) {
                OdometerValidation.Invalid(OdometerError.IMPLAUSIBLE_JUMP)
            } else {
                OdometerValidation.Valid(distance = distance, rolledOver = false, synthetic = synthetic)
            }
        }

        // end < start: either a genuine 999999→0 rollover or a bogus decrement.
        val wrappedDistance = (MAX_ODOMETER + 1 - start) + end
        return if (wrappedDistance in 0..MAX_PLAUSIBLE_TRIP) {
            OdometerValidation.Valid(distance = wrappedDistance, rolledOver = true, synthetic = synthetic)
        } else {
            OdometerValidation.Invalid(OdometerError.DECREMENT)
        }
    }
}
