package com.mileway.feature.whatsnew.viewmodel

import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** A [Clock] pinned to the start of [date] (in the system-default zone) — deterministic "today". */
private class FixedClock(date: LocalDate) : Clock {
    private val instant = date.atStartOfDayIn(TimeZone.currentSystemDefault())

    override fun now(): Instant = instant
}

/** PLAN_V36 P3: [WhatsNewListViewModel]'s sort (delegated to the repository) + 7-day NEW window. */
class WhatsNewListViewModelTest {
    private val today = LocalDate(2026, 7, 16)

    private fun entry(
        id: String,
        releasedOn: LocalDate,
        version: Int = 1,
    ) = WhatsNewEntry(id = id, version = version, title = id, description = "desc", releasedOn = releasedOn)

    @Test
    fun `entries are exposed newest-first, matching the repository`() {
        val oldest = entry("a", LocalDate(2026, 1, 1), version = 1)
        val newest = entry("b", LocalDate(2026, 7, 1), version = 2)
        val repository = BundledWhatsNewRepository(listOf(oldest, newest))

        val vm = WhatsNewListViewModel(repository, FixedClock(today))

        assertEquals(listOf(newest, oldest), vm.uiState.value.entries)
    }

    @Test
    fun `an entry released today is NEW`() {
        val repository = BundledWhatsNewRepository(listOf(entry("today", today)))
        val vm = WhatsNewListViewModel(repository, FixedClock(today))

        assertTrue("today" in vm.uiState.value.newEntryIds)
    }

    @Test
    fun `an entry released exactly 7 days ago is still NEW (inclusive boundary)`() {
        val sevenDaysAgo = LocalDate(2026, 7, 9)
        val repository = BundledWhatsNewRepository(listOf(entry("boundary", sevenDaysAgo)))
        val vm = WhatsNewListViewModel(repository, FixedClock(today))

        assertTrue("boundary" in vm.uiState.value.newEntryIds)
    }

    @Test
    fun `an entry released 8 days ago is no longer NEW`() {
        val eightDaysAgo = LocalDate(2026, 7, 8)
        val repository = BundledWhatsNewRepository(listOf(entry("stale", eightDaysAgo)))
        val vm = WhatsNewListViewModel(repository, FixedClock(today))

        assertFalse("stale" in vm.uiState.value.newEntryIds)
    }

    // ponytail: no "empty catalog" case here — BundledWhatsNewRepository.currentVersion is
    // maxOf { version } over the catalog, which throws on an empty list (and WhatsNewCatalogTest
    // already asserts the real catalog is never empty), so an empty-repository fixture would be
    // testing a state the repository itself can't produce.
    @Test
    fun `isEmpty is false whenever the repository has entries`() {
        val repository = BundledWhatsNewRepository(listOf(entry("only", today)))
        val vm = WhatsNewListViewModel(repository, FixedClock(today))

        assertFalse(vm.uiState.value.isEmpty)
    }
}
