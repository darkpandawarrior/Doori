package com.mileway.feature.whatsnew.viewmodel

import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.data.FakeOpOutbox
import com.mileway.feature.whatsnew.data.WhatsNewEngagementRecorder
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V36 P4/P7: entry lookup by id, the unknown-id error state, [WhatsNewDetailViewModel.selectMedia]
 * clamping, and the once-per-visit `whatsnew_opened` engagement record (spec §8).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsNewDetailViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val releasedOn = LocalDate(2026, 7, 16)

    private fun entryWithMedia(mediaCount: Int) =
        WhatsNewEntry(
            id = "known",
            version = 1,
            title = "Known entry",
            description = "desc",
            media = List(mediaCount) { WhatsNewMedia(path = "files/whatsnew/known/0$it.png") },
            releasedOn = releasedOn,
        )

    private fun vm(
        entryId: String,
        repository: BundledWhatsNewRepository,
        outbox: FakeOpOutbox = FakeOpOutbox(),
    ) = WhatsNewDetailViewModel(entryId = entryId, repository = repository, engagement = WhatsNewEngagementRecorder(outbox))

    @Test
    fun `a known id resolves the matching entry`() =
        runTest {
            val entry = entryWithMedia(mediaCount = 0)
            val repository = BundledWhatsNewRepository(listOf(entry))

            val viewModel = vm("known", repository)

            assertEquals(entry, viewModel.uiState.value.entry)
            assertFalse(viewModel.uiState.value.error)
        }

    @Test
    fun `an unknown id surfaces the error state with no entry`() =
        runTest {
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))

            val viewModel = vm("does-not-exist", repository)

            assertNull(viewModel.uiState.value.entry)
            assertTrue(viewModel.uiState.value.error)
        }

    @Test
    fun `selectMedia clamps below zero to the first index`() =
        runTest {
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
            val viewModel = vm("known", repository)

            viewModel.selectMedia(-5)

            assertEquals(0, viewModel.uiState.value.selectedMediaIndex)
        }

    @Test
    fun `selectMedia clamps above the last index to lastIndex`() =
        runTest {
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
            val viewModel = vm("known", repository)

            viewModel.selectMedia(99)

            assertEquals(2, viewModel.uiState.value.selectedMediaIndex)
        }

    @Test
    fun `selectMedia within range is applied as-is`() =
        runTest {
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
            val viewModel = vm("known", repository)

            viewModel.selectMedia(1)

            assertEquals(1, viewModel.uiState.value.selectedMediaIndex)
        }

    @Test
    fun `selectMedia is a no-op when the entry has no media`() =
        runTest {
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))
            val viewModel = vm("known", repository)

            viewModel.selectMedia(2)

            assertEquals(0, viewModel.uiState.value.selectedMediaIndex)
        }

    @Test
    fun `opening a known entry records whatsnew_opened exactly once`() =
        runTest {
            val outbox = FakeOpOutbox()
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))

            vm("known", repository, outbox)
            advanceUntilIdle()

            val op = outbox.enqueued.single()
            assertEquals(WhatsNewEngagementRecorder.TYPE_OPENED, op.type)
            assertTrue("\"entryId\":\"known\"" in op.payload)
        }

    @Test
    fun `opening an unknown entry records nothing`() =
        runTest {
            val outbox = FakeOpOutbox()
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))

            vm("does-not-exist", repository, outbox)
            advanceUntilIdle()

            assertTrue(outbox.enqueued.isEmpty())
        }

    @Test
    fun `recordShared and recordContact enqueue against the resolved entry`() =
        runTest {
            val outbox = FakeOpOutbox()
            val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))
            val viewModel = vm("known", repository, outbox)

            viewModel.recordShared()
            viewModel.recordContact()
            advanceUntilIdle()

            // [0] is the opened event recorded in init.
            assertEquals(
                listOf(WhatsNewEngagementRecorder.TYPE_OPENED, WhatsNewEngagementRecorder.TYPE_SHARED, WhatsNewEngagementRecorder.TYPE_CONTACT),
                outbox.enqueued.map { it.type },
            )
        }
}
