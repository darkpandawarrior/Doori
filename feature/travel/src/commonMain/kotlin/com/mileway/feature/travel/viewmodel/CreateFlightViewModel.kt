package com.mileway.feature.travel.viewmodel

import com.mileway.feature.travel.repository.FlightDraft
import com.mileway.feature.travel.repository.TravelCreateRepository
import com.mileway.feature.travel.repository.TravelSubmissionResult
import com.siddharth.kmp.mvi.BaseViewModel

data class CreateFlightUiState(
    val fromCity: String = "",
    val toCity: String = "",
    val travelDate: String = "",
    val preferredAirline: String = "",
    val cabinClass: String = "Economy",
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    /** Submit is gated on both cities and a travel date (TR.3 validation). */
    val canSubmit: Boolean
        get() = fromCity.isNotBlank() && toCity.isNotBlank() && travelDate.isNotBlank()
}

sealed interface CreateFlightAction {
    data class SetFromCity(val value: String) : CreateFlightAction

    data class SetToCity(val value: String) : CreateFlightAction

    data class SetTravelDate(val value: String) : CreateFlightAction

    data class SetPreferredAirline(val value: String) : CreateFlightAction

    data class SetCabinClass(val value: String) : CreateFlightAction

    data object Submit : CreateFlightAction
}

/** TR.3: Add-Flight reducer on the shared `FormSubmissionScaffold` + [TravelCreateEffect]. */
class CreateFlightViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateFlightUiState, TravelCreateEffect, CreateFlightAction>(CreateFlightUiState()) {
    override fun onAction(action: CreateFlightAction) {
        when (action) {
            is CreateFlightAction.SetFromCity -> setState { copy(fromCity = action.value) }
            is CreateFlightAction.SetToCity -> setState { copy(toCity = action.value) }
            is CreateFlightAction.SetTravelDate -> setState { copy(travelDate = action.value) }
            is CreateFlightAction.SetPreferredAirline -> setState { copy(preferredAirline = action.value) }
            is CreateFlightAction.SetCabinClass -> setState { copy(cabinClass = action.value) }
            CreateFlightAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitFlight(
                FlightDraft(
                    fromCity = s.fromCity.trim(),
                    toCity = s.toCity.trim(),
                    travelDate = s.travelDate.trim(),
                    preferredAirline = s.preferredAirline.trim(),
                    cabinClass = s.cabinClass,
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
