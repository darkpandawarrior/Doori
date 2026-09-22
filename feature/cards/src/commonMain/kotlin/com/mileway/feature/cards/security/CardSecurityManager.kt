package com.mileway.feature.cards.security

import com.mileway.core.data.util.MillisPerMinute
import kotlin.time.Clock

/** How long a successful PIN entry stays good for before the user is asked again. */
private const val DefaultVerificationWindowMillis = 5L * MillisPerMinute

/**
 * Q.5: gates sensitive card actions (reveal full PAN, controls) behind a PIN, with a verification window
 * so the user isn't re-prompted within [windowMillis]. The PIN is an injectable demo PIN backed by an
 * in-memory timestamp, swap a DataStore-backed store for production persistence.
 */
class CardSecurityManager(
    private val demoPin: String = "1234",
    private val windowMillis: Long = DefaultVerificationWindowMillis,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private var lastVerifiedAtMs: Long = 0L

    /** True if a recent successful verification is still within the window (no re-prompt needed). */
    fun canSkipVerification(): Boolean = lastVerifiedAtMs > 0L && now() - lastVerifiedAtMs < windowMillis

    /** Verifies the PIN; on success starts/refreshes the verification window. */
    fun verifyPin(pin: String): Boolean {
        val ok = pin == demoPin
        if (ok) lastVerifiedAtMs = now()
        return ok
    }

    fun reset() {
        lastVerifiedAtMs = 0L
    }
}
