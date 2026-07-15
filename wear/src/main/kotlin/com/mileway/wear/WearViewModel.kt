package com.mileway.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.watch.WatchFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * P2.4: the Wear app's single-activity ViewModel (biciradar pattern — one `ViewModel` per
 * `ComponentActivity`, no MVI [com.siddharth.kmp.mvi.BaseViewModel] here since `core:ui` is the
 * phone/iOS Compose Multiplatform theming module `:wear` must never depend on — see
 * [WearAppGraph]'s doc comment). Collects [WatchFacade.observeSnapshot] and maps it through
 * [WearPresentation] into [WearRootUiState] for [WearRootScreen].
 *
 * P2.5 combines the snapshot stream with [WatchFacade.recentTrips] and layers [WearScreen]/
 * trip-selection navigation state ([openTripList], [openTripDetail], [onBack]) on top — still one
 * `StateFlow`, still the single-activity `when` pattern from P2.4's doc comment.
 *
 * P2.8 adds [ongoingActivityState]: a second, independent `StateFlow` mapped from
 * [TrackingServiceApi.trackingState] via [WearPresentation.toOngoingActivityState] — kept separate
 * from [uiState] (rather than folded into the same `combine`) since it drives a side effect
 * ([WearActivity] posting/cancelling [TrackingOngoingActivity]), not composable rendering.
 */
class WearViewModel(
    watchFacade: WatchFacade,
    trackingServiceApi: TrackingServiceApi,
) : ViewModel() {
    private val navigation = MutableStateFlow(NavigationState())

    val uiState: StateFlow<WearRootUiState> =
        combine(
            watchFacade.observeSnapshot(),
            watchFacade.recentTrips(TRIP_LIST_LIMIT),
            navigation,
        ) { snapshot, trips, nav ->
            WearPresentation.toUiState(snapshot).copy(
                trips = WearPresentation.toTripListItems(trips),
                screen = nav.screen,
                selectedTripId = nav.selectedTripId,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = WearRootUiState(),
        )

    /** P2.8: drives [TrackingOngoingActivity] start/stop — see the class doc for why it's separate. */
    val ongoingActivityState: StateFlow<OngoingActivityUi> =
        trackingServiceApi.trackingState
            .map(WearPresentation::toOngoingActivityState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = OngoingActivityUi(),
            )

    /** Dashboard → trip list. */
    fun openTripList() {
        navigation.value = NavigationState(screen = WearScreen.TripList)
    }

    /** Trip list → trip detail for [tripId]. */
    fun openTripDetail(tripId: String) {
        navigation.value = NavigationState(screen = WearScreen.TripDetail, selectedTripId = tripId)
    }

    /** Detail → list, or list → dashboard — one step back through [WearScreen], mirroring system back. */
    fun onBack() {
        navigation.value =
            when (navigation.value.screen) {
                WearScreen.Dashboard -> NavigationState()
                WearScreen.TripList -> NavigationState()
                WearScreen.TripDetail -> NavigationState(screen = WearScreen.TripList)
            }
    }

    private data class NavigationState(
        val screen: WearScreen = WearScreen.Dashboard,
        val selectedTripId: String? = null,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val TRIP_LIST_LIMIT = 20
    }
}
