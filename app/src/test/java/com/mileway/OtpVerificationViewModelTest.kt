package com.mileway

import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.ui.auth.OtpError
import com.mileway.ui.auth.OtpVerificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V24 P1.2: [OtpVerificationViewModel] over a real [LocalOtpEngine] — auto-submit on the
 * sixth digit, verified on the correct code, and the wrong/expired error mapping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtpVerificationViewModelTest {
    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private val target = "+919876543210"

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entering the correct code auto-submits and verifies`() =
        runTest {
            val engine = LocalOtpEngine(MutableClock(0))
            val delivery = engine.send(OtpPurpose.LOGIN, target)
            val vm = OtpVerificationViewModel(engine)
            vm.start(OtpPurpose.LOGIN, target, delivery)
            advanceUntilIdle()

            vm.onCodeChange(delivery.code)
            advanceUntilIdle()

            assertTrue(vm.state.value.verified)
            assertNull(vm.state.value.error)
        }

    @Test
    fun `a wrong six-digit code surfaces WRONG with attempts remaining`() =
        runTest {
            val engine = LocalOtpEngine(MutableClock(0))
            val delivery = engine.send(OtpPurpose.LOGIN, target)
            val vm = OtpVerificationViewModel(engine)
            vm.start(OtpPurpose.LOGIN, target, delivery)
            advanceUntilIdle()

            val wrong = if (delivery.code == "000000") "111111" else "000000"
            vm.onCodeChange(wrong)
            advanceUntilIdle()

            assertEquals(OtpError.WRONG, vm.state.value.error)
            assertEquals(2, vm.state.value.attemptsRemaining)
            assertTrue(!vm.state.value.verified)
        }

    @Test
    fun `autofill fills the delivered demo code`() =
        runTest {
            val engine = LocalOtpEngine(MutableClock(0))
            val delivery = engine.send(OtpPurpose.LOGIN, target)
            val vm = OtpVerificationViewModel(engine)
            vm.start(OtpPurpose.LOGIN, target, delivery)
            advanceUntilIdle()

            vm.autofillDemoCode()
            advanceUntilIdle()

            assertTrue(vm.state.value.verified, "autofilling the correct code auto-submits")
        }

    @Test
    fun `an expired code surfaces EXPIRED`() =
        runTest {
            val clock = MutableClock(0)
            val engine = LocalOtpEngine(clock)
            val delivery = engine.send(OtpPurpose.LOGIN, target)
            val vm = OtpVerificationViewModel(engine)
            vm.start(OtpPurpose.LOGIN, target, delivery)
            advanceUntilIdle()

            clock.millis = 10 * 60 * 1000L + 1
            vm.onCodeChange(delivery.code)
            advanceUntilIdle()

            assertEquals(OtpError.EXPIRED, vm.state.value.error)
        }
}
