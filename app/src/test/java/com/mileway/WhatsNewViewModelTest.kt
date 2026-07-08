package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.ui.home.CURRENT_WHATS_NEW_VERSION
import com.mileway.ui.home.WhatsNewViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P2.2 — the what's-new sheet shows only while the persisted last-seen version is behind
 * the current release, and acknowledging advances it.
 */
class WhatsNewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun repo(lastSeen: Int): SessionRepository =
        mockk(relaxed = true) {
            every { sessionState } returns MutableStateFlow(SessionState(whatsNewLastSeenVersion = lastSeen))
        }

    @Test
    fun `visible when last-seen version is behind the current release`() =
        runTest {
            val vm = WhatsNewViewModel(repo(lastSeen = 0))
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isVisible)
            assertTrue(vm.uiState.value.items.isNotEmpty())
        }

    @Test
    fun `hidden once the current version has been seen`() =
        runTest {
            val vm = WhatsNewViewModel(repo(lastSeen = CURRENT_WHATS_NEW_VERSION))
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isVisible)
        }

    @Test
    fun `acknowledge advances the stored version`() =
        runTest {
            val session = repo(lastSeen = 0)
            val vm = WhatsNewViewModel(session)
            vm.acknowledge()
            advanceUntilIdle()
            coVerify { session.markWhatsNewSeen(CURRENT_WHATS_NEW_VERSION) }
        }
}
