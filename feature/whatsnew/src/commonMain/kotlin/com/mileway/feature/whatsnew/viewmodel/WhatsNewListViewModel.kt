package com.mileway.feature.whatsnew.viewmodel

import androidx.lifecycle.ViewModel
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** PLAN_V36 P3: an entry still carries the NEW badge for this many days after [WhatsNewEntry.releasedOn]. */
private const val NEW_WINDOW_DAYS = 7

data class WhatsNewListUiState(
    val entries: List<WhatsNewEntry> = emptyList(),
    val newEntryIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * PLAN_V36 P3 — the full What's New list (the digest sheet only shows the top 3; see
 * `:shared`'s `WhatsNewViewModel`). [repository.entries] is already sorted newest-first.
 *
 * [clock] is constructor-injected (default [Clock.System]) so the 7-day NEW window is testable
 * with a fixed instant — the catalog itself stays clock-free (determinism rule in
 * `WhatsNewCatalog`'s KDoc); only this runtime "is it still new" comparison reads the clock.
 */
class WhatsNewListViewModel(
    repository: WhatsNewRepository,
    clock: Clock = Clock.System,
) : ViewModel() {
    val uiState: StateFlow<WhatsNewListUiState>

    init {
        val entries = repository.entries()
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val newIds = entries.filter { it.releasedOn.isWithinNewWindow(today) }.mapTo(mutableSetOf()) { it.id }
        uiState = MutableStateFlow(WhatsNewListUiState(entries = entries, newEntryIds = newIds)).asStateFlow()
    }
}

/** True when [this] is on or before [today] but no more than [NEW_WINDOW_DAYS] days earlier. */
internal fun LocalDate.isWithinNewWindow(today: LocalDate): Boolean {
    val daysSinceRelease = today.toEpochDays() - this.toEpochDays()
    return daysSinceRelease in 0..NEW_WINDOW_DAYS
}
