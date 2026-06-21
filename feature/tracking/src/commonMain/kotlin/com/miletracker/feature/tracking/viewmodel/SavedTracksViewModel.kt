package com.miletracker.feature.tracking.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.miletracker.core.data.model.display.TrackDisplayData
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Top-level tab of the Saved Tracks screen.
 *
 * - [JOURNEYS] shows every recorded journey card.
 * - [SUBMISSIONS] shows the date-grouped expense submissions derived from submitted tracks.
 */
enum class SavedTracksTab { JOURNEYS, SUBMISSIONS }

/**
 * Quick chip filters for the Journeys tab. They mirror the production "This Week / Kept / All"
 * affordance: see screenshot 16_saved_tracks.
 */
enum class JourneyFilter { THIS_WEEK, KEPT, ALL }

/**
 * Primary chip filters for the Submissions tab ("All / Unclaimed / Filed"), where
 * unclaimed = no voucher created yet and filed = a voucher has been raised.
 */
enum class SubmissionFilter { ALL, UNCLAIMED, FILED }

/**
 * Secondary source chip on the Submissions tab ("All / New Tracker / Other") that narrows
 * submissions by the tracker that produced them.
 */
enum class SubmissionSource { ALL, NEW_TRACKER, OTHER }

/**
 * Flattened, immutable UI model for one submitted-mileage expense card.
 *
 * Everything the [com.miletracker.feature.tracking.ui.components.SubmissionCard] renders is
 * pre-computed here so the composable stays stateless and stable. Derived purely from a
 * submitted [TrackDisplayData]; there is no separate persistence layer in this demo.
 *
 * @property id Stable identity for selection, the underlying track token.
 * @property transId Human-readable expense reference shown as "#<transId>".
 * @property amount Reimbursable amount in the local currency (rendered as "₹<amount>").
 * @property expenseDateMillis Epoch millis the expense is dated to (drives the date grouping).
 * @property attachmentCount Number of attached receipts/photos.
 * @property violationCount Number of policy violations flagged on the submission.
 * @property acknowledged Whether the user has acknowledged the submission.
 * @property isNewTracker True when the submission came from the new in-app tracker.
 * @property voucherCreated Whether a voucher has already been filed for this expense.
 */
data class SubmissionItem(
    val id: String,
    val transId: String,
    val amount: Double,
    val expenseDateMillis: Long,
    val attachmentCount: Int,
    val violationCount: Int,
    val acknowledged: Boolean,
    val isNewTracker: Boolean,
    val voucherCreated: Boolean,
    val approvalStatus: String = "Pending Approval",
    val voucherNumber: String? = null,
) {
    /** Unclaimed = no voucher raised yet; these are the rows eligible for selection. */
    val isUnclaimed: Boolean get() = !voucherCreated
}

/**
 * Single immutable UI state for the Saved Tracks screen (MVI / UDF).
 *
 * The [tracks] list is the source of truth streamed from the repository. Every other list the
 * UI renders, filtered journeys, submissions, selection, is derived from it plus the active
 * filter/search/selection fields, so the screen never holds duplicate mutable state.
 */
@Stable
data class SavedTracksUiState(
    val tracks: List<TrackDisplayData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Tab + per-tab search ----------------------------------------------------
    val tab: SavedTracksTab = SavedTracksTab.JOURNEYS,
    val journeySearch: String = "",
    val submissionSearch: String = "",
    // Chip filters ------------------------------------------------------------
    val journeyFilter: JourneyFilter = JourneyFilter.THIS_WEEK,
    val submissionFilter: SubmissionFilter = SubmissionFilter.ALL,
    val submissionSource: SubmissionSource = SubmissionSource.ALL,
    // Selection (Submissions tab) ---------------------------------------------
    val selectionMode: Boolean = false,
    val selectedSubmissionIds: Set<String> = emptySet(),
    /** One-shot flag set when a voucher is "created"; the screen consumes it to celebrate. */
    val voucherCreatedAck: Boolean = false,
) {
    /** All submissions derived from submitted tracks, newest first. */
    val allSubmissions: List<SubmissionItem> =
        tracks
            .filter { it.isSubmitted }
            .map { it.toSubmissionItem() }
            .sortedByDescending { it.expenseDateMillis }

    /** Submissions remaining after the active primary/source/search filters are applied. */
    val filteredSubmissions: List<SubmissionItem> =
        allSubmissions
            .filter { item ->
                when (submissionFilter) {
                    SubmissionFilter.ALL -> true
                    SubmissionFilter.UNCLAIMED -> item.isUnclaimed
                    SubmissionFilter.FILED -> item.voucherCreated
                }
            }
            .filter { item ->
                when (submissionSource) {
                    SubmissionSource.ALL -> true
                    SubmissionSource.NEW_TRACKER -> item.isNewTracker
                    SubmissionSource.OTHER -> !item.isNewTracker
                }
            }
            .filter { item ->
                submissionSearch.isBlank() ||
                    item.transId.contains(submissionSearch, ignoreCase = true)
            }

    val submissionCount: Int get() = allSubmissions.size
    val unclaimedCount: Int get() = allSubmissions.count { it.isUnclaimed }
    val filedCount: Int get() = allSubmissions.count { it.voucherCreated }

    /** Number of journeys that would show under each chip, used for the chip count labels. */
    val journeyCount: Int get() = tracks.size

    /** Currently selected, still-unclaimed submission ids (the ones a voucher can be raised for). */
    val selectedUnclaimedIds: Set<String>
        get() =
            selectedSubmissionIds.intersect(
                allSubmissions.filter { it.isUnclaimed }.map { it.id }.toSet(),
            )
}

