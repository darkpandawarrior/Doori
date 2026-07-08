package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.feature.profile.viewmodel.MembershipViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P6.1: covers [MembershipViewModel]'s session-backed state mapping and the activate /
 * confetti-shown writes. (app/src/test because SessionRepository is a platform class mocked with
 * mockk, which feature:profile commonTest lacks.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MembershipViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun session(state: SessionState = SessionState()): SessionRepository =
        mockk(relaxed = true) { every { sessionState } returns MutableStateFlow(state) }

    @Test
    fun `a fresh session is not a member`() =
        runTest {
            val vm = MembershipViewModel(session())
            advanceUntilIdle()
            assertFalse(vm.state.value.isMember)
        }

    @Test
    fun `an activated session is a member`() =
        runTest {
            val vm = MembershipViewModel(session(SessionState(clubActivatedAtMs = 1_700_000_000_000L)))
            advanceUntilIdle()
            assertTrue(vm.state.value.isMember)
        }

    @Test
    fun `activate persists the club activation`() =
        runTest {
            val repo = session()
            val vm = MembershipViewModel(repo)
            advanceUntilIdle()

            vm.activate()
            advanceUntilIdle()

            coVerify { repo.activateClub() }
        }

    @Test
    fun `markConfettiShown persists the one-shot flag`() =
        runTest {
            val repo = session()
            val vm = MembershipViewModel(repo)
            advanceUntilIdle()

            vm.markConfettiShown()
            advanceUntilIdle()

            coVerify { repo.markClubConfettiShown() }
        }
}
