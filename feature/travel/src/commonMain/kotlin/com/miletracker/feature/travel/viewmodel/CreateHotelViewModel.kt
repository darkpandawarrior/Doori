package com.miletracker.feature.travel.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.travel.repository.HotelDraft
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.repository.TravelSubmissionResult

data class CreateHotelUiState(
    val city: String = "",
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val guestsText: String = "1",
    val roomPreference: String = "Standard",
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    val guests: Int get() = guestsText.toIntOrNull() ?: 0

    /** Submit is gated on a city and both stay dates (TR.5 validation). */
    val canSubmit: Boolean
        get() = city.isNotBlank() && checkInDate.isNotBlank() && checkOutDate.isNotBlank() && guests > 0
}

sealed interface CreateHotelAction {
    data class SetCity(val value: String) : CreateHotelAction

    data class SetCheckInDate(val value: String) : CreateHotelAction

    data class SetCheckOutDate(val value: String) : CreateHotelAction

    data class SetGuests(val value: String) : CreateHotelAction

    data class SetRoomPreference(val value: String) : CreateHotelAction

    data object Submit : CreateHotelAction
}

/** TR.5 — Add-Hotel reducer on the shared `FormSubmissionScaffold` + [TravelCreateEffect]. */
class CreateHotelViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateHotelUiState, TravelCreateEffect, CreateHotelAction>(CreateHotelUiState()) {
    override fun onAction(action: CreateHotelAction) {
        when (action) {
            is CreateHotelAction.SetCity -> setState { copy(city = action.value) }
            is CreateHotelAction.SetCheckInDate -> setState { copy(checkInDate = action.value) }
            is CreateHotelAction.SetCheckOutDate -> setState { copy(checkOutDate = action.value) }
            is CreateHotelAction.SetGuests -> setState { copy(guestsText = action.value) }
            is CreateHotelAction.SetRoomPreference -> setState { copy(roomPreference = action.value) }
            CreateHotelAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitHotel(
                HotelDraft(
                    city = s.city.trim(),
                    checkInDate = s.checkInDate.trim(),
                    checkOutDate = s.checkOutDate.trim(),
                    guests = s.guests,
                    roomPreference = s.roomPreference,
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
