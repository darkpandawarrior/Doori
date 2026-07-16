package com.mileway.feature.whatsnew.viewmodel

import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** PLAN_V36 P4: entry lookup by id, the unknown-id error state, and [WhatsNewDetailViewModel.selectMedia] clamping. */
class WhatsNewDetailViewModelTest {
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

    @Test
    fun `a known id resolves the matching entry`() {
        val entry = entryWithMedia(mediaCount = 0)
        val repository = BundledWhatsNewRepository(listOf(entry))

        val vm = WhatsNewDetailViewModel(entryId = "known", repository = repository)

        assertEquals(entry, vm.uiState.value.entry)
        assertFalse(vm.uiState.value.error)
    }

    @Test
    fun `an unknown id surfaces the error state with no entry`() {
        val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))

        val vm = WhatsNewDetailViewModel(entryId = "does-not-exist", repository = repository)

        assertNull(vm.uiState.value.entry)
        assertTrue(vm.uiState.value.error)
    }

    @Test
    fun `selectMedia clamps below zero to the first index`() {
        val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
        val vm = WhatsNewDetailViewModel(entryId = "known", repository = repository)

        vm.selectMedia(-5)

        assertEquals(0, vm.uiState.value.selectedMediaIndex)
    }

    @Test
    fun `selectMedia clamps above the last index to lastIndex`() {
        val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
        val vm = WhatsNewDetailViewModel(entryId = "known", repository = repository)

        vm.selectMedia(99)

        assertEquals(2, vm.uiState.value.selectedMediaIndex)
    }

    @Test
    fun `selectMedia within range is applied as-is`() {
        val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 3)))
        val vm = WhatsNewDetailViewModel(entryId = "known", repository = repository)

        vm.selectMedia(1)

        assertEquals(1, vm.uiState.value.selectedMediaIndex)
    }

    @Test
    fun `selectMedia is a no-op when the entry has no media`() {
        val repository = BundledWhatsNewRepository(listOf(entryWithMedia(mediaCount = 0)))
        val vm = WhatsNewDetailViewModel(entryId = "known", repository = repository)

        vm.selectMedia(2)

        assertEquals(0, vm.uiState.value.selectedMediaIndex)
    }
}
