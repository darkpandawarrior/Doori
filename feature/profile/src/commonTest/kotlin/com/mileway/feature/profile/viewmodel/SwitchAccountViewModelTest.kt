package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.sha256Hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V22 P2.3: covers [SwitchAccountViewModel]'s PIN-entry state machine — the local-PIN path of
 * the switch gate (the biometric path is orchestrated by the Android-only `ProfileScreen`, covered
 * separately in `app`'s `ProfileViewModelTest`).
 *
 * `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, so [Dispatchers.setMain] is
 * required here exactly as `app`'s `MainDispatcherRule` does for JVM ViewModel tests — this module
 * has no JUnit `TestWatcher` available in `commonTest`, so it's inlined via `@BeforeTest`/`@AfterTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchAccountViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun enterDigits(
        vm: SwitchAccountViewModel,
        digits: String,
    ) {
        digits.forEach { vm.onDigitEntered(it) }
    }

    @Test
    fun `entering the default PIN verifies successfully for an account with no PIN set`() =
        runTest {
            val vm = SwitchAccountViewModel(FakePinHashSource())
            enterDigits(vm, DEFAULT_SWITCH_ACCOUNT_PIN)

            vm.verify("ACC-001")
            advanceUntilIdle()

            assertTrue(vm.state.value.verified)
            assertEquals(SWITCH_ACCOUNT_MAX_ATTEMPTS, vm.state.value.attemptsRemaining)
        }

    @Test
    fun `entering a custom stored PIN verifies successfully`() =
        runTest {
            val source = FakePinHashSource(mapOf("ACC-002" to sha256Hex("9999")))
            val vm = SwitchAccountViewModel(source)
            enterDigits(vm, "9999")

            vm.verify("ACC-002")
            advanceUntilIdle()

            assertTrue(vm.state.value.verified)
        }

    @Test
    fun `wrong PIN decrements attemptsRemaining and clears entered digits`() =
        runTest {
            val vm = SwitchAccountViewModel(FakePinHashSource())
            enterDigits(vm, "0000")

            vm.verify("ACC-001")
            advanceUntilIdle()

            assertFalse(vm.state.value.verified)
            assertEquals(SWITCH_ACCOUNT_MAX_ATTEMPTS - 1, vm.state.value.attemptsRemaining)
            assertEquals("", vm.state.value.enteredDigits)
            assertFalse(vm.state.value.isLockedOut)
        }

    @Test
    fun `three wrong attempts latch isLockedOut and further verify calls are ignored`() =
        runTest {
            val vm = SwitchAccountViewModel(FakePinHashSource())

            repeat(SWITCH_ACCOUNT_MAX_ATTEMPTS) {
                enterDigits(vm, "0000")
                vm.verify("ACC-001")
                advanceUntilIdle()
            }
            assertTrue(vm.state.value.isLockedOut)
            assertEquals(0, vm.state.value.attemptsRemaining)

            // A correct PIN after lockout must still be rejected — the sheet requires dismiss+reopen.
            enterDigits(vm, DEFAULT_SWITCH_ACCOUNT_PIN)
            vm.verify("ACC-001")
            advanceUntilIdle()
            assertFalse(vm.state.value.verified)
        }

    @Test
    fun `onDigitEntered ignores input once the PIN length is reached`() {
        val vm = SwitchAccountViewModel(FakePinHashSource())
        enterDigits(vm, "12345")
        assertEquals(SWITCH_ACCOUNT_PIN_LENGTH, vm.state.value.enteredDigits.length)
        assertEquals("1234", vm.state.value.enteredDigits)
    }

    @Test
    fun `onBackspace removes the last digit`() {
        val vm = SwitchAccountViewModel(FakePinHashSource())
        enterDigits(vm, "12")
        vm.onBackspace()
        assertEquals("1", vm.state.value.enteredDigits)
    }

    @Test
    fun `reset clears entered digits, attempts, error and lockout`() =
        runTest {
            val vm = SwitchAccountViewModel(FakePinHashSource())
            repeat(SWITCH_ACCOUNT_MAX_ATTEMPTS) {
                enterDigits(vm, "0000")
                vm.verify("ACC-001")
                advanceUntilIdle()
            }
            assertTrue(vm.state.value.isLockedOut)

            vm.reset()

            assertEquals(SwitchAccountUiState(), vm.state.value)
        }

    @Test
    fun `setPin overrides the default for subsequent verify calls`() =
        runTest {
            val source = FakePinHashSource()
            val vm = SwitchAccountViewModel(source)

            vm.setPin("ACC-001", "5555")
            advanceUntilIdle()
            enterDigits(vm, "5555")
            vm.verify("ACC-001")
            advanceUntilIdle()
            assertTrue(vm.state.value.verified)

            vm.reset()
            enterDigits(vm, DEFAULT_SWITCH_ACCOUNT_PIN)
            vm.verify("ACC-001")
            advanceUntilIdle()
            assertFalse(vm.state.value.verified, "the default PIN must no longer work once overridden")
        }
}

/** In-memory fake for [PinHashSource] — mirrors [FakeMockAccountDao]'s in-memory shape. */
private class FakePinHashSource(
    seed: Map<String, String> = emptyMap(),
) : PinHashSource {
    private val hashes = seed.toMutableMap()

    override suspend fun getPinHash(accountId: String): String? = hashes[accountId]

    override suspend fun setPinHash(
        accountId: String,
        pinHash: String,
    ) {
        hashes[accountId] = pinHash
    }
}
