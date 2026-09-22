package com.mileway.core.data.session

/**
 * PLAN_V24 P1.5 — the session login credential (mock). The demo login accepts any non-empty
 * email/password, so this store exists only to back the *change-password* (verify the original)
 * and *forgot-password* (reset) flows. One credential per session, seeded to [DEFAULT_PASSWORD].
 */
const val CREDENTIAL_ACCOUNT_ID: String = "session-login-credential"

/** The seeded starting password so "change password" has an original to verify against. */
const val DEFAULT_PASSWORD: String = "demo"

/**
 * Per-account salted-hash password store. Only the salted SHA-256 digest is persisted, never the
 * raw password — mirrors [PinHashSource]'s interface-plus-platform-store split. The salt is the
 * account id (deterministic; this is a mock, not a real KDF).
 */
interface CredentialSource {
    /** Seeds [DEFAULT_PASSWORD] for [accountId] if it has no password yet (idempotent). */
    suspend fun ensureSeeded(accountId: String)

    suspend fun verify(
        accountId: String,
        password: String,
    ): Boolean

    suspend fun setPassword(
        accountId: String,
        password: String,
    )
}

/** Hash a password with the account id as salt (mock KDF — [sha256Hex] of `salt::password`). */
fun hashPassword(
    accountId: String,
    password: String,
): String = sha256Hex("$accountId::$password")

/**
 * PLAN_V24 P1.5 — password rules for the change/forgot flows (per the reference app's
 * password-change validation shape). Pure, so it unit-tests without a UI.
 */
object PasswordPolicy {
    const val MIN_LENGTH: Int = 8

    /** A password this long scores a second point for length alone. */
    private const val LONG_LENGTH: Int = 12

    /** Score bands: 0..2 is WEAK, 3..4 is FAIR, 5 is STRONG. */
    private const val MAX_WEAK_SCORE: Int = 2
    private const val MAX_FAIR_SCORE: Int = 4

    enum class Strength { WEAK, FAIR, STRONG }

    fun isValid(password: String): Boolean = password.length >= MIN_LENGTH

    fun strength(password: String): Strength {
        var score = 0
        if (password.length >= MIN_LENGTH) score++
        if (password.length >= LONG_LENGTH) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= MAX_WEAK_SCORE -> Strength.WEAK
            score <= MAX_FAIR_SCORE -> Strength.FAIR
            else -> Strength.STRONG
        }
    }
}
