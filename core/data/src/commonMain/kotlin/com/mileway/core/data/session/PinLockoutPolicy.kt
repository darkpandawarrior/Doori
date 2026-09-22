package com.mileway.core.data.session

import com.mileway.core.data.util.MillisPerMinute
import com.mileway.core.data.util.MillisPerSecond

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

    /**
     * The escalation ladder itself, one entry per attempt past [FREE_ATTEMPTS]. It replaced a
     * `when` whose arms compared against bare 5/6/7/8 — the attempt numbers were implied by the
     * arm order anyway, so spelling them out only gave two places to disagree with the KDoc table.
     * Attempts past the end of the ladder hold at its last step.
     */
    private val LockoutLadder =
        listOf(
            30 * MillisPerSecond,
            1 * MillisPerMinute,
            5 * MillisPerMinute,
            15 * MillisPerMinute,
            30 * MillisPerMinute,
        )

    /** Milliseconds the PIN is locked for after [failedAttempts] cumulative wrong entries (0 = not locked). */
    fun lockoutMillisFor(failedAttempts: Int): Long {
        if (failedAttempts <= FREE_ATTEMPTS) return 0L
        val step = (failedAttempts - FREE_ATTEMPTS - 1).coerceAtMost(LockoutLadder.lastIndex)
        return LockoutLadder[step]
    }
}

/** Persisted per-account lockout counters (see [PinLockoutSource]). */
data class PinLockoutState(
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
) {
    /** True when [nowMillis] is still inside the lockout window. */
    fun isLocked(nowMillis: Long): Boolean = nowMillis < lockoutUntilMillis

    /** Whole seconds left, rounded **up** — 0.5s remaining must still read as "1", never "0". */
    fun remainingSeconds(nowMillis: Long): Int {
        val remainingMillis = lockoutUntilMillis - nowMillis
        if (remainingMillis <= 0L) return 0
        return ((remainingMillis + MillisPerSecond - 1) / MillisPerSecond).toInt()
    }
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
