package com.mileway.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.watch.TrackingCommandSender
import kotlinx.coroutines.launch
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
    /**
     * Null on noGms, where there is no Data Layer to carry a command to the phone. Nullable rather
     * than a no-op implementation on purpose: a silent no-op would render a live-looking button
     * that does nothing, which is the failure this whole feature exists to avoid.
     */
    private val commandSender: TrackingCommandSender? = null,
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
                canControlTracking = commandSender != null,
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
    /**
     * Start the trip, or stop the one already running.
     *
     * Stopping reuses [WearRootUiState.activeToken] — the token the phone started with — because
     * `TrackingController.stop` ignores anything else and returns silently. If a trip is live but
     * no token arrived (an old phone build, or a snapshot from before this field existed), this
     * does nothing rather than sending a token that cannot match: failing visibly beats a button
     * that appears to work.
     *
     * Starting mints a token the watch owns. The phone echoes it back in the next snapshot, so the
     * stop path uses the same value even after the watch app is killed and relaunched.
     */
    fun toggleTracking(nowMs: Long) {
        val sender = commandSender ?: return
        val state = uiState.value
        viewModelScope.launch {
            if (state.isTracking) {
                val token = state.activeToken ?: return@launch
                sender.sendStop(token)
            } else {
                sender.sendStart("wear-$nowMs")
            }
        }
    }

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
