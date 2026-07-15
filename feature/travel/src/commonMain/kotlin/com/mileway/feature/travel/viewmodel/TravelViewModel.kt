package com.mileway.feature.travel.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.travel.repository.TravelRepository
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.mvi.BaseViewModel

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
                emitEffect(TravelEffect.ShowMessage(UiText.Dynamic("Boarding pass not available in demo")))
            TravelAction.BookFlight, TravelAction.BookTrain ->
                emitEffect(TravelEffect.ShowMessage(UiText.Dynamic("Booking is illustrative.")))
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
