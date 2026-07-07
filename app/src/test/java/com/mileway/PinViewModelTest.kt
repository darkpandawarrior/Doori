package com.mileway

import com.mileway.core.data.session.PIN_GATE_ACCOUNT_ID
import com.mileway.core.data.session.PinLockoutSource
import com.mileway.core.data.session.PinLockoutState
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.sha256Hex
import com.mileway.ui.auth.LOGIN_PIN_LENGTH
import com.mileway.ui.auth.PinUiState
import com.mileway.ui.auth.PinViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V22 P7.4 + PLAN_V24 P1.4 — covers [PinViewModel]'s set/verify state machine and the tiered,
 * persisted lockout ([com.mileway.core.data.session.PinLockoutPolicy]): the first four wrong
 * entries are free, the fifth locks for 30s, a correct entry clears the counters, and a relaunch
 * re-hydrates a still-active lockout.
 */
class PinViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private class FakePinLockoutSource(seed: PinLockoutState? = null) : PinLockoutSource {
        private val states = mutableMapOf<String, PinLockoutState>()

        init {
            if (seed != null) states[PIN_GATE_ACCOUNT_ID] = seed
        }

        override suspend fun getState(accountId: String) = states[accountId] ?: PinLockoutState()

        override suspend fun setState(
            accountId: String,
            state: PinLockoutState,
        ) {
            states[accountId] = state
        }

        override suspend fun clear(accountId: String) {
            states.remove(accountId)
        }
    }

    private fun enterDigits(
        vm: PinViewModel,
        digits: String,
    ) {
        digits.forEach { vm.onDigitEntered(it) }
    }

    @Test
    fun `set PIN flow persists a hash and marks the session once digits and confirmation match`() =
        runTest {
            val hashSource = FakePinHashSource()
            val sessionRepository = mockk<SessionRepository>(relaxed = true)
            val vm = PinViewModel(hashSource, sessionRepository, FakePinLockoutSource())

            enterDigits(vm, "1234")
            vm.onProceedToConfirm()
            assertTrue(vm.state.value.isConfirmStep)

            enterDigits(vm, "1234")
            vm.confirmSetPin()
            advanceUntilIdle()

            assertTrue(vm.state.value.completed)
            assertEquals(sha256Hex("1234"), hashSource.getPinHash(PIN_GATE_ACCOUNT_ID))
            coVerify { sessionRepository.markPinSet() }
        }

    @Test
    fun `set PIN flow resets the confirm step on a mismatch without completing`() =
        runTest {
            val vm = PinViewModel(FakePinHashSource(), mockk(relaxed = true), FakePinLockoutSource())
            enterDigits(vm, "1234")
            vm.onProceedToConfirm()
            enterDigits(vm, "9999")
            vm.confirmSetPin()
            advanceUntilIdle()

            assertFalse(vm.state.value.completed)
            assertEquals("", vm.state.value.confirmDigits)
        }

    @Test
    fun `correct PIN completes and clears the lockout counters`() =
        runTest {
            val hashSource = FakePinHashSource(mapOf(PIN_GATE_ACCOUNT_ID to sha256Hex("4321")))
            val lockout = FakePinLockoutSource(PinLockoutState(failedAttempts = 2))
            val vm = PinViewModel(hashSource, mockk(relaxed = true), lockout, MutableClock(0))

            enterDigits(vm, "4321")
            vm.verify()
            advanceUntilIdle()

            assertTrue(vm.state.value.completed)
            assertEquals(PinLockoutState(), lockout.getState(PIN_GATE_ACCOUNT_ID))
        }

    @Test
    fun `four wrong attempts stay free, not locked out`() =
        runTest {
            val hashSource = FakePinHashSource(mapOf(PIN_GATE_ACCOUNT_ID to sha256Hex("4321")))
            val lockout = FakePinLockoutSource()
            val vm = PinViewModel(hashSource, mockk(relaxed = true), lockout, MutableClock(0))

            repeat(4) {
                enterDigits(vm, "0000")
                vm.verify()
                advanceUntilIdle()
            }

            assertFalse(vm.state.value.isLockedOut)
            assertEquals(4, lockout.getState(PIN_GATE_ACCOUNT_ID).failedAttempts)
            assertEquals("", vm.state.value.digits)
        }

    @Test
    fun `the fifth wrong attempt locks for thirty seconds`() =
        runTest {
            val hashSource = FakePinHashSource(mapOf(PIN_GATE_ACCOUNT_ID to sha256Hex("4321")))
            val lockout = FakePinLockoutSource()
            val clock = MutableClock(1_000_000)
            val vm = PinViewModel(hashSource, mockk(relaxed = true), lockout, clock)

            repeat(4) {
                enterDigits(vm, "0000")
                vm.verify()
                advanceUntilIdle()
            }
            // Fifth wrong — use runCurrent so the countdown ticker doesn't advance past its first tick.
            enterDigits(vm, "0000")
            vm.verify()
            runCurrent()

            assertTrue(vm.state.value.isLockedOut)
            assertEquals(30, vm.state.value.lockoutRemainingSeconds)
            val persisted = lockout.getState(PIN_GATE_ACCOUNT_ID)
            assertEquals(5, persisted.failedAttempts)
            assertEquals(1_000_000 + 30_000, persisted.lockoutUntilMillis)
        }

    @Test
    fun `a locked-out verify while still inside the window is rejected`() =
        runTest {
            val hashSource = FakePinHashSource(mapOf(PIN_GATE_ACCOUNT_ID to sha256Hex("4321")))
            // Seeded already-locked far in the future so the ticker can't unlock within the test.
            val lockout = FakePinLockoutSource(PinLockoutState(failedAttempts = 5, lockoutUntilMillis = 10_000_000))
            val vm = PinViewModel(hashSource, mockk(relaxed = true), lockout, MutableClock(0))

            enterDigits(vm, "4321")
            vm.verify()
            runCurrent()

            assertFalse(vm.state.value.completed)
            assertTrue(vm.state.value.isLockedOut)
        }

    @Test
    fun `reset re-hydrates a still-active lockout`() =
        runTest {
            val lockout = FakePinLockoutSource(PinLockoutState(failedAttempts = 6, lockoutUntilMillis = 10_000_000))
            val vm = PinViewModel(FakePinHashSource(), mockk(relaxed = true), lockout, MutableClock(0))

            vm.reset()
            runCurrent()

            assertTrue(vm.state.value.isLockedOut, "a relaunch while still locked must re-enter the lockout")
        }

    @Test
    fun `onDigitEntered ignores input once the PIN length is reached`() {
        val vm = PinViewModel(FakePinHashSource(), mockk(relaxed = true), FakePinLockoutSource())
        enterDigits(vm, "12345")
        assertEquals(LOGIN_PIN_LENGTH, vm.state.value.digits.length)
        assertEquals("1234", vm.state.value.digits)
    }

    @Test
    fun `reset with no lockout yields a fresh state`() =
        runTest {
            val vm = PinViewModel(FakePinHashSource(), mockk(relaxed = true), FakePinLockoutSource())
            enterDigits(vm, "12")
            vm.reset()
            runCurrent()
            assertEquals(PinUiState(), vm.state.value)
        }
}
