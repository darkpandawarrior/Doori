package com.mileway.core.data.otp

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Clock

/**
 * PLAN_V24 P0.4 — the one OTP simulator used everywhere (login, MFA, phone change, wallet link,
 * card KYC, corporate email). Fully offline/deterministic: the 6-digit code for a given
 * (purpose, target) is a stable hash, so the demo is reproducible and self-serving — [deliveries]
 * re-emits the code as a "demo SMS/call" banner the UI surfaces on the OTP screen.
 *
 * Merges the reference app's OTP behaviours: 6-digit send/verify, a resend countdown with an
 * OTP-via-call fallback, and a 10-minute code validity window.
 */
enum class OtpPurpose { LOGIN, MFA, PHONE_CHANGE, WALLET_LINK, CARD_KYC, CORPORATE_EMAIL }

enum class OtpChannel { SMS, CALL }

/** A simulated OTP dispatch. [code] is intentionally exposed so the offline demo can autofill it. */
data class OtpDelivery(
    val purpose: OtpPurpose,
    val target: String,
    val code: String,
    val channel: OtpChannel,
    val sentAtMillis: Long,
    val expiresAtMillis: Long,
)

sealed interface OtpVerifyResult {
    data object Success : OtpVerifyResult

    data class WrongCode(val attemptsRemaining: Int) : OtpVerifyResult

    data object Expired : OtpVerifyResult

    data object LockedOut : OtpVerifyResult

    /** No active challenge for this (purpose, target) — caller should [LocalOtpEngine.send] first. */
    data object NoChallenge : OtpVerifyResult
}

class LocalOtpEngine(private val clock: Clock = Clock.System) {
    private data class Key(val purpose: OtpPurpose, val target: String)

    private data class Challenge(
        val code: String,
        val sentAtMillis: Long,
        val expiresAtMillis: Long,
        var attemptsRemaining: Int,
        var lockedOut: Boolean = false,
    )

    private val challenges = mutableMapOf<Key, Challenge>()

    private val _deliveries = MutableSharedFlow<OtpDelivery>(replay = 1, extraBufferCapacity = 8)

    /** The last dispatched OTP (replay = 1) — the OTP screen renders this as a demo SMS/call banner. */
    val deliveries: SharedFlow<OtpDelivery> = _deliveries.asSharedFlow()

    /** The deterministic 6-digit code for a (purpose, target) — stable across sends and platforms. */
    fun codeFor(
        purpose: OtpPurpose,
        target: String,
    ): String {
        val hash = fnv1a("${purpose.name}:${target.trim()}")
        return (hash % 1_000_000u).toString().padStart(6, '0')
    }

    /**
     * Dispatch a fresh challenge (resets attempts + lockout). Returns the delivery and re-emits it
     * on [deliveries] for the UI banner.
     */
    fun send(
        purpose: OtpPurpose,
        target: String,
        channel: OtpChannel = OtpChannel.SMS,
    ): OtpDelivery {
        val now = clock.now().toEpochMilliseconds()
        val code = codeFor(purpose, target)
        val delivery =
            OtpDelivery(
                purpose = purpose,
                target = target,
                code = code,
                channel = channel,
                sentAtMillis = now,
                expiresAtMillis = now + VALIDITY_MILLIS,
            )
        challenges[Key(purpose, target)] =
            Challenge(
                code = code,
                sentAtMillis = now,
                expiresAtMillis = delivery.expiresAtMillis,
                attemptsRemaining = MAX_ATTEMPTS,
            )
        _deliveries.tryEmit(delivery)
        return delivery
    }

    /** The "Get OTP via call" fallback (per the reference app) — same code, different delivery label. */
    fun requestViaCall(
        purpose: OtpPurpose,
        target: String,
    ): OtpDelivery = send(purpose, target, OtpChannel.CALL)

    fun verify(
        purpose: OtpPurpose,
        target: String,
        code: String,
    ): OtpVerifyResult {
        val challenge = challenges[Key(purpose, target)] ?: return OtpVerifyResult.NoChallenge
        if (challenge.lockedOut) return OtpVerifyResult.LockedOut
        if (clock.now().toEpochMilliseconds() > challenge.expiresAtMillis) return OtpVerifyResult.Expired

        if (code.trim() == challenge.code) {
            challenges.remove(Key(purpose, target))
            return OtpVerifyResult.Success
        }
        challenge.attemptsRemaining -= 1
        if (challenge.attemptsRemaining <= 0) {
            challenge.lockedOut = true
            return OtpVerifyResult.LockedOut
        }
        return OtpVerifyResult.WrongCode(challenge.attemptsRemaining)
    }

    /** Seconds until "Resend" becomes available again (10s countdown per the reference app); 0 when idle. */
    fun resendAvailableInSeconds(
        purpose: OtpPurpose,
        target: String,
    ): Int {
        val challenge = challenges[Key(purpose, target)] ?: return 0
        val elapsedSeconds = ((clock.now().toEpochMilliseconds() - challenge.sentAtMillis) / 1000).toInt()
        return (RESEND_COOLDOWN_SECONDS - elapsedSeconds).coerceAtLeast(0)
    }

    /** FNV-1a over UTF-16 code units — a small, platform-stable hash (String.hashCode isn't spec-stable). */
    private fun fnv1a(input: String): UInt {
        var hash = 2166136261u
        for (char in input) {
            hash = hash xor char.code.toUInt()
            hash *= 16777619u
        }
        return hash
    }

    private companion object {
        const val VALIDITY_MILLIS = 10 * 60 * 1000L // 10 minutes (per the reference app)
        const val RESEND_COOLDOWN_SECONDS = 10 // reference app resend countdown
        const val MAX_ATTEMPTS = 3
    }
}
