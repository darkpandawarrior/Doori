package com.mileway.core.media.ocr

import com.mileway.core.media.model.OdometerReading

/**
 * V26 P26.CONV.3: three-way reconciliation over [OdometerReading]'s `userReading` (typed),
 * `deviceOcrReading` (on-device ML Kit/Vision pass via [OdometerOcrService.analyzeSingle]/
 * [OdometerOcrService.analyzeGalleryImage]) and `aiOcrReading` (on-device AI pass via
 * [OdometerOcrService.analyzeAi]) — the single decision point every odometer-capture caller uses to
 * decide whether a reading is trustworthy enough to accept outright, needs the user to pick between
 * disagreeing sources, or should be rejected outright and retaken.
 */
object OdometerReconciler {
    /** Readings within this many units of each other are treated as agreeing (OCR digit-slip noise). */
    const val TOLERANCE = 3

    /**
     * ponytail: a gap this many times [TOLERANCE] means the sources aren't reading the same odometer
     * at all (glare/wrong object/etc.) — reject and ask for a retake instead of asking the user to
     * arbitrate between numbers that aren't even close.
     */
    private const val REJECT_MULTIPLIER = 20

    /** Force-accept the best available reading once retakes are exhausted rather than loop forever. */
    const val MAX_RETAKES = 3

    sealed interface Verdict {
        /** Sources agree (or only one exists) — safe to use [reading] without prompting the user. */
        data class Accepted(val reading: Int) : Verdict

        /** Sources disagree beyond [TOLERANCE] but not wildly — let the user pick or retake. */
        data class Discrepancy(
            val userReading: Int?,
            val deviceReading: Int?,
            val aiReading: Int?,
            val maxDelta: Int,
        ) : Verdict

        /** No usable reading, or the readings are too far apart to arbitrate — must retake. */
        data class Rejected(val reason: String) : Verdict
    }

    fun reconcile(reading: OdometerReading): Verdict {
        if (reading.isRejected) return Verdict.Rejected(reading.rejectionReason ?: "Reading rejected")

        val user = reading.userReading?.toIntOrNull()
        val device = reading.deviceOcrReading?.toIntOrNull()
        val ai = reading.aiOcrReading?.toIntOrNull()
        val candidates = listOfNotNull(user, device, ai)

        if (candidates.isEmpty()) return Verdict.Rejected("No odometer reading could be determined")

        if (reading.retakeCount >= MAX_RETAKES) {
            // Exhausted retakes: trust the user's typed value over an OCR guess if one exists.
            return Verdict.Accepted(user ?: device ?: ai!!)
        }

        val maxDelta = candidates.max() - candidates.min()
        return when {
            maxDelta <= TOLERANCE -> Verdict.Accepted(user ?: device ?: ai!!)
            maxDelta > TOLERANCE * REJECT_MULTIPLIER -> Verdict.Rejected("Readings differ too much to reconcile automatically")
            else -> Verdict.Discrepancy(user, device, ai, maxDelta)
        }
    }

    /** Records a retake: the previous photo/url moves into history and the counter advances. */
    fun withRetake(
        reading: OdometerReading,
        newUrl: String? = null,
    ): OdometerReading =
        reading.copy(
            url = newUrl ?: reading.url,
            retakeCount = reading.retakeCount + 1,
            retakeHistory = reading.retakeHistory + listOfNotNull(reading.url),
        )
}
