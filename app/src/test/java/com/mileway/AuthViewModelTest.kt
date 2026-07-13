package com.mileway

import app.cash.turbine.test
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.ui.auth.AuthViewModel
import com.mileway.ui.auth.MilewayAuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P7.2/P7.3: behavioural tests for [AuthViewModel], the reducer behind [MilewayAuthState] driving
 * [com.mileway.ui.auth.LoginScreen]'s staged sign-in loading UX and "Demo mode" persona picker.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        mockAccountRepository: MockAccountRepository = MockAccountRepository(FakeMockAccountDao()),
        activeAccountSource: FakeActiveAccountSource = FakeActiveAccountSource(),
    ) = AuthViewModel(mockAccountRepository, activeAccountSource)

    @Test
    fun `starts idle`() = runTest {
        val vm = buildViewModel()
        assertEquals(MilewayAuthState.Idle, vm.state.value)
    }

    @Test
    fun `beginSignIn steps through every named stage before reporting success`() = runTest {
        val vm = buildViewModel()

        vm.state.test {
            assertEquals(MilewayAuthState.Idle, awaitItem())

            vm.beginSignIn()

            val step1 = awaitItem()
            assertIs<MilewayAuthState.Loading>(step1)
            assertEquals(1, step1.step)
            assertEquals(3, step1.totalSteps)

            val step2 = awaitItem()
            assertIs<MilewayAuthState.Loading>(step2)
            assertEquals(2, step2.step)
            assertTrue(step2.labelRes != step1.labelRes)

            val step3 = awaitItem()
            assertIs<MilewayAuthState.Loading>(step3)
            assertEquals(3, step3.step)

            assertEquals(MilewayAuthState.Success, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a second beginSignIn call while loading is a no-op`() = runTest {
        val vm = buildViewModel()

        vm.beginSignIn()
        advanceUntilIdle()
        assertEquals(MilewayAuthState.Success, vm.state.value)

        // Reset back to idle, start a fresh run, then try to re-trigger mid-sequence.
        vm.reset()
        vm.beginSignIn()
        runCurrent() // advance just enough to reach the first Loading state (before its delay)
        val midSequenceState = vm.state.value
        assertIs<MilewayAuthState.Loading>(midSequenceState)

        vm.beginSignIn() // no-op: already Loading
        assertEquals(midSequenceState, vm.state.value)

        advanceUntilIdle()
        assertEquals(MilewayAuthState.Success, vm.state.value)
    }

    @Test
    fun `reset cancels an in-flight sequence and returns to idle`() = runTest {
        val vm = buildViewModel()

        vm.beginSignIn()
        runCurrent() // advance just enough to reach the first Loading state (before its delay)
        assertIs<MilewayAuthState.Loading>(vm.state.value)

        vm.reset()
        assertEquals(MilewayAuthState.Idle, vm.state.value)

        // Letting time pass after reset must not resurrect the cancelled sequence.
        advanceUntilIdle()
        assertEquals(MilewayAuthState.Idle, vm.state.value)
    }

    // ── P7.3: "Demo mode" persona picker ────────────────────────────────────────

    @Test
    fun `personas exposes the seeded demo persona list`() = runTest {
        val dao = FakeMockAccountDao()
        val vm = buildViewModel(mockAccountRepository = MockAccountRepository(dao))

        advanceUntilIdle() // let init{}'s seedIfEmpty() run
        vm.personas.test {
            var seeded = awaitItem()
            while (seeded.isEmpty()) seeded = awaitItem() // skip stateIn's pre-seed initial value
            assertEquals(3, seeded.size, "expected the 3 seeded demo personas")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duplicatesFor returns the personas sharing a seeded phone`() = runTest {
        val vm = buildViewModel(mockAccountRepository = MockAccountRepository(FakeMockAccountDao()))
        advanceUntilIdle()
        // Trigger stateIn collection so personas.value is populated.
        vm.personas.test {
            var seeded = awaitItem()
            while (seeded.isEmpty()) seeded = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // P1.6: ACC-001 + ACC-003 share +919876543210; ACC-002 does not.
        val dupes = vm.duplicatesFor("+919876543210")
        assertEquals(setOf("ACC-001", "ACC-003"), dupes.map { it.id }.toSet())
        assertTrue(vm.duplicatesFor("+910000000000").isEmpty())
    }

    @Test
    fun `no picker interaction leaves the active persona unchanged (default behavior)`() = runTest {
        val dao = FakeMockAccountDao()
        val activeAccountSource = FakeActiveAccountSource(seed = "ACC-001")
        val vm = buildViewModel(
            mockAccountRepository = MockAccountRepository(dao),
            activeAccountSource = activeAccountSource,
        )
        advanceUntilIdle()

        assertNull(vm.selectedPersonaId.value)

        vm.beginSignIn()
        advanceUntilIdle()

        assertEquals(MilewayAuthState.Success, vm.state.value)
        assertEquals("ACC-001", activeAccountSource.activeAccountId.first())
    }

    @Test
    fun `picking a persona before signing in makes it the active account on success`() = runTest {
        val dao = FakeMockAccountDao()
        val repository = MockAccountRepository(dao)
        val activeAccountSource = FakeActiveAccountSource(seed = "ACC-001")
        val vm = buildViewModel(mockAccountRepository = repository, activeAccountSource = activeAccountSource)
        advanceUntilIdle() // let init{}'s seedIfEmpty() run

        vm.selectPersona("ACC-003")
        assertEquals("ACC-003", vm.selectedPersonaId.value)

        vm.beginSignIn()
        advanceUntilIdle()

        assertEquals(MilewayAuthState.Success, vm.state.value)
        assertEquals("ACC-003", activeAccountSource.activeAccountId.first())
        assertEquals(true, dao.getById("ACC-003")?.isActive)
        assertEquals(false, dao.getById("ACC-001")?.isActive)
    }
}
