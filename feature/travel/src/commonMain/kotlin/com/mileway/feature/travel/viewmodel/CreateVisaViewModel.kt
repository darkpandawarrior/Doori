package com.mileway.feature.travel.viewmodel

import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.travel.repository.TravelCreateRepository
import com.mileway.feature.travel.repository.TravelSubmissionResult
import com.mileway.feature.travel.repository.VisaDraft

data class CreateVisaUiState(
    val country: String = "",
    val travelDate: String = "",
    val passportNumber: String = "",
    val visaType: String = "Business",
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    /** Submit is gated on a country, a passport number, and a travel date (TR.7 validation). */
    val canSubmit: Boolean
        get() = country.isNotBlank() && passportNumber.isNotBlank() && travelDate.isNotBlank()
}

sealed interface CreateVisaAction {
    data class SetCountry(val value: String) : CreateVisaAction

    data class SetTravelDate(val value: String) : CreateVisaAction

    data class SetPassportNumber(val value: String) : CreateVisaAction

    data class SetVisaType(val value: String) : CreateVisaAction

    data object Submit : CreateVisaAction
}

/** TR.7: Visa-request reducer on the shared `FormSubmissionScaffold` + [TravelCreateEffect]. */
class CreateVisaViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateVisaUiState, TravelCreateEffect, CreateVisaAction>(CreateVisaUiState()) {
    override fun onAction(action: CreateVisaAction) {
        when (action) {
            is CreateVisaAction.SetCountry -> setState { copy(country = action.value) }
            is CreateVisaAction.SetTravelDate -> setState { copy(travelDate = action.value) }
            is CreateVisaAction.SetPassportNumber -> setState { copy(passportNumber = action.value) }
            is CreateVisaAction.SetVisaType -> setState { copy(visaType = action.value) }
            CreateVisaAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitVisa(
                VisaDraft(
                    country = s.country.trim(),
                    travelDate = s.travelDate.trim(),
                    passportNumber = s.passportNumber.trim(),
                    visaType = s.visaType,
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
