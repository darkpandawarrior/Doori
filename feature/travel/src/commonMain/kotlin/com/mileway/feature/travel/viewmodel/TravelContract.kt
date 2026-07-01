package com.mileway.feature.travel.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.travel.model.BookingRecord

/** Assembled travel-home payload: active trip, upcoming bookings, and total travel spend. */
data class TravelData(
    val activeBooking: BookingRecord?,
    val upcoming: List<BookingRecord>,
    val totalSpend: Double,
)

data class TravelUiState(
    val content: ScreenState<TravelData> = ScreenState.Loading,
)

sealed interface TravelAction {
    data object Refresh : TravelAction

    data object ViewBoardingPass : TravelAction

    data object BookFlight : TravelAction

    data object BookTrain : TravelAction
}

sealed interface TravelEffect {
    data class ShowMessage(val message: UiText) : TravelEffect
}
