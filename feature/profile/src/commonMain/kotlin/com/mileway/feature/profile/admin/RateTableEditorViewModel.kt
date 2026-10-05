package com.mileway.feature.profile.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.domain.policy.AnnualDistanceStepDown
import com.mileway.core.data.domain.policy.MileageRateVersion
import com.mileway.core.platform.ShareSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RateTableKind { POLICY, IRS, HMRC, PER_DIEM }

data class RateVersionDraft(
    val kind: RateTableKind = RateTableKind.POLICY,
    val effectiveFrom: String = "",
    val rate: String = "",
    val vehicle: String = "",
    val region: String = "",
    val grade: String = "",
    val currency: String = "INR",
    val bandDistance: String = "",
    val aboveBandRate: String = "",
    val note: String = "",
)

/** Validates input and appends local versions. No role authorization exists in the mock session. */
class RateTableEditorViewModel(
    private val store: RateTableStore,
    private val shareSheet: ShareSheet,
) : ViewModel() {
    data class State(
        val tables: RateTables? = null,
        val draft: RateVersionDraft = RateVersionDraft(),
        val busy: Boolean = false,
        val error: String? = null,
        val message: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun edit(draft: RateVersionDraft) {
        if (!state.value.busy) mutableState.update { it.copy(draft = draft, error = null, message = null) }
    }

    fun load() = perform { mutableState.update { it.copy(tables = store.read()) } }

    fun save() {
        val draft = state.value.draft
        if (state.value.tables == null) return
        perform {
            when (draft.kind) {
                RateTableKind.POLICY -> store.addPolicyVersion(draft.effectiveFrom, draft.vehicle, decimalRate(draft.rate))
                RateTableKind.PER_DIEM ->
                    store.addPerDiemVersion(
                        LocalPerDiemRateVersion(
                            draft.region.trim(),
                            draft.grade.trim(),
                            draft.currency.trim().uppercase(),
                            draft.effectiveFrom,
                            decimalRate(draft.rate),
                        ),
                    )
                RateTableKind.IRS, RateTableKind.HMRC -> addMirror(draft)
            }
            mutableState.update { it.copy(tables = store.read(), message = "Dated version saved locally") }
        }
    }

    private suspend fun addMirror(draft: RateVersionDraft) {
        val mirror = requireNotNull(state.value.tables).mileage.mirrors.getValue(draft.kind.name)
        require(draft.note.isNotBlank()) { "Explain the local employer rate" }
        val band = draft.bandDistance.takeIf { it.isNotBlank() }?.let { requireNotNull(it.toLongOrNull()) { "Enter a whole band distance" } }
        val above = draft.aboveBandRate.takeIf { it.isNotBlank() }?.let { decimalRate(it, places = 5) }
        store.addMirrorVersion(
            draft.kind.name,
            MileageRateVersion(
                draft.effectiveFrom,
                AnnualDistanceStepDown(
                    decimalRate(draft.rate, places = 5),
                    mirror.versions
                        .first()
                        .schedule.distanceUnit,
                    band,
                    above,
                ),
                "Local employer rate: ${draft.note.trim()}",
            ),
        )
    }

    fun export() =
        perform {
            shareSheet.share(text = store.exportJson(), subject = "Local dated rate versions")
            mutableState.update { it.copy(message = "Rate JSON sent to share sheet") }
        }

    // Boundary failures include DataStore IO and platform sharing. Cancellation must propagate.
    @Suppress("TooGenericExceptionCaught")
    private fun perform(operation: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                operation()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(error = failure.message ?: "Unable to update local rates") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