/**
 * Derives a stable [SubmissionItem] from a submitted track. The fields that have no first-class
 * column on [TrackDisplayData] (attachments, violations, source) are produced deterministically
 * from the token so the demo data is stable across recompositions and process restarts.
 */
private fun TrackDisplayData.toSubmissionItem(): SubmissionItem {
    val hash = token.hashCode()
    // Mask the sign off before the modulo so the reference is always a positive 9-digit number,
    // even for the Int.MIN_VALUE hash edge case where abs() would overflow back to negative.
    val reference = "O-INDIAN-${((hash.toLong() and 0x7FFFFFFFL) % 1_000_000_000).toString().padStart(9, '0')}"
    return SubmissionItem(
        id = token,
        transId = reference,
        amount = reimbursableAmount,
        expenseDateMillis = if (submittedAt > 0) submittedAt else endTime,
        attachmentCount = if (hash and 0b1 == 0) 1 else 0,
        violationCount = if (hash % 3 == 0) 1 else 0,
        acknowledged = hash and 0b10 != 0,
        isNewTracker = hash and 0b100 == 0,
        // A voucher is considered filed when the track was submitted longer ago (older submissions
        // have been processed). Keeps a deterministic mix of unclaimed / filed for the demo.
        voucherCreated = hash % 4 == 0,
        approvalStatus =
            when ((hash.toLong() and 0x7FFFFFFFL) % 4) {
                0L -> "Approved"
                1L -> "Rejected"
                2L -> "Reimbursed"
                else -> "Pending Approval"
            },
        voucherNumber = if (hash % 4 == 0) "V-${((hash.toLong() and 0x7FFFFFFFL) % 100_000_000).toString().padStart(8, '0')}" else null,
    )
}

sealed interface SavedTracksAction {
    data class TabSelected(val tab: SavedTracksTab) : SavedTracksAction

    data class JourneySearchChanged(val query: String) : SavedTracksAction

    data class SubmissionSearchChanged(val query: String) : SavedTracksAction

    data class JourneyFilterSelected(val filter: JourneyFilter) : SavedTracksAction

    data class SubmissionFilterSelected(val filter: SubmissionFilter) : SavedTracksAction

    data class SubmissionSourceSelected(val source: SubmissionSource) : SavedTracksAction

    data class SubmissionLongPressed(val id: String) : SavedTracksAction

    data class SubmissionSelectionToggled(val id: String) : SavedTracksAction

    data class SubmissionTapped(val id: String) : SavedTracksAction

    data object ClearSelection : SavedTracksAction

    data object CreateVoucher : SavedTracksAction

    data object VoucherAckConsumed : SavedTracksAction
}

sealed interface SavedTracksEffect

class SavedTracksViewModel(
    private val repository: SavedTrackRepository,
) : BaseViewModel<SavedTracksUiState, SavedTracksEffect, SavedTracksAction>(SavedTracksUiState()) {
    init {
        repository.allTracksFlow()
            .onEach { tracks ->
                setState {
                    val validIds = tracks.filter { it.isSubmitted }.map { it.token }.toSet()
                    val prunedSelection = selectedSubmissionIds.intersect(validIds)
                    copy(
                        tracks = tracks,
                        isLoading = false,
                        error = null,
                        selectedSubmissionIds = prunedSelection,
                        selectionMode = selectionMode && prunedSelection.isNotEmpty(),
                    )
                }
            }
            .catch { e -> setState { copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: SavedTracksAction) {
        when (action) {
            is SavedTracksAction.TabSelected ->
                setState { if (tab == action.tab) this else copy(tab = action.tab, selectionMode = false, selectedSubmissionIds = emptySet()) }
            is SavedTracksAction.JourneySearchChanged ->
                setState { copy(journeySearch = action.query) }
            is SavedTracksAction.SubmissionSearchChanged ->
                setState { copy(submissionSearch = action.query) }
            is SavedTracksAction.JourneyFilterSelected ->
                setState { copy(journeyFilter = action.filter) }
            is SavedTracksAction.SubmissionFilterSelected ->
                setState { copy(submissionFilter = action.filter) }
            is SavedTracksAction.SubmissionSourceSelected ->
                setState { copy(submissionSource = action.source) }
            is SavedTracksAction.SubmissionLongPressed ->
                setState { if (!canSelect(action.id)) this else copy(selectionMode = true, selectedSubmissionIds = selectedSubmissionIds + action.id) }
            is SavedTracksAction.SubmissionSelectionToggled -> {
                if (!currentState.canSelect(action.id)) return
                val newSel =
                    if (action.id in currentState.selectedSubmissionIds) {
                        currentState.selectedSubmissionIds - action.id
                    } else {
                        currentState.selectedSubmissionIds + action.id
                    }
                setState { copy(selectedSubmissionIds = newSel, selectionMode = newSel.isNotEmpty()) }
            }
            is SavedTracksAction.SubmissionTapped ->
                if (currentState.selectionMode) onAction(SavedTracksAction.SubmissionSelectionToggled(action.id))
            SavedTracksAction.ClearSelection ->
                setState { copy(selectionMode = false, selectedSubmissionIds = emptySet()) }
            SavedTracksAction.CreateVoucher ->
                setState { copy(selectionMode = false, selectedSubmissionIds = emptySet(), voucherCreatedAck = true) }
            SavedTracksAction.VoucherAckConsumed ->
                setState { copy(voucherCreatedAck = false) }
        }
    }

    private fun SavedTracksUiState.canSelect(id: String): Boolean = allSubmissions.any { it.id == id && it.isUnclaimed }
}
