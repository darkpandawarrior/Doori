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
 * PLAN_V24 P1.5 — password rules for the change/forgot flows (the reference app `PasswordChangeRequestV2`
 * validation shape). Pure, so it unit-tests without a UI.
 */
object PasswordPolicy {
    const val MIN_LENGTH: Int = 8

    enum class Strength { WEAK, FAIR, STRONG }

    fun isValid(password: String): Boolean = password.length >= MIN_LENGTH

    fun strength(password: String): Strength {
        var score = 0
        if (password.length >= MIN_LENGTH) score++
        if (password.length >= 12) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 2 -> Strength.WEAK
            score <= 4 -> Strength.FAIR
            else -> Strength.STRONG
        }
    }
}
