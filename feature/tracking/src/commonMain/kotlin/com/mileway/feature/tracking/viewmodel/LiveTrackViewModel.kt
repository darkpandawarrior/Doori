package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.state.UiState
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.siddharth.kmp.mvi.BaseViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class LiveTrackingUiState {
    object Initial : LiveTrackingUiState()

    object Loading : LiveTrackingUiState()

    data class Success(val trackData: CurrentTrackData, val locationPoints: List<LocationData>) : LiveTrackingUiState()

    data class Error(val message: String) : LiveTrackingUiState()
}

data class LiveTrackUiState(
    val liveTrackingState: LiveTrackingUiState = LiveTrackingUiState.Initial,
    val locationPointsState: UiState<List<LocationData>> = UiState.Initial,
    val hardwareEventsState: List<Pair<String, Long>> = emptyList(),
)

sealed interface LiveTrackAction {
    data object Refresh : LiveTrackAction

    data object ClearError : LiveTrackAction
}

sealed interface LiveTrackEffect

class LiveTrackViewModel(
    private val locationRepository: LocationRepository,
    private val currentTrackRepository: CurrentTrackRepository,
) : BaseViewModel<LiveTrackUiState, LiveTrackEffect, LiveTrackAction>(LiveTrackUiState()) {
    companion object {
        private const val TAG = "LiveTrackViewModel"
        private const val AUTO_REFRESH_INTERVAL_MS = 5000L
    }

    private val mutableTrackState = MutableStateFlow<UiState<CurrentTrackData>>(UiState.Initial)
    private val mutableLocState = MutableStateFlow<UiState<List<LocationData>>>(UiState.Initial)

    private var autoRefreshJob: Job? = null
    private var locationObserverJob: Job? = null

    init {
        initializeTrackingData()
        setupCombinedState()
    }

    private fun initializeTrackingData() {
        viewModelScope.launch {
            try {
                loadCurrentTrackData()
            } catch (e: Exception) {
                Napier.e("Error initializing tracking data", e, tag = TAG)
                mutableTrackState.value = UiState.Error("Failed to initialize: ${e.message}")
            }
        }
    }

    override fun onAction(action: LiveTrackAction) {
        when (action) {
            LiveTrackAction.Refresh -> viewModelScope.launch { loadCurrentTrackData() }
            LiveTrackAction.ClearError -> {
                if (mutableTrackState.value is UiState.Error) mutableTrackState.value = UiState.Initial
                if (mutableLocState.value is UiState.Error) mutableLocState.value = UiState.Initial
            }
        }
    }

    private fun setupCombinedState() {
        viewModelScope.launch {
            combine(mutableTrackState, mutableLocState) { ts, ls ->
                when {
                    ts is UiState.Loading || ls is UiState.Loading -> LiveTrackingUiState.Loading
                    ts is UiState.Error -> LiveTrackingUiState.Error(ts.message)
                    ls is UiState.Error -> LiveTrackingUiState.Error(ls.message)
                    ts is UiState.Success && ls is UiState.Success ->
                        LiveTrackingUiState.Success(ts.data, ls.data)
                    ts is UiState.Success -> LiveTrackingUiState.Success(ts.data, emptyList())
                    else -> LiveTrackingUiState.Initial
                }
            }.catch { e ->
                emit(LiveTrackingUiState.Error("Error: ${e.message}"))
            }.collect { live ->
                setState { copy(liveTrackingState = live, locationPointsState = mutableLocState.value) }
            }
        }
    }

    private suspend fun loadCurrentTrackData() {
        mutableTrackState.value = UiState.Loading
        currentTrackRepository.getCurrentTrackDataRawAsync().fold(
            onSuccess = { trackData ->
                mutableTrackState.value = UiState.Success(trackData)
                currentTrackRepository.getHardwareEventQueueSnapshot()
                    .onSuccess { setState { copy(hardwareEventsState = it) } }

                if (trackData.isTracking && trackData.token.isNotEmpty()) {
                    startLocationObservation(trackData.token)
                    startAutoRefresh()
                } else {
                    stopLocationObservation()
                    stopAutoRefresh()
                }
            },
            onFailure = { e ->
                Napier.e("Failed to load track data", e, tag = TAG)
                mutableTrackState.value = UiState.Error("Failed to load: ${e.message}")
            },
        )
    }

    private fun startLocationObservation(token: String) {
        locationObserverJob?.cancel()
        locationObserverJob =
            viewModelScope.launch {
                mutableLocState.value = UiState.Loading
                locationRepository.locationsForToken(token)
                    .catch { e ->
                        mutableLocState.value = UiState.Error("Failed to load locations: ${e.message}")
                    }
                    .collect { locations ->
                        mutableLocState.value = UiState.Success(locations)
                    }
            }
    }

    private fun stopLocationObservation() {
        locationObserverJob?.cancel()
        locationObserverJob = null
        mutableLocState.value = UiState.Success(emptyList())
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob =
            viewModelScope.launch {
                while (true) {
                    delay(AUTO_REFRESH_INTERVAL_MS)
                    val snap = mutableTrackState.value
                    if (snap is UiState.Success && snap.data.isTracking) {
                        refreshCurrentTrackData()
                    } else {
                        break
                    }
                }
            }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    private suspend fun refreshCurrentTrackData() {
        currentTrackRepository.getCurrentTrackDataRawAsync().fold(
            onSuccess = { trackData ->
                val prev = mutableTrackState.value
                mutableTrackState.value = UiState.Success(trackData)
                if (prev is UiState.Success && prev.data.isTracking != trackData.isTracking) {
                    if (trackData.isTracking && trackData.token.isNotEmpty()) {
                        startLocationObservation(trackData.token)
                    } else {
                        stopLocationObservation()
                        stopAutoRefresh()
                    }
                }
                currentTrackRepository.getHardwareEventQueueSnapshot()
                    .onSuccess { setState { copy(hardwareEventsState = it) } }
            },
            onFailure = { Napier.w("Refresh failed", tag = TAG) },
        )
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        locationObserverJob?.cancel()
        super.onCleared()
    }
}
