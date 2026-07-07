package com.mileway.core.data.otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V24 P0.4 — the OTP simulator is a security-shaped path (expiry + lockout), so it gets a
 * table of deterministic checks: stable codes, correct-code success, 10-minute expiry, 3-attempt
 * lockout, and the resend countdown.
 */
class LocalOtpEngineTest {
    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private val target = "+919876543210"

    @Test
    fun `code is deterministic and six digits`() {
        val engine = LocalOtpEngine(MutableClock(0))
        val a = engine.codeFor(OtpPurpose.LOGIN, target)
        val b = engine.codeFor(OtpPurpose.LOGIN, target)
        assertEquals(a, b)
        assertEquals(6, a.length)
        assertTrue(a.all { it.isDigit() })
    }

    @Test
    fun `different purpose or target yields a different code`() {
        val engine = LocalOtpEngine(MutableClock(0))
        val login = engine.codeFor(OtpPurpose.LOGIN, target)
        val mfa = engine.codeFor(OtpPurpose.MFA, target)
        val other = engine.codeFor(OtpPurpose.LOGIN, "+911111111111")
        assertTrue(login != mfa)
        assertTrue(login != other)
    }

    @Test
    fun `correct code verifies and consumes the challenge`() {
        val engine = LocalOtpEngine(MutableClock(0))
        val delivery = engine.send(OtpPurpose.LOGIN, target)
        assertEquals(engine.codeFor(OtpPurpose.LOGIN, target), delivery.code)

        assertEquals(OtpVerifyResult.Success, engine.verify(OtpPurpose.LOGIN, target, delivery.code))
        // Consumed: a second verify has no challenge.
        assertEquals(OtpVerifyResult.NoChallenge, engine.verify(OtpPurpose.LOGIN, target, delivery.code))
    }

    @Test
    fun `verify before send has no challenge`() {
        val engine = LocalOtpEngine(MutableClock(0))
        assertEquals(OtpVerifyResult.NoChallenge, engine.verify(OtpPurpose.LOGIN, target, "000000"))
    }

    @Test
    fun `code expires after ten minutes`() {
        val clock = MutableClock(0)
        val engine = LocalOtpEngine(clock)
        val delivery = engine.send(OtpPurpose.LOGIN, target)

        clock.millis = 10 * 60 * 1000L + 1
        assertEquals(OtpVerifyResult.Expired, engine.verify(OtpPurpose.LOGIN, target, delivery.code))
    }

    @Test
    fun `three wrong attempts lock the challenge out`() {
        val engine = LocalOtpEngine(MutableClock(0))
        val delivery = engine.send(OtpPurpose.LOGIN, target)

        assertEquals(OtpVerifyResult.WrongCode(2), engine.verify(OtpPurpose.LOGIN, target, "000000"))
        assertEquals(OtpVerifyResult.WrongCode(1), engine.verify(OtpPurpose.LOGIN, target, "000000"))
        assertEquals(OtpVerifyResult.LockedOut, engine.verify(OtpPurpose.LOGIN, target, "000000"))
        // Even the correct code is rejected once locked out.
        assertEquals(OtpVerifyResult.LockedOut, engine.verify(OtpPurpose.LOGIN, target, delivery.code))
    }

    @Test
    fun `resend obeys the ten second cooldown`() {
        val clock = MutableClock(0)
        val engine = LocalOtpEngine(clock)
        engine.send(OtpPurpose.LOGIN, target)
        assertEquals(10, engine.resendAvailableInSeconds(OtpPurpose.LOGIN, target))

        clock.millis = 4_000
        assertEquals(6, engine.resendAvailableInSeconds(OtpPurpose.LOGIN, target))

        clock.millis = 10_000
        assertEquals(0, engine.resendAvailableInSeconds(OtpPurpose.LOGIN, target))
    }

    @Test
    fun `resend resets attempts and via-call uses the call channel`() {
        val engine = LocalOtpEngine(MutableClock(0))
        engine.send(OtpPurpose.WALLET_LINK, target)
        engine.verify(OtpPurpose.WALLET_LINK, target, "000000")

        val call = engine.requestViaCall(OtpPurpose.WALLET_LINK, target)
        assertEquals(OtpChannel.CALL, call.channel)
        // Fresh challenge → full attempts again.
        assertEquals(OtpVerifyResult.WrongCode(2), engine.verify(OtpPurpose.WALLET_LINK, target, "000000"))
    }
}
