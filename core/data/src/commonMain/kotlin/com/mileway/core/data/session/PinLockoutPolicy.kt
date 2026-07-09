package com.mileway.core.data.session

/**
 * PLAN_V24 P1.4 — tiered PIN lockout, reimplementing the reference app's failed-attempt escalation:
 * the first few wrong attempts are free, then each further wrong attempt locks the PIN for an
 * escalating window. Pure + table-driven so it unit-tests without a store or clock.
 *
 * | cumulative failed attempts | lockout |
 * |---|---|
 * | 1–4 | none |
 * | 5   | 30s |
 * | 6   | 1m  |
 * | 7   | 5m  |
 * | 8   | 15m |
 * | 9+  | 30m |
 */
object PinLockoutPolicy {
    const val FREE_ATTEMPTS: Int = 4

    private const val SECOND = 1_000L
    private const val MINUTE = 60_000L

    /** Milliseconds the PIN is locked for after [failedAttempts] cumulative wrong entries (0 = not locked). */
    fun lockoutMillisFor(failedAttempts: Int): Long =
        when {
            failedAttempts <= FREE_ATTEMPTS -> 0L
            failedAttempts == 5 -> 30 * SECOND
            failedAttempts == 6 -> 1 * MINUTE
            failedAttempts == 7 -> 5 * MINUTE
            failedAttempts == 8 -> 15 * MINUTE
            else -> 30 * MINUTE
        }
}

/** Persisted per-account lockout counters (see [PinLockoutSource]). */
data class PinLockoutState(
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
) {
    /** True when [nowMillis] is still inside the lockout window. */
    fun isLocked(nowMillis: Long): Boolean = nowMillis < lockoutUntilMillis

    fun remainingSeconds(nowMillis: Long): Int = (((lockoutUntilMillis - nowMillis) + 999) / 1000).coerceAtLeast(0).toInt()
}

/**
 * PLAN_V24 P1.4 — per-account persistence for the tiered lockout counters, so a wrong-PIN lockout
 * survives process death (a kill can't reset the backoff). Mirrors [PinHashSource]'s
 * interface-plus-platform-store split; keyed by the same account id ([PIN_GATE_ACCOUNT_ID] for the
 * session login gate).
 */
interface PinLockoutSource {
    suspend fun getState(accountId: String): PinLockoutState

    suspend fun setState(
        accountId: String,
        state: PinLockoutState,
    )

    /** Clear on a successful verify. */
    suspend fun clear(accountId: String)
}
