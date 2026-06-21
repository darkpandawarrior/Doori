package com.miletracker.feature.travel.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.repository.TravelSubmissionResult
import com.miletracker.feature.travel.repository.TripDraft

data class CreateTripUiState(
    val purpose: String = "",
    val fromCity: String = "",
    val toCity: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val advanceRequired: Boolean = false,
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    /** Submit is gated on a purpose and both cities (TR.2 validation). */
    val canSubmit: Boolean
        get() = purpose.isNotBlank() && fromCity.isNotBlank() && toCity.isNotBlank()
}

sealed interface CreateTripAction {
    data class SetPurpose(val value: String) : CreateTripAction

    data class SetFromCity(val value: String) : CreateTripAction

    data class SetToCity(val value: String) : CreateTripAction

    data class SetStartDate(val value: String) : CreateTripAction

    data class SetEndDate(val value: String) : CreateTripAction

    data class SetAdvanceRequired(val value: Boolean) : CreateTripAction

    data object Submit : CreateTripAction
}

/**
 * TR.2 — Create-Trip reducer. Drives the shared `FormSubmissionScaffold`: field setters,
 * [CreateTripUiState.canSubmit] gating, and a submit that runs the rotating-status fake and emits the shared
 * [TravelCreateEffect].
 */
class CreateTripViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateTripUiState, TravelCreateEffect, CreateTripAction>(CreateTripUiState()) {
    override fun onAction(action: CreateTripAction) {
        when (action) {
            is CreateTripAction.SetPurpose -> setState { copy(purpose = action.value) }
            is CreateTripAction.SetFromCity -> setState { copy(fromCity = action.value) }
            is CreateTripAction.SetToCity -> setState { copy(toCity = action.value) }
            is CreateTripAction.SetStartDate -> setState { copy(startDate = action.value) }
            is CreateTripAction.SetEndDate -> setState { copy(endDate = action.value) }
            is CreateTripAction.SetAdvanceRequired -> setState { copy(advanceRequired = action.value) }
            CreateTripAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitTrip(
                TripDraft(
                    purpose = s.purpose.trim(),
                    fromCity = s.fromCity.trim(),
                    toCity = s.toCity.trim(),
                    startDate = s.startDate.trim(),
                    endDate = s.endDate.trim(),
                    advanceRequired = s.advanceRequired,
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
