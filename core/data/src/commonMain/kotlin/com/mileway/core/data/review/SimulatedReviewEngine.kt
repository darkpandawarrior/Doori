package com.mileway.core.data.review

import kotlin.time.Clock

/**
 * PLAN_V24 P0.5 — the async status-transition simulator shared by every "submit → back-office
 * reviews it → approved/rejected" flow (P4 KYC, P5 referral payout, P7 deletion, P12 self-audit).
 *
 * Flow-driven, no background scheduler: a submitted item stays [ReviewResult.Pending] until
 * [simDelayMillis] of wall-clock has elapsed, then resolves the *next time the app observes it*.
 * The outcome is deterministic so a demo can show BOTH paths on purpose — a payload containing the
 * reject marker (optionally `reject: <reason>`) rejects; anything else approves. Each consumer keeps
 * its own status enum and maps this generic result onto it.
 */
sealed interface ReviewResult {
    data object Pending : ReviewResult

    data object Approved : ReviewResult

    data class Rejected(val reason: String) : ReviewResult
}

class SimulatedReviewEngine(
    private val clock: Clock = Clock.System,
    private val simDelayMillis: Long = DEFAULT_SIM_DELAY_MILLIS,
) {
    /**
     * Resolve a pending item submitted at [submittedAtMillis] carrying an opaque [payload]
     * (whatever field a demo can plant the reject marker in — a doc's info text, a reason note).
     * [delayMillis] overrides the engine default per call so different features can review at
     * different speeds.
     */
    fun resolve(
        submittedAtMillis: Long,
        payload: String,
        delayMillis: Long = simDelayMillis,
    ): ReviewResult {
        val elapsed = clock.now().toEpochMilliseconds() - submittedAtMillis
        if (elapsed < delayMillis) return ReviewResult.Pending
        return if (payload.contains(REJECT_MARKER, ignoreCase = true)) {
            ReviewResult.Rejected(reasonFrom(payload))
        } else {
            ReviewResult.Approved
        }
    }

    /** True once enough sim-time has passed for [submittedAtMillis] to have been reviewed. */
    fun isReviewed(
        submittedAtMillis: Long,
        delayMillis: Long = simDelayMillis,
    ): Boolean = clock.now().toEpochMilliseconds() - submittedAtMillis >= delayMillis

    /** Extract a reason after `reject:` if present, else a generic demo reason. */
    private fun reasonFrom(payload: String): String {
        val markerIndex = payload.indexOf(REJECT_MARKER, ignoreCase = true)
        val afterMarker = payload.substring(markerIndex + REJECT_MARKER.length)
        val explicit = afterMarker.substringAfter(':', missingDelimiterValue = "").trim()
        return explicit.ifBlank { DEFAULT_REJECT_REASON }
    }

    companion object {
        /** "review completes next time you look, if >N sim-seconds elapsed" — short for demos. */
        const val DEFAULT_SIM_DELAY_MILLIS = 5_000L
        const val REJECT_MARKER = "reject"
        const val DEFAULT_REJECT_REASON = "Did not meet verification requirements"
    }
}
