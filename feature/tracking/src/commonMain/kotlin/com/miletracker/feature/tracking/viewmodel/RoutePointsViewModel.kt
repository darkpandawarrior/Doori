package com.miletracker.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.paging.LocationPagingSource
import com.miletracker.feature.tracking.repository.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Flat, immutable row model for one GPS fix in the route-points log (keeps the composable stable). */
data class RoutePointUi(
    val id: Long,
    val timeMillis: Long,
    val lat: Double,
    val lng: Double,
    val speedKmh: Double,
    val accuracyM: Float,
    val provider: String,
    val isCheckIn: Boolean,
    val isAbnormal: Boolean,
    val isPaused: Boolean,
)

private fun LocationData.toRoutePointUi(): RoutePointUi =
    RoutePointUi(
        id = id,
        timeMillis = if (date > 0) date else locationTime,
        lat = lat,
        lng = lng,
        speedKmh = (speed * 3.6f).toDouble(),
        accuracyM = accuracy,
        provider = provider,
        isCheckIn = wasCheckInPoint,
        isAbnormal = isAbnormal,
        isPaused = isPaused,
    )

data class RoutePointsUiState(
    val routeId: String? = null,
    /** Total fix count for the header; null until the COUNT query returns. */
    val totalPoints: Int? = null,
)

sealed interface RoutePointsEffect

sealed interface RoutePointsAction {
    data class Load(val routeId: String) : RoutePointsAction
}

/**
 * G1 (Paging 3): backs the route-points log. The paged stream is exposed as [points] (a
 * `Flow<PagingData>` collected via `collectAsLazyPagingItems()` on the screen), while the cheap total
 * count rides in the MVI [state] for the header — the standard "list paged, aggregates via COUNT"
 * split so we never need the whole dataset in memory.
 */
class RoutePointsViewModel(
    private val locationRepository: LocationRepository,
) : BaseViewModel<RoutePointsUiState, RoutePointsEffect, RoutePointsAction>(RoutePointsUiState()) {
    private val tokenFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val points: Flow<PagingData<RoutePointUi>> =
        tokenFlow
            .filterNotNull()
            .flatMapLatest { token ->
                Pager(
                    config =
                        PagingConfig(
                            pageSize = PAGE_SIZE,
                            initialLoadSize = PAGE_SIZE * 2,
                            prefetchDistance = PAGE_SIZE / 2,
                            enablePlaceholders = false,
                        ),
                    pagingSourceFactory = { LocationPagingSource(token, locationRepository) },
                ).flow.map { data -> data.map { it.toRoutePointUi() } }
            }
            .cachedIn(viewModelScope)

    override fun onAction(action: RoutePointsAction) {
        when (action) {
            is RoutePointsAction.Load -> load(action.routeId)
        }
    }

    private fun load(routeId: String) {
        if (currentState.routeId == routeId && currentState.totalPoints != null) return
        setState { copy(routeId = routeId) }
        tokenFlow.value = routeId
        viewModelScope.launch {
            val count = runCatching { locationRepository.countForToken(routeId) }.getOrDefault(0)
            setState { copy(totalPoints = count) }
        }
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
