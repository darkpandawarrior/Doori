package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.feature.whatsnew.data.WhatsNewEngagementRecorder
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.ui.home.WhatsNewViewModel
import com.siddharth.kmp.offlineoutbox.OpOutbox
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P2.2 / PLAN_V36 P2 — the what's-new sheet shows only while the persisted last-seen
 * version is behind the current release, and acknowledging advances it. The "current release"
 * now comes from a [WhatsNewRepository] (bundled catalog seam) instead of a hand-bumped constant.
 */
class WhatsNewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val whatsNewRepository: WhatsNewRepository =
        FakeWhatsNewRepository(
            currentVersion = 3,
            list =
                listOf(
                    WhatsNewEntry(
                        id = "entry-1",
                        version = 3,
                        title = "Title",
                        description = "Description",
                        releasedOn = LocalDate(2026, 7, 8),
                    ),
                ),
        )

    // PLAN_V36 P7: a relaxed OpOutbox mock is enough here — these tests exercise badge/seen
    // logic, not engagement capture (see feature:whatsnew's WhatsNewEngagementRecorderTest for that).
    private val engagementRecorder = WhatsNewEngagementRecorder(mockk<OpOutbox>(relaxed = true))

    private fun repo(lastSeen: Int): SessionRepository =
        mockk(relaxed = true) {
            every { sessionState } returns MutableStateFlow(SessionState(whatsNewLastSeenVersion = lastSeen))
        }

    @Test
    fun `visible when last-seen version is behind the current release`() =
        runTest {
            val vm = WhatsNewViewModel(repo(lastSeen = 0), whatsNewRepository, engagementRecorder)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isVisible)
            assertTrue(vm.uiState.value.entries.isNotEmpty())
        }

    @Test
    fun `hidden once the current version has been seen`() =
        runTest {
            val vm = WhatsNewViewModel(repo(lastSeen = whatsNewRepository.currentVersion), whatsNewRepository, engagementRecorder)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isVisible)
        }

    @Test
    fun `acknowledge advances the stored version`() =
        runTest {
            val session = repo(lastSeen = 0)
            val vm = WhatsNewViewModel(session, whatsNewRepository, engagementRecorder)
            vm.acknowledge()
            advanceUntilIdle()
            coVerify { session.markWhatsNewSeen(whatsNewRepository.currentVersion) }
        }
}

private class FakeWhatsNewRepository(
    override val currentVersion: Int,
    private val list: List<WhatsNewEntry>,
) : WhatsNewRepository {
    override fun entries(): List<WhatsNewEntry> = list

    override fun entry(id: String): WhatsNewEntry? = list.firstOrNull { it.id == id }
}
