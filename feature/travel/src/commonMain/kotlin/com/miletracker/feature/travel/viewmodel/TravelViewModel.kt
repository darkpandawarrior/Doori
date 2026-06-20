package com.miletracker.feature.travel.viewmodel

import com.miletracker.core.common.UiText
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.travel.repository.TravelRepository

class TravelViewModel(
    private val repository: TravelRepository,
) : BaseViewModel<TravelUiState, TravelEffect, TravelAction>(TravelUiState()) {
    init {
        load()
    }

    override fun onAction(action: TravelAction) {
        when (action) {
            TravelAction.Refresh -> load()
            TravelAction.ViewBoardingPass ->
                emitEffect(TravelEffect.ShowMessage(UiText.Static("Boarding pass not available in demo")))
            TravelAction.BookFlight, TravelAction.BookTrain ->
                emitEffect(TravelEffect.ShowMessage(UiText.Static("Booking is illustrative.")))
        }
    }

    private fun load() {
        val data =
            TravelData(
                activeBooking = repository.activeBooking(),
                upcoming = repository.upcomingBookings(),
                totalSpend = repository.totalSpend(),
            )
        setState { copy(content = ScreenState.Content(data)) }
    }
}
