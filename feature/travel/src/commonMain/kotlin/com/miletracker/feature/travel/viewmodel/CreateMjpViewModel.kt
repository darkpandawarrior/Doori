package com.miletracker.feature.travel.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.travel.repository.MjpDraft
import com.miletracker.feature.travel.repository.MjpLeg
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.repository.TravelSubmissionResult

/** A single editable leg row in the MJP form (TR.6). */
data class MjpLegInput(
    val fromCity: String = "",
    val toCity: String = "",
    val travelDate: String = "",
) {
    val isComplete: Boolean get() = fromCity.isNotBlank() && toCity.isNotBlank() && travelDate.isNotBlank()
}

data class CreateMjpUiState(
    val purpose: String = "",
    val legs: List<MjpLegInput> = listOf(MjpLegInput()),
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    /** Submit is gated on a purpose and at least one fully-completed leg (TR.6 validation). */
    val canSubmit: Boolean
        get() = purpose.isNotBlank() && legs.isNotEmpty() && legs.all { it.isComplete }
}

sealed interface CreateMjpAction {
    data class SetPurpose(val value: String) : CreateMjpAction

    data object AddLeg : CreateMjpAction

    data class RemoveLeg(val index: Int) : CreateMjpAction

    data class SetLegFrom(val index: Int, val value: String) : CreateMjpAction

    data class SetLegTo(val index: Int, val value: String) : CreateMjpAction

    data class SetLegDate(val index: Int, val value: String) : CreateMjpAction

    data object Submit : CreateMjpAction
}

/** TR.6 — multi-city Journey-Plan reducer on the shared `FormSubmissionScaffold` + [TravelCreateEffect]. */
class CreateMjpViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateMjpUiState, TravelCreateEffect, CreateMjpAction>(CreateMjpUiState()) {
    override fun onAction(action: CreateMjpAction) {
        when (action) {
            is CreateMjpAction.SetPurpose -> setState { copy(purpose = action.value) }
            CreateMjpAction.AddLeg -> setState { copy(legs = legs + MjpLegInput()) }
            is CreateMjpAction.RemoveLeg ->
                setState { copy(legs = if (legs.size <= 1) legs else legs.filterIndexed { i, _ -> i != action.index }) }
            is CreateMjpAction.SetLegFrom -> updateLeg(action.index) { it.copy(fromCity = action.value) }
            is CreateMjpAction.SetLegTo -> updateLeg(action.index) { it.copy(toCity = action.value) }
            is CreateMjpAction.SetLegDate -> updateLeg(action.index) { it.copy(travelDate = action.value) }
            CreateMjpAction.Submit -> submit()
        }
    }

    private fun updateLeg(
        index: Int,
        transform: (MjpLegInput) -> MjpLegInput,
    ) {
        setState { copy(legs = legs.mapIndexed { i, leg -> if (i == index) transform(leg) else leg }) }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitMjp(
                MjpDraft(
                    purpose = s.purpose.trim(),
                    legs = s.legs.map { MjpLeg(it.fromCity.trim(), it.toCity.trim(), it.travelDate.trim()) },
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
